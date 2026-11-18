package cu.todus.android.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cu.todus.android.data.SessionStore
import cu.todus.android.data.db.ChatDao
import cu.todus.android.data.db.ChatEntity
import cu.todus.android.data.db.ContactDao
import cu.todus.android.data.db.ContactEntity
import cu.todus.android.protocol.xmpp.XmppSession
import cu.todus.android.protocol.xmpp.XmppStatus
import cu.todus.android.protocol.xmpp.StanzaBuilder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val contactDao: ContactDao,
    private val chatDao: ChatDao,
    val xmpp: XmppSession,
    val sessionStore: SessionStore,
) : ViewModel() {

    val chats: StateFlow<List<ChatEntity>> =
        chatDao.observeAll()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val contacts: StateFlow<List<ContactEntity>> =
        contactDao.observeAll()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val xmppConnected: StateFlow<Boolean> = combine(
        xmpp.xmppStatus, xmpp.state,
    ) { status, _ -> status is XmppStatus.Connected }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun blockContact(phone: String, blocked: Boolean) {
        viewModelScope.launch {
            contactDao.setBlocked(phone, blocked)
        }
    }
}
