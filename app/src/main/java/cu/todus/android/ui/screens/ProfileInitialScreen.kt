package cu.todus.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cu.todus.android.ui.components.Avatar
import cu.todus.android.ui.theme.TodusRed
import cu.todus.android.ui.viewmodel.AuthViewModel

/**
 * Initial profile screen (per UX spec):
 *   - Large centered avatar with a camera overlay in the bottom-right corner (opens the gallery).
 *   - Below: a "Nombre" field label (small, top-left) and an editable text field for the name.
 *   - Bottom: "Comenzar" button.
 *   - On open, the app fetches the user's profile from the server (here simulated by reading the
 *     saved phone as a fallback name).
 */
@Composable
fun ProfileInitialScreen(
    onStart: () -> Unit,
    vm: AuthViewModel,
) {
    var name by remember { mutableStateOf("") }

    // Auto-load the profile name from the session (in a real client we'd call the server here).
    LaunchedEffect(Unit) {
        vm.sessionStore.profileNameFlow.collect { saved ->
            if (saved != null && name.isBlank()) name = saved
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(64.dp))
        // Avatar with camera overlay
        Box(contentAlignment = Alignment.BottomEnd) {
            Avatar(name = name.ifBlank { "?" }, photoUrl = null, size = 120)
            Box(
                Modifier
                    .size(36.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .background(TodusRed)
                    .padding(6.dp),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.CameraAlt, contentDescription = "Cambiar foto", tint = Color.White)
            }
        }

        Spacer(Modifier.weight(1f))

        // Name field with small label
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                "Nombre",
                fontSize = 12.sp,
                color = Color(0xFF6B6B6B),
                modifier = Modifier.padding(start = 4.dp, bottom = 4.dp),
            )
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TodusRed,
                    unfocusedBorderColor = Color(0xFFE0E0E0),
                ),
            )
        }

        Spacer(Modifier.weight(1f))

        Button(
            onClick = {
                if (name.isNotBlank()) {
                    vm.sessionStore.saveProfile(name, info = null, photo = null, toodusId = null)
                }
                onStart()
            },
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = TodusRed),
            enabled = name.isNotBlank(),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .padding(bottom = 32.dp),
        ) {
            Text("Comenzar", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
        }
    }
}
