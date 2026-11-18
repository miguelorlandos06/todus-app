package cu.todus.android.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.todusDataStore by preferencesDataStore("todus_session")

/**
 * Persisted session: phone + JWT token + profile snapshot.
 * Stored in DataStore so it survives process death. The token is equivalent to a password
 * (see guía.txt §16.4) — we exclude this file from backup (xml/backup_rules.xml).
 */
@Singleton
class SessionStore @Inject constructor(
    private val context: Context,
) {
    private val ds = context.todusDataStore

    val phoneFlow: Flow<String?> = ds.data.map { it[KEY_PHONE] }
    val tokenFlow: Flow<String?> = ds.data.map { it[KEY_TOKEN] }
    val profileNameFlow: Flow<String?> = ds.data.map { it[KEY_NAME] }
    val profileInfoFlow: Flow<String?> = ds.data.map { it[KEY_INFO] }
    val profilePhotoFlow: Flow<String?> = ds.data.map { it[KEY_PHOTO] }
    val toodusIdFlow: Flow<String?> = ds.data.map { it[KEY_TODUS_ID] }
    val installUuidFlow: Flow<String?> = ds.data.map { it[KEY_INSTALL_UUID] }

    val isLoggedIn: Flow<Boolean> = ds.data.map { !it[KEY_TOKEN].isNullOrBlank() }

    suspend fun saveSession(phone: String, token: String) {
        ds.edit { it[KEY_PHONE] = phone; it[KEY_TOKEN] = token }
    }

    suspend fun saveProfile(name: String?, info: String?, photo: String?, toodusId: String?) {
        ds.edit {
            if (name != null) it[KEY_NAME] = name
            if (info != null) it[KEY_INFO] = info
            if (photo != null) it[KEY_PHOTO] = photo
            if (todusId != null) it[KEY_TODUS_ID] = toodusId
        }
    }

    suspend fun saveInstallUuid(uuid: String) = ds.edit { it[KEY_INSTALL_UUID] = uuid }

    suspend fun clear() = ds.edit { it.clear() }

    suspend fun snapshot(): SessionSnapshot {
        val p = ds.data.first()
        return SessionSnapshot(
            phone = p[KEY_PHONE],
            token = p[KEY_TOKEN],
            name = p[KEY_NAME],
            info = p[KEY_INFO],
            photo = p[KEY_PHOTO],
            toodusId = p[KEY_TODUS_ID],
            installUuid = p[KEY_INSTALL_UUID],
        )
    }

    companion object {
        private val KEY_PHONE = stringPreferencesKey("phone")
        private val KEY_TOKEN = stringPreferencesKey("token")
        private val KEY_NAME = stringPreferencesKey("name")
        private val KEY_INFO = stringPreferencesKey("info")
        private val KEY_PHOTO = stringPreferencesKey("photo")
        private val KEY_TODUS_ID = stringPreferencesKey("todus_id")
        private val KEY_INSTALL_UUID = stringPreferencesKey("install_uuid")
    }
}

data class SessionSnapshot(
    val phone: String?,
    val token: String?,
    val name: String?,
    val info: String?,
    val photo: String?,
    val toodusId: String?,
    val installUuid: String?,
)
