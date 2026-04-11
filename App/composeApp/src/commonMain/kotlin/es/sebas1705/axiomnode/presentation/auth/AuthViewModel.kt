package es.sebas1705.axiomnode.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import es.sebas1705.axiomnode.auth.GoogleSignInResult
import es.sebas1705.axiomnode.auth.GoogleSignInService
import es.sebas1705.axiomnode.domain.models.User
import es.sebas1705.axiomnode.domain.usecases.AuthUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AuthState(
    val isLoading: Boolean = false,
    val user: User? = null,
    val error: String? = null,
    val isAuthenticated: Boolean = false,
)

class AuthViewModel(
    private val authUseCase: AuthUseCase,
    private val googleSignInService: GoogleSignInService,
) : ViewModel() {
    private val _state = MutableStateFlow(AuthState())
    val state: StateFlow<AuthState> = _state.asStateFlow()

    init {
        checkCurrentUser()
    }

    /**
     * Launches the full Google Sign-In flow:
     * 1. Credential Manager / platform sign-in → Google idToken
     * 2. Firebase Auth → Firebase idToken
     * 3. Backend sync → User profile
     */
    fun launchGoogleSignIn() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)

            when (val result = googleSignInService.signIn()) {
                is GoogleSignInResult.Success -> {
                    // Got Firebase idToken, sync with backend
                    authUseCase.signInWithGoogle(
                        idToken = result.idToken,
                        email = result.email,
                        displayName = result.displayName,
                        photoUrl = result.photoUrl,
                    )
                        .onSuccess { user ->
                            _state.value = AuthState(
                                user = user,
                                isAuthenticated = true,
                            )
                        }
                        .onFailure { error ->
                            _state.value = _state.value.copy(
                                isLoading = false,
                                error = error.message ?: "Error sincronizando con el servidor",
                            )
                        }
                }

                is GoogleSignInResult.Cancelled -> {
                    _state.value = _state.value.copy(isLoading = false)
                }

                is GoogleSignInResult.Error -> {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = result.message,
                    )
                }
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            googleSignInService.signOut()
            authUseCase.signOut()
            _state.value = AuthState()
        }
    }

    private fun checkCurrentUser() {
        viewModelScope.launch {
            authUseCase.getCurrentUser()
                .onSuccess { user ->
                    if (user != null) {
                        _state.value = AuthState(
                            user = user,
                            isAuthenticated = true,
                        )
                    }
                }
        }
    }
}
