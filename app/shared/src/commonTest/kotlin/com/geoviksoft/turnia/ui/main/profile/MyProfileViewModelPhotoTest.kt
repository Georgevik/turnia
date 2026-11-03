package com.geoviksoft.turnia.ui.main.profile

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageBitmapConfig
import androidx.compose.ui.graphics.colorspace.ColorSpace
import androidx.compose.ui.graphics.colorspace.ColorSpaces
import com.geoviksoft.turnia.core.domain.model.ProfilePhoto
import com.geoviksoft.turnia.core.domain.model.ProfilePhotoError
import com.geoviksoft.turnia.core.domain.repository.UserRepository
import com.geoviksoft.turnia.core.domain.username.UsernameFactory
import com.geoviksoft.turnia.core.system.Outcome
import com.geoviksoft.turnia.core.system.toFailure
import com.geoviksoft.turnia.core.system.toSuccess
import com.geoviksoft.turnia.demo.DemoUserRepository
import com.geoviksoft.turnia.ui.system.photo.PhotoProcessor
import com.geoviksoft.turnia.ui.system.photo.PickedPhoto
import com.geoviksoft.turnia.ui.system.photo.ProfilePhotoCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okio.FileSystem
import kotlin.random.Random
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class MyProfileViewModelPhotoTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private val users = PhotoUserRepository()
    private val directory = FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "profile-vm-test-${Random.nextLong()}"
    private val cache = ProfilePhotoCache(directory, download = { null })

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
        FileSystem.SYSTEM.deleteRecursively(directory)
    }

    private fun viewModel(processor: PhotoProcessor = FakeProcessor()) =
        MyProfileViewModel(users, UsernameFactory(), cache, processor, dispatcher)

    @Test
    fun aPickedPhotoIsCroppedThenUploaded() = runTest(dispatcher) {
        val viewModel = viewModel()

        viewModel.onPhotoPicked(PickedPhoto.Image(byteArrayOf(1)))
        assertIs<PhotoStep.Cropping>(viewModel.uiState.value.photoStep)
        viewModel.onCropped(FakeImage)

        assertEquals(listOf(ENCODED.toList()), users.uploads.map { it.toList() })
        assertEquals(users.uploaded, viewModel.uiState.value.photo)
        assertEquals(PhotoStep.Idle, viewModel.uiState.value.photoStep)
        assertTrue(cache.load(users.uploaded)!!.bytes.contentEquals(ENCODED), "Primed, never downloaded")
    }

    @Test
    fun aFailedUploadKeepsThePhotoAndSaysSo() = runTest(dispatcher) {
        users.uploadResult = ProfilePhotoError.UploadFailed.toFailure()
        val viewModel = viewModel()
        val before = viewModel.uiState.value.photo

        viewModel.onPhotoPicked(PickedPhoto.Image(byteArrayOf(1)))
        viewModel.onCropped(FakeImage)

        assertEquals(before, viewModel.uiState.value.photo)
        assertEquals(ProfileMessage.PhotoUploadFailed, viewModel.uiState.value.userMessage)
        assertEquals(PhotoStep.Idle, viewModel.uiState.value.photoStep)
    }

    @Test
    fun anImageThatCannotBeReadIsNeverCropped() = runTest(dispatcher) {
        val viewModel = viewModel(FakeProcessor(decodes = false))

        viewModel.onPhotoPicked(PickedPhoto.Image(byteArrayOf(1)))

        assertEquals(PhotoStep.Idle, viewModel.uiState.value.photoStep)
        assertEquals(ProfileMessage.PhotoUnreadable, viewModel.uiState.value.userMessage)
    }

    @Test
    fun aPickThatCannotBeReadSaysSo() = runTest(dispatcher) {
        val viewModel = viewModel()

        viewModel.onPhotoPicked(PickedPhoto.Unreadable)

        assertEquals(PhotoStep.Idle, viewModel.uiState.value.photoStep)
        assertEquals(ProfileMessage.PhotoUnreadable, viewModel.uiState.value.userMessage)
    }

    @Test
    fun aCancelledPickOrCropUploadsNothing() = runTest(dispatcher) {
        val viewModel = viewModel()

        viewModel.onPhotoPicked(PickedPhoto.Cancelled)
        assertEquals(PhotoStep.Idle, viewModel.uiState.value.photoStep)
        viewModel.onPhotoPicked(PickedPhoto.Image(byteArrayOf(1)))
        viewModel.onCropCancelled()

        assertEquals(PhotoStep.Idle, viewModel.uiState.value.photoStep)
        assertTrue(users.uploads.isEmpty())
    }

    @Test
    fun removingThePhotoBringsTheAnimalBack() = runTest(dispatcher) {
        val viewModel = viewModel()
        viewModel.onPhotoPicked(PickedPhoto.Image(byteArrayOf(1)))
        viewModel.onCropped(FakeImage)

        viewModel.onRemovePhotoChosen()

        assertNull(viewModel.uiState.value.photo)
        assertEquals(1, users.removals)
    }

    @Test
    fun aFailedRemovalKeepsThePhoto() = runTest(dispatcher) {
        val viewModel = viewModel()
        viewModel.onPhotoPicked(PickedPhoto.Image(byteArrayOf(1)))
        viewModel.onCropped(FakeImage)
        users.removeResult = ProfilePhotoError.RemoveFailed.toFailure()

        viewModel.onRemovePhotoChosen()

        assertEquals(users.uploaded, viewModel.uiState.value.photo)
        assertEquals(ProfileMessage.PhotoRemoveFailed, viewModel.uiState.value.userMessage)
    }

    @Test
    fun anUnsavedNameSurvivesAnUpload() = runTest(dispatcher) {
        val viewModel = viewModel()
        viewModel.onNameChanged("Lucía Nueva")

        viewModel.onPhotoPicked(PickedPhoto.Image(byteArrayOf(1)))
        viewModel.onCropped(FakeImage)

        assertEquals("Lucía Nueva", viewModel.uiState.value.name)
        assertTrue(viewModel.uiState.value.canSave)
    }

    private class PhotoUserRepository(
        private val demo: UserRepository = DemoUserRepository(),
    ) : UserRepository by demo {
        val uploads = mutableListOf<ByteArray>()
        var removals = 0
        val uploaded = ProfilePhoto(demo.loggedUser!!.id, "p1")
        var uploadResult: Outcome<ProfilePhoto, ProfilePhotoError> = uploaded.toSuccess()
        var removeResult: Outcome<Unit, ProfilePhotoError> = Unit.toSuccess()

        override suspend fun uploadProfilePhoto(bytes: ByteArray): Outcome<ProfilePhoto, ProfilePhotoError> {
            uploads += bytes
            return uploadResult
        }

        override suspend fun removeProfilePhoto(): Outcome<Unit, ProfilePhotoError> {
            removals++
            return removeResult
        }
    }

    private class FakeProcessor(private val decodes: Boolean = true) : PhotoProcessor {
        override fun decode(bytes: ByteArray): ImageBitmap? = FakeImage.takeIf { decodes }
        override fun encode(square: ImageBitmap): ByteArray = ENCODED
    }

    /** An image no test ever draws: only its identity travels through the ViewModel. */
    private object FakeImage : ImageBitmap {
        override val width = 1
        override val height = 1
        override val config = ImageBitmapConfig.Argb8888
        override val colorSpace: ColorSpace = ColorSpaces.Srgb
        override val hasAlpha = false
        override fun readPixels(buffer: IntArray, startX: Int, startY: Int, width: Int, height: Int, bufferOffset: Int, stride: Int) = Unit
        override fun prepareToDraw() = Unit
    }

    private companion object {
        val ENCODED = byteArrayOf(9, 9, 9)
    }
}
