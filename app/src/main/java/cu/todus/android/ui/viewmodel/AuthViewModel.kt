package cu.todus.android.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cu.todus.android.data.SessionStore
import cu.todus.android.protocol.auth.AuthClient
import cu.todus.android.protocol.xmpp.StanzaBuilder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Auth-related UI state: phone entry, profile fetching after login-with-phone-only.
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    val sessionStore: SessionStore,
    private val authClient: AuthClient,
) : ViewModel() {

    sealed interface PhoneUiState {
        data object Idle : PhoneUiState
        data object Loading : PhoneUiState
        data class Error(val message: String) : PhoneUiState
        data class Success(val token: String) : PhoneUiState
    }

    sealed interface ProfileInitialUiState {
        data object Loading : ProfileInitialUiState
        data class Loaded(val name: String, val photoUrl: String?) : ProfileInitialUiState
        data class Error(val message: String) : ProfileInitialUiState
    }

    private val _phoneState = MutableStateFlow<PhoneUiState>(PhoneUiState.Idle)
    val phoneState: StateFlow<PhoneUiState> = _phoneState.asStateFlow()

    private val _profileState = MutableStateFlow<ProfileInitialUiState>(ProfileInitialUiState.Loading)
    val profileState: StateFlow<ProfileInitialUiState> = _profileState.asStateFlow()

    /**
     * Phone-only login flow (guía.txt §2.2). Validates the 8-digit Cuban number, calls the auth
     * endpoint with the hardcoded weak secret, and persists the session.
     */
    fun loginWithPhoneOnly(rawPhone: String) {
        val normalized = rawPhone.filter { it.isDigit() }
        if (normalized.length != 8) {
            _phoneState.value = PhoneUiState.Error("El número debe tener 8 dígitos")
            return
        }
        val fullPhone = "53$normalized"  // server expects country code + number (10 digits)
        _phoneState.value = PhoneUiState.Loading
        viewModelScope.launch {
            val installUuid = sessionStore.snapshot().installUuid
                ?: StanzaBuilder.newId().let { sessionStore.saveInstallUuid(it); it }
            val result = authClient.loginWithPhoneOnly(fullPhone)
            result.onSuccess { token ->
                sessionStore.saveSession(fullPhone, token)
                _phoneState.value = PhoneUiState.Success(token)
            }.onFailure { t ->
                _phoneState.value = PhoneUiState.Error(
                    t.message ?: "No se pudo autenticar. Verifica tu conexión."
                )
            }
        }
    }

    fun resetPhoneState() {
        _phoneState.value = PhoneUiState.Idle
    }
}
