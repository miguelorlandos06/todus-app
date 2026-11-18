package cu.todus.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import cu.todus.android.ui.theme.TextSecondary
import cu.todus.android.ui.theme.TodusRed
import cu.todus.android.ui.viewmodel.ProfileViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangeTodusIdScreen(
    vm: ProfileViewModel,
    onBack: () -> Unit,
    onChanged: () -> Unit,
) {
    val form by vm.form.collectAsState()
    val saved by vm.saved.collectAsState()
    var newId by remember(form.todusId) { mutableStateOf(form.todusId) }

    LaunchedEffect(saved) {
        if (saved) {
            onChanged()
            vm.resetSaved()
        }
    }

    Scaffold(
        containerColor = Color.White,
        topBar = {
            TopAppBar(
                title = { Text("Cambiar toDusID", fontWeight = FontWeight.SemiBold) },
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
        ) {
            Spacer(Modifier.height(24.dp))
            Text("toDusID", fontSize = 12.sp, color = TextSecondary, modifier = Modifier.padding(bottom = 4.dp))
            OutlinedTextField(
                value = newId,
                onValueChange = { newId = it },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TodusRed,
                    unfocusedBorderColor = Color(0xFFE0E0E0),
                ),
            )
            Spacer(Modifier.weight(1f))
            Button(
                onClick = { vm.changeTodusId(newId) },
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = TodusRed),
                enabled = newId.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .padding(bottom = 32.dp),
            ) {
                Text("Cambiar", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}
