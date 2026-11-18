package cu.todus.android.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cu.todus.android.data.db.MessageEntity
import cu.todus.android.ui.components.Avatar
import cu.todus.android.ui.components.MessageStatusIcon
import cu.todus.android.ui.theme.*
import cu.todus.android.ui.viewmodel.ConversationViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

/**
 * Conversation screen. Per UX spec:
 *   - Top bar: back, peer avatar + name + status (En línea / Escribiendo… / última conexión).
 *   - Body: bubble list — received bubbles red on the left, sent bubbles white on the right.
 *     Each bubble shows the time + (if sent) a status icon at the bottom-right corner.
 *   - Bottom: input container + paper-plane send button.
 *   - Swipe right on a received bubble reveals a reply preview above the input.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationScreen(
    peerPhone: String,
    vm: ConversationViewModel,
    onBack: () -> Unit,
    onProfileClick: () -> Unit,
) {
    val messages by vm.messages(peerPhone).collectAsState()
    val contact by vm.contact(peerPhone).collectAsState()
    val xmppConnected by vm.xmppConnected.collectAsState()

    var input by remember { mutableStateOf("") }
    var replyTo by remember { mutableStateOf<MessageEntity?>(null) }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex)
        vm.markRead(peerPhone)
    }

    Scaffold(
        containerColor = Color(0xFFF5F5F5),
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                }
                Box(modifier = Modifier.clickableTodus { onProfileClick() }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Avatar(name = contact?.name ?: peerPhone, photoUrl = contact?.photoUrl, size = 40)
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                contact?.name ?: peerPhone,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            val statusText = when {
                                contact?.typing == true -> "Escribiendo…"
                                contact?.online == true -> "En línea"
                                contact != null && contact.lastSeenSeconds > 0 ->
                                    "últ. vez ${formatLastSeen(contact.lastSeenSeconds)}"
                                !xmppConnected -> "Esperando red…"
                                else -> "En línea"
                            }
                            Text(
                                statusText,
                                fontSize = 12.sp,
                                color = if (contact?.typing == true) TodusRed else TextSecondary,
                            )
                        }
                    }
                }
            }
        },
        bottomBar = {
            Column(modifier = Modifier.background(Color.White)) {
                // Reply preview
                AnimatedVisibility(visible = replyTo != null) {
                    if (replyTo != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFF5F5F5))
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                Modifier
                                    .width(3.dp)
                                    .height(36.dp)
                                    .background(TodusRed)
                                    .padding(end = 8.dp),
                            )
                            Column(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
                                Text("Respondiendo a", fontSize = 11.sp, color = TodusRed, fontWeight = FontWeight.Bold)
                                Text(
                                    replyTo!!.body.ifBlank { "(mensaje sin texto)" },
                                    fontSize = 13.sp,
                                    color = TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            IconButton(onClick = { replyTo = null }) {
                                Icon(Icons.Default.Cancel, contentDescription = "Cancelar respuesta")
                            }
                        }
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                        .navigationBarsPadding(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value = input,
                        onValueChange = {
                            input = it
                            vm.onTyping(peerPhone)
                        },
                        placeholder = { Text("Escribe un mensaje") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFE0E0E0),
                            unfocusedBorderColor = Color(0xFFE0E0E0),
                        ),
                    )
                    Spacer(Modifier.width(8.dp))
                    FloatingActionButton(
                        onClick = {
                            if (input.isNotBlank()) {
                                vm.send(peerPhone, input.trim(), replyToId = replyTo?.id)
                                input = ""
                                replyTo = null
                            }
                        },
                        containerColor = TodusRed,
                        shape = RoundedCornerShape(24.dp),
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Enviar", tint = Color.White)
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 12.dp),
            contentPadding = PaddingValues(vertical = 8.dp),
        ) {
            items(messages, key = { it.id }) { msg ->
                MessageBubble(
                    msg = msg,
                    onSwipeRight = { replyTo = msg },
                )
                Spacer(Modifier.height(6.dp))
            }
        }
    }
}

@Composable
private fun MessageBubble(
    msg: MessageEntity,
    onSwipeRight: () -> Unit,
) {
    val bubbleColor = if (msg.fromMe) ChatBubbleOut else ChatBubbleIn
    val textColor = if (msg.fromMe) ChatBubbleOutText else ChatBubbleInText
    val alignment = if (msg.fromMe) Alignment.End else Alignment.Start

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = alignment,
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 12.dp,
                        topEnd = 12.dp,
                        bottomStart = if (msg.fromMe) 12.dp else 4.dp,
                        bottomEnd = if (msg.fromMe) 4.dp else 12.dp,
                    )
                )
                .background(bubbleColor)
                .pointerInput(msg.id) {
                    detectHorizontalDragGestures { _, dragAmount ->
                        if (dragAmount > 20) onSwipeRight()
                    }
                }
                .padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            Column {
                if (msg.replyToId != null) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .background(Color.Black.copy(alpha = 0.06f))
                            .padding(6.dp),
                    ) {
                        Text("(respondiendo)", fontSize = 11.sp, color = textColor.copy(alpha = 0.8f))
                    }
                    Spacer(Modifier.height(4.dp))
                }
                Text(
                    text = msg.body.ifBlank {
                        if (msg.deletedId != null) "Mensaje eliminado" else ""
                    },
                    fontSize = 15.sp,
                    color = textColor,
                )
                Row(
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .align(Alignment.End),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(msg.timestamp)),
                        fontSize = 10.sp,
                        color = textColor.copy(alpha = 0.7f),
                    )
                    if (msg.fromMe) {
                        Spacer(Modifier.width(4.dp))
                        MessageStatusIcon(msg.status)
                    }
                }
            }
        }
    }
}

private fun formatLastSeen(seconds: Long): String {
    val now = System.currentTimeMillis() / 1000
    val diff = now - seconds
    return when {
        diff < 60 -> "hace un momento"
        diff < 3600 -> "hace ${diff / 60} min"
        diff < 86_400 -> "hace ${diff / 3600} h"
        else -> SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(seconds * 1000))
    }
}
