package cu.todus.android.protocol.xmpp

import cu.todus.android.protocol.Attr
import cu.todus.android.protocol.Ns
import cu.todus.android.protocol.TodusConstants as Proto

/**
 * Helpers to build the abbreviated XML stanzas used by the toDus XMPP dialect.
 * Stanzas are serialized as raw strings; attribute names use the abbreviated form.
 */
object StanzaBuilder {

    // ----- Stream / handshake -----

    fun streamOpen(from: String = Proto.SERVER_DOMAIN): String =
        "<stream:stream xmlns='${Ns.JC}' o='$from' xmlns:stream='${Ns.STREAM}' v='1.0'>"

    fun streamRestart(): String =
        "<stream:stream xmlns='${Ns.JC}' o='${Proto.SERVER_DOMAIN}' " +
            "xmlns:stream='${Ns.STREAM}' v='1.0'>"

    fun saslAuth(phone: String, token: String): String {
        // SASL PLAIN: base64(\0 phone \0 token)
        val payload = "\u0000$phone\u0000$token"
        val b64 = android.util.Base64.encodeToString(
            payload.toByteArray(Charsets.UTF_8),
            android.util.Base64.NO_WRAP
        )
        return "<${Ns.SASL_AUTH_ELEMENT} xmlns='${Ns.SASL_AUTH_NS}' e='PLAIN'>$b64</${Ns.SASL_AUTH_ELEMENT}>"
    }

    fun bind(phone: String, sid: String): String {
        val resource = "md5($phone)_Android"
        return "<iq i='$sid' t='set'><b1 xmlns='${Ns.BIND}'><re>$resource</re></b1></iq>"
    }

    fun presenceInitial(): String =
        "<p xmlns='${Ns.JC}'><status></status></p>"

    fun ping(sid: String): String =
        "<iq i='$sid' t='get'><ping xmlns='${Ns.PING}'/></iq>"

    fun getOffline(sid: String): String =
        "<iq i='$sid' t='get'><query xmlns='${Ns.OFFLINE}'/></iq>"

    fun ackOffline(sid: String, ids: List<String>): String {
        val idsXml = ids.joinToString("") { "<id>${StanzaParser.escape(it)}</id>" }
        return "<iq i='$sid' t='set'><query xmlns='${Ns.OFFLINE_DEL}'>$idsXml</query></iq>"
    }

    // ----- Messages -----

    /**
     * Plain text chat message. Includes the mandatory <k xmlns='x8'/> marker.
     * Caller supplies the message id (32 hex chars).
     */
    fun textMessage(to: String, msgId: String, body: String): String {
        val safeBody = StanzaParser.escape(body)
        return "<m o='$to' t='c' i='$msgId' xmlns='${Ns.JC}'>" +
            "<k xmlns='${Ns.KEYS}'/>" +
            "<b>$safeBody</b>" +
            "</m>"
    }

    /**
     * Reply to a previous message. [replyToId] is the original message's i=.
     */
    fun replyMessage(to: String, msgId: String, replyToId: String, body: String): String {
        val safeBody = StanzaParser.escape(body)
        return "<m o='$to' t='c' i='$msgId' xmlns='${Ns.JC}'>" +
            "<k xmlns='${Ns.KEYS}'/>" +
            "<reply xmlns='${Ns.REPLY}' mi='${replyToId}'/>" +
            "<b>$safeBody</b>" +
            "</m>"
    }

    fun editMessage(to: String, originalId: String, editId: String, body: String): String {
        val safeBody = StanzaParser.escape(body)
        return "<m o='$to' t='c' i='$originalId' xmlns='${Ns.JC}'>" +
            "<k xmlns='${Ns.KEYS}'/>" +
            "<edited xmlns='${Ns.EDITED}' i='$editId' mi='$originalId'/>" +
            "<b>$safeBody</b>" +
            "</m>"
    }

    fun deleteMessage(to: String, msgId: String, deleteId: String): String =
        "<m o='$to' t='c' i='$msgId' xmlns='${Ns.JC}'>" +
            "<k xmlns='${Ns.KEYS}'/>" +
            "<deleted xmlns='${Ns.DELETED}' i='$deleteId' mi='$msgId'/>" +
            "<b/>" +
            "</m>"

    // ----- Receipts / chat states -----

