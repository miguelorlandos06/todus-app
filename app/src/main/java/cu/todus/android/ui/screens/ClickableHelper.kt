package cu.todus.android.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.ui.Modifier

/** Simple clickable wrapper with no ripple customization for v0.0.1. */
fun Modifier.clickableTodus(onClick: () -> Unit): Modifier = this.clickable(onClick = onClick)
