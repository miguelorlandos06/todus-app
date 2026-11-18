package cu.todus.android.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cu.todus.android.R
import cu.todus.android.protocol.TodusConstants
import cu.todus.android.ui.theme.TodusRed

/**
 * Welcome screen (per the UX spec):
 *   - Centered toDus logo
 *   - Title "¡Bienvenido a toDus!" (toDus in red)
 *   - Subtitle "Una aplicación de mensajería pensada para tí"
 *   - Bottom: Términos (red, clickable → terms URL) y Condiciones (red, clickable)
 *   - "Aceptar y Continuar" button spanning most of the width, with semi-rounded corners
 */
@Composable
fun WelcomeScreen(onAccept: () -> Unit) {
    val uriHandler = LocalUriHandler.current

    val termsText = buildAnnotatedString {
        append("Al continuar, aceptas nuestros ")
        pushStringAnnotation("url", TodusConstants.TERMS_URL)
        withStyle(SpanStyle(color = TodusRed, fontWeight = FontWeight.Bold)) {
            append("Términos")
        }
        pop()
        append(" y ")
        pushStringAnnotation("url", TodusConstants.TERMS_URL)
        withStyle(SpanStyle(color = TodusRed, fontWeight = FontWeight.Bold)) {
            append("Condiciones")
        }
        pop()
        append(" de uso")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(horizontal = 24.dp)
            .statusBarsPadding()
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Spacer(Modifier.height(48.dp))
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.weight(1f),
        ) {
            Image(
                painter = painterResource(R.drawable.ic_todus_logo),
                contentDescription = "toDus",
                modifier = Modifier.size(140.dp),
            )
            Spacer(Modifier.height(24.dp))
            Text(
                text = buildAnnotatedString {
                    append("¡Bienvenido a ")
                    withStyle(SpanStyle(color = TodusRed, fontWeight = FontWeight.Bold)) {
                        append("toDus")
                    }
                    append("!")
                },
                fontSize = 26.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1F1F1F),
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "Una aplicación de mensajería pensada para tí",
                fontSize = 15.sp,
                color = Color(0xFF6B6B6B),
            )
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(bottom = 32.dp),
        ) {
            Text(
                text = termsText,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(bottom = 12.dp)
                    .clickableTodus {
                        // Open terms on any tap of the line for v0.0.1
                        uriHandler.openUri(TodusConstants.TERMS_URL)
                    },
            )
            Button(
                onClick = onAccept,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = TodusRed),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            ) {
                Text("Aceptar y Continuar", color = Color.White, fontSize = 16.sp)
            }
        }
    }
}
