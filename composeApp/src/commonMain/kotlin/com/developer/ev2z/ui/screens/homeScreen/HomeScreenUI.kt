package com.developer.ev2z.ui.screens.homeScreen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.painterResource
import eva2z_connect_multiplatform.composeapp.generated.resources.Res
import eva2z_connect_multiplatform.composeapp.generated.resources.map_placeholder
import eva2z_connect_multiplatform.composeapp.generated.resources.scooter
import org.jetbrains.compose.ui.tooling.preview.Preview

// ================== MAIN SCREEN ==================

@Preview
@Composable
fun HomeScreenUI() {

    Scaffold(
        containerColor = Color(0xFFF4F6FA),
        bottomBar = { ModernBottomBar() }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {

            ModernTopHeader()

            Spacer(Modifier.height(16.dp))

            ModernMapCard()

            Spacer(Modifier.height(24.dp))

            ModernFeaturesSection()
        }
    }
}

// ================== HEADER ==================

@Composable
fun ModernTopHeader() {

    Card(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(6.dp)
    ) {

        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Image(
                painter = painterResource(Res.drawable.scooter),
                contentDescription = null,
                modifier = Modifier.size(56.dp)
            )

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {

                Text(
                    "Suzuki Gixxer SF",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E1E1E)
                )

                Spacer(Modifier.height(4.dp))

                Row {
                    Text(
                        "MH02JP2249 ",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )

                    Text(
                        "AGS",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1E88E5)
                    )
                }
            }

            Icon(
                imageVector = Icons.Default.Notifications,
                contentDescription = null,
                tint = Color.DarkGray
            )
        }
    }
}

// ================== MAP CARD ==================

@Composable
fun ModernMapCard() {

    Card(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {

        Column {

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp)
            ) {
                Image(
                    painter = painterResource(Res.drawable.map_placeholder),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {

                Column {
                    Text("Search", fontWeight = FontWeight.Medium)

                    Spacer(Modifier.height(6.dp))

                    Text(
                        "0.0 km",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF00A86B)
                    )

                    Text(
                        "Nil 25/onica. 0y 2s",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }

                Column(horizontalAlignment = Alignment.End) {

                    Text("Speed: 0 km/hr", fontSize = 14.sp)
                    Spacer(Modifier.height(4.dp))
                    Text("Top Speed: 0 km/hr", fontSize = 14.sp)
                }
            }
        }
    }
}

// ================== FEATURES ==================

@Composable
fun ModernFeaturesSection() {

    Column(
        modifier = Modifier.padding(horizontal = 16.dp)
    ) {

        Text(
            "Upcoming Features",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1E1E1E)
        )

        Spacer(Modifier.height(20.dp))

        val features = listOf(
            "Driver Score" to Icons.Default.EmojiEvents,
            "Service Marketplace" to Icons.Default.Build,
            "Repair Marketplace" to Icons.Default.DirectionsCar,
            "Charging Station" to Icons.Default.EvStation,
            "Parking Station" to Icons.Default.LocalParking,
            "SOS" to Icons.Default.Warning,
            "Driver Marketplace" to Icons.Default.Group
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            userScrollEnabled = false,
            modifier = Modifier.height(340.dp)
        ) {
            items(features) { (title, icon) ->
                ModernFeatureItem(title, icon)
            }
        }
    }
}

@Composable
fun ModernFeatureItem(title: String, icon: ImageVector) {

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(12.dp)
    ) {

        Box(
            modifier = Modifier
                .size(60.dp)
                .background(Color(0xFFF1F3F7), RoundedCornerShape(18.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = Color(0xFF2C2C2C),
                modifier = Modifier.size(28.dp)
            )
        }

        Spacer(Modifier.height(10.dp))

        Text(
            title,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Medium
        )
    }
}

// ================== BOTTOM NAV ==================

@Composable
fun ModernBottomBar() {

    NavigationBar(
        containerColor = Color.White,
        tonalElevation = 8.dp
    ) {

        NavigationBarItem(
            selected = true,
            onClick = {},
            icon = { Icon(Icons.Default.Home, null) }
        )

        NavigationBarItem(
            selected = false,
            onClick = {},
            icon = { Icon(Icons.Default.Menu, null) }
        )

        NavigationBarItem(
            selected = false,
            onClick = {},
            icon = { Icon(Icons.Default.BarChart, null) }
        )

        NavigationBarItem(
            selected = false,
            onClick = {},
            icon = { Icon(Icons.Default.Person, null) }
        )
    }
}