package cu.todus.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import cu.todus.android.ui.theme.TextSecondary
import cu.todus.android.ui.theme.TodusRed
import cu.todus.android.ui.viewmodel.ProfileViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    vm: ProfileViewModel,
    onBack: () -> Unit,
    onChangeTodusId: () -> Unit,
    onSaved: () -> Unit,
) {
    val form by vm.form.collectAsState()
    val saved by vm.saved.collectAsState()
    var name by remember(form.name) { mutableStateOf(form.name) }
    var info by remember(form.info) { mutableStateOf(form.info) }

    LaunchedEffect(saved) {
        if (saved) {
            onSaved()
            vm.resetSaved()
        }
    }

    Scaffold(
        containerColor = Color.White,
        topBar = {
            TopAppBar(
                title = { Text("Editar perfil", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    TextButton(onClick = {
                        vm.updateName(name)
                        vm.updateInfo(info)
                        vm.save()
                    }) {
                        Text("Guardar perfil", color = TodusRed)
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
            Box(contentAlignment = Alignment.BottomEnd) {
                Avatar(name = name.ifBlank { "?" }, photoUrl = form.photoUrl, size = 120)
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
            Spacer(Modifier.height(24.dp))
            EditField(
                label = "Nombre",
                value = name,
                onValueChange = { name = it },
            )
            Spacer(Modifier.height(16.dp))
            EditField(
                label = "Info",
                value = info,
                onValueChange = { info = it },
            )
            Spacer(Modifier.height(16.dp))
            // toDusID is read-only here — opening it goes to a separate screen.
            Text("toDusID", fontSize = 12.sp, color = TextSecondary, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(
                value = form.todusId.ifBlank { "(no asignado)" },
                onValueChange = {},
                readOnly = true,
                enabled = false,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickableTodus { onChangeTodusId() },
                shape = RoundedCornerShape(8.dp),
            )
        }
    }
}

@Composable
private fun EditField(label: String, value: String, onValueChange: (String) -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Text(label, fontSize = 12.sp, color = TextSecondary)
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = TodusRed,
                unfocusedBorderColor = Color(0xFFE0E0E0),
            ),
        )
    }
}