    fun deliveryReceipt(to: String, msgId: String): String =
        "<m o='$to' t='c' i='${newId()}' xmlns='${Ns.JC}'>" +
            "<dd xmlns='${Ns.KEYS}' i='$msgId'/></m>"

    fun readReceipt(to: String, msgId: String): String =
        "<m o='$to' t='c' i='${newId()}' xmlns='${Ns.JC}'>" +
            "<rd xmlns='${Ns.KEYS}' i='$msgId'/></m>"

    fun ack(to: String, msgId: String): String =
        "<ak xmlns='${Ns.KEYS}' i='$msgId' o='$to'/>"

    fun chatState(to: String, state: String): String =
        // Chat states ride on a zero-body <m>
        "<m o='$to' t='c' i='${newId()}' xmlns='${Ns.JC}'>" +
            "<$state xmlns='${Ns.CHAT_STATE}'/></m>"

    // ----- Privacy / profile -----

    fun getPrivacy(): String =
        "<iq i='${newId()}' t='get'><query xmlns='${Ns.PRIVACY}'/></iq>"

    fun setPrivacy(profilePhoto: String, last: String, info: String): String =
        "<iq i='${newId()}' t='set'>" +
            "<query xmlns='${Ns.PRIVACY}' profile_photo='$profilePhoto' last='$last' info='$info'/>" +
            "</iq>"

    fun getLastSeen(jid: String): String =
        "<iq i='${newId()}' t='get' o='$jid'><query xmlns='${Ns.LAST}'/></iq>"

    fun getBlockList(): String =
        "<iq i='${newId()}' t='get'><query xmlns='${Ns.BLOCK_GET}'/></iq>"

    fun blockUser(jid: String): String =
        "<iq i='${newId()}' t='set'><query xmlns='${Ns.BLOCK_SET}' jid='$jid'/></iq>"

    /** IMPORTANT (guía.txt §9): unblock sends the phone-only, NOT the full JID. */
    fun unblockUser(phone: String): String =
        "<iq i='${newId()}' t='set'><query xmlns='${Ns.BLOCK_UNSET}' contact='$phone'/></iq>"

    // ----- Files (S3) -----

    fun reserveUploadUrl(sid: String, type: Int, size: Long, persistent: Boolean): String =
        "<iq i='$sid-3' t='get'>" +
            "<query xmlns='${Ns.PURL}' type='$type' persistent='${if (persistent) "true" else "false"}' " +
            "size='$size' room=''/></iq>"

    fun resolveDownloadUrl(sid: String, shortUrl: String): String =
        "<iq i='$sid-2' t='get'><query xmlns='${Ns.GURL}' url='$shortUrl'/></iq>"

    // ----- Push (Firebase) -----

    fun registerPush(pushId: String): String =
        "<iq i='${newId()}' t='set'><query xmlns='${Ns.FB_PUSH}'><push_id>$pushId</push_id></query></iq>"

    // ----- MAM (history) -----

    fun mamQuery(queryId: String, with: String? = null, max: Int = 50): String {
        val withField = if (with != null)
            "<field var='with'><value>$with</value></field>" else ""
        return "<iq t='set' i='$queryId'>" +
            "<query xmlns='${Ns.MAM}' queryid='$queryId'>" +
            "<x xmlns='${Ns.DATA}' type='form'>" +
            "<field var='FORM_TYPE' type='hidden'><value>${Ns.MAM}</value></field>" +
            withField +
            "</x>" +
            "<set xmlns='${Ns.RSM}'><max>$max</max></set>" +
            "</query></iq>"
    }

    // ----- ID generators -----

    private val hexChars = "0123456789abcdef".toCharArray()

    fun newId(): String {
        val sb = StringBuilder(32)
        for (i in 0 until 32) sb.append(hexChars[(Math.random() * 16).toInt()])
        return sb.toString()
    }

    /** Stable alias for the legacy name. */
    fun newMsgId(): String = newId()
}

/**
 * Builds a toDus JID for a phone number: <phone>@im.todus.cu
 */
fun jidFor(phone: String): String = "$phone@${Proto.SERVER_DOMAIN}"

/** Extracts the phone (localpart) from a JID like 5350000000@im.todus.cu. */
fun phoneOf(jid: String): String = jid.substringBefore('@')
