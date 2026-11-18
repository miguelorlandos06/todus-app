package cu.todus.android.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cu.todus.android.ui.theme.CheckDelivered
import cu.todus.android.ui.theme.CheckRead
import cu.todus.android.ui.theme.CheckSent
import cu.todus.android.ui.theme.OfflineRed
import cu.todus.android.ui.theme.OnlineGreen
import cu.todus.android.ui.theme.TodusRed

/**
 * Initials avatar (no real photo). Shows 1-2 uppercase initials on a red disc.
 * When [photoUrl] is set we'd load via Coil; v0.0.1 keeps it as initials.
 */
@Composable
fun Avatar(
    name: String,
    photoUrl: String?,
    size: Int = 48,
    modifier: Modifier = Modifier,
) {
    val initials = remember(name) { initialsOf(name) }
    Box(
        modifier = modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(TodusRed),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initials,
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
            fontSize = (size * 0.4f).sp,
        )
    }
}

private fun initialsOf(name: String): String {
    val parts = name.trim().split(' ').filter { it.isNotEmpty() }
    if (parts.isEmpty()) return "?"
    return if (parts.size == 1) parts[0].take(2).uppercase()
    else (parts[0].take(1) + parts[1].take(1)).uppercase()
}

/**
 * Status dot on the avatar: green when XMPP is connected, red when offline.
 * Drawn as a small overlay circle with a white border to mimic WhatsApp.
 */
@Composable
fun StatusDot(connected: Boolean, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(12.dp)
            .clip(CircleShape)
            .background(Color.White)
        ,
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(9.dp)
                .clip(CircleShape)
                .background(if (connected) OnlineGreen else OfflineRed)
        )
    }
}

/**
 * Message status icon: clock (sending), single check (sent), double check (delivered),
 * double check red (read). Per guía.txt §4.21.
 */
@Composable
fun MessageStatusIcon(status: Int, modifier: Modifier = Modifier) {
    when (status) {
        0 -> Icon(Icons.Default.Schedule, contentDescription = "Enviando",
            tint = CheckSent, modifier = modifier.size(14.dp))
        1 -> Icon(Icons.Default.Check, contentDescription = "Enviado",
            tint = CheckSent, modifier = modifier.size(14.dp))
        2 -> Icon(Icons.Default.DoneAll, contentDescription = "Entregado",
            tint = CheckDelivered, modifier = modifier.size(14.dp))
        3 -> Icon(Icons.Default.DoneAll, contentDescription = "Leído",
            tint = CheckRead, modifier = modifier.size(14.dp))
    }
}

/**
 * The Home app-bar status label: "Esperando red…", "Conectando…", "Actualizando…", or "toDus".
 * The three dots animate (alpha cycling) for the intermediate states, and "toDus" is shown
 * in toDus-red once connected. Per the UX spec the transition uses a fade.
 */
@Composable
fun ConnectionStatusBar(connected: Boolean, stateLabel: String, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "dots")
    val alpha by transition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "alpha",
    )
    Text(
        text = stateLabel,
        modifier = modifier.alpha(if (connected) 1f else alpha),
        color = if (connected) TodusRed else MaterialTheme.colorScheme.onSurface,
        fontWeight = if (connected) FontWeight.Bold else FontWeight.Normal,
        fontSize = 18.sp,
    )
}
