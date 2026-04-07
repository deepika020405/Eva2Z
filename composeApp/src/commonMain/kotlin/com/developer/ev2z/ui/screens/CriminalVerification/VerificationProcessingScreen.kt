package com.developer.ev2z.ui.screens.CriminalVerification

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ProcessingScreen(message: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CV_Background),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = CV_Surface),
            elevation = CardDefaults.cardElevation(2.dp),
            modifier = Modifier
                .fillMaxWidth(0.75f)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(
                    color = CV_Primary,
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(40.dp)
                )
                Spacer(Modifier.height(20.dp))
                Text(
                    message,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = CV_TextSec
                )
            }
        }
    }
}