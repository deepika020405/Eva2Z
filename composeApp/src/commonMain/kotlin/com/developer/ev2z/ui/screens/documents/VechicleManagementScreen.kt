package com.developer.ev2z.ui.screens.documents

// ══════════════════════════════════════════════════════════════════
// COMMENTED OUT: DUMMY UI CODE (for reference)
// ══════════════════════════════════════════════════════════════════

// 
// import androidx.compose.animation.*
// import androidx.compose.animation.core.*
// import androidx.compose.foundation.*
// import androidx.compose.foundation.layout.*
// import androidx.compose.foundation.lazy.LazyColumn
// import androidx.compose.foundation.lazy.LazyRow
// import androidx.compose.foundation.lazy.items
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
// import androidx.compose.ui.text.style.TextOverflow
// import androidx.compose.ui.unit.dp
// import androidx.compose.ui.unit.sp
// import androidx.compose.ui.window.Dialog
// import androidx.compose.ui.window.DialogProperties
// 
// // ═══════════════════════════════════════
// //  TOKENS
// // ═══════════════════════════════════════
// 
// private object V {
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
//     val Orange        = Color(0xFFFF9800)
//     val OrangeBg      = Color(0xFFFFF3E0)
//     val Red           = Color(0xFFE53935)
//     val RedBg         = Color(0xFFFFEBEE)
//     val CardShadow    = Color(0x0D4CAF50)
// }
// 
// // ═══════════════════════════════════════
// //  DATA MODELS
// // ═══════════════════════════════════════
// 
// enum class VehicleStatus { COMPLIANT, ATTENTION_REQUIRED, EXPIRED }
// 
// data class VehicleItem(
//     val id: String,
//     val name: String,
//     val regNo: String,
//     val status: VehicleStatus,
//     val actionLabel: String? = null,
//     val iconTint: Color = Color(0xFF4CAF50)
// )
// 
// data class DocumentFile(
//     val name: String,
//     val type: String  // "pdf" or "image"
// )
// 
// data class ComplianceItem(
//     val label: String,
//     val detail: String,
//     val status: VehicleStatus
// )
// 
// private val sampleVehicles = listOf(
//     VehicleItem("1", "Honda Activa 6G", "Reg No: DL 1S AB 1234", VehicleStatus.COMPLIANT),
//     VehicleItem("2", "Hyundai i20 Aste", "Reg No: DL 3C CC 5678", VehicleStatus.ATTENTION_REQUIRED, "Renew Insurance"),
//     VehicleItem("3", "Royal Enfield Classic", "Reg No: HR 26 RQ 0001", VehicleStatus.COMPLIANT)
// )
// 
// // ═══════════════════════════════════════
// //  MY VEHICLES SCREEN
// // ═══════════════════════════════════════
// 
// @Composable
// fun MyVehiclesScreen(
//     onVehicleClick: (VehicleItem) -> Unit = {},
//     onAddVehicle: () -> Unit = {},
//     onBack: () -> Unit = {}
// ) {
//     var searchQuery by remember { mutableStateOf("") }
//     val vehicles = remember { sampleVehicles }
// 
//     Scaffold(
//         containerColor = V.Background,
//         topBar = {
//             Surface(color = V.Surface, shadowElevation = 1.dp) {
//                 Row(
//                     modifier = Modifier
//                         .fillMaxWidth()
//                         .windowInsetsPadding(WindowInsets.statusBars)
//                         .height(54.dp)
//                         .padding(horizontal = 4.dp),
//                     verticalAlignment = Alignment.CenterVertically
//                 ) {
//                     IconButton(onClick = onBack) {
//                         Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = V.TextPrimary)
//                     }
//                     Text(
//                         "My Vehicles",
//                         modifier = Modifier.weight(1f),
//                         textAlign = TextAlign.Center,
//                         style = MaterialTheme.typography.titleMedium.copy(
//                             fontWeight = FontWeight.SemiBold,
//                             fontSize = 16.sp,
//                             color = V.TextPrimary
//                         )
//                     )
//                     IconButton(onClick = {}) {
//                         Icon(Icons.Default.MoreVert, null, tint = V.TextSecondary)
//                     }
//                 }
//             }
//         },
//         // bottomBar removed — main app provides navigation
//         floatingActionButton = {
//             FloatingActionButton(
//                 onClick = onAddVehicle,
//                 containerColor = V.Primary,
//                 contentColor = Color.White,
//                 shape = CircleShape,
//                 modifier = Modifier.size(52.dp)
//             ) {
//                 Icon(Icons.Default.Add, "Add Vehicle", modifier = Modifier.size(24.dp))
//             }
//         }
//     ) { padding ->
//         LazyColumn(
//             modifier = Modifier
//                 .fillMaxSize()
//                 .padding(padding),
//             contentPadding = PaddingValues(
//                 start = 16.dp, end = 16.dp,
//                 top = 12.dp, bottom = 96.dp
//             ),
//             verticalArrangement = Arrangement.spacedBy(10.dp)
//         ) {
//             // Search bar
//             item {
//                 OutlinedTextField(
//                     value = searchQuery,
//                     onValueChange = { searchQuery = it },
//                     placeholder = {
//                         Text(
//                             "Search your vehicles",
//                             color = V.TextHint,
//                             fontSize = 14.sp
//                         )
//                     },
//                     leadingIcon = {
//                         Icon(Icons.Outlined.Search, null, tint = V.TextHint, modifier = Modifier.size(19.dp))
//                     },
//                     modifier = Modifier.fillMaxWidth(),
//                     singleLine = true,
//                     shape = RoundedCornerShape(11.dp),
//                     colors = OutlinedTextFieldDefaults.colors(
//                         unfocusedBorderColor = V.Border,
//                         focusedBorderColor   = V.Primary,
//                         unfocusedContainerColor = V.Surface,
//                         focusedContainerColor   = V.Surface,
//                         cursorColor = V.Primary
//                     ),
//                     textStyle = MaterialTheme.typography.bodyMedium.copy(
//                         fontSize = 14.sp, color = V.TextPrimary
//                     )
//                 )
//             }
// 
//             items(vehicles, key = { it.id }) { vehicle ->
//                 VehicleListCard(
//                     vehicle = vehicle,
//                     onViewDetails = { onVehicleClick(vehicle) },
//                     onAction = {}
//                 )
//             }
//         }
//     }
// }
// 
// // ── Vehicle list card ──────────────────────────────────────
// 
// @Composable
// private fun VehicleListCard(
//     vehicle: VehicleItem,
//     onViewDetails: () -> Unit,
//     onAction: () -> Unit
// ) {
//     val (statusColor, statusBg, statusLabel) = when (vehicle.status) {
//         VehicleStatus.COMPLIANT          -> Triple(V.Green,  V.GreenBg,  "COMPLIANT")
//         VehicleStatus.ATTENTION_REQUIRED -> Triple(V.Orange, V.OrangeBg, "ATTENTION REQUIRED")
//         VehicleStatus.EXPIRED            -> Triple(V.Red,    V.RedBg,    "EXPIRED")
//     }
// 
//     Card(
//         modifier = Modifier
//             .fillMaxWidth()
//             .shadow(2.dp, RoundedCornerShape(14.dp), spotColor = V.CardShadow),
//         shape = RoundedCornerShape(14.dp),
//         colors = CardDefaults.cardColors(containerColor = V.Surface),
//         elevation = CardDefaults.cardElevation(0.dp)
//     ) {
//         Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
//             // Status chip
//             Box(
//                 modifier = Modifier
//                     .background(statusBg, RoundedCornerShape(4.dp))
//                     .padding(horizontal = 7.dp, vertical = 3.dp)
//             ) {
//                 Row(
//                     verticalAlignment = Alignment.CenterVertically,
//                     horizontalArrangement = Arrangement.spacedBy(4.dp)
//                 ) {
//                     Box(
//                         modifier = Modifier
//                             .size(6.dp)
//                             .background(statusColor, CircleShape)
//                     )
//                     Text(
//                         statusLabel,
//                         style = MaterialTheme.typography.labelSmall.copy(
//                             fontSize = 9.5.sp,
//                             fontWeight = FontWeight.Bold,
//                             color = statusColor,
//                             letterSpacing = 0.3.sp
//                         )
//                     )
//                 }
//             }
// 
//             Spacer(Modifier.height(10.dp))
// 
//             Row(
//                 modifier = Modifier.fillMaxWidth(),
//                 verticalAlignment = Alignment.CenterVertically,
//                 horizontalArrangement = Arrangement.spacedBy(12.dp)
//             ) {
//                 Column(modifier = Modifier.weight(1f)) {
//                     Text(
//                         vehicle.name,
//                         style = MaterialTheme.typography.titleSmall.copy(
//                             fontWeight = FontWeight.Bold,
//                             fontSize = 15.sp,
//                             color = V.TextPrimary
//                         )
//                     )
//                     Spacer(Modifier.height(3.dp))
//                     Text(
//                         vehicle.regNo,
//                         style = MaterialTheme.typography.bodySmall.copy(
//                             color = V.TextSecondary,
//                             fontSize = 12.sp
//                         )
//                     )
//                 }
//                 // Vehicle thumbnail
//                 Box(
//                     modifier = Modifier
//                         .size(width = 60.dp, height = 44.dp)
//                         .background(V.PrimaryLight, RoundedCornerShape(9.dp)),
//                     contentAlignment = Alignment.Center
//                 ) {
//                     Icon(
//                         Icons.Outlined.TwoWheeler, null,
//                         Modifier.size(28.dp), tint = V.Primary.copy(0.5f)
//                     )
//                 }
//             }
// 
//             Spacer(Modifier.height(12.dp))
// 
//             // Action row
//             Row(
//                 modifier = Modifier.fillMaxWidth(),
//                 horizontalArrangement = Arrangement.spacedBy(10.dp),
//                 verticalAlignment = Alignment.CenterVertically
//             ) {
//                 if (vehicle.actionLabel != null) {
//                     Button(
//                         onClick = onAction,
//                         modifier = Modifier.weight(1f).height(38.dp),
//                         shape = RoundedCornerShape(9.dp),
//                         colors = ButtonDefaults.buttonColors(containerColor = V.Primary),
//                         contentPadding = PaddingValues(horizontal = 12.dp)
//                     ) {
//                         Text(
//                             vehicle.actionLabel,
//                             fontSize = 12.5.sp,
//                             fontWeight = FontWeight.SemiBold,
//                             color = Color.White,
//                             maxLines = 1
//                         )
//                     }
//                     IconButton(
//                         onClick = {},
//                         modifier = Modifier
//                             .size(38.dp)
//                             .border(1.dp, V.Border, RoundedCornerShape(9.dp))
//                     ) {
//                         Icon(Icons.Outlined.Warning, null, Modifier.size(17.dp), tint = V.Orange)
//                     }
//                 } else {
//                     Button(
//                         onClick = onViewDetails,
//                         modifier = Modifier.weight(1f).height(38.dp),
//                         shape = RoundedCornerShape(9.dp),
//                         colors = ButtonDefaults.buttonColors(containerColor = V.Primary),
//                         contentPadding = PaddingValues(horizontal = 12.dp)
//                     ) {
//                         Text(
//                             "View Details",
//                             fontSize = 12.5.sp,
//                             fontWeight = FontWeight.SemiBold,
//                             color = Color.White
//                         )
//                     }
//                     IconButton(
//                         onClick = {},
//                         modifier = Modifier
//                             .size(38.dp)
//                             .border(1.dp, V.Border, RoundedCornerShape(9.dp))
//                     ) {
//                         Icon(Icons.Outlined.Settings, null, Modifier.size(17.dp), tint = V.TextSecondary)
//                     }
//                 }
//             }
//         }
//     }
// }
// 
// // ═══════════════════════════════════════
// //  VEHICLE DETAILS SCREEN
// // ═══════════════════════════════════════
// 
// @Composable
// fun VehicleDetailsScreen(
//     onBack: () -> Unit = {},
//     onDeleteVehicle: () -> Unit = {},
//     onUpdateDocuments: () -> Unit = {},
//     onGenerateProof: () -> Unit = {}
// ) {
//     var selectedTab by remember { mutableStateOf(0) }
//     val tabs = listOf("Vehicle Info", "Documents", "Compliance")
// 
//     Scaffold(
//         containerColor = V.Background,
//         topBar = {
//             Surface(color = V.Surface, shadowElevation = 1.dp) {
//                 Row(
//                     modifier = Modifier
//                         .fillMaxWidth()
//                         .windowInsetsPadding(WindowInsets.statusBars)
//                         .height(54.dp)
//                         .padding(horizontal = 4.dp),
//                     verticalAlignment = Alignment.CenterVertically
//                 ) {
//                     IconButton(onClick = onBack) {
//                         Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = V.TextPrimary)
//                     }
//                     Text(
//                         "Vehicle Details",
//                         modifier = Modifier.weight(1f),
//                         textAlign = TextAlign.Center,
//                         style = MaterialTheme.typography.titleMedium.copy(
//                             fontWeight = FontWeight.SemiBold,
//                             fontSize = 16.sp,
//                             color = V.TextPrimary
//                         )
//                     )
//                     IconButton(onClick = {}) {
//                         Icon(Icons.Default.MoreVert, null, tint = V.TextSecondary)
//                     }
//                 }
//             }
//         }
//     ) { padding ->
//         Column(
//             modifier = Modifier
//                 .fillMaxSize()
//                 .padding(padding)
//         ) {
//             // Vehicle hero card
//             VehicleHeroCard()
// 
//             // Tab row
//             Surface(color = V.Surface, shadowElevation = 0.dp) {
//                 Row(
//                     modifier = Modifier
//                         .fillMaxWidth()
//                         .padding(horizontal = 16.dp),
//                 ) {
//                     tabs.forEachIndexed { index, title ->
//                         val sel = index == selectedTab
//                         Column(
//                             modifier = Modifier
//                                 .weight(1f)
//                                 .clickable { selectedTab = index },
//                             horizontalAlignment = Alignment.CenterHorizontally
//                         ) {
//                             Text(
//                                 title,
//                                 modifier = Modifier.padding(vertical = 12.dp),
//                                 style = MaterialTheme.typography.labelMedium.copy(
//                                     fontSize = 12.5.sp,
//                                     fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal,
//                                     color = if (sel) V.Primary else V.TextSecondary
//                                 )
//                             )
//                             Box(
//                                 Modifier.fillMaxWidth().height(2.dp)
//                                     .background(if (sel) V.Primary else Color.Transparent)
//                             )
//                         }
//                     }
//                 }
//                 HorizontalDivider(color = V.Divider)
//             }
// 
//             // Tab content
//             AnimatedContent(
//                 targetState = selectedTab,
//                 transitionSpec = { fadeIn() togetherWith fadeOut() },
//                 label = "tab"
//             ) { tab ->
//                 when (tab) {
//                     0 -> VehicleInfoTab()
//                     1 -> DocumentsTab()
//                     2 -> ComplianceTab(
//                         onDeleteVehicle   = onDeleteVehicle,
//                         onUpdateDocuments = onUpdateDocuments,
//                         onGenerateProof   = onGenerateProof
//                     )
//                     else -> VehicleInfoTab()
//                 }
//             }
//         }
//     }
// }
// 
// // ── Hero Card ──────────────────────────────────────────────
// 
// @Composable
// private fun VehicleHeroCard() {
//     Column(
//         modifier = Modifier
//             .fillMaxWidth()
//             .background(V.Surface)
//             .padding(horizontal = 16.dp, vertical = 14.dp),
//         horizontalAlignment = Alignment.CenterHorizontally,
//         verticalArrangement = Arrangement.spacedBy(8.dp)
//     ) {
//         // Vehicle illustration
//         Box(
//             modifier = Modifier
//                 .size(100.dp)
//                 .background(
//                     Brush.radialGradient(listOf(Color(0xFFE8F5E9), V.Background)),
//                     RoundedCornerShape(16.dp)
//                 ),
//             contentAlignment = Alignment.Center
//         ) {
//             // Online indicator dot
//             Box(modifier = Modifier.fillMaxSize()) {
//                 Icon(
//                     Icons.Outlined.ElectricScooter, null,
//                     Modifier.align(Alignment.Center).size(58.dp),
//                     tint = V.Primary.copy(0.4f)
//                 )
//                 Box(
//                     modifier = Modifier
//                         .size(12.dp)
//                         .align(Alignment.TopEnd)
//                         .padding(top = 2.dp, end = 2.dp)
//                         .background(V.Green, CircleShape)
//                         .border(2.dp, V.Surface, CircleShape)
//                 )
//             }
//         }
// 
//         Text(
//             "Ather 450X Gen 3",
//             style = MaterialTheme.typography.titleMedium.copy(
//                 fontWeight = FontWeight.ExtraBold,
//                 fontSize = 18.sp,
//                 color = V.TextPrimary
//             )
//         )
//         Text(
//             "MH-12-AB-1234",
//             style = MaterialTheme.typography.bodySmall.copy(
//                 color = V.TextSecondary,
//                 fontSize = 13.sp
//             )
//         )
// 
//         // Connected + battery
//         Row(
//             verticalAlignment = Alignment.CenterVertically,
//             horizontalArrangement = Arrangement.spacedBy(10.dp)
//         ) {
//             Row(
//                 modifier = Modifier
//                     .background(V.GreenBg, RoundedCornerShape(20.dp))
//                     .padding(horizontal = 9.dp, vertical = 4.dp),
//                 verticalAlignment = Alignment.CenterVertically,
//                 horizontalArrangement = Arrangement.spacedBy(4.dp)
//             ) {
//                 Box(Modifier.size(6.dp).background(V.Green, CircleShape))
//                 Text(
//                     "Connected",
//                     style = MaterialTheme.typography.labelSmall.copy(
//                         fontSize = 11.sp, color = V.Green, fontWeight = FontWeight.SemiBold
//                     )
//                 )
//             }
//             Row(
//                 modifier = Modifier
//                     .background(V.PrimaryLight, RoundedCornerShape(20.dp))
//                     .padding(horizontal = 9.dp, vertical = 4.dp),
//                 verticalAlignment = Alignment.CenterVertically,
//                 horizontalArrangement = Arrangement.spacedBy(3.dp)
//             ) {
//                 Icon(Icons.Default.BatteryChargingFull, null, Modifier.size(13.dp), tint = V.Primary)
//                 Text(
//                     "85%",
//                     style = MaterialTheme.typography.labelSmall.copy(
//                         fontSize = 11.sp, color = V.Primary, fontWeight = FontWeight.Bold
//                     )
//                 )
//             }
//         }
//     }
// }
// 
// // ── Tab: Vehicle Info ──────────────────────────────────────
// 
// @Composable
// private fun VehicleInfoTab() {
//     LazyColumn(
//         modifier = Modifier.fillMaxSize(),
//         contentPadding = PaddingValues(16.dp),
//         verticalArrangement = Arrangement.spacedBy(14.dp)
//     ) {
//         item {
//             SectionCard(title = "Manufacturer Details") {
//                 listOf(
//                     "Manufacturer" to "Ather Energy",
//                     "Model" to "450X (2023)",
//                     "Chassis Number" to "ATH789…X21"
//                 ).forEachIndexed { i, (label, value) ->
//                     if (i > 0) HorizontalDivider(color = V.Divider, modifier = Modifier.padding(vertical = 8.dp))
//                     DetailRow(label = label, value = value)
//                 }
//             }
//         }
//     }
// }
// 
// // ── Tab: Documents ─────────────────────────────────────────
// 
// @Composable
// private fun DocumentsTab() {
//     val docs = remember {
//         listOf(
//             DocumentFile("RC_Book.pdf", "pdf"),
//             DocumentFile("Owner_ID.jpg", "image")
//         )
//     }
//     LazyColumn(
//         modifier = Modifier.fillMaxSize(),
//         contentPadding = PaddingValues(16.dp),
//         verticalArrangement = Arrangement.spacedBy(14.dp)
//     ) {
//         item {
//             Text(
//                 "Documents",
//                 style = MaterialTheme.typography.titleSmall.copy(
//                     fontWeight = FontWeight.Bold,
//                     fontSize = 14.sp,
//                     color = V.TextPrimary
//                 )
//             )
//         }
//         item {
//             LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
//                 items(docs) { doc ->
//                     DocumentFileCard(doc = doc)
//                 }
//             }
//         }
//     }
// }
// 
// @Composable
// private fun DocumentFileCard(doc: DocumentFile) {
//     Card(
//         modifier = Modifier.size(width = 110.dp, height = 100.dp),
//         shape = RoundedCornerShape(12.dp),
//         colors = CardDefaults.cardColors(containerColor = V.Surface),
//         elevation = CardDefaults.cardElevation(1.dp)
//     ) {
//         Column(
//             modifier = Modifier.fillMaxSize().padding(10.dp),
//             horizontalAlignment = Alignment.CenterHorizontally,
//             verticalArrangement = Arrangement.spacedBy(8.dp)
//         ) {
//             Box(
//                 Modifier.size(44.dp).background(V.PrimaryLight, RoundedCornerShape(9.dp)),
//                 contentAlignment = Alignment.Center
//             ) {
//                 Icon(
//                     if (doc.type == "pdf") Icons.Outlined.PictureAsPdf else Icons.Outlined.Image,
//                     null, Modifier.size(22.dp),
//                     tint = V.Primary
//                 )
//             }
//             Text(
//                 doc.name,
//                 style = MaterialTheme.typography.labelSmall.copy(
//                     fontSize = 10.5.sp, color = V.TextSecondary,
//                     fontWeight = FontWeight.Medium
//                 ),
//                 maxLines = 1,
//                 overflow = TextOverflow.Ellipsis,
//                 textAlign = TextAlign.Center
//             )
//         }
//     }
// }
// 
// // ── Tab: Compliance ────────────────────────────────────────
// 
// @Composable
// private fun ComplianceTab(
//     onDeleteVehicle: () -> Unit,
//     onUpdateDocuments: () -> Unit,
//     onGenerateProof: () -> Unit
// ) {
//     var showDeleteDialog by remember { mutableStateOf(false) }
// 
//     LazyColumn(
//         modifier = Modifier.fillMaxSize(),
//         contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 24.dp),
//         verticalArrangement = Arrangement.spacedBy(14.dp)
//     ) {
//         item {
//             SectionCard(title = "Compliance Status") {
//                 ComplianceRow(
//                     icon = Icons.Outlined.Shield,
//                     title = "Insurance Policy",
//                     detail = "Valid until 12 Oct 2024",
//                     status = VehicleStatus.COMPLIANT
//                 )
//                 HorizontalDivider(color = V.Divider, modifier = Modifier.padding(vertical = 10.dp))
//                 ComplianceRow(
//                     icon = Icons.Outlined.Eco,
//                     title = "PUC Certificate",
//                     detail = "Expired 15 days ago",
//                     status = VehicleStatus.ATTENTION_REQUIRED
//                 )
//             }
//         }
// 
//         // Delete vehicle
//         item {
//             TextButton(
//                 onClick = { showDeleteDialog = true },
//                 modifier = Modifier.fillMaxWidth().height(46.dp),
//                 shape = RoundedCornerShape(11.dp),
//                 colors = ButtonDefaults.textButtonColors(contentColor = V.Red)
//             ) {
//                 Icon(Icons.Outlined.Delete, null, Modifier.size(17.dp), tint = V.Red)
//                 Spacer(Modifier.width(7.dp))
//                 Text("Delete Vehicle", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = V.Red)
//             }
//         }
// 
//         // Update documents
//         item {
//             Button(
//                 onClick = onUpdateDocuments,
//                 modifier = Modifier.fillMaxWidth().height(50.dp),
//                 shape = RoundedCornerShape(12.dp),
//                 colors = ButtonDefaults.buttonColors(containerColor = V.Primary)
//             ) {
//                 Icon(Icons.Outlined.Upload, null, Modifier.size(18.dp), tint = Color.White)
//                 Spacer(Modifier.width(7.dp))
//                 Text("Update Documents", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Color.White)
//             }
//         }
// 
//         // Generate proof
//         item {
//             OutlinedButton(
//                 onClick = onGenerateProof,
//                 modifier = Modifier.fillMaxWidth().height(50.dp),
//                 shape = RoundedCornerShape(12.dp),
//                 border = BorderStroke(1.5.dp, V.Primary),
//                 colors = ButtonDefaults.outlinedButtonColors(contentColor = V.Primary)
//             ) {
//                 Icon(Icons.Outlined.Shield, null, Modifier.size(18.dp), tint = V.Primary)
//                 Spacer(Modifier.width(7.dp))
//                 Text("Generate Proof", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = V.Primary)
//             }
//         }
//     }
// 
//     // Delete dialog
//     if (showDeleteDialog) {
//         AlertDialog(
//             onDismissRequest = { showDeleteDialog = false },
//             shape = RoundedCornerShape(20.dp),
//             icon = {
//                 Box(
//                     Modifier.size(48.dp).background(V.RedBg, CircleShape),
//                     contentAlignment = Alignment.Center
//                 ) {
//                     Icon(Icons.Outlined.Delete, null, Modifier.size(24.dp), tint = V.Red)
//                 }
//             },
//             title = {
//                 Text(
//                     "Delete Vehicle",
//                     fontWeight = FontWeight.Bold,
//                     color = V.TextPrimary,
//                     textAlign = TextAlign.Center
//                 )
//             },
//             text = {
//                 Text(
//                     "Are you sure you want to delete this vehicle? All documents and data will be removed.",
//                     color = V.TextSecondary,
//                     fontSize = 14.sp,
//                     textAlign = TextAlign.Center,
//                     lineHeight = 20.sp
//                 )
//             },
//             confirmButton = {
//                 Button(
//                     onClick = { showDeleteDialog = false; onDeleteVehicle() },
//                     colors = ButtonDefaults.buttonColors(containerColor = V.Red),
//                     shape = RoundedCornerShape(10.dp)
//                 ) { Text("Delete", color = Color.White, fontWeight = FontWeight.SemiBold) }
//             },
//             dismissButton = {
//                 OutlinedButton(
//                     onClick = { showDeleteDialog = false },
//                     shape = RoundedCornerShape(10.dp),
//                     border = BorderStroke(1.dp, V.Border)
//                 ) { Text("Cancel", color = V.TextSecondary) }
//             }
//         )
//     }
// }
// 
// // ── Shared sub-composables ─────────────────────────────────
// 
// @Composable
// private fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
//     Card(
//         modifier = Modifier.fillMaxWidth(),
//         shape = RoundedCornerShape(14.dp),
//         colors = CardDefaults.cardColors(containerColor = V.Surface),
//         elevation = CardDefaults.cardElevation(1.dp)
//     ) {
//         Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
//             Text(
//                 title,
//                 style = MaterialTheme.typography.labelMedium.copy(
//                     fontWeight = FontWeight.Bold,
//                     fontSize = 12.sp,
//                     color = V.TextSecondary,
//                     letterSpacing = 0.5.sp
//                 )
//             )
//             Spacer(Modifier.height(12.dp))
//             content()
//         }
//     }
// }
// 
// @Composable
// private fun DetailRow(label: String, value: String) {
//     Row(
//         modifier = Modifier.fillMaxWidth(),
//         horizontalArrangement = Arrangement.SpaceBetween,
//         verticalAlignment = Alignment.CenterVertically
//     ) {
//         Text(
//             label,
//             style = MaterialTheme.typography.bodySmall.copy(
//                 color = V.TextSecondary, fontSize = 13.sp
//             )
//         )
//         Text(
//             value,
//             style = MaterialTheme.typography.bodySmall.copy(
//                 color = V.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp
//             )
//         )
//     }
// }
// 
// @Composable
// private fun ComplianceRow(
//     icon: ImageVector,
//     title: String,
//     detail: String,
//     status: VehicleStatus
// ) {
//     val (iconTint, iconBg, badgeIcon) = when (status) {
//         VehicleStatus.COMPLIANT          -> Triple(V.Green,  V.GreenBg,  Icons.Default.CheckCircle)
//         VehicleStatus.ATTENTION_REQUIRED -> Triple(V.Orange, V.OrangeBg, Icons.Default.Warning)
//         VehicleStatus.EXPIRED            -> Triple(V.Red,    V.RedBg,    Icons.Default.Cancel)
//     }
//     Row(
//         modifier = Modifier.fillMaxWidth(),
//         verticalAlignment = Alignment.CenterVertically,
//         horizontalArrangement = Arrangement.spacedBy(12.dp)
//     ) {
//         Box(
//             Modifier.size(38.dp).background(iconBg, RoundedCornerShape(9.dp)),
//             contentAlignment = Alignment.Center
//         ) {
//             Icon(icon, null, Modifier.size(19.dp), tint = iconTint)
//         }
//         Column(modifier = Modifier.weight(1f)) {
//             Text(
//                 title,
//                 style = MaterialTheme.typography.bodyMedium.copy(
//                     fontWeight = FontWeight.SemiBold,
//                     fontSize = 13.5.sp,
//                     color = V.TextPrimary
//                 )
//             )
//             Text(
//                 detail,
//                 style = MaterialTheme.typography.bodySmall.copy(
//                     color = if (status != VehicleStatus.COMPLIANT) iconTint else V.TextSecondary,
//                     fontSize = 11.5.sp
//                 )
//             )
//         }
//         Icon(badgeIcon, null, Modifier.size(20.dp), tint = iconTint)
//     }
// }
// 
// @Composable
// private fun VehicleBottomNav(selected: Int) {
//     Surface(color = V.Surface, shadowElevation = 8.dp) {
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
//                 Triple(Icons.Default.Home,          Icons.Outlined.Home,         "Home"),
//                 Triple(Icons.Default.DirectionsCar, Icons.Outlined.DirectionsCar,"Vehicles"),
//                 Triple(Icons.Default.Notifications, Icons.Outlined.Notifications,"Alerts"),
//                 Triple(Icons.Default.Person,        Icons.Outlined.Person,       "Profile")
//             ).forEachIndexed { i, (filled, outlined, label) ->
//                 val sel = i == selected
//                 Column(
//                     horizontalAlignment = Alignment.CenterHorizontally,
//                     modifier = Modifier
//                         .clip(RoundedCornerShape(8.dp))
//                         .clickable {}
//                         .padding(horizontal = 10.dp, vertical = 5.dp)
//                 ) {
//                     Icon(
//                         if (sel) filled else outlined, label,
//                         Modifier.size(21.dp),
//                         tint = if (sel) V.Primary else V.TextHint
//                     )
//                     Spacer(Modifier.height(2.dp))
//                     Text(
//                         label,
//                         style = MaterialTheme.typography.labelSmall.copy(
//                             fontSize = 10.sp,
//                             fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal,
//                             color = if (sel) V.Primary else V.TextHint
//                         )
//                     )
//                 }
//             }
//         }
//     }
// }