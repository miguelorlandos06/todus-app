package cu.todus.android.protocol.xmpp

import cu.todus.android.protocol.ChatState
import cu.todus.android.protocol.Ns
import cu.todus.android.protocol.TodusConstants
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.InetSocketAddress
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

/**
 * Connection state surfaced to the UI (Home app bar).
 * The three "..." states come from guía.txt's spec: Esperando red → Conectando → Actualizando → toDus.
 */
sealed interface ConnectionState {
    data object WaitingForNetwork : ConnectionState   // "Esperando red…"
    data object Connecting : ConnectionState          // "Conectando…"
    data object Updating : ConnectionState            // "Actualizando…"
    data object Connected : ConnectionState           // "toDus" (red text)
    data object Disconnected : ConnectionState        // back to waiting
}

/**
 * XMPP-level connection status (XMPP presence / socket).
 * Used for the red/green dot on the user's avatar in Home.
 */
sealed interface XmppStatus {
    data object Disconnected : XmppStatus
    data object Connected : XmppStatus
}

/**
 * Events emitted by the XMPP session to the rest of the app (UI, persistence).
 */
sealed interface XmppEvent {
    data class ChatMessage(val msg: IncomingChatMessage) : XmppEvent
    data class DeliveryReceipt(val from: String, val msgId: String) : XmppEvent
    data class ReadReceipt(val from: String, val msgId: String) : XmppEvent
    data class ChatStateEvent(val from: String, val state: String) : XmppEvent
    data class LastSeen(val jid: String, val timestampSeconds: Long) : XmppEvent
    data class ProfileInfo(val name: String?, val info: String?, val photoUrl: String?) : XmppEvent
    data class BlockedContacts(val jids: List<String>) : XmppEvent
    data class OfflineComplete(val ids: List<String>) : XmppEvent
    data object AuthSuccess : XmppEvent
    data class AuthFailed(val reason: String) : XmppEvent
    data class ConnectionLost(val reason: String) : XmppEvent
}

data class IncomingChatMessage(
    val from: String,           // full JID or phone
    val to: String,
    val msgId: String,
    val type: String,           // "c" or "gc"
    val body: String,
    val replyToId: String? = null,
    val editedId: String? = null,
    val deletedId: String? = null,
    val timestampMs: Long = System.currentTimeMillis(),
)

/**
 * The XMPP session. Owns a TLS socket against im.todus.cu:1756, drives the abbreviated XMPP
 * dialect, and exposes the connection state and events as Flows.
 *
 * Critical design notes from guía.txt:
 *  - The server only allows ONE session per JID. A second connection kills the first, so the
 *    session owns the only socket and all sends go through [send].
 *  - After connecting, we must request offline messages (`t:offline`), process the
 *    `<todus_offline ts='...'/>` ... `<todus_end_offline/>` block, then ack the IDs.
 *  - Keepalive: <iq i='…' t='get'><ping xmlns='urn:xmpp:ping'/></iq> every 30 s.
 *  - Reconnect with exponential backoff + ±20% jitter (base 15 s, max 300 s).
 *
 * Self-signed certs: toDus servers sometimes use self-signed certificates. To keep the app
 * working in those environments while still being explicit about the risk, we install a
 * permissive X509TrustManager ONLY against the toDus host. In production you should pin the
 * real cert; see NETWORK_SECURITY_CONFIG note in the manifest.
 */
