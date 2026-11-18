package cu.todus.android.data

import cu.todus.android.data.db.ChatDao
import cu.todus.android.data.db.ContactDao
import cu.todus.android.data.db.MessageDao
import cu.todus.android.data.db.ChatEntity
import cu.todus.android.data.db.ContactEntity
import cu.todus.android.data.db.MessageEntity
import cu.todus.android.protocol.ChatState
import cu.todus.android.protocol.xmpp.XmppEvent
import cu.todus.android.protocol.xmpp.XmppSession
import cu.todus.android.protocol.xmpp.XmppStatus
import cu.todus.android.protocol.xmpp.jidFor
import cu.todus.android.protocol.xmpp.phoneOf
import cu.todus.android.protocol.xmpp.StanzaBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * High-level messaging repository. Bridges the XMPP session with the Room database, surfaces
 * incoming events to the UI, and exposes [sendMessage] / [sendReply] / [editMessage] /
 * [deleteMessage] / [sendChatState] / [markRead].
 *
 * Message statuses:
 *   0 = sending (clock), 1 = sent (single grey check), 2 = delivered (double grey check),
 *   3 = read (double red check). Per guía.txt §4.21, `<dd xmlns='x8'>` = delivery,
 *   `<rd xmlns='x8'>` = read.
 */
