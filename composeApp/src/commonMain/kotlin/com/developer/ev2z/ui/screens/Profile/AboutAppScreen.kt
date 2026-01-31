package com.developer.ev2z.ui.screens.Profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.developer.ev2z.navigation.PlatformBackHandler


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutAppScreen(
    onBackClick: () -> Unit
) {

    PlatformBackHandler(enabled = true) {
        onBackClick()
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "About EVA2Z Connect",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        },
        contentWindowInsets = WindowInsets(0)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF6F7F9))
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            InfoCard {
                SectionTitle("Welcome to EVA2Z Connect")
                SectionText(
                    "EVA2Z Connect is an advanced GPS tracking and vehicle security platform designed to deliver real-time monitoring, safety, and complete control.\n\n" +
                            "Ideal for individuals, families, and fleet operators looking for reliability and peace of mind."
                )
            }

            InfoCard {
                SectionTitle("Key Features")
                FeatureItem("Live GPS Tracking", "Real-time vehicle location")
                FeatureItem("Remote Engine Control", "Secure vehicle instantly")
                FeatureItem("Route History", "Complete travel logs")
                FeatureItem("Daily Analytics", "Trips, mileage & insights")
                FeatureItem("Smart Alerts", "Ignition, speed & geofence")
                FeatureItem("Multi-Vehicle Support", "Cars, bikes & fleets")
            }

            InfoCard {
                SectionTitle("Available Products")
                SimpleItem("Wireless GPS Tracker")
                SimpleItem("Wired GPS Tracker (Car / Bike)")
                SimpleItem("Kids & Pet Tracker")
                SimpleItem("Elderly Safety Tracker")
                SimpleItem("Smart Document Tracker")
            }

            InfoCard {
                SectionTitle("Why Choose EVA2Z")
                SimpleItem("Accurate & fast tracking")
                SimpleItem("Reliable engine control")
                SimpleItem("Advanced safety alerts")
                SimpleItem("Trusted by thousands of users")
            }

            InfoCard {
                SectionTitle("Company Information")
                SectionText(
                    "Evatoz Solutions Private Limited specializes in electric mobility and intelligent GPS tracking solutions."
                )
                Spacer(Modifier.height(8.dp))
                SectionSubtitle("Our Mission")
                SectionText(
                    "To empower users with intelligent tracking, real-time alerts, and seamless digital experience."
                )
            }

            InfoCard {
                SectionTitle("Contact & Support")
                ContactRow("Address", "B-46, Sector 57, Noida, UP 201301")
                ContactRow("Phone", "+91 9266884678")
                ContactRow("Email", "info@eva2z.com")
                ContactRow("Website", "www.eva2z.com")
            }

            HighlightCard()
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun InfoCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            content = content
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        fontSize = 17.sp,
        fontWeight = FontWeight.Bold,
        color = Color(0xFF1F2937)
    )
}

@Composable
private fun SectionSubtitle(text: String) {
    Text(
        text,
        fontSize = 15.sp,
        fontWeight = FontWeight.SemiBold,
        color = Color(0xFF4CAF50)
    )
}

@Composable
private fun SectionText(text: String) {
    Text(
        text,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        color = Color(0xFF6B7280)
    )
}

@Composable
private fun FeatureItem(title: String, desc: String) {
    Column {
        Text(title, fontWeight = FontWeight.Medium, fontSize = 14.sp)
        Text(desc, fontSize = 13.sp, color = Color(0xFF6B7280))
    }
}

@Composable
private fun SimpleItem(text: String) {
    Text(
        "• $text",
        fontSize = 14.sp,
        color = Color(0xFF4B5563)
    )
}

@Composable
private fun ContactRow(label: String, value: String) {
    Column {
        Text(label, fontWeight = FontWeight.Medium, fontSize = 13.sp)
        Text(value, fontSize = 14.sp, color = Color(0xFF6B7280))
    }
}

@Composable
private fun HighlightCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF4CAF50))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Protect What Matters Most",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = Color.White
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Track smarter. Stay safer. Stay in control.",
                fontSize = 14.sp,
                color = Color.White
            )
        }
    }
}


