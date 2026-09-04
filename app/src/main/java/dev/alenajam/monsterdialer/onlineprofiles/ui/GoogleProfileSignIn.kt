package dev.alenajam.monsterdialer.onlineprofiles.ui

import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

/** Obtains a Google ID token with Credential Manager for Firebase Authentication. */
object GoogleProfileSignIn {
    suspend fun idToken(context: Context, serverClientId: String): String {
        val credentialManager = CredentialManager.create(context)
        val credential = credentialManager.getCredential(context, request(serverClientId)).credential
        require(credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL)
        return GoogleIdTokenCredential.createFrom(credential.data).idToken
    }

    private fun request(serverClientId: String): GetCredentialRequest =
        GetCredentialRequest.Builder()
            .addCredentialOption(
                GetSignInWithGoogleOption.Builder(serverClientId)
                    .build(),
            )
            .build()

    suspend fun clearCredentialState(context: Context) {
        CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest())
    }
}
