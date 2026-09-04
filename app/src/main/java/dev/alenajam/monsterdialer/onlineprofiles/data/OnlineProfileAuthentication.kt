package dev.alenajam.monsterdialer.onlineprofiles.data

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.alenajam.monsterdialer.R
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.tasks.await

/** Owns the durable Firebase identity required to publish an Online Profile. */
@Singleton
class OnlineProfileAuthentication @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun currentUserId(): String? = FirebaseApp.initializeApp(context)
        ?.let(FirebaseAuth::getInstance)
        ?.currentUser
        ?.uid

    suspend fun signInWithGoogle(idToken: String): String {
        val app = requireNotNull(FirebaseApp.initializeApp(context)) {
            context.getString(R.string.online_profile_firebase_not_configured)
        }
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        val user = FirebaseAuth.getInstance(app).signInWithCredential(credential).await().user
        return requireNotNull(user?.uid) { context.getString(R.string.online_profile_google_sign_in_error) }
    }

    fun requireCurrentUserId(): String = requireNotNull(currentUserId()) {
        context.getString(R.string.online_profile_google_sign_in_required)
    }

    fun signOut() {
        FirebaseApp.initializeApp(context)?.let(FirebaseAuth::getInstance)?.signOut()
    }
}
