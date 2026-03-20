package com.developer.ev2z.ui.screens.market_screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import coil3.compose.AsyncImage
import com.developer.ev2z.util.VideoPlayerView

// ── Light premium green palette ────────────────────────────────
private val Hero_Green       = Color(0xFF00875A)
private val Hero_GreenLight  = Color(0xFF00A86B)
private val Hero_GreenSoft   = Color(0xFFE8F5EE)   // very light mint
private val Hero_GreenMint   = Color(0xFFF0FAF5)   // almost white-green
private val Hero_GreenBorder = Color(0xFFB7E4CC)
private val Hero_Teal        = Color(0xFF00796B)
private val Hero_Dark        = Color(0xFF0A0F0D)
private val Hero_Mid         = Color(0xFF4A5568)
private val Hero_Light       = Color(0xFF9AA5B1)
private val Hero_Border      = Color(0xFFE8ECF0)
private val Hero_Bg          = Color(0xFFF4FBF7)   // light green-tinted bg
private val Hero_Orange      = Color(0xFFFF6B35)
private val Hero_Gold        = Color(0xFFF59E0B)


class GpsHeroScreen(
    val wiredProduct:    ShopProduct?,
    val wirelessProduct: ShopProduct?,
    val onAddToCart:     (ShopProduct, String, String) -> Unit
) : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.current

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .background(Hero_Bg)
        ) {

            // ══════════════════════════════════════════════════
            //  TOP BAR
            // ══════════════════════════════════════════════════
            Surface(
                color           = Color.White,
                shadowElevation = 0.dp,
                modifier        = Modifier.fillMaxWidth()
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 13.dp),
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Back button
                        Box(
                            modifier = Modifier
                                .size(40.dp).clip(RoundedCornerShape(12.dp))
                                .background(Hero_Bg)
                                .border(1.dp, Hero_Border, RoundedCornerShape(12.dp))
                                .clickable { navigator?.pop() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.ArrowBack, null,
                                tint = Hero_Dark, modifier = Modifier.size(18.dp))
                        }

                        // Title
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Eva2z GPS Tracker",
                                fontSize   = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color      = Hero_Dark)
                            Row(
                                verticalAlignment     = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                repeat(5) {
                                    Icon(Icons.Default.Star, null,
                                        tint     = Hero_Gold,
                                        modifier = Modifier.size(9.dp))
                                }
                                Text(" 4.9 · 6,988 reviews",
                                    fontSize = 10.sp, color = Hero_Light)
                            }
                        }

                        // Cart icon
                        Box(
                            modifier = Modifier
                                .size(40.dp).clip(RoundedCornerShape(12.dp))
                                .background(Hero_GreenSoft)
                                .border(1.dp, Hero_GreenBorder, RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.ShoppingBag, null,
                                tint     = Hero_Green,
                                modifier = Modifier.size(19.dp))
                        }
                    }
                    HorizontalDivider(color = Hero_GreenBorder.copy(alpha = 0.4f),
                        thickness = 0.5.dp)
                }
            }

            // ══════════════════════════════════════════════════
            //  HERO CARD — light mint green
            // ══════════════════════════════════════════════════
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.White)
                    .border(1.5.dp, Hero_GreenBorder, RoundedCornerShape(24.dp))
                    .padding(22.dp)
            ) {
                // Live badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Hero_GreenSoft)
                        .border(1.dp, Hero_GreenBorder, RoundedCornerShape(20.dp))
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Row(
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp).clip(CircleShape)
                                .background(Hero_Green)
                        )
                        Text("INDIA'S #1 GPS SECURITY PLATFORM",
                            fontSize   = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color      = Hero_Green,
                            letterSpacing = 0.8.sp)
                    }
                }

                Spacer(Modifier.height(18.dp))

                Text("Protect Your\nCar & Bike",
                    fontSize   = 30.sp,
                    fontWeight = FontWeight.Black,
                    color      = Hero_Dark,
                    lineHeight = 38.sp)
                Text("From Theft — Live",
                    fontSize   = 30.sp,
                    fontWeight = FontWeight.Black,
                    color      = Hero_Green,
                    lineHeight = 38.sp)

                Spacer(Modifier.height(10.dp))
                Text(
                    "Real-time GPS tracking, engine cut-off & anti-theft alerts. Trusted by 5,000+ owners across India.",
                    fontSize  = 13.sp,
                    color     = Hero_Mid,
                    lineHeight = 20.sp
                )

                Spacer(Modifier.height(22.dp))

                // CTA
                Button(
                    onClick = {
                        navigator?.push(GpsTrackerFlowScreen(
                            wiredProduct    = wiredProduct,
                            wirelessProduct = wirelessProduct,
                            onAddToCart     = onAddToCart
                        ))
                    },
                    modifier  = Modifier.fillMaxWidth().height(54.dp),
                    shape     = RoundedCornerShape(14.dp),
                    colors    = ButtonDefaults.buttonColors(containerColor = Hero_Green),
                    elevation = ButtonDefaults.buttonElevation(0.dp)
                ) {
                    Icon(Icons.Default.ShoppingCart, null,
                        modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Buy Eva2z GPS",
                        fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.Default.ArrowForward, null,
                        modifier = Modifier.size(16.dp))
                }

                Spacer(Modifier.height(14.dp))

                // Trust row
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Avatar stack
                    Box(modifier = Modifier.width(68.dp).height(28.dp)) {
                        listOf(
                            Hero_Green        to "R",
                            Hero_GreenLight   to "A",
                            Hero_Teal         to "P"
                        ).forEachIndexed { i, (color, label) ->
                            Box(
                                modifier = Modifier
                                    .offset(x = (i * 18).dp)
                                    .size(28.dp).clip(CircleShape)
                                    .background(color)
                                    .border(2.dp, Color.White, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(label, fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                    Column {
                        Text("5,000+ happy owners",
                            fontSize   = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color      = Hero_Dark)
                        Text("across India",
                            fontSize = 10.sp, color = Hero_Light)
                    }
                }
            }

            // ══════════════════════════════════════════════════
            //  STATS ROW
            // ══════════════════════════════════════════════════
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Hero_GreenSoft)
                    .border(1.dp, Hero_GreenBorder, RoundedCornerShape(16.dp))
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                listOf(
                    "5K+"  to "Secured",
                    "4.9★" to "Rating",
                    "1 Yr" to "Warranty",
                    "24/7" to "Support"
                ).forEachIndexed { i, (value, label) ->
                    if (i > 0) {
                        Box(modifier = Modifier
                            .width(1.dp).height(32.dp)
                            .background(Hero_GreenBorder))
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(value, fontSize = 18.sp,
                            fontWeight = FontWeight.Black, color = Hero_Green)
                        Text(label, fontSize = 9.sp,
                            color = Hero_Mid, textAlign = TextAlign.Center)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ══════════════════════════════════════════════════
            //  VIDEO PLAYER SECTION
            // ══════════════════════════════════════════════════
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White)
                    .border(1.5.dp, Hero_GreenBorder, RoundedCornerShape(20.dp))
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier              = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Column {
                        Text("See It In Action",
                            fontSize   = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color      = Hero_Dark)
                        Text("Watch Eva2z GPS live demo",
                            fontSize = 12.sp, color = Hero_Light)
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Hero_GreenSoft)
                            .border(1.dp, Hero_GreenBorder, RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Row(
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp).clip(CircleShape)
                                    .background(Hero_Green)
                            )
                            Text("LIVE DEMO",
                                fontSize   = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color      = Hero_Green)
                        }
                    }
                }

                // ── Video Player ──────────────────────────────
                // KMP mein expect/actual video player use karo
                // Android side: AndroidView with VideoView/ExoPlayer
                // iOS side: AVPlayer
                // Abhi ke liye thumbnail + play indication show karte hain
                // Apne project mein VideoPlayerView expect/actual implement karo

                // Video placeholder ki jagah ye lagao:
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(210.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Hero_GreenSoft)
                        .border(1.dp, Hero_GreenBorder, RoundedCornerShape(14.dp))
                ) {
                    VideoPlayerView(
                        url      = "https://www.youtube.com/watch?v=gMR4ZqFlplg",  // ← apna URL
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(Modifier.height(10.dp))

                // URL hint text
                Row(
                    modifier          = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.Link, null,
                        tint     = Hero_Light,
                        modifier = Modifier.size(13.dp))
                    Text(
                        "Features Video Clip",
                        fontSize = 10.sp,
                        color    = Hero_Light
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // ══════════════════════════════════════════════════
            //  FEATURES GRID
            // ══════════════════════════════════════════════════
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White)
                    .border(1.5.dp, Hero_GreenBorder, RoundedCornerShape(20.dp))
                    .padding(16.dp)
            ) {
                Row(
                    modifier              = Modifier.fillMaxWidth().padding(bottom = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Text("All Features",
                        fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Hero_Dark)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Hero_GreenSoft)
                            .border(1.dp, Hero_GreenBorder, RoundedCornerShape(6.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text("10 Features",
                            fontSize   = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color      = Hero_Green)
                    }
                }

                val featureItems = listOf(
                    Icons.Default.LocationOn          to "Live Location",
                    Icons.Default.LocalGasStation     to "Geo-fencing",
                    Icons.Default.History             to "Trip History",
                    Icons.Default.PowerOff            to "Engine Cut Off",
                    Icons.Default.NotificationsActive to "Anti-Theft",
                    Icons.Default.Description         to "Doc Vault",
                    Icons.Default.Assessment          to "Daily Reports",
                    Icons.Default.Verified            to "Aadhaar Verify",
                    Icons.Default.VerifiedUser        to "Criminal Check",
                    Icons.Default.Speed               to "Driving Score",
                )

                featureItems.chunked(5).forEach { rowItems ->
                    Row(
                        modifier              = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowItems.forEach { (icon, label) ->
                            Column(
                                modifier            = Modifier.weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp).clip(RoundedCornerShape(13.dp))
                                        .background(Hero_GreenSoft)
                                        .border(1.dp, Hero_GreenBorder,
                                            RoundedCornerShape(13.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(icon, null,
                                        tint     = Hero_Green,
                                        modifier = Modifier.size(21.dp))
                                }
                                Spacer(Modifier.height(6.dp))
                                Text(label,
                                    fontSize   = 9.sp,
                                    color      = Hero_Mid,
                                    textAlign  = TextAlign.Center,
                                    lineHeight = 12.sp,
                                    fontWeight = FontWeight.Medium)
                            }
                        }
                        repeat(5 - rowItems.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ══════════════════════════════════════════════════
            //  WHY EVA2Z CARDS
            // ══════════════════════════════════════════════════
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Text("Why Choose Eva2z?",
                    fontSize   = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color      = Hero_Dark,
                    modifier   = Modifier.padding(bottom = 12.dp))

                listOf(
                    Triple(Icons.Default.FlashOn,
                        "Instant Alerts",
                        "Get notified when your vehicle moves, ignition starts, or geofence is crossed."),
                    Triple(Icons.Default.BatteryFull,
                        "30-Day Battery Backup",
                        "6000mAh battery keeps tracking even when vehicle power is cut — no blind spots."),
                    Triple(Icons.Default.HeadsetMic,
                        "24/7 Expert Support",
                        "Our team is always available to help you track, protect, and manage your vehicle.")
                ).forEach { (icon, title, desc) ->
                    Card(
                        modifier  = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                        shape     = RoundedCornerShape(16.dp),
                        colors    = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(0.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, Hero_GreenBorder, RoundedCornerShape(16.dp))
                                .padding(16.dp),
                            verticalAlignment     = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp).clip(RoundedCornerShape(13.dp))
                                    .background(Hero_GreenSoft)
                                    .border(1.dp, Hero_GreenBorder, RoundedCornerShape(13.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(icon, null,
                                    tint     = Hero_Green,
                                    modifier = Modifier.size(22.dp))
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(title,
                                    fontSize   = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color      = Hero_Dark)
                                Spacer(Modifier.height(4.dp))
                                Text(desc,
                                    fontSize  = 12.sp,
                                    color     = Hero_Mid,
                                    lineHeight = 18.sp)
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ══════════════════════════════════════════════════
            //  BOTTOM CTA — light green premium card
            // ══════════════════════════════════════════════════
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Hero_GreenSoft)
                    .border(1.5.dp, Hero_GreenBorder, RoundedCornerShape(24.dp))
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Shield icon
                Box(
                    modifier = Modifier
                        .size(60.dp).clip(CircleShape)
                        .background(Color.White)
                        .border(1.5.dp, Hero_GreenBorder, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Shield, null,
                        tint     = Hero_Green,
                        modifier = Modifier.size(30.dp))
                }

                Spacer(Modifier.height(16.dp))

                Text("Ready to Secure\nYour Vehicle?",
                    fontSize   = 24.sp,
                    fontWeight = FontWeight.Black,
                    color      = Hero_Dark,
                    lineHeight = 32.sp,
                    textAlign  = TextAlign.Center)

                Spacer(Modifier.height(8.dp))

                Text(
                    "Join 5,000+ vehicle owners. Get real-time GPS, anti-theft & engine cut-off today.",
                    fontSize  = 13.sp,
                    color     = Hero_Mid,
                    lineHeight = 19.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(22.dp))

                Button(
                    onClick = {
                        navigator?.push(GpsTrackerFlowScreen(
                            wiredProduct    = wiredProduct,
                            wirelessProduct = wirelessProduct,
                            onAddToCart     = onAddToCart
                        ))
                    },
                    modifier  = Modifier.fillMaxWidth().height(54.dp),
                    shape     = RoundedCornerShape(14.dp),
                    colors    = ButtonDefaults.buttonColors(containerColor = Hero_Green),
                    elevation = ButtonDefaults.buttonElevation(0.dp)
                ) {
                    Icon(Icons.Default.ShoppingCart, null,
                        modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Buy Eva2z GPS Now",
                        fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(Modifier.height(14.dp))

                // Trust badges
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    listOf(
                        Icons.Default.Security      to "Secure Pay",
                        Icons.Default.LocalShipping to "Free Delivery",
                        Icons.Default.Replay        to "Easy Return"
                    ).forEach { (icon, label) ->
                        Row(
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(icon, null,
                                tint     = Hero_Green,
                                modifier = Modifier.size(13.dp))
                            Text(label,
                                fontSize   = 10.sp,
                                color      = Hero_Mid,
                                fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

// ═══════════════════════════════════════════════════════════════
//  Video Placeholder — tab tak dikhega jab tak expect/actual
//  VideoPlayerView implement na ho
// ═══════════════════════════════════════════════════════════════
@Composable
private fun VideoPlaceholder() {
    val Hero_Green      = Color(0xFF00875A)
    val Hero_GreenSoft  = Color(0xFFE8F5EE)
    val Hero_GreenBorder= Color(0xFFB7E4CC)
    val Hero_GreenLight = Color(0xFF00A86B)

    Box(
        modifier         = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // Concentric rings
        Box(
            modifier = Modifier
                .size(100.dp).clip(CircleShape)
                .background(Hero_Green.copy(alpha = 0.08f))
        )
        Box(
            modifier = Modifier
                .size(74.dp).clip(CircleShape)
                .background(Hero_Green.copy(alpha = 0.13f))
        )
        // Play button
        Box(
            modifier = Modifier
                .size(54.dp).clip(CircleShape)
                .background(Hero_Green),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.PlayArrow, null,
                tint     = Color.White,
                modifier = Modifier.size(28.dp))
        }

        // Top-left label
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.White.copy(alpha = 0.9f))
                .border(1.dp, Hero_GreenBorder, RoundedCornerShape(8.dp))
                .padding(horizontal = 10.dp, vertical = 5.dp)
        ) {
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp).clip(CircleShape)
                        .background(Hero_Green)
                )
                Text("PRODUCT DEMO",
                    fontSize   = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color      = Hero_Green,
                    letterSpacing = 0.5.sp)
            }
        }

        // Bottom bar — like a video progress bar
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color.White.copy(alpha = 0.92f))
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            // Progress bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Hero_GreenBorder)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.3f)
                        .fillMaxHeight()
                        .background(Hero_Green, RoundedCornerShape(2.dp))
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.PlayCircle, null,
                        tint     = Hero_Green,
                        modifier = Modifier.size(18.dp))
                    Text("Eva2z GPS Demo",
                        fontSize   = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color      = Color(0xFF0A0F0D))
                }
                Text("0:45 / 2:30",
                    fontSize = 10.sp,
                    color    = Color(0xFF9AA5B1))
            }
        }
    }
}