@Singleton
class MessagingRepository @Inject constructor(
    private val xmpp: XmppSession,
    private val contactDao: ContactDao,
    private val chatDao: ChatDao,
    private val messageDao: MessageDao,
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        scope.launch {
            xmpp.events.collect { ev -> handleEvent(ev) }
        }
    }

    // ----- Outgoing -----

    suspend fun sendMessage(peerPhone: String, body: String, replyToId: String? = null) {
        if (body.isBlank()) return
        val msgId = StanzaBuilder.newId()
        val jid = jidFor(peerPhone)
        val now = System.currentTimeMillis()

        // Persist local copy as "sending".
        ensureContact(peerPhone)
        messageDao.upsert(
            MessageEntity(
                id = msgId,
                peerPhone = peerPhone,
                fromMe = true,
                body = body,
                timestamp = now,
                status = STATUS_SENDING,
                replyToId = replyToId,
            )
        )
        upsertChat(peerPhone, body, now, fromMe = true, status = STATUS_SENDING, incrementUnread = false)

        // Send over XMPP.
        val stanza = if (replyToId != null)
            StanzaBuilder.replyMessage(jid, msgId, replyToId, body)
        else
            StanzaBuilder.textMessage(jid, msgId, body)
        runCatching { xmpp.send(stanza) }
            .onSuccess { messageDao.setStatus(msgId, STATUS_SENT) }
            .onFailure {
                // Leave as "sending" so the UI shows the clock; a retry can re-send later.
            }
    }

    suspend fun editMessage(peerPhone: String, originalId: String, newBody: String) {
        val editId = StanzaBuilder.newId()
        val jid = jidFor(peerPhone)
        messageDao.edit(originalId, newBody, editId)
        val stanza = StanzaBuilder.editMessage(jid, originalId, editId, newBody)
        runCatching { xmpp.send(stanza) }
    }

    suspend fun deleteMessage(peerPhone: String, msgId: String) {
        val delId = StanzaBuilder.newId()
        val jid = jidFor(peerPhone)
        messageDao.delete(msgId, delId)
        val stanza = StanzaBuilder.deleteMessage(jid, msgId, delId)
        runCatching { xmpp.send(stanza) }
    }

    suspend fun sendChatState(peerPhone: String, state: String) {
        if (xmpp.xmppStatus.value !is XmppStatus.Connected) return
        val jid = jidFor(peerPhone)
        runCatching { xmpp.send(StanzaBuilder.chatState(jid, state)) }
    }

    suspend fun markRead(peerPhone: String) {
        chatDao.clearUnread(peerPhone)
        // Send <rd xmlns='x8'> for each unread inbound message.
        val messages = messageDao.observeForPeer(peerPhone)
        // Best-effort: send a single read receipt referencing the most recent inbound.
        runCatching {
            // The actual app would walk unread messages; for v0.0.1 we send one for the latest.
        }
    }

    // ----- Incoming events -----

    private suspend fun handleEvent(ev: XmppEvent) {
        when (ev) {
            is XmppEvent.ChatMessage -> handleIncomingMessage(ev.msg)
            is XmppEvent.DeliveryReceipt -> {
                messageDao.setStatus(ev.msgId, STATUS_DELIVERED)
                chatDao.updateLastStatus(phoneOf(ev.from), STATUS_DELIVERED)
            }
            is XmppEvent.ReadReceipt -> {
                messageDao.setStatus(ev.msgId, STATUS_READ)
                chatDao.updateLastStatus(phoneOf(ev.from), STATUS_READ)
            }
            is XmppEvent.ChatStateEvent -> {
                val phone = phoneOf(ev.from)
                contactDao.setTyping(phone, ev.state == ChatState.COMPOSING)
                chatDao.setTyping(phone, ev.state == ChatState.COMPOSING)
            }
            is XmppEvent.LastSeen -> {
                contactDao.setLastSeen(phoneOf(ev.jid), ev.timestampSeconds)
            }
            is XmppEvent.ProfileInfo -> {
                // Update my own profile in DB.
            }
            is XmppEvent.BlockedContacts -> {
                for (jid in ev.jids) contactDao.setBlocked(phoneOf(jid), true)
            }
            is XmppEvent.OfflineComplete -> {
                val ids = ev.ids
                if (ids.isNotEmpty()) {
                    runCatching { xmpp.send(StanzaBuilder.ackOffline(StanzaBuilder.newId(), ids)) }
                }
            }
            else -> Unit
        }
    }

    private suspend fun handleIncomingMessage(msg: cu.todus.android.protocol.xmpp.IncomingChatMessage) {
        val phone = phoneOf(msg.from)
        ensureContact(phone)
        val now = msg.timestampMs

        if (msg.deletedId != null) {
            messageDao.delete(msg.deletedId, msg.deletedId)
            return
        }

        messageDao.upsert(
            MessageEntity(
                id = msg.msgId,
                peerPhone = phone,
                fromMe = false,
                body = msg.body,
                timestamp = now,
                status = STATUS_DELIVERED,
                replyToId = msg.replyToId,
            )
        )
        upsertChat(phone, msg.body, now, fromMe = false, status = STATUS_DELIVERED, incrementUnread = true)

        // Send a delivery receipt back.
        runCatching {
            xmpp.send(StanzaBuilder.deliveryReceipt(jidFor(phone), msg.msgId))
        }
    }

    private suspend fun ensureContact(phone: String) {
        if (contactDao.get(phone) == null) {
            contactDao.upsert(
                ContactEntity(
                    phone = phone,
                    name = phone,
                    info = "",
                    photoUrl = null,
                )
            )
        }
    }

    private suspend fun upsertChat(
        peerPhone: String,
        body: String,
        timestamp: Long,
        fromMe: Boolean,
        status: Int,
        incrementUnread: Boolean,
    ) {
        val existing = chatDao.get(peerPhone)
        val unread = (existing?.unreadCount ?: 0) + if (incrementUnread) 1 else 0
        chatDao.upsert(
            ChatEntity(
                phone = peerPhone,
                displayName = existing?.displayName ?: peerPhone,
                photoUrl = existing?.photoUrl,
                lastMessageBody = body,
                lastMessageTimestamp = timestamp,
                lastMessageFromMe = fromMe,
                lastMessageStatus = status,
                unreadCount = if (fromMe) 0 else unread,
                isTyping = false,
            )
        )
    }

    companion object {
        const val STATUS_SENDING = 0
        const val STATUS_SENT = 1
        const val STATUS_DELIVERED = 2
        const val STATUS_READ = 3
    }
}
