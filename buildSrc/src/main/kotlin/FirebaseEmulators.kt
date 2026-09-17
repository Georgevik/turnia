import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import java.io.File
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.TimeUnit

/**
 * Starts the Firebase emulators the E2E suite talks to, unless something already serves them
 * (`firebase emulators:exec` on CI, or a `firebase emulators:start` left open), and stops the ones
 * it started when the build ends. Android Studio runs instrumented tests through Gradle too, so a
 * test launched from the gutter gets them as well.
 */
abstract class FirebaseEmulators : BuildService<FirebaseEmulators.Params>, AutoCloseable {
    interface Params : BuildServiceParameters {
        val firebaseDir: DirectoryProperty
        val firebaseCli: Property<String>
    }

    private val ports = listOf(9099, 8080, 5001)
    private var process: Process? = null

    @Synchronized
    fun ensureRunning() {
        if (process != null || ports.all(::listening)) return
        check(ports.none(::listening)) {
            "Only some Firebase emulator ports ($ports) are in use: stop whatever holds them and run again."
        }
        val dir = parameters.firebaseDir.get().asFile
        val cli = resolveCli()
        // The CLI is a node script: node and npm sit next to it (nvm, Homebrew), and the Gradle
        // daemon started by Android Studio does not inherit the shell's PATH.
        val path = listOf(cli.parent, "${System.getProperty("java.home")}/bin", System.getenv("PATH")).joinToString(":")
        val functions = dir.resolve("functions")
        if (!functions.resolve("node_modules").isDirectory) run(listOf("npm", "ci"), functions, path)
        run(listOf("npm", "run", "build"), functions, path)

        val log = dir.resolve("build/emulators.log").apply { parentFile.mkdirs() }
        val started = ProcessBuilder(
            cli.path, "emulators:start", "--only", "auth,firestore,functions", "--project", "turnia-23ebc",
        ).directory(dir).redirectErrorStream(true).redirectOutput(log)
            .apply { environment()["PATH"] = path }
            .start()
        process = started

        val deadline = System.nanoTime() + TimeUnit.MINUTES.toNanos(3)
        while (!log.readText().contains("All emulators ready")) {
            check(started.isAlive) { "The Firebase emulators did not start; see ${log.path}" }
            check(System.nanoTime() < deadline) { "The Firebase emulators took too long to start; see ${log.path}" }
            Thread.sleep(500)
        }
    }

    override fun close() {
        val started = process ?: return
        // SIGINT, like Ctrl+C: the CLI then shuts down the Java emulators it spawned.
        ProcessBuilder("kill", "-INT", started.pid().toString()).start().waitFor()
        if (!started.waitFor(30, TimeUnit.SECONDS)) {
            started.descendants().forEach { it.destroyForcibly() }
            started.destroyForcibly()
        }
    }

    private fun resolveCli(): File {
        parameters.firebaseCli.orNull?.let { return File(it) }
        val candidates = System.getenv("PATH").orEmpty().split(":").map { File(it, "firebase") } +
            File(System.getProperty("user.home"), ".nvm/versions/node").listFiles().orEmpty()
                .sortedDescending().map { it.resolve("bin/firebase") }
        return candidates.firstOrNull { it.canExecute() } ?: error(
            "Firebase CLI not found. Install it (npm install -g firebase-tools) or set " +
                "turnia.firebaseCli=/path/to/firebase in ~/.gradle/gradle.properties."
        )
    }

    private fun run(command: List<String>, dir: File, path: String) {
        val exe = File(path.split(":").first(), command.first()).takeIf { it.canExecute() }?.path ?: command.first()
        val process = ProcessBuilder(listOf(exe) + command.drop(1)).directory(dir).inheritIO()
            .apply { environment()["PATH"] = path }
            .start()
        check(process.waitFor() == 0) { "`${command.joinToString(" ")}` failed in $dir" }
    }

    private fun listening(port: Int): Boolean = runCatching {
        Socket().use { it.connect(InetSocketAddress("127.0.0.1", port), 300) }
    }.isSuccess

    companion object {
        /** Makes every `connected…AndroidTest` task of [project] run with the emulators up. */
        fun attachTo(project: Project) {
            val service = project.gradle.sharedServices.registerIfAbsent("firebaseEmulators", FirebaseEmulators::class.java) {
                parameters.firebaseDir.set(project.rootProject.layout.projectDirectory.dir("firebase"))
                parameters.firebaseCli.set(project.providers.gradleProperty("turnia.firebaseCli"))
            }
            project.tasks.named { it.startsWith("connected") && it.endsWith("AndroidTest") }.configureEach {
                usesService(service)
                doFirst { service.get().ensureRunning() }
            }
        }
    }
}