class XmppSession(
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {

    private val _state = MutableStateFlow<ConnectionState>(ConnectionState.WaitingForNetwork)
    val state: StateFlow<ConnectionState> = _state.asStateFlow()

    private val _xmppStatus = MutableStateFlow<XmppStatus>(XmppStatus.Disconnected)
    val xmppStatus: StateFlow<XmppStatus> = _xmppStatus.asStateFlow()

    private val _events = MutableSharedFlow<XmppEvent>(extraBufferCapacity = 256)
    val events: SharedFlow<XmppEvent> = _events.asSharedFlow()

    private var socket: SSLSocket? = null
    private var reader: BufferedReader? = null
    private var writer: BufferedWriter? = null
    private var sessionJob: Job? = null
    private var pingJob: Job? = null
    private var receiveJob: Job? = null

    @Volatile private var phone: String = ""
    @Volatile private var token: String = ""

    private val sendLock = Any()
    private val seenIds = object : LinkedHashMap<String, Boolean>(1024, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Boolean>?): Boolean {
            return size > 10_000
        }
    }

    private val scope = CoroutineScope(ioDispatcher + SupervisorJob())

    /** Connects and runs the full handshake. Blocks the calling coroutine until disconnect. */
    suspend fun connect(phone: String, token: String) = withContext(ioDispatcher) {
        if (this@XmppSession.phone == phone && _xmppStatus.value == XmppStatus.Connected) return@withContext
        this@XmppSession.phone = phone
        this@XmppSession.token = token

        var backoff = TodusConstants.RECONNECT_BASE_BACKOFF_MS
        while (scope.isActive && _state.value !is ConnectionState.Connected) {
            _state.value = ConnectionState.Connecting
            try {
                openSocket()
                runHandshake()
                _state.value = ConnectionState.Updating
                fetchOffline()
                _state.value = ConnectionState.Connected
                _xmppStatus.value = XmppStatus.Connected
                _events.tryEmit(XmppEvent.AuthSuccess)
                startKeepalive()
                startReceiveLoop()
                return@withContext
            } catch (t: Throwable) {
                _events.tryEmit(XmppEvent.ConnectionLost(t.message ?: "unknown"))
                cleanup()
                _state.value = ConnectionState.Disconnected
                _xmppStatus.value = XmppStatus.Disconnected
                // Exponential backoff with ±20% jitter.
                val jitter = (backoff * 0.2 * (Math.random() * 2 - 1)).toLong()
                delay(backoff + jitter)
                backoff = (backoff * 2).coerceAtMost(TodusConstants.RECONNECT_MAX_BACKOFF_MS)
            }
        }
    }

    fun disconnect() {
        scope.launch { cleanup() }
        _state.value = ConnectionState.Disconnected
        _xmppStatus.value = XmppStatus.Disconnected
    }

    /** Send a raw stanza (newline-terminated so the server flushes promptly). Thread-safe. */
    suspend fun send(stanza: String) = withContext(ioDispatcher) {
        synchronized(sendLock) {
            val w = writer ?: throw IllegalStateException("XMPP not connected")
            w.write(stanza)
            w.write('\n')
            w.flush()
        }
    }

    // ----- internals -----

    private fun openSocket() {
        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(null, arrayOf<TrustManager>(TodusTrustManager), null)
        val factory = sslContext.socketFactory
        val sock = factory.createSocket() as SSLSocket
        sock.useClientMode = true
        sock.keepAlive = true
        sock.tcpNoDelay = true
        sock.connect(InetSocketAddress(TodusConstants.IM_HOST, TodusConstants.IM_PORT), 15_000)
        sock.startHandshake()
        socket = sock
        reader = BufferedReader(InputStreamReader(sock.inputStream, Charsets.UTF_8))
        writer = BufferedWriter(OutputStreamWriter(sock.outputStream, Charsets.UTF_8))
    }

    private fun runHandshake() {
        val w = writer ?: throw IllegalStateException("no writer")
        val r = reader ?: throw IllegalStateException("no reader")

        // Step 1: stream open
        w.write(StanzaBuilder.streamOpen()); w.flush()
        readUntilFeatures(r)

        // Step 2: SASL PLAIN auth
        val authPayload = "\u0000$phone\u0000$token"
        val b64 = android.util.Base64.encodeToString(
            authPayload.toByteArray(Charsets.UTF_8),
            android.util.Base64.NO_WRAP
        )
        w.write("<${Ns.SASL_AUTH_ELEMENT} xmlns='${Ns.SASL_AUTH_NS}' e='PLAIN'>$b64</${Ns.SASL_AUTH_ELEMENT}>")
        w.flush()
        // Expect <ok xmlns='x2'/>
        readUntilSuccess(r, expectedElement = "ok")

        // Step 3: stream restart
        w.write(StanzaBuilder.streamRestart()); w.flush()
        readUntilFeatures(r)

        // Step 4: bind
        val bindSid = StanzaBuilder.newId()
        w.write("<iq i='$bindSid' t='set'><b1 xmlns='${Ns.BIND}'><re>md5($phone)_Android</re></b1></iq>")
        w.flush()
        readUntilIqResult(r, bindSid)

        // Step 5: presence
        w.write("<p xmlns='${Ns.JC}'><status></status></p>")
        w.flush()
    }

    private fun readUntilFeatures(r: BufferedReader) {
        val buf = StringBuilder()
        while (true) {
            val line = r.readLine() ?: throw java.io.IOException("stream closed")
            buf.append(line)
            if (buf.contains("stream:features")) return
            if (buf.contains("</stream:stream")) throw java.io.IOException("server closed stream")
            if (buf.length > 65_536) throw java.io.IOException("features not received")
        }
    }

    private fun readUntilSuccess(r: BufferedReader, expectedElement: String) {
        val buf = StringBuilder()
        while (true) {
            val line = r.readLine() ?: throw java.io.IOException("stream closed")
            buf.append(line)
            if (buf.contains("<$expectedElement")) return
            if (buf.contains("<failure") || buf.contains("</stream:stream"))
                throw java.io.IOException("auth failed: $buf")
            if (buf.length > 65_536) throw java.io.IOException("$expectedElement not received")
        }
    }

    private fun readUntilIqResult(r: BufferedReader, sid: String) {
        val buf = StringBuilder()
        while (true) {
            val line = r.readLine() ?: throw java.io.IOException("stream closed")
            buf.append(line)
            if (buf.contains("i='$sid'") && buf.contains("t='result'")) return
            if (buf.contains("t='error'") && buf.contains("i='$sid'"))
                throw java.io.IOException("iq error: $buf")
            if (buf.length > 65_536) throw java.io.IOException("iq result timeout for $sid")
        }
    }

    private fun fetchOffline() {
        scope.launch {
            send(StanzaBuilder.getOffline(StanzaBuilder.newId()))
        }
    }

    private fun startKeepalive() {
        pingJob?.cancel()
        pingJob = scope.launch {
            while (isActive && _xmppStatus.value == XmppStatus.Connected) {
                delay(TodusConstants.PING_INTERVAL_SECONDS * 1000)
                runCatching {
                    send(StanzaBuilder.ping(StanzaBuilder.newId()))
                }
            }
        }
    }

    private fun startReceiveLoop() {
        receiveJob?.cancel()
        receiveJob = scope.launch {
            val r = reader ?: return@launch
            val buffer = StringBuilder()
            try {
                while (isActive) {
                    val line = r.readLine() ?: break
                    buffer.append(line)
                    val (stanzas, consumed) = StanzaParser.parse(buffer.toString())
                    if (consumed > 0) {
                        buffer.delete(0, consumed)
                    }
                    stanzas.forEach { handle(it) }
                }
            } catch (t: Throwable) {
                _events.tryEmit(XmppEvent.ConnectionLost(t.message ?: "receive error"))
                cleanup()
                _state.value = ConnectionState.Disconnected
                _xmppStatus.value = XmppStatus.Disconnected
                // Trigger reconnect loop
                scope.launch { connect(phone, token) }
            }
        }
    }

    private fun handle(stanza: Stanza) {
        when (stanza.name) {
            "m" -> handleMessage(stanza)
            "iq" -> handleIq(stanza)
            "p" -> { /* presence — out of scope for v0.0.1 */ }
        }
    }

    private fun handleMessage(stanza: Stanza) {
        // Normalize both abbreviated and legacy attributes.
        val from = stanza.attr("o") ?: stanza.attr("to") ?: return
        val to = stanza.attr("f") ?: stanza.attr("from") ?: phone
        val msgId = stanza.attr("i") ?: stanza.attr("id") ?: return
        val type = stanza.attr("t") ?: stanza.attr("type") ?: "c"

        // Dedup by msg_id (LRU, ~10000 entries).
        synchronized(seenIds) {
            if (seenIds.containsKey(msgId)) return
            seenIds[msgId] = true
        }

        // Chat state?
        val composingChild = stanza.children.firstOrNull { it.attributes["xmlns"] == Ns.CHAT_STATE }
        if (composingChild != null) {
            _events.tryEmit(XmppEvent.ChatStateEvent(from, composingChild.name))
            // If there is no body, this is a pure chat-state notification.
            if (stanza.child("b") == null) return
        }

        // Receipts
        val delivery = stanza.children.firstOrNull { it.name == "dd" }
        if (delivery != null) {
            val rid = delivery.attr("i") ?: return
            _events.tryEmit(XmppEvent.DeliveryReceipt(from, rid))
            return
        }
        val read = stanza.children.firstOrNull { it.name == "rd" }
        if (read != null) {
            val rid = read.attr("i") ?: return
            _events.tryEmit(XmppEvent.ReadReceipt(from, rid))
            return
        }

        // Reply / edited / deleted
        val reply = stanza.children.firstOrNull { it.attributes["xmlns"] == Ns.REPLY }
        val edited = stanza.children.firstOrNull { it.attributes["xmlns"] == Ns.EDITED }
        val deleted = stanza.children.firstOrNull { it.attributes["xmlns"] == Ns.DELETED }

        val body = stanza.child("b")?.text ?: ""
        if (deleted != null) {
            val did = deleted.attr("mi") ?: return
            _events.tryEmit(XmppEvent.ChatMessage(
                IncomingChatMessage(
                    from = from, to = to, msgId = did, type = type, body = "",
                    deletedId = did
                )
            ))
            return
        }

        val msg = IncomingChatMessage(
            from = from,
            to = to,
            msgId = msgId,
            type = type,
            body = body,
            replyToId = reply?.attr("mi"),
            editedId = edited?.attr("mi"),
        )
        _events.tryEmit(XmppEvent.ChatMessage(msg))
    }

    private fun handleIq(stanza: Stanza) {
        val type = stanza.attr("t") ?: stanza.attr("type") ?: return
        if (type != "result" && type != "set") return
        val query = stanza.child("query") ?: return
        val ns = query.attributes["xmlns"] ?: return

        when (ns) {
            Ns.PRIVACY -> {
                _events.tryEmit(XmppEvent.ProfileInfo(
                    name = query.attr("name"),
                    info = query.attr("info"),
                    photoUrl = query.attr("profile_photo"),
                ))
            }
            Ns.LAST -> {
                val secs = query.attr("last")?.toLongOrNull() ?: 0L
                val from = stanza.attr("f") ?: stanza.attr("from") ?: ""
                _events.tryEmit(XmppEvent.LastSeen(from, secs))
            }
            Ns.BLOCK_GET, "todus:block:get:3" -> {
                val blist = query.attr("blist") ?: ""
                val jids = if (blist.isBlank()) emptyList() else blist.split(",").map { it.trim() }
                _events.tryEmit(XmppEvent.BlockedContacts(jids))
            }
            Ns.OFFLINE -> {
                // Server starts the offline block; we'll see <todus_offline ts='…'/> markers
                // around each message and a <todus_end_offline/> at the end.
            }
        }

        // Catch the offline-end marker.
        if (stanza.rawXml.contains(Ns.OFFLINE_END)) {
            _events.tryEmit(XmppEvent.OfflineComplete(emptyList()))
        }
    }

    private fun cleanup() {
        runCatching { pingJob?.cancel() }
        runCatching { receiveJob?.cancel() }
        runCatching { reader?.close() }
        runCatching { writer?.close() }
        runCatching { socket?.close() }
        reader = null; writer = null; socket = null
    }

    /**
     * Trust manager that accepts any cert from the toDus host. This is necessary because many
     * toDus deployments use self-signed certificates (see guía.txt §16.3). The risk is MITM —
     * pinned certs would be preferable in a hardening pass.
     */
    private object TodusTrustManager : X509TrustManager {
        override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
        override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
        override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
    }
}
