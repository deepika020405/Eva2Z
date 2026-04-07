package com.developer.ev2z.ui.screens.InstallationSupport

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.developer.ev2z.ui.screens.social_media.openUrl

private val BrandGreen = Color(0xFF34A853)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InstallationSupportScreen(
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Installation Support", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color(0xFFF6F6F6)
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {

            Text(
                text = "Select Your Vehicle Type",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E1E1E)
            )

            Text(
                text = "Watch the installation guide video for your vehicle",
                fontSize = 14.sp,
                color = Color(0xFF777777)
            )

            Spacer(modifier = Modifier.height(8.dp))

            VehicleOptionCard(
                title = "2 Wheeler Vehicle",
                subtitle = "Bike, Scooter, Electric 2W",
                icon = Icons.Default.TwoWheeler,
                onClick = {
                    openUrl("https://youtu.be/sYjUGsugOtY?si=aP8lZJ4Lg5RPeo3f")
                }
            )

            VehicleOptionCard(
                title = "4 Wheeler Vehicle",
                subtitle = "Car, SUV, Truck, Bus",
                icon = Icons.Default.DirectionsCar,
                onClick = {
                    openUrl("https://youtu.be/KrQRf6XZ-Ok?si=iODpWPEVbBtl1Ev5")
                }
            )
        }
    }
}

@Composable
private fun VehicleOptionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .background(
                        BrandGreen.copy(alpha = 0.12f),
                        RoundedCornerShape(16.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = BrandGreen,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1E1E1E)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    color = Color(0xFF777777)
                )
            }

            Icon(
                imageVector = Icons.Default.TwoWheeler,
                contentDescription = null,
                tint = Color.Transparent,
                modifier = Modifier.size(0.dp)
            )

            Text(
                text = "Watch",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = BrandGreen
            )
        }
    }
}
