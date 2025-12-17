// CommonImage.kt (commonMain)
package com.developer.ev2z.ui.screens.community

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun NetworkImage(
    url: String,
    modifier: Modifier = Modifier
)