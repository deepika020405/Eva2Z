package com.developer.ev2z.ui.screens.CriminalVerification

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CriminalErrorScreen(
    message: String,
    onRetry: () -> Unit,
    onBack: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CV_Background),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .padding(16.dp),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = CV_Surface),
            elevation = CardDefaults.cardElevation(3.dp)
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Error icon
                Surface(
                    shape = CircleShape,
                    color = CV_Error.copy(alpha = 0.10f),
                    modifier = Modifier.size(56.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = CV_Error,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Spacer(Modifier.height(18.dp))

                Text(
                    "Something went wrong",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = CV_TextPrimary
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    message,
                    fontSize = 14.sp,
                    color = CV_TextSec,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )

                Spacer(Modifier.height(24.dp))

                Button(
                    onClick = onRetry,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CV_Primary)
                ) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Try Again",
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(Modifier.height(8.dp))

                TextButton(onClick = onBack) {
                    Text(
                        "Go Back",
                        color = CV_TextSec,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}