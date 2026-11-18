package cu.todus.android.protocol

/**
 * Abbreviated XML namespaces used by the toDus XMPP dialect.
 *
 * Mapping (from guía.txt §3.2):
 *   jc            base stream namespace (replaces jabber:client)
 *   x1            stream namespace (replaces http://etherx.jabber.org/streams)
 *   x2            SASL (replaces urn:ietf:params:xml:ns:xmpp-sasl)
 *   x4            bind (replaces urn:ietf:params:xml:ns:xmpp-bind)
 *   x8            keys / acks / receipts (delivery, read, ack)
 *   x10           group info (rename/subject/avatar)
 *   x11           group members (query)
 *   x13           group leave
 *   x14           group invite link
 *   x16           group create
 *   uc1           chat states (composing / paused / active / inactive / gone)
 *   reply:n       message reply
 *   resend:n      forward (reenvío)
 *   edited:n      message edit
 *   deleted:n     message delete
 *   image:n, video:n, voice:n, file:n, gif:n, sticker:n,
 *   contact:n, location:n, event:n, button:n, reaction:n,
 *   mention:n, streamvideo:n, tcall:n  → media payload namespaces
 *   t:offline, t:offline:del → offline message flow
 *   todus:purl, todus:gurl   → S3 URL reservation / resolution
 *   todus:privacy, todus:group:privacy → privacy settings
 *   todus:block:set, todus:block:unset:2, todus:block:get:2, todus:block:get:3
 *   todus:last:2  → last seen
 *   todus:fb:push → Firebase push registration
 *   urn:xmpp:ping → keepalive (standard)
 *   urn:xmpp:mam:1 → message archive (MAM)
 */
object Ns {
    const val JC = "jc"
    const val STREAM = "x1"
    const val SASL = "x2"
    const val BIND = "x4"
    const val KEYS = "x8"
    const val GROUP_INFO = "x10"
    const val GROUP_MEMBERS = "x11"
    const val GROUP_LEAVE = "x13"
    const val GROUP_INVITE = "x14"
    const val GROUP_CREATE = "x16"
    const val CHAT_STATE = "uc1"
    const val REPLY = "reply:n"
    const val RESEND = "resend:n"
    const val EDITED = "edited:n"
    const val DELETED = "deleted:n"
    const val IMAGE = "image:n"
    const val VIDEO = "video:n"
    const val VOICE = "voice:n"
    const val FILE = "file:n"
    const val GIF = "gif:n"
    const val STICKER = "sticker:n"
    const val CONTACT = "contact:n"
    const val LOCATION = "location:n"
    const val EVENT = "event:n"
    const val BUTTON = "button:n"
    const val REACTION = "reaction:n"
    const val MENTION = "mention:n"
    const val STREAM_VIDEO = "streamvideo:n"
    const val TCALL = "tcall:n"

    const val OFFLINE = "t:offline"
    const val OFFLINE_DEL = "t:offline:del"
    const val OFFLINE_MARK = "todus_offline"
    const val OFFLINE_END = "todus_end_offline"

    const val PURL = "todus:purl"
    const val GURL = "todus:gurl"
    const val PRIVACY = "todus:privacy"
    const val GROUP_PRIVACY = "todus:group:privacy"

    const val BLOCK_SET = "todus:block:set"
    const val BLOCK_UNSET = "todus:block:unset:2"
    const val BLOCK_GET = "todus:block:get:2"
    const val BLOCK_GET_PAGED = "todus:block:get:3"

    const val LAST = "todus:last:2"
    const val FB_PUSH = "todus:fb:push"

    const val PING = "urn:xmpp:ping"
    const val MAM = "urn:xmpp:mam:1"
    const val MUC = "http://jabber.org/protocol/muc"
    const val RSM = "http://jabber.org/protocol/rsm"
    const val DATA = "jabber:x:data"

    // SASL mechanism element local name.
    const val SASL_AUTH_ELEMENT = "ah"
    const val SASL_AUTH_NS = "ah:ns"
}

/**
 * Abbreviated attribute names. toDus replaces standard XMPP attrs with one/two letters.
 *   to   → o
 *   from → f
 *   id   → i
 *   type → t
 * The parser accepts both forms for legacy compatibility.
 */
object Attr {
    const val TO = "o"
    const val TO_LEGACY = "to"
    const val FROM = "f"
    const val FROM_LEGACY = "from"
    const val ID = "i"
    const val ID_LEGACY = "id"
    const val TYPE = "t"
    const val TYPE_LEGACY = "type"
}

/**
 * Element local names in the abbreviated dialect.
 */
object El {
    const val MESSAGE = "m"
    const val BODY = "b"
    const val PRESENCE = "p"
    const val IQ = "iq"
    const val QUERY = "query"
    const val KEY = "k"            // <k xmlns='x8'/> marker on outgoing chat messages
    const val DELIVERY = "dd"      // <dd xmlns='x8' i='...'/>  → delivery receipt
    const val READ = "rd"          // <rd xmlns='x8' i='...'/>  → read receipt
    const val ACK = "ak"           // <ak xmlns='x8' i='...' o='...'/>  → protocol ack
    const val STREAM = "stream:stream"
    const val FEATURES = "stream:features"
}

/**
 * Message type values used in the `t=` attribute.
 */
object MsgType {
    const val CHAT = "c"           // 1:1 chat
    const val GROUP = "gc"         // MUC Light group chat
    const val ERROR = "error"
}

/**
 * Chat-state elements (XEP-0085 equivalent in the toDus dialect).
 * IMPORTANT (guía.txt §4.20): the mapping is INVERTED compared to legacy SDKs.
 *   csp → composing   (was paused in old SDKs)
 *   csc → paused      (was composing in old SDKs)
 *   csa → active
 *   csi → inactive
 *   csg → gone
 */
object ChatState {
    const val COMPOSING = "csp"
    const val PAUSED = "csc"
    const val ACTIVE = "csa"
    const val INACTIVE = "csi"
    const val GONE = "csg"
}