//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun AboutAppScreen(
//    onBackClick: () -> Unit
//) {
//    Scaffold(
//        topBar = {
//            TopAppBar(
//                title = {
//                    Text(
//                        text = "About EVA2Z Connect",
//                        fontSize = 20.sp,
//                        fontWeight = FontWeight.SemiBold
//                    )
//                },
//                navigationIcon = {
//                    IconButton(onClick = onBackClick) {
//                        Icon(
//                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
//                            contentDescription = "Back"
//                        )
//                    }
//                },
//                colors = TopAppBarDefaults.topAppBarColors(
//                    containerColor = Color.White
//                )
//            )
//        },
//        contentWindowInsets = WindowInsets(0) // 🔥 IMPORTANT
//
//    ) { paddingValues ->
//        Column(
//            modifier = Modifier
//                .fillMaxSize()
//                .padding(paddingValues)
//                .background(Color(0xFFF5F5F5))
//                .verticalScroll(rememberScrollState())
//                .padding(24.dp),
//            verticalArrangement = Arrangement.spacedBy(16.dp)
//        ) {
//            // Welcome
//            SectionHeader("🚗 Welcome to EVA2Z Connect!")
//            SectionText(
//                "EVA2Z Connect is your all-in-one GPS tracking and vehicle security app — designed to give you " +
//                        "total control, real-time safety, and complete peace of mind.\n\n" +
//                        "Whether you're a personal vehicle owner, fleet manager, or someone who wants to protect loved ones " +
//                        "and valuable assets, EVA2Z Connect gives you everything in one powerful app."
//            )
//
//            Spacer(modifier = Modifier.height(8.dp))
//
//            // Features
//            SectionHeader("🚀 What You Can Do")
//            BulletPoint("📍 Live GPS Tracking", "See your vehicle location in real time.")
//            BulletPoint("🔐 Remote Engine Control", "Instantly secure your vehicle from anywhere.")
//            BulletPoint("🛣️ Route History", "Check every route throughout the day.")
//            BulletPoint("📊 Daily Analytics", "Understand trips, mileage, and usage.")
//            BulletPoint("🔔 Smart Alerts", "Ignition, speeding, battery, geofence & more.")
//            BulletPoint("🚘 Multi-Vehicle", "Manage car, bike, truck, or fleet.")
//            BulletPoint("🛒 In-App Shop", "Buy GPS devices directly.")
//            BulletPoint("👤 Profile & Preferences", "Manage settings easily.")
//            BulletPoint("🎨 Beautiful UI", "Modern & effortless navigation.")
//
//            Spacer(modifier = Modifier.height(8.dp))
//
//            // Products
//            SectionHeader("🛍️ Products Available")
//            BulletPoint("🔋 GPS Tracker", "Wireless")
//            BulletPoint("🔧 GPS Tracker", "Wired (Car/Bike)")
//            BulletPoint("🧒 Kids Tracker", "Keep children safe")
//            BulletPoint("🐶 Pet Tracker", "Never lose your pet")
//            BulletPoint("🧓 Elderly Tracker", "Care for loved ones")
//            BulletPoint("📄 Document Tracker", "Smart Card Tag")
//
//            Spacer(modifier = Modifier.height(8.dp))
//
//            // Why Us
//            SectionHeader("💡 Why EVA2Z Stands Out")
//            SimpleList(
//                listOf(
//                    "Faster & accurate tracking",
//                    "Reliable engine lock/unlock",
//                    "Best safety alerts",
//                    "Trusted by 1,000+ users",
//                    "Personal & fleet management"
//                )
//            )
//
//            Spacer(modifier = Modifier.height(8.dp))
//
//            // Company
//            SectionHeader("🏢 About the Company")
//            SectionText(
//                "Evatoz Solutions Private Limited delivers innovative electric mobility and GPS tracking solutions."
//            )
//
//            SectionSubHeader("🎯 Our Mission")
//            SectionText(
//                "To empower users with real-time tracking, smart alerts, and seamless mobile experience."
//            )
//
//            Spacer(modifier = Modifier.height(8.dp))
//
//            // Contact
//            SectionHeader("📞 Contact & Support")
//            ContactInfo("Address:", "B-46, Sector 57, Noida, UP 201301")
//            ContactInfo("Phone:", "+91 9266884678")
//            ContactInfo("Email:", "info@eva2z.com")
//            ContactInfo("Website:", "https://eva2z.com")
//
//            Spacer(modifier = Modifier.height(8.dp))
//
//            // CTA
//            Card(
//                modifier = Modifier.fillMaxWidth(),
//                colors = CardDefaults.cardColors(containerColor = Color(0xFF4CAF50)),
//                shape = MaterialTheme.shapes.medium
//            ) {
//                Column(
//                    modifier = Modifier.padding(16.dp),
//                    horizontalAlignment = Alignment.CenterHorizontally
//                ) {
//                    Text(
//                        text = "⏳ Don't Wait — Protect What Matters!",
//                        fontSize = 16.sp,
//                        fontWeight = FontWeight.Bold,
//                        color = Color.White
//                    )
//                    Spacer(modifier = Modifier.height(8.dp))
//                    Text(
//                        text = "Every minute without tracking is a risk.",
//                        fontSize = 14.sp,
//                        color = Color.White
//                    )
//                    Spacer(modifier = Modifier.height(8.dp))
//                    Text(
//                        text = "🚗 Track Smarter. Live Safer. Stay in Control.",
//                        fontSize = 14.sp,
//                        fontWeight = FontWeight.Bold,
//                        color = Color.White
//                    )
//                }
//            }
//
//            Spacer(modifier = Modifier.height(32.dp))
//        }
//    }
//}
//
//@Composable
//private fun SectionHeader(text: String) {
//    Text(
//        text = text,
//        fontSize = 18.sp,
//        fontWeight = FontWeight.Bold,
//        color = Color(0xFF333333)
//    )
//}
//
//@Composable
//private fun SectionSubHeader(text: String) {
//    Text(
//        text = text,
//        fontSize = 16.sp,
//        fontWeight = FontWeight.SemiBold,
//        color = Color(0xFF4CAF50)
//    )
//}
//
//@Composable
//private fun SectionText(text: String) {
//    Text(
//        text = text,
//        fontSize = 14.sp,
//        color = Color(0xFF666666),
//        lineHeight = 20.sp
//    )
//}
//
//@Composable
//private fun BulletPoint(title: String, description: String) {
//    Row(
//        modifier = Modifier.fillMaxWidth(),
//        horizontalArrangement = Arrangement.spacedBy(8.dp)
//    ) {
//        Text(
//            text = "•",
//            fontSize = 14.sp,
//            color = Color(0xFF4CAF50),
//            modifier = Modifier.padding(top = 2.dp)
//        )
//        Column {
//            Text(
//                text = buildAnnotatedString {
//                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
//                        append(title)
//                    }
//                },
//                fontSize = 14.sp,
//                color = Color(0xFF333333)
//            )
//            if (description.isNotEmpty()) {
//                Text(
//                    text = description,
//                    fontSize = 13.sp,
//                    color = Color(0xFF666666),
//                    lineHeight = 18.sp
//                )
//            }
//        }
//    }
//}
//
//@Composable
//private fun SimpleList(items: List<String>) {
//    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
//        items.forEach { item ->
//            Row(
//                modifier = Modifier.fillMaxWidth(),
//                horizontalArrangement = Arrangement.spacedBy(8.dp)
//            ) {
//                Text(text = "•", fontSize = 14.sp, color = Color(0xFF4CAF50))
//                Text(text = item, fontSize = 14.sp, color = Color(0xFF666666), lineHeight = 20.sp)
//            }
//        }
//    }
//}
//
//@Composable
//private fun ContactInfo(label: String, value: String) {
//    Column(
//        modifier = Modifier.fillMaxWidth(),
//        verticalArrangement = Arrangement.spacedBy(4.dp)
//    ) {
//        Text(
//            text = label,
//            fontSize = 14.sp,
//            fontWeight = FontWeight.Bold,
//            color = Color(0xFF333333)
//        )
//        Text(
//            text = value,
//            fontSize = 14.sp,
//            color = Color(0xFF666666)
//        )
//    }
//}