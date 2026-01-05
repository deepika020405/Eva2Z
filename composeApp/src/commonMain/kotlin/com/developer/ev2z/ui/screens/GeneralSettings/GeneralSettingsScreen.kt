package com.developer.ev2z.ui.screens.GeneralSettings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.developer.ev2z.navigation.PlatformBackHandler

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeneralSettingsScreen(
    onBackPressed: () -> Unit
) {
    var notificationsEnabled by remember { mutableStateOf(true) }
    var darkModeEnabled by remember { mutableStateOf(false) }
    var locationEnabled by remember { mutableStateOf(true) }

    PlatformBackHandler(enabled = true) {
        onBackPressed()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "General Settings",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        }
    ) { padding ->
//        LazyColumn(
//            modifier = Modifier
//                .fillMaxSize()
//                .padding(padding)
//                .background(Color(0xFFF5F5F5))
//        ) {
//            item {
//                Spacer(modifier = Modifier.height(16.dp))
//
//                // Notifications Section
//                Text(
//                    text = "Preferences",
//                    fontSize = 14.sp,
//                    color = Color.Gray,
//                    modifier = Modifier
//                        .fillMaxWidth()
//                        .padding(horizontal = 24.dp, vertical = 8.dp)
//                )
//
//                Card(
//                    modifier = Modifier
//                        .fillMaxWidth()
//                        .padding(horizontal = 24.dp, vertical = 8.dp),
//                    shape = RoundedCornerShape(16.dp),
//                    colors = CardDefaults.cardColors(containerColor = Color.White),
//                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
//                ) {
//                    Column {
//                        SettingSwitchItem(
//                            icon = Icons.Default.Notifications,
//                            title = "Notifications",
//                            subtitle = "Enable push notifications",
//                            checked = notificationsEnabled,
//                            onCheckedChange = { notificationsEnabled = it }
//                        )
//
//                        HorizontalDivider(
//                            modifier = Modifier.padding(horizontal = 16.dp),
//                            color = Color(0xFFE0E0E0)
//                        )
//
//                        SettingSwitchItem(
//                            icon = Icons.Default.DarkMode,
//                            title = "Dark Mode",
//                            subtitle = "Switch to dark theme",
//                            checked = darkModeEnabled,
//                            onCheckedChange = { darkModeEnabled = it }
//                        )
//
//                        HorizontalDivider(
//                            modifier = Modifier.padding(horizontal = 16.dp),
//                            color = Color(0xFFE0E0E0)
//                        )
//
//                        SettingSwitchItem(
//                            icon = Icons.Default.LocationOn,
//                            title = "Location Services",
//                            subtitle = "Allow location access",
//                            checked = locationEnabled,
//                            onCheckedChange = { locationEnabled = it }
//                        )
//                    }
//                }
//
//                Spacer(modifier = Modifier.height(16.dp))
//
//                // App Settings Section
//                Text(
//                    text = "App Settings",
//                    fontSize = 14.sp,
//                    color = Color.Gray,
//                    modifier = Modifier
//                        .fillMaxWidth()
//                        .padding(horizontal = 24.dp, vertical = 8.dp)
//                )
//
//                Card(
//                    modifier = Modifier
//                        .fillMaxWidth()
//                        .padding(horizontal = 24.dp, vertical = 8.dp),
//                    shape = RoundedCornerShape(16.dp),
//                    colors = CardDefaults.cardColors(containerColor = Color.White),
//                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
//                ) {
//                    Column {
//                        SettingNavigationItem(
//                            icon = Icons.Default.Language,
//                            title = "Language",
//                            subtitle = "English",
//                            onClick = { /* TODO: Language selection */ }
//                        )
//
//                        HorizontalDivider(
//                            modifier = Modifier.padding(horizontal = 16.dp),
//                            color = Color(0xFFE0E0E0)
//                        )
//
//                        SettingNavigationItem(
//                            icon = Icons.Default.Storage,
//                            title = "Storage",
//                            subtitle = "Manage app storage",
//                            onClick = { /* TODO: Storage management */ }
//                        )
//
//                        HorizontalDivider(
//                            modifier = Modifier.padding(horizontal = 16.dp),
//                            color = Color(0xFFE0E0E0)
//                        )
//
//                        SettingNavigationItem(
//                            icon = Icons.Default.Update,
//                            title = "App Version",
//                            subtitle = "v1.0.0",
//                            onClick = { /* TODO: Check for updates */ }
//                        )
//                    }
//                }
//
//                Spacer(modifier = Modifier.height(100.dp))
//            }
//        }
    }
}

@Composable
fun SettingSwitchItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = Color(0xFF4CAF50),
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF333333)
            )
            Text(
                text = subtitle,
                fontSize = 13.sp,
                color = Color.Gray
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF4CAF50),
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Color.LightGray
            )
        )
    }
}

@Composable
fun SettingNavigationItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = Color(0xFF4CAF50),
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF333333)
            )
            Text(
                text = subtitle,
                fontSize = 13.sp,
                color = Color.Gray
            )
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = "Navigate",
            tint = Color(0xFF999999)
        )
    }
}