package cu.todus.android.protocol

/**
 * Hardcoded protocol constants extracted from the decompiled toDus APK
 * (constants/Service*.smali) and the guía.txt spec.
 *
 * Endpoints (production):
 *   auth.todus.cu       → HTTP authentication (protobuf wire-format).
 *   im.todus.cu:1756    → XMPP-over-TLS (main session port).
 *   im.todus.cu:5443    → XMPP alternate.
 *   im.todus.cu:5222    → XMPP legacy (obsoleto).
 *   muclight.im.todus.cu → MUC Light service.
 *   ch.im.todus.cu      → channels.
 *   status.im.todus.cu  → stories/estados.
 *   s3.todus.cu         → media storage.
 *
 * XMPP uses abbreviated attributes and namespaces — see [Ns] and [Attr].
 */
object TodusConstants {
    // ---- HTTP auth ----
    const val AUTH_BASE_URL = "https://auth.todus.cu/"
    const val AUTH_VERSION_NAME = "2.1.1"           // UA used by the phone-only login path
    const val AUTH_VERSION_CODE = "30102"
    const val AUTH_VERSION_NAME_FULL = "2.1.2"

    const val PATH_TOKEN = "v2/auth/token"
    const val PATH_USERS_RESERVE = "v2/auth/users.reserve"
    const val PATH_USERS_REGISTER = "v2/auth/users.register"
    const val PATH_CHECK = "v2/auth/check"
    const val PATH_UPDATE_PROFILE = "v2/todus/users.me"

    // Hardcoded weak secret accepted by the server when authenticating by phone only.
    // See guía.txt §2.2 — "fake1234567890abcdef12345678" (32 hex chars).
    const val PHONE_ONLY_SECRET = "fake1234567890abcdef12345678"

    // ---- XMPP ----
    const val IM_HOST = "im.todus.cu"
    const val IM_PORT = 1756
    const val IM_PORT_ALT = 5443
    const val IM_PORT_LEGACY = 5222
    const val SERVER_DOMAIN = "im.todus.cu"
    const val MUC_LIGHT_SERVICE = "muclight.im.todus.cu"
    const val CHANNEL_SERVICE = "ch.im.todus.cu"
    const val STATUS_RECEPTOR_JID = "status@status.im.todus.cu"
    const val BLOCKS_SERVICE_JID = "blocks@blocks.im.todus.cu"
    const val NEAR_SERVICE_JID = "near@near.im.todus.cu"

    // ---- Limits (from Service$Config.smali) ----
    const val MAX_MESSAGE_TEXT_SIZE = 0x2000        // 8 KiB
    const val MAX_MESSAGE_LENGTH = 0x10000         // 64 KiB (binary payloads)
    const val COUNTER_VISIBLE_THRESHOLD = 0xC8      // 200

    // ---- Resource/URL helpers ----
    const val S3_URL = "https://s3.todus.cu/"
    const val STICKERS_URL = "https://stickers.todus.cu/"
    const val CHAT_URL = "https://chat.todus.cu/"
    const val CLIENT_URL = "https://client.todus.cu/"
    const val TERMS_URL = "https://todus.cu/terms.html"

    // ---- XMPP keepalive ----
    const val PING_INTERVAL_SECONDS = 30L
    const val RECONNECT_BASE_BACKOFF_MS = 15_000L
    const val RECONNECT_MAX_BACKOFF_MS = 300_000L
}
