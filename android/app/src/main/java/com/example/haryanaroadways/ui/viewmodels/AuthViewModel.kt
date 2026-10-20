package com.example.haryanaroadways.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.haryanaroadways.data.api.ApiClient
import com.example.haryanaroadways.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AuthMode {
    LOGIN,
    REGISTER
}

data class AuthUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val mobile: String = "",
    val name: String = "",
    val password: String = "",
    val mode: AuthMode = AuthMode.LOGIN,
    val isSuccess: Boolean = false
)

class AuthViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    val isLoggedIn: StateFlow<Boolean> = authRepository.isLoggedInFlow
        .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), false)

    val userRole: StateFlow<String?> = authRepository.userRoleFlow
        .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), null)

    val userName: StateFlow<String?> = authRepository.userNameFlow
        .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), null)

    val userMobile: StateFlow<String?> = authRepository.userMobileFlow
        .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), null)

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun updateMobile(mobile: String) {
        _uiState.update { it.copy(mobile = mobile, error = null) }
    }

    fun updateName(name: String) {
        _uiState.update { it.copy(name = name, error = null) }
    }

    fun updatePassword(password: String) {
        _uiState.update { it.copy(password = password, error = null) }
    }
    
    fun toggleMode() {
        _uiState.update { 
            it.copy(
                mode = if (it.mode == AuthMode.LOGIN) AuthMode.REGISTER else AuthMode.LOGIN,
                error = null 
            ) 
        }
    }

    fun submit() {
        viewModelScope.launch {
            val state = _uiState.value
            if (state.mobile.isBlank() || state.mobile.length < 10) {
                _uiState.update { it.copy(error = "Invalid mobile number") }
                return@launch
            }
            if (state.password.length < 6) {
                _uiState.update { it.copy(error = "Password must be at least 6 characters") }
                return@launch
            }
            if (state.mode == AuthMode.REGISTER && state.name.isBlank()) {
                _uiState.update { it.copy(error = "Name is required for registration") }
                return@launch
            }
            
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val response = if (state.mode == AuthMode.LOGIN) {
                    authRepository.login(state.mobile, state.password)
                } else {
                    authRepository.register(state.mobile, state.name, state.password)
                }
                
                if (response.success) {
                    ApiClient.reset()
                    _uiState.update { it.copy(isSuccess = true) }
                } else {
                    _uiState.update { it.copy(error = response.message) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Authentication failed") }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun reset() {
        _uiState.update { AuthUiState() }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            _uiState.update { AuthUiState() }
        }
    }
}
