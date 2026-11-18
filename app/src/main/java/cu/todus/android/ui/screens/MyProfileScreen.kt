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
fun MyProfileScreen(
    vm: ProfileViewModel,
    onBack: () -> Unit,
    onEdit: () -> Unit,
) {
    val form by vm.form.collectAsState()

    Scaffold(
        containerColor = Color.White,
        topBar = {
            TopAppBar(
                title = { Text("Mi Perfil", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    TextButton(onClick = onEdit) {
                        Text("Editar Perfil", color = TodusRed)
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
            Avatar(name = form.name.ifBlank { "?" }, photoUrl = form.photoUrl, size = 120)
            Spacer(Modifier.height(16.dp))
            ProfileField("Nombre", form.name)
            Spacer(Modifier.height(16.dp))
            ProfileField("Info", form.info.ifBlank { "(sin info)" })
            Spacer(Modifier.height(16.dp))
            ProfileField("toDusID", form.todusId.ifBlank { "(no asignado)" })
        }
    }
}

@Composable
private fun ProfileField(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(label, fontSize = 12.sp, color = TextSecondary)
        Text(value, fontSize = 16.sp, color = TextPrimary, fontWeight = FontWeight.Medium)
        Divider(Modifier.padding(top = 6.dp))
    }
}
