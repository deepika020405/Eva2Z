package com.developer.ev2z.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.developer.ev2z.util.openCall
import com.developer.ev2z.util.openWhatsApp

@Composable
fun FloatingContactButton(
    phoneNumber: String,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.BottomEnd
    ) {

        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {

                    // 📞 Call Button
                    MiniFab(
                        icon = Icons.Default.Call,
                        color = Color(0xFF4CAF50)
                    ) {
                        openCall(phoneNumber)
                        expanded = false
                    }

                    // 💬 WhatsApp Button
                    MiniFab(
                        icon = Icons.Default.Chat,
                        color = Color(0xFF25D366)
                    ) {
                        openWhatsApp(
                            phoneNumber = phoneNumber,
                            message = "Hi, Eva2z Team\nI have a query"
                        )
                        expanded = false
                    }
                }
            }

            // 🔵 Main FAB
            FloatingActionButton(
                onClick = { expanded = !expanded },
                containerColor = Color(0xFF2196F3),
                shape = CircleShape
            ) {
                Icon(
                    imageVector = Icons.Default.SupportAgent,
                    contentDescription = "Support",
                    tint = Color.White
                )
            }
        }
    }
}

@Composable
private fun MiniFab(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .background(color, CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White
        )
    }
}