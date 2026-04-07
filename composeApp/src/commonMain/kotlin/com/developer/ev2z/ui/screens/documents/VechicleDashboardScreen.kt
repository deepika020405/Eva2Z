package com.developer.ev2z.ui.screens.documents

// ══════════════════════════════════════════════════════════════════
// COMMENTED OUT: DUMMY UI CODE (for reference)
// ══════════════════════════════════════════════════════════════════

// 
// import androidx.compose.foundation.*
// import androidx.compose.foundation.layout.*
// import androidx.compose.foundation.shape.CircleShape
// import androidx.compose.foundation.shape.RoundedCornerShape
// import androidx.compose.material.icons.Icons
// import androidx.compose.material.icons.automirrored.filled.ArrowBack
// import androidx.compose.material.icons.filled.*
// import androidx.compose.material.icons.outlined.*
// import androidx.compose.material3.*
// import androidx.compose.runtime.*
// import androidx.compose.ui.Alignment
// import androidx.compose.ui.Modifier
// import androidx.compose.ui.draw.clip
// import androidx.compose.ui.draw.shadow
// import androidx.compose.ui.graphics.Brush
// import androidx.compose.ui.graphics.Color
// import androidx.compose.ui.graphics.vector.ImageVector
// import androidx.compose.ui.text.font.FontWeight
// import androidx.compose.ui.text.style.TextAlign
// import androidx.compose.ui.unit.dp
// import androidx.compose.ui.unit.sp
// import androidx.compose.ui.window.Dialog
// import androidx.compose.ui.window.DialogProperties
// 
// // ════════════════════════════════════════════
// //  DESIGN TOKENS
// // ════════════════════════════════════════════
// 
// private object T {
//     val Primary       = Color(0xFF4CAF50)
//     val PrimaryDark   = Color(0xFF388E3C)
//     val PrimaryLight  = Color(0xFFE8F5E9)
//     val Surface       = Color(0xFFFFFFFF)
//     val Background    = Color(0xFFF6F6F6)
//     val TextPrimary   = Color(0xFF1E1E1E)
//     val TextSecondary = Color(0xFF777777)
//     val TextHint      = Color(0xFFAAAAAA)
//     val Border        = Color(0xFFE0E0E0)
//     val Divider       = Color(0xFFEEEEEE)
//     val Green         = Color(0xFF4CAF50)
//     val GreenBg       = Color(0xFFE8F5E9)
//     val GreenDark     = Color(0xFF388E3C)
//     val Red           = Color(0xFFE53935)
//     val RedBg         = Color(0xFFFFEBEE)
//     val Orange        = Color(0xFFFF9800)
//     val OrangeBg      = Color(0xFFFFF3E0)
//     val CardShadow    = Color(0x0D4CAF50)
//     val Dark          = Color(0xFF1E1E1E)
//     val VerifiedBadge = Color(0xFF4CAF50)
//     val CompliantText = Color(0xFF4CAF50)
// }
// 
// // ════════════════════════════════════════════
// //  SHARED HELPERS
// // ════════════════════════════════════════════
// 
// @Composable
// private fun TopBar(
//     title: String,
//     onBack: (() -> Unit)? = null,
//     leadingContent: (@Composable () -> Unit)? = null,
//     trailingContent: (@Composable () -> Unit)? = null
// ) {
//     Surface(color = T.Surface, shadowElevation = 1.dp) {
//         Row(
//             modifier = Modifier
//                 .fillMaxWidth()
//                 .windowInsetsPadding(WindowInsets.statusBars)
//                 .height(54.dp)
//                 .padding(horizontal = 4.dp),
//             verticalAlignment = Alignment.CenterVertically
//         ) {
//             Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
//                 when {
//                     onBack != null -> IconButton(onClick = onBack) {
//                         Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = T.TextPrimary)
//                     }
//                     leadingContent != null -> leadingContent()
//                 }
//             }
//             Text(
//                 title,
//                 modifier = Modifier.weight(1f),
//                 textAlign = TextAlign.Center,
//                 style = MaterialTheme.typography.titleMedium.copy(
//                     fontSize = 16.sp,
//                     fontWeight = FontWeight.SemiBold,
//                     color = T.TextPrimary
//                 )
//             )
//             Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
//                 trailingContent?.invoke()
//             }
//         }
//     }
// }
// 
// @Composable
// private fun StatusChip(label: String, color: Color = T.Green) {
//     Box(
//         modifier = Modifier
//             .background(color.copy(alpha = 0.12f), RoundedCornerShape(20.dp))
//             .padding(horizontal = 9.dp, vertical = 4.dp)
//     ) {
//         Text(
//             label,
//             style = MaterialTheme.typography.labelSmall.copy(
//                 fontSize = 10.sp,
//                 fontWeight = FontWeight.SemiBold,
//                 color = color,
//                 letterSpacing = 0.2.sp
//             )
//         )
//     }
// }
// 
// @Composable
// private fun ValidChip() {
//     Row(
//         verticalAlignment = Alignment.CenterVertically,
//         horizontalArrangement = Arrangement.spacedBy(3.dp),
//         modifier = Modifier
//             .background(T.GreenBg, RoundedCornerShape(20.dp))
//             .padding(horizontal = 8.dp, vertical = 3.dp)
//     ) {
//         Icon(Icons.Default.CheckCircle, null, Modifier.size(11.dp), tint = T.Green)
//         Text(
//             "VALID",
//             style = MaterialTheme.typography.labelSmall.copy(
//                 fontSize = 10.sp,
//                 fontWeight = FontWeight.Bold,
//                 color = T.Green,
//                 letterSpacing = 0.3.sp
//             )
//         )
//     }
// }
// 
// // ════════════════════════════════════════════
// //  SCREEN 1 — VEHICLE DASHBOARD
// // ════════════════════════════════════════════
// 
// @Composable
// fun VehicleDashboardScreen(
//     onBack: () -> Unit = {},
//     onShowVerifiedProof: () -> Unit = {},
//     onUploadDoc: () -> Unit = {},
//     onEmergency: () -> Unit = {},
//     onVehicleDetails: () -> Unit = {}
// ) {
//     Scaffold(
//         containerColor = T.Background,
//         topBar = {
//             Surface(color = T.Surface, shadowElevation = 1.dp) {
//                 Row(
//                     modifier = Modifier
//                         .fillMaxWidth()
//                         .windowInsetsPadding(WindowInsets.statusBars)
//                         .height(54.dp)
//                         .padding(horizontal = 8.dp),
//                     verticalAlignment = Alignment.CenterVertically
//                 ) {
//                     IconButton(onClick = onBack) {
//                         Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = T.TextPrimary)
//                     }
//                     Text(
//                         "EVA2Z",
//                         modifier = Modifier.weight(1f),
//                         textAlign = TextAlign.Center,
//                         style = MaterialTheme.typography.titleLarge.copy(
//                             fontWeight = FontWeight.Bold,
//                             fontSize = 18.sp,
//                             color = T.Primary
//                         )
//                     )
//                     BadgedBox(
//                         badge = {
//                             Badge(containerColor = T.Red, modifier = Modifier.size(7.dp)) {}
//                         }
//                     ) {
//                         IconButton(onClick = {}) {
//                             Icon(Icons.Outlined.Notifications, null, tint = T.TextPrimary)
//                         }
//                     }
//                 }
//             }
//         },
//         // bottomBar removed — main app provides navigation
//     ) { padding ->
//         Column(
//             modifier = Modifier
//                 .fillMaxSize()
//                 .padding(padding)
//                 .verticalScroll(rememberScrollState())
//                 .padding(horizontal = 16.dp, vertical = 12.dp),
//             verticalArrangement = Arrangement.spacedBy(12.dp)
//         ) {
//             // Vehicle selector
//             VehicleSelectorRow()
// 
//             // Vehicle card
//             VehicleCard(onTap = onVehicleDetails)
// 
//             // Status text
//             Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
//                 Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
//                     Text(
//                         "Vehicle Status: ",
//                         style = MaterialTheme.typography.bodyMedium.copy(
//                             fontSize = 13.5.sp,
//                             color = T.TextPrimary
//                         )
//                     )
//                     Text(
//                         "COMPLIANT",
//                         style = MaterialTheme.typography.bodyMedium.copy(
//                             fontSize = 13.5.sp,
//                             fontWeight = FontWeight.Bold,
//                             color = T.CompliantText
//                         )
//                     )
//                 }
//                 Text(
//                     "Your Insurance, PUC and Registration Certificate (RC) are currently up to date and valid.",
//                     style = MaterialTheme.typography.bodySmall.copy(
//                         color = T.TextSecondary,
//                         fontSize = 12.sp,
//                         lineHeight = 17.sp
//                     )
//                 )
//             }
// 
//             // Status chips
//             Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
//                 StatusChip("INSURANCE ACTIVE")
//                 StatusChip("PUC ACTIVE")
//                 StatusChip("RC ACTIVE")
//             }
// 
//             // Show Verified Proof button
//             Button(
//                 onClick = onShowVerifiedProof,
//                 modifier = Modifier.fillMaxWidth().height(46.dp),
//                 shape = RoundedCornerShape(11.dp),
//                 colors = ButtonDefaults.buttonColors(containerColor = T.Primary)
//             ) {
//                 Icon(Icons.Outlined.Shield, null, Modifier.size(17.dp), tint = Color.White)
//                 Spacer(Modifier.width(7.dp))
//                 Text(
//                     "Show Verified Proof",
//                     fontWeight = FontWeight.SemiBold,
//                     fontSize = 14.sp,
//                     color = Color.White
//                 )
//             }
// 
//             // SOS + Upload row
//             Row(
//                 modifier = Modifier.fillMaxWidth(),
//                 horizontalArrangement = Arrangement.spacedBy(10.dp)
//             ) {
//                 Button(
//                     onClick = onEmergency,
//                     modifier = Modifier.weight(1f).height(42.dp),
//                     shape = RoundedCornerShape(10.dp),
//                     colors = ButtonDefaults.buttonColors(containerColor = T.Red)
//                 ) {
//                     Icon(Icons.Default.Warning, null, Modifier.size(15.dp), tint = Color.White)
//                     Spacer(Modifier.width(5.dp))
//                     Text("Emergency", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = Color.White)
//                 }
//                 OutlinedButton(
//                     onClick = onUploadDoc,
//                     modifier = Modifier.weight(1f).height(42.dp),
//                     shape = RoundedCornerShape(10.dp),
//                     border = BorderStroke(1.5.dp, T.Border),
//                     colors = ButtonDefaults.outlinedButtonColors(contentColor = T.TextPrimary)
//                 ) {
//                     Icon(Icons.Outlined.Upload, null, Modifier.size(15.dp), tint = T.TextPrimary)
//                     Spacer(Modifier.width(5.dp))
//                     Text("Upload Doc", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = T.TextPrimary)
//                 }
//             }
// 
//             // Upcoming alerts
//             Text(
//                 "Upcoming Alerts",
//                 style = MaterialTheme.typography.titleSmall.copy(
//                     fontWeight = FontWeight.Bold,
//                     fontSize = 14.sp,
//                     color = T.TextPrimary
//                 )
//             )
// 
//             AlertCard(
//                 icon = Icons.Default.Warning,
//                 iconTint = T.Orange,
//                 iconBg = T.OrangeBg,
//                 title = "PUC Renewal Required",
//                 subtitle = "Expires in 45 days (Oct 04, 2023)"
//             )
// 
//             Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
//         }
//     }
// }
// 
// @Composable
// private fun VehicleSelectorRow() {
//     Row(
//         modifier = Modifier
//             .fillMaxWidth()
//             .background(T.Surface, RoundedCornerShape(10.dp))
//             .border(BorderStroke(1.dp, T.Border), RoundedCornerShape(10.dp))
//             .padding(horizontal = 12.dp, vertical = 10.dp),
//         verticalAlignment = Alignment.CenterVertically,
//         horizontalArrangement = Arrangement.SpaceBetween
//     ) {
//         Column {
//             Text(
//                 "Select Vehicle",
//                 style = MaterialTheme.typography.labelSmall.copy(
//                     fontSize = 10.sp,
//                     color = T.TextHint,
//                     letterSpacing = 0.3.sp
//                 )
//             )
//             Text(
//                 "Honda Active • KA01AB1234",
//                 style = MaterialTheme.typography.bodyMedium.copy(
//                     fontWeight = FontWeight.SemiBold,
//                     fontSize = 14.sp,
//                     color = T.TextPrimary
//                 )
//             )
//         }
//         Icon(Icons.Default.KeyboardArrowDown, null, tint = T.TextSecondary)
//     }
// }
// 
// @Composable
// private fun VehicleCard(onTap: () -> Unit) {
//     Card(
//         modifier = Modifier
//             .fillMaxWidth()
//             .shadow(3.dp, RoundedCornerShape(16.dp), spotColor = T.CardShadow)
//             .clickable { onTap() },
//         shape = RoundedCornerShape(16.dp),
//         colors = CardDefaults.cardColors(containerColor = T.Surface),
//         elevation = CardDefaults.cardElevation(0.dp)
//     ) {
//         Box(modifier = Modifier.fillMaxWidth().height(160.dp)) {
//             // Gradient background
//             Box(
//                 modifier = Modifier.fillMaxSize().background(
//                     Brush.horizontalGradient(
//                         listOf(Color(0xFF388E3C), Color(0xFF4CAF50), Color(0xFF66BB6A))
//                     )
//                 )
//             )
//             // Subtle mesh
//             Box(
//                 modifier = Modifier.fillMaxSize().background(
//                     Brush.radialGradient(
//                         listOf(Color.White.copy(0.07f), Color.Transparent),
//                         radius = 400f
//                     )
//                 )
//             )
//             // Vehicle silhouette
//             Box(
//                 modifier = Modifier.fillMaxSize(),
//                 contentAlignment = Alignment.Center
//             ) {
//                 Icon(
//                     Icons.Outlined.TwoWheeler,
//                     null,
//                     Modifier.size(90.dp),
//                     tint = Color.White.copy(alpha = 0.22f)
//                 )
//             }
//             // Verified badge
//             Row(
//                 modifier = Modifier
//                     .align(Alignment.TopStart)
//                     .padding(12.dp)
//                     .background(T.Green, RoundedCornerShape(20.dp))
//                     .padding(horizontal = 10.dp, vertical = 4.dp),
//                 verticalAlignment = Alignment.CenterVertically,
//                 horizontalArrangement = Arrangement.spacedBy(4.dp)
//             ) {
//                 Icon(Icons.Default.Verified, null, Modifier.size(12.dp), tint = Color.White)
//                 Text(
//                     "Verified",
//                     style = MaterialTheme.typography.labelSmall.copy(
//                         fontWeight = FontWeight.Bold,
//                         fontSize = 10.sp,
//                         color = Color.White
//                     )
//                 )
//             }
//         }
//     }
// }
// 
// @Composable
// private fun AlertCard(
//     icon: ImageVector,
//     iconTint: Color,
//     iconBg: Color,
//     title: String,
//     subtitle: String
// ) {
//     Card(
//         modifier = Modifier.fillMaxWidth(),
//         shape = RoundedCornerShape(12.dp),
//         colors = CardDefaults.cardColors(containerColor = T.Surface),
//         elevation = CardDefaults.cardElevation(1.dp)
//     ) {
//         Row(
//             modifier = Modifier
//                 .fillMaxWidth()
//                 .padding(12.dp),
//             verticalAlignment = Alignment.CenterVertically,
//             horizontalArrangement = Arrangement.spacedBy(12.dp)
//         ) {
//             Box(
//                 Modifier.size(38.dp).background(iconBg, RoundedCornerShape(9.dp)),
//                 contentAlignment = Alignment.Center
//             ) {
//                 Icon(icon, null, Modifier.size(19.dp), tint = iconTint)
//             }
//             Column(modifier = Modifier.weight(1f)) {
//                 Text(
//                     title,
//                     style = MaterialTheme.typography.bodyMedium.copy(
//                         fontWeight = FontWeight.SemiBold,
//                         fontSize = 13.sp,
//                         color = T.TextPrimary
//                     )
//                 )
//                 Text(
//                     subtitle,
//                     style = MaterialTheme.typography.bodySmall.copy(
//                         fontSize = 11.5.sp,
//                         color = T.TextSecondary
//                     )
//                 )
//             }
//             Icon(Icons.Default.ChevronRight, null, tint = T.TextHint, modifier = Modifier.size(18.dp))
//         }
//     }
// }
// 
// @Composable
// private fun DashboardBottomNav() {
//     Surface(color = T.Surface, shadowElevation = 8.dp) {
//         Row(
//             modifier = Modifier
//                 .fillMaxWidth()
//                 .navigationBarsPadding()
//                 .height(58.dp)
//                 .padding(horizontal = 8.dp),
//             horizontalArrangement = Arrangement.SpaceAround,
//             verticalAlignment = Alignment.CenterVertically
//         ) {
//             listOf(
//                 Triple(Icons.Default.Home,         Icons.Outlined.Home,         "Home"),
//                 Triple(Icons.Default.DirectionsCar,Icons.Outlined.DirectionsCar,"Vehicle"),
//                 Triple(Icons.Default.Notifications, Icons.Outlined.Notifications,"Alerts"),
//                 Triple(Icons.Default.Person,        Icons.Outlined.Person,       "Profile")
//             ).forEachIndexed { i, (filled, outlined, label) ->
//                 val sel = i == 0
//                 Column(
//                     horizontalAlignment = Alignment.CenterHorizontally,
//                     modifier = Modifier
//                         .clip(RoundedCornerShape(8.dp))
//                         .clickable {}
//                         .padding(horizontal = 12.dp, vertical = 6.dp)
//                 ) {
//                     Icon(
//                         if (sel) filled else outlined,
//                         label, Modifier.size(21.dp),
//                         tint = if (sel) T.Primary else T.TextHint
//                     )
//                     Spacer(Modifier.height(2.dp))
//                     Text(
//                         label, style = MaterialTheme.typography.labelSmall.copy(
//                             fontSize = 10.sp,
//                             fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal,
//                             color = if (sel) T.Primary else T.TextHint
//                         )
//                     )
//                 }
//             }
//         }
//     }
// }
// 
// // ════════════════════════════════════════════
// //  SCREEN 2 — VERIFIED VEHICLE PROOF
// // ════════════════════════════════════════════
// 
// @Composable
// fun VerifiedVehicleProofScreen(
//     onBack: () -> Unit = {},
//     onRefresh: () -> Unit = {},
//     onShare: () -> Unit = {}
// ) {
//     Scaffold(
//         containerColor = T.Background,
//         topBar = { TopBar("Verified Vehicle Proof", onBack = onBack) }
//     ) { padding ->
//         Column(
//             modifier = Modifier
//                 .fillMaxSize()
//                 .padding(padding)
//         ) {
//             Column(
//                 modifier = Modifier
//                     .weight(1f)
//                     .verticalScroll(rememberScrollState())
//                     .padding(horizontal = 16.dp, vertical = 14.dp),
//                 horizontalAlignment = Alignment.CenterHorizontally,
//                 verticalArrangement = Arrangement.spacedBy(16.dp)
//             ) {
//                 // Vehicle image + identity
//                 Column(
//                     horizontalAlignment = Alignment.CenterHorizontally,
//                     verticalArrangement = Arrangement.spacedBy(8.dp)
//                 ) {
//                     // Vehicle illustration
//                     Box(
//                         modifier = Modifier
//                             .size(130.dp)
//                             .background(
//                                 Brush.radialGradient(listOf(Color(0xFFE8F5E9), Color(0xFFF6F6F6))),
//                                 RoundedCornerShape(16.dp)
//                             ),
//                         contentAlignment = Alignment.Center
//                     ) {
//                         Icon(
//                             Icons.Outlined.TwoWheeler, null,
//                             Modifier.size(75.dp), tint = T.Primary.copy(0.35f)
//                         )
//                     }
//                     Text(
//                         "KA01AB1234",
//                         style = MaterialTheme.typography.headlineMedium.copy(
//                             fontWeight = FontWeight.Bold,
//                             fontSize = 24.sp,
//                             color = T.TextPrimary
//                         )
//                     )
//                     Text(
//                         "Rahul Sharma",
//                         style = MaterialTheme.typography.bodyLarge.copy(
//                             fontWeight = FontWeight.SemiBold,
//                             fontSize = 15.sp,
//                             color = T.TextPrimary
//                         )
//                     )
//                     Text(
//                         "Two Wheeler · Electric",
//                         style = MaterialTheme.typography.bodySmall.copy(
//                             color = T.TextSecondary,
//                             fontSize = 12.5.sp
//                         )
//                     )
//                 }
// 
//                 // QR code card
//                 Card(
//                     modifier = Modifier.fillMaxWidth(),
//                     shape = RoundedCornerShape(16.dp),
//                     colors = CardDefaults.cardColors(containerColor = T.Surface),
//                     elevation = CardDefaults.cardElevation(2.dp)
//                 ) {
//                     Column(
//                         modifier = Modifier
//                             .fillMaxWidth()
//                             .padding(18.dp),
//                         horizontalAlignment = Alignment.CenterHorizontally,
//                         verticalArrangement = Arrangement.spacedBy(12.dp)
//                     ) {
//                         // QR placeholder
//                         Box(
//                             modifier = Modifier
//                                 .size(120.dp)
//                                 .background(Color(0xFF1B3D1E), RoundedCornerShape(12.dp))
//                                 .padding(10.dp),
//                             contentAlignment = Alignment.Center
//                         ) {
//                             QrCodePlaceholder()
//                         }
//                         Text(
//                             "SCAN TO VERIFY AUTHENTICITY",
//                             style = MaterialTheme.typography.labelSmall.copy(
//                                 fontSize = 10.sp,
//                                 color = T.TextHint,
//                                 letterSpacing = 0.7.sp,
//                                 fontWeight = FontWeight.SemiBold
//                             )
//                         )
//                     }
//                 }
// 
//                 // Document status
//                 Card(
//                     modifier = Modifier.fillMaxWidth(),
//                     shape = RoundedCornerShape(16.dp),
//                     colors = CardDefaults.cardColors(containerColor = T.Surface),
//                     elevation = CardDefaults.cardElevation(1.dp)
//                 ) {
//                     Column(
//                         modifier = Modifier.fillMaxWidth().padding(16.dp),
//                         verticalArrangement = Arrangement.spacedBy(0.dp)
//                     ) {
//                         Text(
//                             "DOCUMENT STATUS",
//                             style = MaterialTheme.typography.labelSmall.copy(
//                                 fontSize = 10.5.sp,
//                                 fontWeight = FontWeight.Bold,
//                                 color = T.TextSecondary,
//                                 letterSpacing = 0.6.sp
//                             )
//                         )
//                         Spacer(Modifier.height(12.dp))
// 
//                         listOf(
//                             Pair(Icons.Outlined.Article, "Registration (RC)"),
//                             Pair(Icons.Outlined.Shield,  "Insurance"),
//                             Pair(Icons.Outlined.Eco,     "PUC Certificate")
//                         ).forEachIndexed { i, (icon, name) ->
//                             if (i > 0) HorizontalDivider(color = T.Divider, modifier = Modifier.padding(vertical = 8.dp))
//                             Row(
//                                 modifier = Modifier.fillMaxWidth(),
//                                 verticalAlignment = Alignment.CenterVertically,
//                                 horizontalArrangement = Arrangement.spacedBy(10.dp)
//                             ) {
//                                 Icon(icon, null, Modifier.size(18.dp), tint = T.Primary)
//                                 Text(
//                                     name,
//                                     modifier = Modifier.weight(1f),
//                                     style = MaterialTheme.typography.bodyMedium.copy(
//                                         fontSize = 13.5.sp,
//                                         color = T.TextPrimary,
//                                         fontWeight = FontWeight.Medium
//                                     )
//                                 )
//                                 ValidChip()
//                             }
//                         }
// 
//                         Spacer(Modifier.height(14.dp))
//                         HorizontalDivider(color = T.Divider)
//                         Spacer(Modifier.height(10.dp))
// 
//                         Row(
//                             modifier = Modifier.fillMaxWidth(),
//                             horizontalArrangement = Arrangement.SpaceBetween,
//                             verticalAlignment = Alignment.CenterVertically
//                         ) {
//                             Text(
//                                 "Generated: 05 Mar 2026 | 10:42 AM",
//                                 style = MaterialTheme.typography.labelSmall.copy(
//                                     color = T.TextHint,
//                                     fontSize = 11.sp
//                                 )
//                             )
//                         }
//                         Spacer(Modifier.height(2.dp))
//                         Text(
//                             "✦ VERIFIED VIA EVA2Z",
//                             style = MaterialTheme.typography.labelSmall.copy(
//                                 color = T.Primary,
//                                 fontWeight = FontWeight.Bold,
//                                 fontSize = 11.sp,
//                                 letterSpacing = 0.3.sp
//                             )
//                         )
//                     }
//                 }
//             }
// 
//             // Bottom action buttons
//             Surface(color = T.Surface, shadowElevation = 4.dp) {
//                 Column(
//                     modifier = Modifier
//                         .fillMaxWidth()
//                         .navigationBarsPadding()
//                         .padding(horizontal = 16.dp, vertical = 12.dp),
//                     verticalArrangement = Arrangement.spacedBy(10.dp)
//                 ) {
//                     Button(
//                         onClick = onRefresh,
//                         modifier = Modifier.fillMaxWidth().height(48.dp),
//                         shape = RoundedCornerShape(12.dp),
//                         colors = ButtonDefaults.buttonColors(containerColor = T.Primary)
//                     ) {
//                         Icon(Icons.Default.Refresh, null, Modifier.size(17.dp), tint = Color.White)
//                         Spacer(Modifier.width(6.dp))
//                         Text("Refresh Proof", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Color.White)
//                     }
//                     OutlinedButton(
//                         onClick = onShare,
//                         modifier = Modifier.fillMaxWidth().height(48.dp),
//                         shape = RoundedCornerShape(12.dp),
//                         border = BorderStroke(1.5.dp, T.Border),
//                         colors = ButtonDefaults.outlinedButtonColors(contentColor = T.TextPrimary)
//                     ) {
//                         Icon(Icons.Outlined.Share, null, Modifier.size(17.dp), tint = T.TextPrimary)
//                         Spacer(Modifier.width(6.dp))
//                         Text("Share Proof", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = T.TextPrimary)
//                     }
//                 }
//             }
//         }
//     }
// }
// 
// @Composable
// private fun QrCodePlaceholder() {
//     // Simplified QR-like grid
//     val cells = remember {
//         (0 until 7).map { r -> (0 until 7).map { c ->
//             val corner = (r < 2 && c < 2) || (r < 2 && c > 4) || (r > 4 && c < 2)
//             corner || (r == 3 && c % 2 == 0) || (r % 2 == 0 && c == 3) || (r + c) % 3 == 0
//         }}
//     }
//     Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
//         cells.forEach { row ->
//             Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
//                 row.forEach { filled ->
//                     Box(
//                         Modifier.size(12.dp).background(
//                             if (filled) Color.White else Color.Transparent,
//                             RoundedCornerShape(2.dp)
//                         )
//                     )
//                 }
//             }
//         }
//     }
// }
// 
// // ════════════════════════════════════════════
// //  SCREEN 3 — EVA2Z VERIFICATION
// // ════════════════════════════════════════════
// 
// @Composable
// fun Eva2zVerificationScreen(
//     onBack: () -> Unit = {},
//     onExportPdf: () -> Unit = {},
//     onAuditLog: () -> Unit = {}
// ) {
//     Scaffold(
//         containerColor = T.Background,
//         topBar = {
//             Surface(color = T.Surface, shadowElevation = 1.dp) {
//                 Row(
//                     modifier = Modifier
//                         .fillMaxWidth()
//                         .windowInsetsPadding(WindowInsets.statusBars)
//                         .height(54.dp)
//                         .padding(horizontal = 4.dp),
//                     verticalAlignment = Alignment.CenterVertically
//                 ) {
//                     Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
//                         Icon(Icons.Outlined.Shield, null, Modifier.size(22.dp), tint = T.Primary)
//                     }
//                     Text(
//                         "EVA2Z Verification",
//                         modifier = Modifier.weight(1f),
//                         textAlign = TextAlign.Center,
//                         style = MaterialTheme.typography.titleMedium.copy(
//                             fontWeight = FontWeight.SemiBold,
//                             fontSize = 16.sp,
//                             color = T.TextPrimary
//                         )
//                     )
//                     IconButton(onClick = {}) {
//                         Icon(Icons.Default.MoreVert, null, tint = T.TextSecondary)
//                     }
//                 }
//             }
//         }
//     ) { padding ->
//         Column(
//             modifier = Modifier
//                 .fillMaxSize()
//                 .padding(padding)
//                 .verticalScroll(rememberScrollState())
//                 .padding(horizontal = 16.dp, vertical = 14.dp),
//             verticalArrangement = Arrangement.spacedBy(14.dp)
//         ) {
//             // Hero card
//             Card(
//                 modifier = Modifier.fillMaxWidth(),
//                 shape = RoundedCornerShape(18.dp),
//                 colors = CardDefaults.cardColors(containerColor = T.Surface),
//                 elevation = CardDefaults.cardElevation(2.dp)
//             ) {
//                 Column(
//                     modifier = Modifier.fillMaxWidth().padding(18.dp),
//                     horizontalAlignment = Alignment.CenterHorizontally,
//                     verticalArrangement = Arrangement.spacedBy(10.dp)
//                 ) {
//                     Box(
//                         modifier = Modifier
//                             .size(110.dp)
//                             .background(
//                                 Brush.radialGradient(listOf(Color(0xFFE8F5E9), Color(0xFFF6F6F6))),
//                                 RoundedCornerShape(14.dp)
//                             ),
//                         contentAlignment = Alignment.Center
//                     ) {
//                         Icon(
//                             Icons.Outlined.ElectricScooter, null,
//                             Modifier.size(65.dp), tint = T.Primary.copy(0.3f)
//                         )
//                     }
// 
//                     Text(
//                         "KA 01 AB 1234",
//                         style = MaterialTheme.typography.headlineMedium.copy(
//                             fontWeight = FontWeight.ExtraBold,
//                             fontSize = 26.sp,
//                             color = T.TextPrimary
//                         )
//                     )
//                     Row(
//                         verticalAlignment = Alignment.CenterVertically,
//                         horizontalArrangement = Arrangement.spacedBy(5.dp)
//                     ) {
//                         Icon(Icons.Default.CheckCircle, null, Modifier.size(13.dp), tint = T.Green)
//                         Text(
//                             "Active Registration · Verified",
//                             style = MaterialTheme.typography.bodySmall.copy(
//                                 color = T.Green,
//                                 fontWeight = FontWeight.SemiBold,
//                                 fontSize = 12.5.sp
//                             )
//                         )
//                     }
// 
//                     // Verified banner
//                     Row(
//                         modifier = Modifier
//                             .fillMaxWidth()
//                             .background(T.Primary.copy(0.08f), RoundedCornerShape(10.dp))
//                             .padding(horizontal = 14.dp, vertical = 10.dp),
//                         verticalAlignment = Alignment.CenterVertically,
//                         horizontalArrangement = Arrangement.spacedBy(10.dp)
//                     ) {
//                         Icon(Icons.Default.Verified, null, Modifier.size(22.dp), tint = T.Primary)
//                         Column {
//                             Text(
//                                 "Verified by EVA2Z System",
//                                 style = MaterialTheme.typography.bodyMedium.copy(
//                                     fontWeight = FontWeight.Bold,
//                                     fontSize = 13.sp,
//                                     color = T.Primary
//                                 )
//                             )
//                             Text(
//                                 "Official Credentials Confirmed",
//                                 style = MaterialTheme.typography.bodySmall.copy(
//                                     color = T.Primary.copy(0.7f),
//                                     fontSize = 11.5.sp
//                                 )
//                             )
//                         }
//                         Spacer(Modifier.weight(1f))
//                         Box(
//                             Modifier.size(30.dp).background(T.Primary.copy(0.15f), CircleShape),
//                             contentAlignment = Alignment.Center
//                         ) {
//                             Icon(Icons.Outlined.Verified, null, Modifier.size(16.dp), tint = T.Primary)
//                         }
//                     }
//                 }
//             }
// 
//             // Vehicle details card
//             Card(
//                 modifier = Modifier.fillMaxWidth(),
//                 shape = RoundedCornerShape(16.dp),
//                 colors = CardDefaults.cardColors(containerColor = T.Surface),
//                 elevation = CardDefaults.cardElevation(1.dp)
//             ) {
//                 Column(
//                     modifier = Modifier.fillMaxWidth().padding(16.dp),
//                     verticalArrangement = Arrangement.spacedBy(0.dp)
//                 ) {
//                     Row(
//                         verticalAlignment = Alignment.CenterVertically,
//                         horizontalArrangement = Arrangement.spacedBy(6.dp)
//                     ) {
//                         Icon(Icons.Outlined.Info, null, Modifier.size(15.dp), tint = T.TextSecondary)
//                         Text(
//                             "Vehicle Details",
//                             style = MaterialTheme.typography.titleSmall.copy(
//                                 fontWeight = FontWeight.Bold,
//                                 fontSize = 14.sp,
//                                 color = T.TextPrimary
//                             )
//                         )
//                     }
// 
//                     Spacer(Modifier.height(14.dp))
// 
//                     // 2-col detail grid
//                     Row(
//                         modifier = Modifier.fillMaxWidth(),
//                         horizontalArrangement = Arrangement.spacedBy(16.dp)
//                     ) {
//                         VerifDetailCell("OWNER NAME", "John Doe", modifier = Modifier.weight(1f))
//                         VerifDetailCell("VEHICLE TYPE", "Electric Scooter", modifier = Modifier.weight(1f))
//                     }
// 
//                     Spacer(Modifier.height(14.dp))
// 
//                     Row(
//                         modifier = Modifier.fillMaxWidth(),
//                         horizontalArrangement = Arrangement.spacedBy(16.dp)
//                     ) {
//                         VerifDetailCell(
//                             "INSURANCE VALIDITY", "20 Mar 2027",
//                             valueColor = T.Green, modifier = Modifier.weight(1f)
//                         )
//                         VerifDetailCell(
//                             "PUC VALIDITY", "15 Dec 2026",
//                             valueColor = T.Red, modifier = Modifier.weight(1f)
//                         )
//                     }
// 
//                     Spacer(Modifier.height(14.dp))
//                     HorizontalDivider(color = T.Divider)
//                     Spacer(Modifier.height(10.dp))
// 
//                     Text(
//                         "◎  Verification ID: EV-9038-4621 · 4815",
//                         style = MaterialTheme.typography.labelSmall.copy(
//                             fontSize = 11.sp,
//                             color = T.TextHint
//                         )
//                     )
//                     Spacer(Modifier.height(3.dp))
//                     Text(
//                         "Generated: 09 Mar 2026 | 10:42 AM",
//                         style = MaterialTheme.typography.labelSmall.copy(
//                             fontSize = 11.sp,
//                             color = T.TextHint
//                         )
//                     )
//                 }
//             }
// 
//             // Action row
//             Row(
//                 modifier = Modifier.fillMaxWidth(),
//                 horizontalArrangement = Arrangement.spacedBy(12.dp)
//             ) {
//                 Button(
//                     onClick = onExportPdf,
//                     modifier = Modifier.weight(1f).height(46.dp),
//                     shape = RoundedCornerShape(12.dp),
//                     colors = ButtonDefaults.buttonColors(containerColor = T.Primary)
//                 ) {
//                     Icon(Icons.Outlined.PictureAsPdf, null, Modifier.size(16.dp), tint = Color.White)
//                     Spacer(Modifier.width(5.dp))
//                     Text("Export PDF", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = Color.White)
//                 }
//                 OutlinedButton(
//                     onClick = onAuditLog,
//                     modifier = Modifier.weight(1f).height(46.dp),
//                     shape = RoundedCornerShape(12.dp),
//                     border = BorderStroke(1.5.dp, T.Border),
//                     colors = ButtonDefaults.outlinedButtonColors(contentColor = T.TextPrimary)
//                 ) {
//                     Icon(Icons.Outlined.History, null, Modifier.size(16.dp), tint = T.TextSecondary)
//                     Spacer(Modifier.width(5.dp))
//                     Text("Audit Log", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = T.TextPrimary)
//                 }
//             }
// 
//             Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
//         }
//     }
// }
// 
// @Composable
// private fun VerifDetailCell(
//     label: String,
//     value: String,
//     valueColor: Color = T.TextPrimary,
//     modifier: Modifier = Modifier
// ) {
//     Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(3.dp)) {
//         Text(
//             label,
//             style = MaterialTheme.typography.labelSmall.copy(
//                 fontSize = 10.sp,
//                 color = T.TextHint,
//                 letterSpacing = 0.5.sp,
//                 fontWeight = FontWeight.SemiBold
//             )
//         )
//         Text(
//             value,
//             style = MaterialTheme.typography.bodyMedium.copy(
//                 fontSize = 14.sp,
//                 fontWeight = FontWeight.Bold,
//                 color = valueColor
//             )
//         )
//     }
// }
// 
// // ════════════════════════════════════════════
// //  SCREEN 4 — VEHICLE DETAILS + SHARE SHEET
// // ════════════════════════════════════════════
// 
// @Composable
// fun VehicleDetailsScreen(
//     onBack: () -> Unit = {}
// ) {
//     var showShareSheet by remember { mutableStateOf(false) }
// 
//     Box(modifier = Modifier.fillMaxSize()) {
//         Scaffold(
//             containerColor = T.Background,
//             topBar = {
//                 Surface(color = T.Surface, shadowElevation = 1.dp) {
//                     Row(
//                         modifier = Modifier
//                             .fillMaxWidth()
//                             .windowInsetsPadding(WindowInsets.statusBars)
//                             .height(54.dp)
//                             .padding(horizontal = 4.dp),
//                         verticalAlignment = Alignment.CenterVertically
//                     ) {
//                         IconButton(onClick = onBack) {
//                             Icon(Icons.Default.ChevronLeft, "Back", tint = T.TextPrimary, modifier = Modifier.size(26.dp))
//                         }
//                         Text(
//                             "Vehicle Details",
//                             modifier = Modifier.weight(1f),
//                             textAlign = TextAlign.Center,
//                             style = MaterialTheme.typography.titleMedium.copy(
//                                 fontWeight = FontWeight.SemiBold,
//                                 fontSize = 16.sp,
//                                 color = T.TextPrimary
//                             )
//                         )
//                         IconButton(onClick = {}) {
//                             Icon(Icons.Default.MoreVert, null, tint = T.TextSecondary)
//                         }
//                     }
//                 }
//             }
//         ) { padding ->
//             Column(
//                 modifier = Modifier
//                     .fillMaxSize()
//                     .padding(padding)
//                     .verticalScroll(rememberScrollState())
//             ) {
//                 // Hero vehicle image
//                 Box(
//                     modifier = Modifier
//                         .fillMaxWidth()
//                         .height(200.dp)
//                 ) {
//                     Box(
//                         modifier = Modifier.fillMaxSize().background(
//                             Brush.verticalGradient(
//                                 listOf(Color(0xFF1B2B4D), Color(0xFF0D1B3E))
//                             )
//                         )
//                     )
//                     Box(
//                         modifier = Modifier.fillMaxSize().background(
//                             Brush.radialGradient(
//                                 listOf(Color(0xFF4CAF50).copy(0.3f), Color.Transparent),
//                                 radius = 500f
//                             )
//                         )
//                     )
//                     // Car silhouette
//                     Box(
//                         modifier = Modifier.fillMaxSize(),
//                         contentAlignment = Alignment.Center
//                     ) {
//                         Icon(
//                             Icons.Outlined.ElectricCar, null,
//                             Modifier.size(110.dp),
//                             tint = Color.White.copy(0.18f)
//                         )
//                     }
//                 }
// 
//                 Column(
//                     modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
//                     verticalArrangement = Arrangement.spacedBy(14.dp)
//                 ) {
//                     // Name + verified row
//                     Row(
//                         modifier = Modifier.fillMaxWidth(),
//                         verticalAlignment = Alignment.CenterVertically,
//                         horizontalArrangement = Arrangement.SpaceBetween
//                     ) {
//                         Column {
//                             Text(
//                                 "EVA2Z Pro Connect",
//                                 style = MaterialTheme.typography.titleLarge.copy(
//                                     fontWeight = FontWeight.ExtraBold,
//                                     fontSize = 19.sp,
//                                     color = T.TextPrimary
//                                 )
//                             )
//                             Text(
//                                 "Reg: MH-02-EV-2024",
//                                 style = MaterialTheme.typography.bodySmall.copy(
//                                     color = T.TextSecondary,
//                                     fontSize = 12.sp
//                                 )
//                             )
//                         }
//                         Box(
//                             modifier = Modifier
//                                 .background(T.GreenBg, RoundedCornerShape(8.dp))
//                                 .padding(horizontal = 10.dp, vertical = 5.dp)
//                         ) {
//                             Row(
//                                 verticalAlignment = Alignment.CenterVertically,
//                                 horizontalArrangement = Arrangement.spacedBy(4.dp)
//                             ) {
//                                 Icon(Icons.Default.Verified, null, Modifier.size(13.dp), tint = T.Green)
//                                 Text(
//                                     "VERIFIED",
//                                     style = MaterialTheme.typography.labelSmall.copy(
//                                         fontWeight = FontWeight.ExtraBold,
//                                         fontSize = 10.sp,
//                                         color = T.Green,
//                                         letterSpacing = 0.4.sp
//                                     )
//                                 )
//                             }
//                         }
//                     }
// 
//                     // Quick stats row
//                     Row(
//                         modifier = Modifier.fillMaxWidth(),
//                         horizontalArrangement = Arrangement.spacedBy(10.dp)
//                     ) {
//                         QuickStatCard("Insurance", "Active", T.Green, T.GreenBg, Modifier.weight(1f))
//                         QuickStatCard("PUC", "Valid", T.Green, T.GreenBg, Modifier.weight(1f))
//                         QuickStatCard("RC", "Active", T.Green, T.GreenBg, Modifier.weight(1f))
//                     }
// 
//                     // Generate proof button
//                     Button(
//                         onClick = { showShareSheet = true },
//                         modifier = Modifier.fillMaxWidth().height(50.dp),
//                         shape = RoundedCornerShape(12.dp),
//                         colors = ButtonDefaults.buttonColors(containerColor = T.Primary)
//                     ) {
//                         Icon(Icons.Outlined.Shield, null, Modifier.size(17.dp), tint = Color.White)
//                         Spacer(Modifier.width(7.dp))
//                         Text("Generate & Share Proof", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Color.White)
//                     }
//                 }
//             }
//         }
// 
//         // Proof share options sheet
//         if (showShareSheet) {
//             ProofShareOptionsSheet(onDismiss = { showShareSheet = false })
//         }
//     }
// }
// 
// @Composable
// private fun QuickStatCard(
//     label: String,
//     value: String,
//     valueColor: Color,
//     bgColor: Color,
//     modifier: Modifier = Modifier
// ) {
//     Column(
//         modifier = modifier
//             .background(bgColor, RoundedCornerShape(10.dp))
//             .padding(vertical = 10.dp, horizontal = 8.dp),
//         horizontalAlignment = Alignment.CenterHorizontally,
//         verticalArrangement = Arrangement.spacedBy(3.dp)
//     ) {
//         Text(
//             label,
//             style = MaterialTheme.typography.labelSmall.copy(
//                 fontSize = 10.sp,
//                 color = T.TextSecondary,
//                 fontWeight = FontWeight.Medium
//             )
//         )
//         Text(
//             value,
//             style = MaterialTheme.typography.labelMedium.copy(
//                 fontSize = 12.sp,
//                 fontWeight = FontWeight.Bold,
//                 color = valueColor
//             )
//         )
//     }
// }
// 
// // ════════════════════════════════════════════
// //  PROOF SHARE OPTIONS SHEET
// // ════════════════════════════════════════════
// 
// @Composable
// private fun ProofShareOptionsSheet(onDismiss: () -> Unit) {
//     Dialog(
//         onDismissRequest = onDismiss,
//         properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = true)
//     ) {
//         Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
//             Box(
//                 modifier = Modifier
//                     .fillMaxSize()
//                     .background(Color.Black.copy(0.45f))
//                     .clickable { onDismiss() }
//             )
//             Surface(
//                 modifier = Modifier.fillMaxWidth(),
//                 shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp),
//                 color = T.Surface
//             ) {
//                 Column(
//                     modifier = Modifier
//                         .fillMaxWidth()
//                         .navigationBarsPadding()
//                         .padding(bottom = 16.dp)
//                 ) {
//                     // Handle
//                     Box(
//                         Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 6.dp),
//                         contentAlignment = Alignment.Center
//                     ) {
//                         Box(
//                             Modifier.width(36.dp).height(4.dp)
//                                 .background(T.Border, RoundedCornerShape(2.dp))
//                         )
//                     }
// 
//                     Column(modifier = Modifier.padding(horizontal = 18.dp)) {
//                         Text(
//                             "Proof Share Options",
//                             style = MaterialTheme.typography.titleMedium.copy(
//                                 fontWeight = FontWeight.Bold,
//                                 fontSize = 16.sp,
//                                 color = T.TextPrimary
//                             )
//                         )
//                         Spacer(Modifier.height(3.dp))
//                         Text(
//                             "Select how you'd like to share vehicle verification proof.",
//                             style = MaterialTheme.typography.bodySmall.copy(
//                                 color = T.TextSecondary,
//                                 fontSize = 12.sp,
//                                 lineHeight = 16.sp
//                             )
//                         )
// 
//                         Spacer(Modifier.height(16.dp))
// 
//                         listOf(
//                             ShareOption(Icons.Outlined.Link,           "Share Link",         "Send a direct access link",          T.Primary,  T.PrimaryLight),
//                             ShareOption(Icons.Outlined.PictureAsPdf,   "Download PDF",       "Offline verification document",       T.Red,      T.RedBg),
//                             ShareOption(Icons.Outlined.Chat,           "Send via WhatsApp",  "Fastest way to share",               T.Green,    T.GreenBg),
//                             ShareOption(Icons.Outlined.ContentCopy,    "Copy Link",          "Save link to clipboard",             T.Orange,   T.OrangeBg)
//                         ).forEach { opt ->
//                             ShareOptionRow(opt = opt, onClick = { onDismiss() })
//                             HorizontalDivider(color = T.Divider, modifier = Modifier.padding(vertical = 2.dp))
//                         }
// 
//                         Spacer(Modifier.height(12.dp))
// 
//                         OutlinedButton(
//                             onClick = onDismiss,
//                             modifier = Modifier.fillMaxWidth().height(48.dp),
//                             shape = RoundedCornerShape(12.dp),
//                             border = BorderStroke(1.5.dp, T.Border),
//                             colors = ButtonDefaults.outlinedButtonColors(contentColor = T.TextPrimary)
//                         ) {
//                             Text("Cancel", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = T.TextPrimary)
//                         }
//                     }
//                 }
//             }
//         }
//     }
// }
// 
// private data class ShareOption(
//     val icon: ImageVector,
//     val title: String,
//     val subtitle: String,
//     val iconTint: Color,
//     val iconBg: Color
// )
// 
// @Composable
// private fun ShareOptionRow(opt: ShareOption, onClick: () -> Unit) {
//     Row(
//         modifier = Modifier
//             .fillMaxWidth()
//             .clip(RoundedCornerShape(10.dp))
//             .clickable { onClick() }
//             .padding(vertical = 10.dp),
//         verticalAlignment = Alignment.CenterVertically,
//         horizontalArrangement = Arrangement.spacedBy(14.dp)
//     ) {
//         Box(
//             Modifier.size(42.dp).background(opt.iconBg, RoundedCornerShape(10.dp)),
//             contentAlignment = Alignment.Center
//         ) {
//             Icon(opt.icon, null, Modifier.size(20.dp), tint = opt.iconTint)
//         }
//         Column(modifier = Modifier.weight(1f)) {
//             Text(
//                 opt.title,
//                 style = MaterialTheme.typography.bodyMedium.copy(
//                     fontWeight = FontWeight.SemiBold,
//                     fontSize = 14.sp,
//                     color = T.TextPrimary
//                 )
//             )
//             Text(
//                 opt.subtitle,
//                 style = MaterialTheme.typography.bodySmall.copy(
//                     color = T.TextSecondary,
//                     fontSize = 11.5.sp
//                 )
//             )
//         }
//         Icon(Icons.Default.ChevronRight, null, tint = T.TextHint, modifier = Modifier.size(18.dp))
//     }
// }