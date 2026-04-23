package es.sebas1705.axiomnode.presentation.auth

import es.sebas1705.axiomnode.auth.GoogleSignInResult
import es.sebas1705.axiomnode.testsupport.FakeAuthUseCase
import es.sebas1705.axiomnode.testsupport.FakeGoogleSignInClient
import es.sebas1705.axiomnode.testsupport.MainDispatcherRule
import es.sebas1705.axiomnode.testsupport.sampleUser
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    private val mainRule = MainDispatcherRule()
    private lateinit var authUseCase: FakeAuthUseCase
    private lateinit var googleClient: FakeGoogleSignInClient

    @BeforeTest
    fun setUp() {
        mainRule.install()
        authUseCase = FakeAuthUseCase()
        googleClient = FakeGoogleSignInClient()
    }

    @AfterTest
    fun tearDown() {
        mainRule.uninstall()
    }

    @Test
    fun `init with existing user sets authenticated state`() = runTest {
        val existingUser = sampleUser(firebaseUid = "existing")
        authUseCase.currentUserResult = Result.success(existingUser)

        val viewModel = AuthViewModel(authUseCase, googleClient)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertTrue(state.isAuthenticated)
        assertEquals(existingUser, state.user)
        assertNull(state.error)
        assertEquals(1, authUseCase.getCurrentUserCalls)
    }

    @Test
    fun `init with null user keeps unauthenticated state`() = runTest {
        authUseCase.currentUserResult = Result.success(null)

        val viewModel = AuthViewModel(authUseCase, googleClient)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.isAuthenticated)
        assertNull(state.user)
        assertNull(state.error)
    }

    @Test
    fun `launchGoogleSignIn success authenticates and forwards metadata`() = runTest {
        authUseCase.currentUserResult = Result.success(null)
        val signedUser = sampleUser(firebaseUid = "signed")
        authUseCase.signInWithGoogleResult = Result.success(signedUser)
        googleClient.signInResult = GoogleSignInResult.Success(
            idToken = "firebase-token",
            email = "new@axiomnode.es",
            displayName = "New User",
            photoUrl = "https://img",
        )

        val viewModel = AuthViewModel(authUseCase, googleClient)
        advanceUntilIdle()

        viewModel.launchGoogleSignIn()
        advanceUntilIdle()

        val state = viewModel.state.value
        assertTrue(state.isAuthenticated)
        assertEquals(signedUser, state.user)
        assertNull(state.error)
        assertEquals(1, googleClient.signInCalls)
        assertEquals(1, authUseCase.signInWithGoogleCalls)
        assertEquals("firebase-token", authUseCase.lastIdToken)
        assertEquals("new@axiomnode.es", authUseCase.lastEmail)
        assertEquals("New User", authUseCase.lastDisplayName)
        assertEquals("https://img", authUseCase.lastPhotoUrl)
    }

    @Test
    fun `launchGoogleSignIn cancelled clears loading without error`() = runTest {
        authUseCase.currentUserResult = Result.success(null)
        googleClient.signInResult = GoogleSignInResult.Cancelled

        val viewModel = AuthViewModel(authUseCase, googleClient)
        advanceUntilIdle()

        viewModel.launchGoogleSignIn()
        advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertFalse(state.isAuthenticated)
        assertNull(state.error)
        assertEquals(0, authUseCase.signInWithGoogleCalls)
    }

    @Test
    fun `launchGoogleSignIn service error exposes message`() = runTest {
        authUseCase.currentUserResult = Result.success(null)
        googleClient.signInResult = GoogleSignInResult.Error("google-failure")

        val viewModel = AuthViewModel(authUseCase, googleClient)
        advanceUntilIdle()

        viewModel.launchGoogleSignIn()
        advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertEquals("google-failure", state.error)
        assertFalse(state.isAuthenticated)
    }

    @Test
    fun `launchGoogleSignIn backend sync error exposes fallback message`() = runTest {
        authUseCase.currentUserResult = Result.success(null)
        googleClient.signInResult = GoogleSignInResult.Success(idToken = "token")
        authUseCase.signInWithGoogleResult = Result.failure(IllegalStateException())

        val viewModel = AuthViewModel(authUseCase, googleClient)
        advanceUntilIdle()

        viewModel.launchGoogleSignIn()
        advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertEquals("Error sincronizando con el servidor", state.error)
        assertFalse(state.isAuthenticated)
    }

    @Test
    fun `signOut invokes both dependencies and resets state`() = runTest {
        authUseCase.currentUserResult = Result.success(sampleUser(firebaseUid = "existing"))

        val viewModel = AuthViewModel(authUseCase, googleClient)
        advanceUntilIdle()
        assertTrue(viewModel.state.value.isAuthenticated)

        viewModel.signOut()
        advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.isAuthenticated)
        assertNull(state.user)
        assertNull(state.error)
        assertEquals(1, googleClient.signOutCalls)
        assertEquals(1, authUseCase.signOutCalls)
    }
}
