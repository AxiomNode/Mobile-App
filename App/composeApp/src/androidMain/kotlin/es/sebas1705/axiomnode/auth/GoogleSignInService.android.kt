package es.sebas1705.axiomnode.auth

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import es.sebas1705.axiomnode.config.AppConfig
import kotlinx.coroutines.tasks.await

private const val TAG = "GoogleSignInService"

/**
 * Android Google Sign-In using Credential Manager + Firebase Auth.
 *
 * Flow:
 * 1. Credential Manager shows One-Tap UI with Google accounts
 * 2. User picks account → receives Google ID token
 * 3. Token is exchanged with Firebase Auth for a Firebase idToken
 * 4. Firebase idToken is returned to be sent to backend
 *
 * IMPORTANT: [context] MUST be an Activity context, not Application.
 */
actual class GoogleSignInService(
    private val context: Context,
    private val config: AppConfig,
) {
    private val activity: Activity = context as? Activity
        ?: throw IllegalArgumentException(
            "GoogleSignInService requires an Activity context. " +
            "Ensure androidContext(this@MainActivity) is used in Koin setup."
        )
    private val credentialManager = CredentialManager.create(context)
    private val firebaseAuth = FirebaseAuth.getInstance()

    actual suspend fun signIn(): GoogleSignInResult {
        return try {
            // Try the Sign-In with Google button flow (more reliable on emulators)
            signInWithGoogleButton()
        } catch (e: GetCredentialCancellationException) {
            Log.d(TAG, "Sign-in cancelled by user")
            GoogleSignInResult.Cancelled
        } catch (e: NoCredentialException) {
            Log.e(TAG, "No credentials available. Ensure a Google account is signed in on the device/emulator.", e)
            GoogleSignInResult.Error(
                "No hay cuentas de Google disponibles. " +
                "Asegurate de tener una cuenta de Google configurada en Ajustes > Cuentas del dispositivo/emulador."
            )
        } catch (e: GetCredentialException) {
            Log.e(TAG, "GetCredentialException: type=${e.type}, message=${e.message}", e)
            // Fallback: try the legacy Google ID option
            try {
                signInWithGoogleIdOption()
            } catch (fallbackEx: Exception) {
                Log.e(TAG, "Fallback sign-in also failed", fallbackEx)
                GoogleSignInResult.Error(
                    "Error en Google Sign-In: ${e.message}\n" +
                    "Verifica que: \n" +
                    "1) Hay una cuenta de Google en el dispositivo\n" +
                    "2) Google Play Services esta actualizado\n" +
                    "3) El SHA-1 de debug esta registrado en Firebase"
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected sign-in error", e)
            GoogleSignInResult.Error(e.message ?: "Error desconocido en Google Sign-In")
        }
    }

    /**
     * Uses GetSignInWithGoogleOption – shows a full-screen Google Sign-In dialog.
     * More reliable on emulators and first-time sign-ins.
     */
    private suspend fun signInWithGoogleButton(): GoogleSignInResult {
        val signInOption = GetSignInWithGoogleOption.Builder(config.googleWebClientId)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(signInOption)
            .build()

        Log.d(TAG, "Launching Sign-In with Google button flow, webClientId=${config.googleWebClientId}")

        val result = credentialManager.getCredential(
            request = request,
            context = activity,
        )

        return handleCredentialResult(result)
    }

    /**
     * Uses GetGoogleIdOption – shows One-Tap bottom sheet UI.
     * Fallback if the button flow fails.
     */
    private suspend fun signInWithGoogleIdOption(): GoogleSignInResult {
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(config.googleWebClientId)
            .setAutoSelectEnabled(false) // false for first-time & emulators
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        Log.d(TAG, "Launching Google ID Option flow (fallback)")

        val result = credentialManager.getCredential(
            request = request,
            context = activity,
        )

        return handleCredentialResult(result)
    }

    private suspend fun handleCredentialResult(
        result: androidx.credentials.GetCredentialResponse,
    ): GoogleSignInResult {
        val credential = result.credential
        if (credential !is CustomCredential ||
            credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            Log.w(TAG, "Unexpected credential type: ${credential.type}")
            return GoogleSignInResult.Error("Tipo de credencial no soportado: ${credential.type}")
        }

        val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
        val googleIdToken = googleIdTokenCredential.idToken
        Log.d(TAG, "Got Google ID token, exchanging with Firebase...")

        // Exchange with Firebase Auth
        val firebaseCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
        val authResult = firebaseAuth.signInWithCredential(firebaseCredential).await()

        val firebaseIdToken = authResult.user
            ?.getIdToken(true)
            ?.await()
            ?.token

        return if (firebaseIdToken != null) {
            val user = authResult.user
            Log.d(TAG, "Firebase sign-in successful, uid=${user?.uid}, email=${user?.email}")
            GoogleSignInResult.Success(
                idToken = firebaseIdToken,
                email = user?.email ?: "",
                displayName = user?.displayName,
                photoUrl = user?.photoUrl?.toString(),
            )
        } else {
            GoogleSignInResult.Error("No se pudo obtener el token de Firebase")
        }
    }

    actual suspend fun signOut() {
        try {
            firebaseAuth.signOut()
            credentialManager.clearCredentialState(
                androidx.credentials.ClearCredentialStateRequest()
            )
        } catch (_: Exception) {
            // Best-effort sign out
        }
    }
}

