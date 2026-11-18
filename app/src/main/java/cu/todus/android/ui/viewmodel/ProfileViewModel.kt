package cu.todus.android.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cu.todus.android.data.SessionStore
import cu.todus.android.protocol.xmpp.StanzaBuilder
import cu.todus.android.protocol.xmpp.XmppSession
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    val sessionStore: SessionStore,
    private val xmpp: XmppSession,
) : ViewModel() {

    data class ProfileForm(
        val name: String = "",
        val info: String = "",
        val toodusId: String = "",
        val photoUrl: String? = null,
        val phone: String = "",
    )

    private val _form = MutableStateFlow(ProfileForm())
    val form: StateFlow<ProfileForm> = _form.asStateFlow()

    private val _saved = MutableStateFlow(false)
    val saved: StateFlow<Boolean> = _saved.asStateFlow()

    init {
        viewModelScope.launch {
            val snap = sessionStore.snapshot()
            _form.value = ProfileForm(
                name = snap.name ?: "",
                info = snap.info ?: "",
                toodusId = snap.todusId ?: "",
                photoUrl = snap.photo,
                phone = snap.phone ?: "",
            )
        }
    }

    fun updateName(value: String) { _form.value = _form.value.copy(name = value) }
    fun updateInfo(value: String) { _form.value = _form.value.copy(info = value) }
    fun updateTodusId(value: String) { _form.value = _form.value.copy(todusId = value) }

    /**
     * Save profile changes. Empty fields keep their previous values (per guía UX spec).
     */
    fun save() {
        viewModelScope.launch {
            val current = _form.value
            sessionStore.saveProfile(
                name = current.name.ifBlank { sessionStore.snapshot().name },
                info = current.info,
                photo = current.photoUrl,
                toodusId = current.todusId.ifBlank { sessionStore.snapshot().todusId },
            )
            _saved.value = true
        }
    }

    fun changeTodusId(newId: String) {
        viewModelScope.launch {
            sessionStore.saveProfile(name = null, info = null, photo = null, toodusId = newId)
            _form.value = _form.value.copy(todusId = newId)
            _saved.value = true
        }
    }

    fun resetSaved() { _saved.value = false }
}
