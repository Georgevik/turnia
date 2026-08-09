package com.georgevik.turnia.core.data.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import com.georgevik.turnia.core.data.logger.Logger
import com.georgevik.turnia.core.domain.model.GoogleSignInError
import com.georgevik.turnia.core.domain.model.GoogleSignInResult
import com.georgevik.turnia.core.domain.model.GoogleSignInToken
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import org.koin.dsl.module

private const val TAG = "AuthProviderAndroid"

class AuthProviderAndroid(
    private val context: Context,
    private val webClientId: String,
) : AuthProvider {

    override suspend fun getGoogleToken(): GoogleSignInResult {
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(
                GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(webClientId)
                    .build()
            )
            .build()

        return try {
            val credential = CredentialManager.create(context)
                .getCredential(context, request)
                .credential
            val idToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
            Logger.i(TAG, "Google sign-in succeeded")
            GoogleSignInResult.Success(GoogleSignInToken(idToken = idToken, accessToken = null))
        } catch (e: GetCredentialCancellationException) {
            Logger.i(TAG, "Google sign-in cancelled by the user")
            GoogleSignInResult.Failure(GoogleSignInError.Cancelled)
        } catch (e: NoCredentialException) {
            Logger.e(TAG, "No Google credential available (no account, or client ID / SHA-1 mismatch)", e)
            GoogleSignInResult.Failure(GoogleSignInError.NoAccount)
        } catch (e: Exception) {
            Logger.e(TAG, "Google sign-in failed", e)
            GoogleSignInResult.Failure(GoogleSignInError.Unknown)
        }
    }
}

fun androidAuthModule(webClientId: String) = module {
    single<AuthProvider> { AuthProviderAndroid(get(), webClientId) }
}
