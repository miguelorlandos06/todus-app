package cu.todus.android.protocol.auth

/**
 * Auth request/response DTOs. Field numbers come from guía.txt §2.
 *
 *   POST /v2/auth/users.reserve   { 1: phone, 2: install_uuid }
 *   POST /v2/auth/users.register  { 1: phone, 2: install_uuid, 3: code }
 *     → returns field 1 = password (96 chars hex)
 *   POST /v2/auth/token           { 1: phone, 2: password, 3: install_uuid, 4: version_code }
 *     → returns field 1 = JWT
 *   POST /v2/auth/token (phone-only path) { 1: phone, 2: PHONE_ONLY_SECRET }
 *     → returns field 1 = JWT (server accepts any UUID as password)
 */
data class AuthTokenRequest(
    val phone: String,
    val password: String,
    val installUuid: String,
    val versionCode: String = "30102",
) {
    fun encode(): ByteArray = PbMsg()
        .str(1, phone)
        .str(2, password)
        .str(3, installUuid)
        .str(4, versionCode)
        .build()
}

data class AuthTokenPhoneOnlyRequest(
    val phone: String,
    val secret: String,
) {
    fun encode(): ByteArray = PbMsg()
        .str(1, phone)
        .str(2, secret)
        .build()
}

data class AuthRegisterRequest(
    val phone: String,
    val installUuid: String,
    val code: String,
) {
    fun encode(): ByteArray = PbMsg()
        .str(1, phone)
        .str(2, installUuid)
        .str(3, code)
        .build()
}

data class AuthReserveRequest(
    val phone: String,
    val installUuid: String,
) {
    fun encode(): ByteArray = PbMsg()
        .str(1, phone)
        .str(2, installUuid)
        .build()
}

data class AuthTokenResponse(val token: String) {
    companion object {
        fun decode(data: ByteArray): AuthTokenResponse {
            val fields = PbReader.parse(data)
            val token = fields[1]?.toString(Charsets.UTF_8) ?: ""
            return AuthTokenResponse(token)
        }
    }
}

data class AuthRegisterResponse(val password: String) {
    companion object {
        fun decode(data: ByteArray): AuthRegisterResponse {
            val fields = PbReader.parse(data)
            val password = fields[1]?.toString(Charsets.UTF_8) ?: ""
            return AuthRegisterResponse(password)
        }
    }
}
