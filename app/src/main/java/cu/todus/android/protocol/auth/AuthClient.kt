package cu.todus.android.protocol.auth

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import cu.todus.android.protocol.TodusConstants
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * toDus authentication client (HTTP/protobuf against auth.todus.cu).
 *
 * Implements the login-with-phone-only flow described in guía.txt §2.2: the server accepts a
 * hard-coded secret in place of the real password, so a JWT can be obtained with just the
 * phone number (no SMS). Falls back to the full reserve → register → token flow if the
 * server ever patches the weakness.
 */
@Singleton
class AuthClient @Inject constructor() {

    private val http: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    /** 150-char random installation UUID (matches the official SDK length). */
    fun newInstallUuid(): String = UUID.randomUUID().toString().replace("-", "").let {
        if (it.length < 150) it + "0".repeat(150 - it.length) else it.take(150)
    }

    /**
     * Phone-only login: gets a JWT using the known weak secret.
     * Returns the JWT or null on failure.
     */
    suspend fun loginWithPhoneOnly(phone: String): Result<String> = runCatching {
        val payload = AuthTokenPhoneOnlyRequest(
            phone = phone,
            secret = TodusConstants.PHONE_ONLY_SECRET,
        ).encode()

        val body = post(
            path = TodusConstants.PATH_TOKEN,
            payload = payload,
            contentType = "application/octet-stream",
            userAgent = "ToDus ${TodusConstants.AUTH_VERSION_NAME}",
        )
        AuthTokenResponse.decode(body).token
    }

    /**
     * Full reserve → register → token flow (used if phone-only is patched).
     */
    suspend fun requestSmsCode(phone: String, installUuid: String): Result<Unit> = runCatching {
        val payload = AuthReserveRequest(phone, installUuid).encode()
        post(
            path = TodusConstants.PATH_USERS_RESERVE,
            payload = payload,
            contentType = "application/x-protobuf",
            userAgent = "ToDus ${TodusConstants.AUTH_VERSION_NAME_FULL} Auth",
        )
        Unit
    }

    suspend fun validateSmsCode(
        phone: String,
        installUuid: String,
        code: String,
    ): Result<String> = runCatching {
        val payload = AuthRegisterRequest(phone, installUuid, code).encode()
        val body = post(
            path = TodusConstants.PATH_USERS_REGISTER,
            payload = payload,
            contentType = "application/x-protobuf",
            userAgent = "ToDus ${TodusConstants.AUTH_VERSION_NAME_FULL} Auth",
        )
        AuthRegisterResponse.decode(body).password
    }

    suspend fun login(
        phone: String,
        password: String,
        installUuid: String,
    ): Result<String> = runCatching {
        val payload = AuthTokenRequest(phone, password, installUuid).encode()
        val body = post(
            path = TodusConstants.PATH_TOKEN,
            payload = payload,
            contentType = "application/x-protobuf",
            userAgent = "ToDus ${TodusConstants.AUTH_VERSION_NAME_FULL} Auth",
        )
        AuthTokenResponse.decode(body).token
    }

    private fun post(
        path: String,
        payload: ByteArray,
        contentType: String,
        userAgent: String,
    ): ByteArray {
        val url = TodusConstants.AUTH_BASE_URL + path
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", userAgent)
            .header("Content-Type", contentType)
            .header("Accept", "application/octet-stream")
            .post(payload.toRequestBody(contentType.toMediaType()))
            .build()
        http.newCall(request).execute().use { resp ->
            val bytes = resp.body?.bytes() ?: ByteArray(0)
            if (!resp.isSuccessful) {
                throw AuthException("HTTP ${resp.code} on $path", resp.code, bytes)
            }
            return bytes
        }
    }
}

class AuthException(message: String, val code: Int, val body: ByteArray) : Exception(message)
