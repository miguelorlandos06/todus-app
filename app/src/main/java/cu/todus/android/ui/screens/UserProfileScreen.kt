package cu.todus.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cu.todus.android.ui.components.Avatar
import cu.todus.android.ui.theme.TextPrimary
import cu.todus.android.ui.theme.TextSecondary
import cu.todus.android.ui.theme.TodusRed
import cu.todus.android.ui.viewmodel.ProfileViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileScreen(
    phone: String,
    vm: ProfileViewModel,
    onBack: () -> Unit,
) {
    // For v0.0.1 we show the phone-derived info; in a real client we'd query the user's profile.
    Scaffold(
        containerColor = Color.White,
        topBar = {
            TopAppBar(
                title = { Text("Perfil", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White),
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(24.dp))
            Avatar(name = phone, photoUrl = null, size = 120)
            Spacer(Modifier.height(12.dp))
            Text(phone, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Spacer(Modifier.height(6.dp))
            Text("En línea", fontSize = 13.sp, color = TodusRed)
            Spacer(Modifier.height(16.dp))
            ProfileRow("Info", "(sin info)")
            ProfileRow("toDusID", "(no asignado)")
            ProfileRow("Número", phone)
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = { /* block / unblock */ },
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = TodusRed),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
            ) {
                Text("Bloquear", color = Color.White)
            }
        }
    }
}

@Composable
private fun ProfileRow(label: String, value: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
    ) {
        Text(label, fontSize = 12.sp, color = TextSecondary)
        Text(value, fontSize = 15.sp, color = TextPrimary)
    }
}
