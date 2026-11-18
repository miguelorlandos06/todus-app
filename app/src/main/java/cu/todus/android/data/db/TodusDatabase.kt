package cu.todus.android.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

// ----- Entities -----

@Entity(tableName = "contacts")
data class ContactEntity(
    @PrimaryKey val phone: String,
    val name: String,
    val info: String = "",
    val photoUrl: String? = null,
    val toodusId: String? = null,
    val isBlocked: Boolean = false,
    val lastSeenSeconds: Long = 0,
    val online: Boolean = false,
    val typing: Boolean = false,
)

@Entity(tableName = "chats", foreignKeys = [
    ForeignKey(entity = ContactEntity::class, parentColumns = ["phone"], childColumns = ["phone"], onDelete = ForeignKey.CASCADE)
])
data class ChatEntity(
    @PrimaryKey val phone: String,
    val displayName: String,
    val photoUrl: String?,
    val lastMessageBody: String,
    val lastMessageTimestamp: Long,
    val lastMessageFromMe: Boolean,
    val lastMessageStatus: Int,    // 0 sending, 1 sent, 2 delivered, 3 read
    val unreadCount: Int,
    val isTyping: Boolean,
    val isMuted: Boolean = false,
)

@Entity(
    tableName = "messages",
    indices = [Index("peerPhone"), Index(value = ["peerPhone", "timestamp"])],
    foreignKeys = [
        ForeignKey(entity = ContactEntity::class, parentColumns = ["phone"], childColumns = ["peerPhone"], onDelete = ForeignKey.CASCADE)
    ]
)
data class MessageEntity(
    @PrimaryKey val id: String,
    val peerPhone: String,
    val fromMe: Boolean,
    val body: String,
    val timestamp: Long,
    val status: Int,         // 0 sending, 1 sent, 2 delivered, 3 read
    val replyToId: String? = null,
    val editedId: String? = null,
    val deletedId: String? = null,
)

// ----- DAOs -----

@Dao
interface ContactDao {
    @Query("SELECT * FROM contacts ORDER BY name COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<ContactEntity>>

    @Query("SELECT * FROM contacts WHERE phone = :phone")
    suspend fun get(phone: String): ContactEntity?

    @Query("SELECT * FROM contacts WHERE phone = :phone")
    fun observe(phone: String): Flow<ContactEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(contact: ContactEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(contacts: List<ContactEntity>)

    @Query("UPDATE contacts SET isBlocked = :blocked WHERE phone = :phone")
    suspend fun setBlocked(phone: String, blocked: Boolean)

    @Query("UPDATE contacts SET online = :online WHERE phone = :phone")
    suspend fun setOnline(phone: String, online: Boolean)

    @Query("UPDATE contacts SET typing = :typing WHERE phone = :phone")
    suspend fun setTyping(phone: String, typing: Boolean)

    @Query("UPDATE contacts SET lastSeenSeconds = :secs WHERE phone = :phone")
    suspend fun setLastSeen(phone: String, secs: Long)

    @Query("DELETE FROM contacts WHERE phone = :phone")
    suspend fun delete(phone: String)
}

@Dao
interface ChatDao {
    @Query("""
        SELECT * FROM chats
        ORDER BY (CASE WHEN isTyping = 1 THEN 1 ELSE 0 END) DESC,
                 lastMessageTimestamp DESC
    """)
    fun observeAll(): Flow<List<ChatEntity>>

    @Query("SELECT * FROM chats WHERE phone = :phone")
    fun observe(phone: String): Flow<ChatEntity?>

    @Query("SELECT * FROM chats WHERE phone = :phone")
    suspend fun get(phone: String): ChatEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(chat: ChatEntity)

    @Query("UPDATE chats SET unreadCount = 0 WHERE phone = :phone")
    suspend fun clearUnread(phone: String)

    @Query("UPDATE chats SET isTyping = :typing WHERE phone = :phone")
    suspend fun setTyping(phone: String, typing: Boolean)

    @Query("UPDATE chats SET lastMessageStatus = :status WHERE phone = :phone")
    suspend fun updateLastStatus(phone: String, status: Int)

    @Query("DELETE FROM chats WHERE phone = :phone")
    suspend fun delete(phone: String)
}

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE peerPhone = :phone ORDER BY timestamp ASC")
    fun observeForPeer(phone: String): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE id = :id")
    suspend fun get(id: String): MessageEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(message: MessageEntity)

    @Query("UPDATE messages SET status = :status WHERE id = :id")
    suspend fun setStatus(id: String, status: Int)

    @Query("UPDATE messages SET body = :body, editedId = :editId WHERE id = :id")
    suspend fun edit(id: String, body: String, editId: String)

    @Query("UPDATE messages SET body = '', deletedId = :delId WHERE id = :id")
    suspend fun delete(id: String, delId: String)

    @Query("SELECT COUNT(*) FROM messages WHERE peerPhone = :phone AND fromMe = 0 AND status < 3")
    fun unreadCount(phone: String): Flow<Int>
}

// ----- Database -----

@Database(
    entities = [ContactEntity::class, ChatEntity::class, MessageEntity::class],
    version = 1,
    exportSchema = false
)
abstract class TodusDatabase : RoomDatabase() {
    abstract fun contactDao(): ContactDao
    abstract fun chatDao(): ChatDao
    abstract fun messageDao(): MessageDao
}
