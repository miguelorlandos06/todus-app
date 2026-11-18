package cu.todus.android.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cu.todus.android.data.db.ChatEntity
import cu.todus.android.protocol.xmpp.ConnectionState
import cu.todus.android.protocol.xmpp.XmppStatus
import cu.todus.android.ui.components.Avatar
import cu.todus.android.ui.components.ConnectionStatusBar
import cu.todus.android.ui.components.MessageStatusIcon
import cu.todus.android.ui.components.StatusDot
import cu.todus.android.ui.theme.TextPrimary
import cu.todus.android.ui.theme.TextSecondary
import cu.todus.android.ui.theme.TodusRed
import cu.todus.android.ui.viewmodel.HomeViewModel

/**
 * Home screen — the hub of the app. Per UX spec:
 *   - Top bar: connection status label (left, animated dots when not connected, red "toDus" when
 *     connected), my avatar + name + XMPP status dot (right).
 *   - Body: recent chats list. Left: avatar. Middle: contact name (top) + last message
 *     (bottom, with status icon if from-me, or "Escribiendo…" if peer is composing). Right:
 *     time (top) + unread count (bottom, centered).
 *   - Bottom-right: rounded button "Nuevo Chat" with a square message bubble icon.
 */
@Composable
fun HomeScreen(
    vm: HomeViewModel,
    onNewChat: () -> Unit,
    onChatClick: (String) -> Unit,
    onMyProfile: () -> Unit,
) {
    val chats by vm.chats.collectAsState()
    val state by vm.xmpp.state.collectAsState()
    val xmppConnected by vm.xmppConnected.collectAsState()
    val name by vm.sessionStore.profileNameFlow.collectAsState(initial = null)

    val stateLabel = when (state) {
        is ConnectionState.WaitingForNetwork -> "Esperando red…"
        is ConnectionState.Connecting -> "Conectando…"
        is ConnectionState.Updating -> "Actualizando…"
        is ConnectionState.Connected -> "toDus"
        is ConnectionState.Disconnected -> "Esperando red…"
    }

    Scaffold(
        containerColor = Color.White,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .statusBarsPadding(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ConnectionStatusBar(
                    connected = state is ConnectionState.Connected,
                    stateLabel = stateLabel,
                )
                Spacer(Modifier.weight(1f))
                // Avatar with status dot (clickable → My Profile)
                Box(
                    modifier = Modifier.clickableTodus { onMyProfile() },
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (!name.isNullOrBlank()) {
                            Text(
                                name ?: "",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = TextPrimary,
                                modifier = Modifier.padding(end = 8.dp),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Box(contentAlignment = Alignment.BottomEnd) {
                            Avatar(
                                name = name ?: "?",
                                photoUrl = null,
                                size = 36,
                                modifier = Modifier.clickableTodus { onMyProfile() },
                            )
                            StatusDot(
                                connected = xmppConnected,
                                modifier = Modifier.padding(2.dp).align(Alignment.BottomEnd),
                            )
                        }
                    }
                }
            }
            Divider()
        },
        floatingActionButton = {
            // "Nuevo Chat" button — rounded rectangle with a square bubble icon + label.
            ExtendedFloatingActionButton(
                text = { Text("Nuevo Chat", color = Color.White) },
                icon = { Icon(Icons.Default.Add, contentDescription = "Nuevo chat", tint = Color.White) },
                onClick = onNewChat,
                containerColor = TodusRed,
                shape = RoundedCornerShape(28.dp),
            )
        },
    ) { padding ->
        if (chats.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text("No tienes conversaciones todavía.", color = TextSecondary, fontSize = 15.sp)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Toca \"Nuevo Chat\" para empezar a chatear con tus contactos.",
                    color = TextSecondary,
                    fontSize = 13.sp,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                items(chats, key = { it.phone }) { chat ->
                    ChatRow(
                        chat = chat,
                        onClick = { onChatClick(chat.phone) },
                    )
                    Divider(modifier = Modifier.padding(start = 80.dp))
                }
            }
        }
    }
}

@Composable
private fun ChatRow(chat: ChatEntity, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickableTodus(onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Avatar(name = chat.displayName, photoUrl = chat.photoUrl, size = 56)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                chat.displayName,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (chat.isTyping) {
                    Text(
                        "Escribiendo…",
                        fontSize = 13.sp,
                        color = TodusRed,
                        fontWeight = FontWeight.Medium,
                    )
                } else {
                    if (chat.lastMessageFromMe) {
                        MessageStatusIcon(chat.lastMessageStatus, Modifier.padding(end = 4.dp))
                    }
                    Text(
                        chat.lastMessageBody.ifBlank { "" },
                        fontSize = 13.sp,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                }
            }
        }
        Spacer(Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(formatTime(chat.lastMessageTimestamp), fontSize = 12.sp, color = TextSecondary)
            if (chat.unreadCount > 0) {
                Box(
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .size(20.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(TodusRed),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        chat.unreadCount.toString(),
                        fontSize = 11.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

private fun formatTime(timestampMs: Long): String {
    val now = java.util.Calendar.getInstance()
    val time = java.util.Calendar.getInstance().apply { timeInMillis = timestampMs }
    val isToday = now.get(java.util.Calendar.YEAR) == time.get(java.util.Calendar.YEAR) &&
        now.get(java.util.Calendar.DAY_OF_YEAR) == time.get(java.util.Calendar.DAY_OF_YEAR)
    return if (isToday) {
        java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date(timestampMs))
    } else {
        java.text.SimpleDateFormat("dd/MM", java.util.Locale.getDefault()).format(java.util.Date(timestampMs))
    }
}
