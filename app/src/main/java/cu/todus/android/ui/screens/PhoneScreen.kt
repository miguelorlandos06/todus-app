package cu.todus.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cu.todus.android.ui.theme.TodusRed
import cu.todus.android.ui.viewmodel.AuthViewModel

/**
 * Phone entry screen. Layout (per UX spec):
 *   - Title "Número de teléfono" centered at the top
 *   - Subtitle "Por favor, ingresa tu número de teléfono para continuar"
 *   - Center: square container with the Cuban flag, "+53" code, and 8-digit phone field
 *   - Bottom: "Continuar" button
 */
@Composable
fun PhoneScreen(
    onContinue: () -> Unit,
    vm: AuthViewModel,
) {
    val state by vm.phoneState.collectAsState()
    var phone by remember { mutableStateOf("") }
    val isError = state is AuthViewModel.PhoneUiState.Error

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(40.dp))
        Text(
            "Número de teléfono",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1F1F1F),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Por favor, ingresa tu número de teléfono para continuar.",
            fontSize = 14.sp,
            color = Color(0xFF6B6B6B),
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.weight(1f))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth(),
        ) {
            // Country code container (square with Cuban flag)
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(Color(0xFFF5F5F5), RoundedCornerShape(8.dp))
                    .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center,
            ) {
                CubanFlag(modifier = Modifier.size(36.dp))
            }
            Spacer(Modifier.width(8.dp))
            Text(
                "+53",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1F1F1F),
            )
            Spacer(Modifier.width(8.dp))
            // Phone input
            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it.filter(Char::isDigit).take(8) },
                placeholder = { Text("Número de teléfono") },
                isError = isError,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier
                    .width(200.dp),
                textStyle = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Medium),
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TodusRed,
                    unfocusedBorderColor = Color(0xFFE0E0E0),
                    errorBorderColor = TodusRed,
                ),
            )
        }

        if (isError) {
            val msg = (state as AuthViewModel.PhoneUiState.Error).message
            Text(
                msg,
                color = TodusRed,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 12.dp),
                textAlign = TextAlign.Center,
            )
        }

        Spacer(Modifier.weight(1f))

        Button(
            onClick = {
                vm.loginWithPhoneOnly(phone)
            },
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = TodusRed),
            enabled = state !is AuthViewModel.PhoneUiState.Loading && phone.length == 8,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .padding(bottom = 32.dp),
        ) {
            if (state is AuthViewModel.PhoneUiState.Loading) {
                CircularProgressIndicator(
                    color = Color.White,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(20.dp),
                )
            } else {
                Text("Continuar", color = Color.White, fontSize = 16.sp)
            }
        }
    }

    LaunchedEffect(state) {
        if (state is AuthViewModel.PhoneUiState.Success) {
            onContinue()
            vm.resetPhoneState()
        }
    }
}

/** Minimal Cuban flag: three blue/white stripes + red triangle + white star (simplified). */
@Composable
private fun CubanFlag(modifier: Modifier = Modifier) {
    Box(modifier.background(Color(0xFF00338D))) {
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxWidth().weight(1f).background(Color.White))
            Box(Modifier.fillMaxWidth().weight(1f).background(Color(0xFF00338D)))
            Box(Modifier.fillMaxWidth().weight(1f).background(Color.White))
        }
        Box(
            Modifier.size(24.dp).align(Alignment.CenterStart),
        ) {
            // Red triangle (simplified — full star omitted for v0.0.1)
            Box(
                Modifier
                    .size(24.dp)
                    .background(Color(0xFFC8102E), RoundedCornerShape(0.dp))
            )
        }
    }
}
