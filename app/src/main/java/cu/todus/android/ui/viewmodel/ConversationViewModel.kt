package cu.todus.android.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cu.todus.android.data.MessagingRepository
import cu.todus.android.data.SessionStore
import cu.todus.android.data.db.ContactDao
import cu.todus.android.data.db.ContactEntity
import cu.todus.android.data.db.MessageDao
import cu.todus.android.data.db.MessageEntity
import cu.todus.android.protocol.ChatState
import cu.todus.android.protocol.xmpp.XmppSession
import cu.todus.android.protocol.xmpp.XmppStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ConversationViewModel @Inject constructor(
    private val messageDao: MessageDao,
    private val contactDao: ContactDao,
    private val xmpp: XmppSession,
    private val messaging: MessagingRepository,
    val sessionStore: SessionStore,
) : ViewModel() {

    private var typingJob: Job? = null

    fun messages(peerPhone: String): StateFlow<List<MessageEntity>> =
        messageDao.observeForPeer(peerPhone)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun contact(peerPhone: String): StateFlow<ContactEntity?> =
        contactDao.observe(peerPhone)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val xmppConnected: StateFlow<Boolean> =
        xmpp.xmppStatus
            .map { it is XmppStatus.Connected }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun send(peerPhone: String, body: String, replyToId: String? = null) {
        viewModelScope.launch {
            messaging.sendMessage(peerPhone, body, replyToId)
            sendChatState(peerPhone, ChatState.PAUSED)
        }
    }

    fun edit(peerPhone: String, msgId: String, newBody: String) {
        viewModelScope.launch { messaging.editMessage(peerPhone, msgId, newBody) }
    }

    fun delete(peerPhone: String, msgId: String) {
        viewModelScope.launch { messaging.deleteMessage(peerPhone, msgId) }
    }

    fun onTyping(peerPhone: String) {
        viewModelScope.launch {
            messaging.sendChatState(peerPhone, ChatState.COMPOSING)
            typingJob?.cancel()
            typingJob = viewModelScope.launch {
                delay(4_000)
                messaging.sendChatState(peerPhone, ChatState.PAUSED)
            }
        }
    }

    fun markRead(peerPhone: String) {
        viewModelScope.launch { messaging.markRead(peerPhone) }
    }

    private suspend fun sendChatState(peerPhone: String, state: String) {
        messaging.sendChatState(peerPhone, state)
    }
}
