package es.sebas1705.axiomnode.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
) : ViewModel() {
    private val _state = MutableStateFlow(AuthState())
    val state: StateFlow<AuthState> = _state.asStateFlow()

    init {
        checkCurrentUser()
    }

    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            authUseCase.signInWithGoogle(idToken)
                .onSuccess { user ->
                    _state.value = AuthState(
                        user = user,
                        isAuthenticated = true,
                    )
                }
                .onFailure { error ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = error.message ?: "Error desconocido",
                    )
                }
        }
    }

    fun signOut() {
        viewModelScope.launch {
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

