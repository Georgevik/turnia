package com.georgevik.turnia.interfaces

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import com.georgevik.turnia.core.domain.model.GoogleSignInToken
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import org.koin.dsl.module

private const val TAG = "AuthProviderAndroid"

class AuthProviderAndroid(
    private val context: Context
) : AuthProvider {

    override suspend fun getGoogleToken(): GoogleSignInToken? {
        val credentialManager = CredentialManager.create(context)
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId( "570433560233-ip7aut9frd2l629vkhe34s5b6j49ojgc.apps.googleusercontent.com")
            .build()

        Logger.d(TAG, "googleIdOption: $googleIdOption")

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        return try {
            Logger.d(TAG, "request: $request")
            val result = credentialManager.getCredential(context, request)
            Logger.d(TAG, "result: $result")

            val credential = result.credential
            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
            Logger.d(TAG, "googleIdTokenCredential: $googleIdTokenCredential")
            GoogleSignInToken(
                idToken = googleIdTokenCredential.idToken,
                accessToken = null,
            )
        } catch (e: NoCredentialException) {
            // No hay cuentas Google en el dispositivo o la configuración del Client ID / SHA-1 no coincide
            Logger.e(TAG, "NoCredentialException", e)
            e.printStackTrace()
            null
        } catch (e: GetCredentialCancellationException) {
            Logger.e(TAG, "GetCredentialCancellationException", e)
            // El usuario cerró el diálogo sin seleccionar cuenta
            null
        } catch (e: Exception) {
            Logger.e(TAG, "Exception", e)
            e.printStackTrace()
            null
        }
    }
}

val androidAuthModule = module {
    single<AuthProvider> { AuthProviderAndroid(get()) }
}
