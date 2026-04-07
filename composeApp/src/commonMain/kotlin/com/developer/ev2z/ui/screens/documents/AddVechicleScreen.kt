package com.developer.ev2z.ui.screens.documents

// ══════════════════════════════════════════════════════════════════
// COMMENTED OUT: DUMMY UI CODE (for reference)
// ══════════════════════════════════════════════════════════════════

// 
// 
// import androidx.compose.animation.*
// import androidx.compose.animation.core.*
// import androidx.compose.foundation.*
// import androidx.compose.foundation.layout.*
// import androidx.compose.foundation.shape.CircleShape
// import androidx.compose.foundation.shape.RoundedCornerShape
// import androidx.compose.foundation.text.KeyboardOptions
// import androidx.compose.material.icons.Icons
// import androidx.compose.material.icons.automirrored.filled.ArrowBack
// import androidx.compose.material.icons.automirrored.filled.ArrowForward
// import androidx.compose.material.icons.filled.*
// import androidx.compose.material.icons.outlined.*
// import androidx.compose.material3.*
// import androidx.compose.runtime.*
// import androidx.compose.ui.Alignment
// import androidx.compose.ui.Modifier
// import androidx.compose.ui.draw.clip
// import androidx.compose.ui.graphics.Brush
// import androidx.compose.ui.graphics.Color
// import androidx.compose.ui.graphics.StrokeCap
// import androidx.compose.ui.graphics.vector.ImageVector
// import androidx.compose.ui.text.font.FontWeight
// import androidx.compose.ui.text.input.KeyboardCapitalization
// import androidx.compose.ui.text.input.KeyboardType
// import androidx.compose.ui.text.style.TextAlign
// import androidx.compose.ui.unit.dp
// import androidx.compose.ui.unit.sp
// 
// // ─────────────────────────────────────────────
// //  DESIGN TOKENS
// // ─────────────────────────────────────────────
// 
// private object C {
//     val Primary       = Color(0xFF4CAF50)
//     val PrimaryLight  = Color(0xFFE8F5E9)
//     val PrimaryDark   = Color(0xFF388E3C)
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
//     val Red           = Color(0xFFE53935)
//     val Dark          = Color(0xFF1E1E1E)
//     val TabSelected   = Color(0xFF4CAF50)
//     val TabUnselected = Color(0xFF777777)
//     val InputBg       = Color(0xFFF8F8F8)
//     val UploadBg      = Color(0xFF4CAF50)
// }
// 
// // ─────────────────────────────────────────────
// //  STEP ENUM
// // ─────────────────────────────────────────────
// 
// private enum class VehicleStep {
//     ADD_VEHICLE,        // Step 1 — Registration Number / Add Manually
//     ENTER_DETAILS,      // Step 2 — Manual form
//     CONFIRM_VEHICLE,    // Step 3 — Detected model confirmation
//     SECURE_DOCUMENTS,   // Step 4 — Document upload
//     REVIEW_DOCUMENT     // Step 5 — Extracted details review
// }
// 
// // ─────────────────────────────────────────────
// //  ROOT: ADD VEHICLE FLOW
// // ─────────────────────────────────────────────
// 
// @Composable
// fun AddVehicleScreen(onBack: () -> Unit = {}, onComplete: () -> Unit = {}) {
//     var step by remember { mutableStateOf(VehicleStep.ADD_VEHICLE) }
//     var regNumber   by remember { mutableStateOf("") }
//     var manufacturer by remember { mutableStateOf("") }
//     var model       by remember { mutableStateOf("") }
//     var regYear     by remember { mutableStateOf("2022") }
//     var fuelType    by remember { mutableStateOf("") }
//     var vin         by remember { mutableStateOf("") }
//     var useManual   by remember { mutableStateOf(false) }
// 
//     AnimatedContent(
//         targetState = step,
//         transitionSpec = {
//             slideInHorizontally { it } + fadeIn() togetherWith
//                     slideOutHorizontally { -it } + fadeOut()
//         },
//         label = "step_transition"
//     ) { currentStep ->
//         when (currentStep) {
//             VehicleStep.ADD_VEHICLE -> Step1AddVehicle(
//                 regNumber = regNumber,
//                 onRegNumberChange = { regNumber = it.uppercase() },
//                 useManual = useManual,
//                 onTabChange = { useManual = it },
//                 onBack = onBack,
//                 onContinue = {
//                     if (useManual) step = VehicleStep.ENTER_DETAILS
//                     else step = VehicleStep.CONFIRM_VEHICLE
//                 }
//             )
//             VehicleStep.ENTER_DETAILS -> Step2EnterDetails(
//                 manufacturer = manufacturer,
//                 model = model,
//                 regYear = regYear,
//                 fuelType = fuelType,
//                 vin = vin,
//                 onManufacturerChange = { manufacturer = it },
//                 onModelChange = { model = it },
//                 onRegYearChange = { regYear = it },
//                 onFuelTypeChange = { fuelType = it },
//                 onVinChange = { vin = it },
//                 onBack = { step = VehicleStep.ADD_VEHICLE },
//                 onContinue = { step = VehicleStep.CONFIRM_VEHICLE }
//             )
//             VehicleStep.CONFIRM_VEHICLE -> Step3ConfirmVehicle(
//                 regNumber = regNumber.ifBlank { "ABC 1234" },
//                 onBack = { step = if (useManual) VehicleStep.ENTER_DETAILS else VehicleStep.ADD_VEHICLE },
//                 onConfirm = { step = VehicleStep.SECURE_DOCUMENTS }
//             )
//             VehicleStep.SECURE_DOCUMENTS -> Step4SecureDocuments(
//                 onBack = { step = VehicleStep.CONFIRM_VEHICLE },
//                 onContinue = { step = VehicleStep.REVIEW_DOCUMENT }
//             )
//             VehicleStep.REVIEW_DOCUMENT -> Step5ReviewDocument(
//                 onBack = { step = VehicleStep.SECURE_DOCUMENTS },
//                 onConfirm = onComplete
//             )
//         }
//     }
// }
// 
// // ─────────────────────────────────────────────
// //  SHARED COMPONENTS
// // ─────────────────────────────────────────────
// 
// @Composable
// private fun VehicleTopBar(
//     title: String,
//     onBack: () -> Unit,
//     trailingIcon: (@Composable () -> Unit)? = null
// ) {
//     Surface(color = C.Surface, shadowElevation = 0.5.dp) {
//         Row(
//             modifier = Modifier
//                 .fillMaxWidth()
//                 .windowInsetsPadding(WindowInsets.statusBars)
//                 .height(54.dp)
//                 .padding(horizontal = 4.dp),
//             verticalAlignment = Alignment.CenterVertically
//         ) {
//             IconButton(onClick = onBack) {
//                 Icon(
//                     Icons.AutoMirrored.Filled.ArrowBack,
//                     "Back",
//                     tint = C.TextPrimary
//                 )
//             }
//             Text(
//                 text = title,
//                 modifier = Modifier.weight(1f),
//                 textAlign = TextAlign.Center,
//                 style = MaterialTheme.typography.titleMedium.copy(
//                     fontSize = 16.sp,
//                     fontWeight = FontWeight.SemiBold,
//                     color = C.TextPrimary
//                 )
//             )
//             Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
//                 trailingIcon?.invoke()
//             }
//         }
//     }
// }
// 
// @Composable
// private fun PrimaryButton(
//     text: String,
//     onClick: () -> Unit,
//     modifier: Modifier = Modifier,
//     enabled: Boolean = true
// ) {
//     Button(
//         onClick = onClick,
//         enabled = enabled,
//         modifier = modifier
//             .fillMaxWidth()
//             .height(50.dp),
//         shape = RoundedCornerShape(12.dp),
//         colors = ButtonDefaults.buttonColors(
//             containerColor = C.Primary,
//             disabledContainerColor = C.Primary.copy(alpha = 0.4f)
//         )
//     ) {
//         Text(
//             text,
//             fontWeight = FontWeight.SemiBold,
//             fontSize = 15.sp,
//             color = Color.White
//         )
//         Spacer(Modifier.width(6.dp))
//         Icon(
//             Icons.AutoMirrored.Filled.ArrowForward,
//             null,
//             Modifier.size(16.dp),
//             tint = Color.White
//         )
//     }
// }
// 
// @Composable
// private fun StyledTextField(
//     value: String,
//     onValueChange: (String) -> Unit,
//     label: String,
//     placeholder: String = "",
//     trailingIcon: (@Composable () -> Unit)? = null,
//     keyboardType: KeyboardType = KeyboardType.Text,
//     capitalization: KeyboardCapitalization = KeyboardCapitalization.Characters,
//     modifier: Modifier = Modifier
// ) {
//     Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(5.dp)) {
//         Text(
//             label.uppercase(),
//             style = MaterialTheme.typography.labelSmall.copy(
//                 fontSize = 10.5.sp,
//                 fontWeight = FontWeight.SemiBold,
//                 color = C.TextSecondary,
//                 letterSpacing = 0.6.sp
//             )
//         )
//         OutlinedTextField(
//             value = value,
//             onValueChange = onValueChange,
//             placeholder = {
//                 Text(placeholder, color = C.TextHint, fontSize = 14.sp)
//             },
//             trailingIcon = trailingIcon,
//             modifier = Modifier.fillMaxWidth(),
//             singleLine = true,
//             shape = RoundedCornerShape(10.dp),
//             colors = OutlinedTextFieldDefaults.colors(
//                 focusedBorderColor   = C.Primary,
//                 unfocusedBorderColor = C.Border,
//                 focusedContainerColor   = C.Surface,
//                 unfocusedContainerColor = C.InputBg,
//                 cursorColor = C.Primary
//             ),
//             keyboardOptions = KeyboardOptions(
//                 keyboardType = keyboardType,
//                 capitalization = capitalization
//             ),
//             textStyle = MaterialTheme.typography.bodyMedium.copy(
//                 fontSize = 14.sp,
//                 color = C.TextPrimary,
//                 fontWeight = FontWeight.Medium
//             )
//         )
//     }
// }
// 
// // ─────────────────────────────────────────────
// //  STEP 1 — ADD VEHICLE (REG / MANUAL)
// // ─────────────────────────────────────────────
// 
// @Composable
// private fun Step1AddVehicle(
//     regNumber: String,
//     onRegNumberChange: (String) -> Unit,
//     useManual: Boolean,
//     onTabChange: (Boolean) -> Unit,
//     onBack: () -> Unit,
//     onContinue: () -> Unit
// ) {
//     Scaffold(
//         containerColor = C.Surface,
//         topBar = { VehicleTopBar("Add Vehicle", onBack) }
//     ) { padding ->
//         Column(
//             modifier = Modifier
//                 .fillMaxSize()
//                 .padding(padding)
//                 .padding(horizontal = 20.dp)
//                 .verticalScroll(rememberScrollState()),
//             verticalArrangement = Arrangement.spacedBy(0.dp)
//         ) {
//             Spacer(Modifier.height(28.dp))
// 
//             Text(
//                 "Add Your First Vehicle",
//                 style = MaterialTheme.typography.headlineMedium.copy(
//                     fontWeight = FontWeight.Bold,
//                     fontSize = 24.sp,
//                     color = C.TextPrimary,
//                     lineHeight = 30.sp
//                 )
//             )
//             Spacer(Modifier.height(8.dp))
//             Text(
//                 "Let's get your vehicle connected to EVA2Z for\nsmart tracking and diagnostics.",
//                 style = MaterialTheme.typography.bodyMedium.copy(
//                     color = C.TextSecondary,
//                     fontSize = 13.5.sp,
//                     lineHeight = 19.sp
//                 )
//             )
// 
//             Spacer(Modifier.height(28.dp))
// 
//             // Tabs
//             AddVehicleTabs(selected = useManual, onSelect = onTabChange)
// 
//             Spacer(Modifier.height(24.dp))
// 
//             AnimatedContent(targetState = useManual, label = "tab_content") { isManual ->
//                 if (!isManual) {
//                     // Registration number tab
//                     Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
//                         StyledTextField(
//                             value = regNumber,
//                             onValueChange = onRegNumberChange,
//                             label = "Registration Number",
//                             placeholder = "KA01AB1234",
//                             trailingIcon = {
//                                 Icon(
//                                     Icons.Outlined.DirectionsCar,
//                                     null,
//                                     tint = C.TextHint,
//                                     modifier = Modifier.size(20.dp)
//                                 )
//                             },
//                             capitalization = KeyboardCapitalization.Characters
//                         )
// 
//                         Row(
//                             horizontalArrangement = Arrangement.spacedBy(8.dp),
//                             verticalAlignment = Alignment.Top
//                         ) {
//                             Icon(
//                                 Icons.Outlined.Info,
//                                 null,
//                                 Modifier.size(15.dp).padding(top = 1.dp),
//                                 tint = C.Primary
//                             )
//                             Text(
//                                 "Your vehicle number will be verified with official records to sync your data automatically.",
//                                 style = MaterialTheme.typography.bodySmall.copy(
//                                     color = C.TextSecondary,
//                                     fontSize = 12.sp,
//                                     lineHeight = 17.sp
//                                 )
//                             )
//                         }
//                     }
//                 } else {
//                     // Manual tab — just a nudge
//                     Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
//                         Text(
//                             "Enter your vehicle details manually on the next screen.",
//                             style = MaterialTheme.typography.bodyMedium.copy(
//                                 color = C.TextSecondary,
//                                 fontSize = 13.sp
//                             )
//                         )
//                     }
//                 }
//             }
// 
//             Spacer(Modifier.weight(1f))
//             Spacer(Modifier.height(20.dp))
// 
//             PrimaryButton(
//                 text = "Continue",
//                 onClick = onContinue,
//                 enabled = useManual || regNumber.isNotBlank()
//             )
// 
//             Spacer(Modifier.height(24.dp))
//             Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
//         }
//     }
// }
// 
// @Composable
// private fun AddVehicleTabs(selected: Boolean, onSelect: (Boolean) -> Unit) {
//     Row(modifier = Modifier.fillMaxWidth()) {
//         listOf(false to "Vehicle Number", true to "Add Manually").forEach { (isManual, label) ->
//             val isSelected = selected == isManual
//             Column(
//                 modifier = Modifier
//                     .weight(1f)
//                     .clickable { onSelect(isManual) }
//                     .padding(bottom = 0.dp),
//                 horizontalAlignment = Alignment.CenterHorizontally
//             ) {
//                 Text(
//                     label,
//                     style = MaterialTheme.typography.labelMedium.copy(
//                         fontSize = 13.sp,
//                         fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
//                         color = if (isSelected) C.TabSelected else C.TabUnselected
//                     ),
//                     modifier = Modifier.padding(bottom = 8.dp)
//                 )
//                 Box(
//                     modifier = Modifier
//                         .fillMaxWidth()
//                         .height(2.dp)
//                         .background(if (isSelected) C.Primary else C.Divider)
//                 )
//             }
//         }
//     }
// }
// 
// // ─────────────────────────────────────────────
// //  STEP 2 — ENTER DETAILS (MANUAL)
// // ─────────────────────────────────────────────
// 
// @Composable
// private fun Step2EnterDetails(
//     manufacturer: String,
//     model: String,
//     regYear: String,
//     fuelType: String,
//     vin: String,
//     onManufacturerChange: (String) -> Unit,
//     onModelChange: (String) -> Unit,
//     onRegYearChange: (String) -> Unit,
//     onFuelTypeChange: (String) -> Unit,
//     onVinChange: (String) -> Unit,
//     onBack: () -> Unit,
//     onContinue: () -> Unit
// ) {
//     Scaffold(
//         containerColor = C.Surface,
//         topBar = { VehicleTopBar("Add Vehicle", onBack) }
//     ) { padding ->
//         Column(
//             modifier = Modifier
//                 .fillMaxSize()
//                 .padding(padding)
//                 .padding(horizontal = 20.dp)
//                 .verticalScroll(rememberScrollState()),
//             verticalArrangement = Arrangement.spacedBy(0.dp)
//         ) {
//             Spacer(Modifier.height(24.dp))
// 
//             Text(
//                 "Enter Details",
//                 style = MaterialTheme.typography.headlineMedium.copy(
//                     fontWeight = FontWeight.Bold,
//                     fontSize = 24.sp,
//                     color = C.TextPrimary
//                 )
//             )
//             Spacer(Modifier.height(6.dp))
//             Text(
//                 "Provide your vehicle information manually to\ncontinue registration.",
//                 style = MaterialTheme.typography.bodyMedium.copy(
//                     color = C.TextSecondary,
//                     fontSize = 13.5.sp,
//                     lineHeight = 19.sp
//                 )
//             )
// 
//             Spacer(Modifier.height(20.dp))
// 
//             // Tab indicator — "Add Manually" selected
//             Row(modifier = Modifier.fillMaxWidth()) {
//                 listOf("Vehicle Number" to false, "Add Manually" to true).forEach { (label, isSelected) ->
//                     Column(
//                         modifier = Modifier.weight(1f),
//                         horizontalAlignment = Alignment.CenterHorizontally
//                     ) {
//                         Text(
//                             label,
//                             style = MaterialTheme.typography.labelMedium.copy(
//                                 fontSize = 13.sp,
//                                 fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
//                                 color = if (isSelected) C.TabSelected else C.TabUnselected
//                             ),
//                             modifier = Modifier.padding(bottom = 8.dp)
//                         )
//                         Box(
//                             Modifier.fillMaxWidth().height(2.dp)
//                                 .background(if (isSelected) C.Primary else C.Divider)
//                         )
//                     }
//                 }
//             }
// 
//             Spacer(Modifier.height(22.dp))
// 
//             // Manufacturer
//             StyledTextField(
//                 value = manufacturer,
//                 onValueChange = onManufacturerChange,
//                 label = "Manufacturer",
//                 placeholder = "e.g. Honda",
//                 trailingIcon = {
//                     Icon(Icons.Outlined.QrCodeScanner, null, tint = C.TextHint, modifier = Modifier.size(20.dp))
//                 },
//                 capitalization = KeyboardCapitalization.Words
//             )
// 
//             Spacer(Modifier.height(14.dp))
// 
//             // Model
//             StyledTextField(
//                 value = model,
//                 onValueChange = onModelChange,
//                 label = "Model",
//                 placeholder = "e.g. Activa",
//                 trailingIcon = {
//                     Icon(Icons.Outlined.QrCodeScanner, null, tint = C.TextHint, modifier = Modifier.size(20.dp))
//                 },
//                 capitalization = KeyboardCapitalization.Words
//             )
// 
//             Spacer(Modifier.height(14.dp))
// 
//             // Reg Year + Fuel Type row
//             Row(
//                 modifier = Modifier.fillMaxWidth(),
//                 horizontalArrangement = Arrangement.spacedBy(12.dp)
//             ) {
//                 DropdownField(
//                     label = "Reg. Year",
//                     value = regYear,
//                     options = (2010..2025).map { it.toString() },
//                     onSelect = onRegYearChange,
//                     modifier = Modifier.weight(1f)
//                 )
//                 DropdownField(
//                     label = "Fuel Type",
//                     value = fuelType,
//                     options = listOf("Petrol", "Diesel", "Electric", "CNG", "Hybrid"),
//                     onSelect = onFuelTypeChange,
//                     placeholder = "Select",
//                     modifier = Modifier.weight(1f)
//                 )
//             }
// 
//             Spacer(Modifier.height(14.dp))
// 
//             // VIN (optional)
//             StyledTextField(
//                 value = vin,
//                 onValueChange = onVinChange,
//                 label = "VIN / Chassis Number (Optional)",
//                 placeholder = "Enter VIN Number",
//                 capitalization = KeyboardCapitalization.Characters
//             )
// 
//             Spacer(Modifier.height(24.dp))
// 
//             PrimaryButton("Continue", onContinue)
// 
//             Spacer(Modifier.height(10.dp))
//             Text(
//                 "You can also share these details later in settings.",
//                 modifier = Modifier.fillMaxWidth(),
//                 textAlign = TextAlign.Center,
//                 style = MaterialTheme.typography.bodySmall.copy(
//                     color = C.TextSecondary,
//                     fontSize = 11.5.sp
//                 )
//             )
//             Spacer(Modifier.height(20.dp))
//             Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
//         }
//     }
// }
// 
// @Composable
// private fun DropdownField(
//     label: String,
//     value: String,
//     options: List<String>,
//     onSelect: (String) -> Unit,
//     placeholder: String = "Select",
//     modifier: Modifier = Modifier
// ) {
//     var expanded by remember { mutableStateOf(false) }
//     Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(5.dp)) {
//         Text(
//             label.uppercase(),
//             style = MaterialTheme.typography.labelSmall.copy(
//                 fontSize = 10.5.sp,
//                 fontWeight = FontWeight.SemiBold,
//                 color = C.TextSecondary,
//                 letterSpacing = 0.6.sp
//             )
//         )
//         Box {
//             OutlinedTextField(
//                 value = value,
//                 onValueChange = {},
//                 readOnly = true,
//                 placeholder = { Text(placeholder, color = C.TextHint, fontSize = 14.sp) },
//                 trailingIcon = {
//                     Icon(
//                         if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
//                         null,
//                         tint = C.TextHint,
//                         modifier = Modifier.size(20.dp)
//                     )
//                 },
//                 modifier = Modifier
//                     .fillMaxWidth()
//                     .clickable { expanded = true },
//                 shape = RoundedCornerShape(10.dp),
//                 colors = OutlinedTextFieldDefaults.colors(
//                     focusedBorderColor   = C.Primary,
//                     unfocusedBorderColor = C.Border,
//                     unfocusedContainerColor = C.InputBg,
//                     focusedContainerColor   = C.Surface
//                 ),
//                 textStyle = MaterialTheme.typography.bodyMedium.copy(
//                     fontSize = 14.sp,
//                     color = C.TextPrimary,
//                     fontWeight = FontWeight.Medium
//                 )
//             )
//             DropdownMenu(
//                 expanded = expanded,
//                 onDismissRequest = { expanded = false },
//                 modifier = Modifier.background(C.Surface)
//             ) {
//                 options.forEach { option ->
//                     DropdownMenuItem(
//                         text = {
//                             Text(
//                                 option, fontSize = 13.sp,
//                                 color = if (option == value) C.Primary else C.TextPrimary,
//                                 fontWeight = if (option == value) FontWeight.SemiBold else FontWeight.Normal
//                             )
//                         },
//                         onClick = { onSelect(option); expanded = false }
//                     )
//                 }
//             }
//         }
//     }
// }
// 
// // ─────────────────────────────────────────────
// //  STEP 3 — CONFIRM YOUR VEHICLE
// // ─────────────────────────────────────────────
// 
// @Composable
// private fun Step3ConfirmVehicle(
//     regNumber: String,
//     onBack: () -> Unit,
//     onConfirm: () -> Unit
// ) {
//     Scaffold(
//         containerColor = C.Surface,
//         topBar = { VehicleTopBar("Vehicle Details", onBack) }
//     ) { padding ->
//         Column(
//             modifier = Modifier
//                 .fillMaxSize()
//                 .padding(padding)
//                 .padding(horizontal = 20.dp)
//                 .verticalScroll(rememberScrollState())
//         ) {
//             Spacer(Modifier.height(20.dp))
// 
//             Text(
//                 "Confirm Your Vehicle",
//                 style = MaterialTheme.typography.headlineSmall.copy(
//                     fontWeight = FontWeight.Bold,
//                     fontSize = 21.sp,
//                     color = C.TextPrimary
//                 )
//             )
//             Spacer(Modifier.height(4.dp))
//             Text(
//                 "We've auto detected these details. Please verify them\nbefore proceeding.",
//                 style = MaterialTheme.typography.bodyMedium.copy(
//                     color = C.TextSecondary,
//                     fontSize = 13.sp,
//                     lineHeight = 18.sp
//                 )
//             )
// 
//             Spacer(Modifier.height(20.dp))
// 
//             // Vehicle image card
//             Card(
//                 modifier = Modifier.fillMaxWidth(),
//                 shape = RoundedCornerShape(14.dp),
//                 colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F8F1)),
//                 elevation = CardDefaults.cardElevation(0.dp)
//             ) {
//                 Box(modifier = Modifier.fillMaxWidth().height(170.dp)) {
//                     // Gradient background
//                     Box(
//                         modifier = Modifier
//                             .fillMaxSize()
//                             .background(
//                                 Brush.verticalGradient(
//                                     listOf(Color(0xFFE8F5E9), Color(0xFFC8E6C9))
//                                 )
//                             )
//                     )
//                     // Vehicle silhouette placeholder
//                     Box(
//                         modifier = Modifier.fillMaxSize(),
//                         contentAlignment = Alignment.Center
//                     ) {
//                         Column(horizontalAlignment = Alignment.CenterHorizontally) {
//                             Icon(
//                                 Icons.Outlined.DirectionsCar,
//                                 null,
//                                 Modifier.size(70.dp),
//                                 tint = C.Primary.copy(alpha = 0.25f)
//                             )
//                         }
//                     }
//                     // Auto Detected badge
//                     Box(
//                         modifier = Modifier
//                             .align(Alignment.BottomEnd)
//                             .padding(10.dp)
//                             .background(C.Primary, RoundedCornerShape(6.dp))
//                             .padding(horizontal = 8.dp, vertical = 4.dp)
//                     ) {
//                         Row(
//                             verticalAlignment = Alignment.CenterVertically,
//                             horizontalArrangement = Arrangement.spacedBy(4.dp)
//                         ) {
//                             Icon(Icons.Default.AutoFixHigh, null, Modifier.size(11.dp), tint = Color.White)
//                             Text(
//                                 "AUTO DETECTED",
//                                 style = MaterialTheme.typography.labelSmall.copy(
//                                     fontSize = 9.sp,
//                                     fontWeight = FontWeight.Bold,
//                                     color = Color.White,
//                                     letterSpacing = 0.4.sp
//                                 )
//                             )
//                         }
//                     }
//                 }
//             }
// 
//             Spacer(Modifier.height(14.dp))
// 
//             // Detected model label
//             Row(
//                 modifier = Modifier.fillMaxWidth(),
//                 horizontalArrangement = Arrangement.SpaceBetween,
//                 verticalAlignment = Alignment.CenterVertically
//             ) {
//                 Column {
//                     Text(
//                         "DETECTED MODEL",
//                         style = MaterialTheme.typography.labelSmall.copy(
//                             fontSize = 10.sp,
//                             fontWeight = FontWeight.Bold,
//                             color = C.TextSecondary,
//                             letterSpacing = 0.5.sp
//                         )
//                     )
//                     Text(
//                         "EVA2Z Model X",
//                         style = MaterialTheme.typography.titleMedium.copy(
//                             fontWeight = FontWeight.Bold,
//                             fontSize = 16.sp,
//                             color = C.TextPrimary
//                         )
//                     )
//                 }
//                 IconButton(
//                     onClick = {},
//                     modifier = Modifier
//                         .size(34.dp)
//                         .background(C.PrimaryLight, CircleShape)
//                 ) {
//                     Icon(Icons.Outlined.Settings, null, Modifier.size(18.dp), tint = C.Primary)
//                 }
//             }
// 
//             Spacer(Modifier.height(16.dp))
//             HorizontalDivider(color = C.Divider)
//             Spacer(Modifier.height(14.dp))
// 
//             // Detail rows
//             listOf(
//                 Triple(Icons.Outlined.ConfirmationNumber, "VEHICLE NUMBER", regNumber),
//                 Triple(Icons.Outlined.Apartment,          "MANUFACTURER",   "EVA2Z Motors"),
//                 Triple(Icons.Outlined.DirectionsCar,      "MODEL",          "Model X (2024)")
//             ).forEach { (icon, label, value) ->
//                 ConfirmDetailRow(icon = icon, label = label, value = value)
//                 Spacer(Modifier.height(12.dp))
//             }
// 
//             Spacer(Modifier.height(12.dp))
//             PrimaryButton("Confirm Vehicle", onConfirm)
//             Spacer(Modifier.height(20.dp))
//             Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
//         }
//     }
// }
// 
// @Composable
// private fun ConfirmDetailRow(
//     icon: ImageVector,
//     label: String,
//     value: String
// ) {
//     Row(
//         modifier = Modifier.fillMaxWidth(),
//         verticalAlignment = Alignment.CenterVertically,
//         horizontalArrangement = Arrangement.spacedBy(12.dp)
//     ) {
//         Box(
//             modifier = Modifier
//                 .size(38.dp)
//                 .background(C.PrimaryLight, RoundedCornerShape(9.dp)),
//             contentAlignment = Alignment.Center
//         ) {
//             Icon(icon, null, Modifier.size(18.dp), tint = C.Primary)
//         }
//         Column(modifier = Modifier.weight(1f)) {
//             Text(
//                 label,
//                 style = MaterialTheme.typography.labelSmall.copy(
//                     fontSize = 10.sp,
//                     color = C.TextSecondary,
//                     letterSpacing = 0.4.sp,
//                     fontWeight = FontWeight.SemiBold
//                 )
//             )
//             Text(
//                 value,
//                 style = MaterialTheme.typography.bodyMedium.copy(
//                     fontSize = 14.sp,
//                     fontWeight = FontWeight.SemiBold,
//                     color = C.TextPrimary
//                 )
//             )
//         }
//         IconButton(
//             onClick = {},
//             modifier = Modifier.size(32.dp)
//         ) {
//             Icon(Icons.Outlined.Edit, null, Modifier.size(16.dp), tint = C.TextHint)
//         }
//     }
// }
// 
// // ─────────────────────────────────────────────
// //  STEP 4 — SECURE YOUR VEHICLE DOCUMENTS
// // ─────────────────────────────────────────────
// 
// @Composable
// private fun Step4SecureDocuments(
//     onBack: () -> Unit,
//     onContinue: () -> Unit
// ) {
//     // Track which docs are uploaded
//     var rcDone      by remember { mutableStateOf(true) }
//     var insDone     by remember { mutableStateOf(false) }
//     var pucDone     by remember { mutableStateOf(false) }
//     var licenseDone by remember { mutableStateOf(true) }
// 
//     val uploadedCount = listOf(rcDone, insDone, pucDone, licenseDone).count { it }
// 
//     Scaffold(
//         containerColor = C.Background,
//         topBar = {
//             VehicleTopBar(
//                 title = "Secure Your Vehicle Documents",
//                 onBack = onBack,
//                 trailingIcon = {
//                     Icon(Icons.Outlined.Info, null, tint = C.Primary, modifier = Modifier.size(20.dp))
//                 }
//             )
//         }
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
//                 verticalArrangement = Arrangement.spacedBy(0.dp)
//             ) {
//                 // Progress card
//                 Card(
//                     modifier = Modifier.fillMaxWidth(),
//                     shape = RoundedCornerShape(14.dp),
//                     colors = CardDefaults.cardColors(containerColor = C.Surface),
//                     elevation = CardDefaults.cardElevation(1.dp)
//                 ) {
//                     Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
//                         Row(
//                             modifier = Modifier.fillMaxWidth(),
//                             horizontalArrangement = Arrangement.SpaceBetween,
//                             verticalAlignment = Alignment.CenterVertically
//                         ) {
//                             Text(
//                                 "Documents Added",
//                                 style = MaterialTheme.typography.labelSmall.copy(
//                                     fontSize = 11.sp,
//                                     color = C.TextSecondary
//                                 )
//                             )
//                             Box(
//                                 modifier = Modifier
//                                     .background(C.GreenBg, RoundedCornerShape(6.dp))
//                                     .padding(horizontal = 8.dp, vertical = 3.dp)
//                             ) {
//                                 Text(
//                                     "${(uploadedCount * 100 / 4)}% COMPLETE",
//                                     style = MaterialTheme.typography.labelSmall.copy(
//                                         color = C.Green,
//                                         fontWeight = FontWeight.Bold,
//                                         fontSize = 10.sp,
//                                         letterSpacing = 0.3.sp
//                                     )
//                                 )
//                             }
//                         }
//                         Row(
//                             verticalAlignment = Alignment.CenterVertically,
//                             horizontalArrangement = Arrangement.spacedBy(6.dp)
//                         ) {
//                             Text(
//                                 "$uploadedCount",
//                                 style = MaterialTheme.typography.headlineMedium.copy(
//                                     fontWeight = FontWeight.Bold,
//                                     fontSize = 28.sp,
//                                     color = C.TextPrimary
//                                 )
//                             )
//                             Text(
//                                 "/ 4",
//                                 style = MaterialTheme.typography.bodyMedium.copy(
//                                     color = C.TextSecondary,
//                                     fontSize = 16.sp
//                                 )
//                             )
//                         }
//                         LinearProgressIndicator(
//                             progress = { uploadedCount / 4f },
//                             modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
//                             color = C.Primary,
//                             trackColor = C.Divider,
//                             strokeCap = StrokeCap.Round
//                         )
//                         Spacer(Modifier.height(2.dp))
//                         Text(
//                             "Keep your original documents ready for scanning.",
//                             style = MaterialTheme.typography.bodySmall.copy(
//                                 color = C.TextSecondary,
//                                 fontSize = 11.5.sp
//                             )
//                         )
//                     }
//                 }
// 
//                 Spacer(Modifier.height(18.dp))
// 
//                 Text(
//                     "REQUIRED DOCUMENTS",
//                     style = MaterialTheme.typography.labelSmall.copy(
//                         fontSize = 11.sp,
//                         fontWeight = FontWeight.Bold,
//                         color = C.TextSecondary,
//                         letterSpacing = 0.5.sp
//                     )
//                 )
// 
//                 Spacer(Modifier.height(10.dp))
// 
//                 // RC
//                 DocumentUploadRow(
//                     icon    = Icons.Outlined.Article,
//                     title   = "Registration Certificate (RC)",
//                     subtitle = "Upload a valid RC document",
//                     isDone  = rcDone,
//                     onCamera  = { rcDone = true },
//                     onGallery = { rcDone = true },
//                     onUpload  = { rcDone = true }
//                 )
// 
//                 Spacer(Modifier.height(10.dp))
// 
//                 DocumentUploadRow(
//                     icon    = Icons.Outlined.Shield,
//                     title   = "Vehicle Insurance",
//                     subtitle = "Upload a valid insurance policy document",
//                     isDone  = insDone,
//                     onCamera  = { insDone = true },
//                     onGallery = { insDone = true },
//                     onUpload  = { insDone = true }
//                 )
// 
//                 Spacer(Modifier.height(10.dp))
// 
//                 DocumentUploadRow(
//                     icon    = Icons.Outlined.Eco,
//                     title   = "PUC Certificate",
//                     subtitle = "Pollution Under Control certificate",
//                     isDone  = pucDone,
//                     onCamera  = { pucDone = true },
//                     onGallery = { pucDone = true },
//                     onUpload  = { pucDone = true }
//                 )
// 
//                 Spacer(Modifier.height(10.dp))
// 
//                 DocumentUploadRow(
//                     icon    = Icons.Outlined.Badge,
//                     title   = "Driving License",
//                     subtitle = "Validate by EVA2Z",
//                     isDone  = licenseDone,
//                     onCamera  = { licenseDone = true },
//                     onGallery = { licenseDone = true },
//                     onUpload  = { licenseDone = true },
//                     validatedByEva2z = true
//                 )
// 
//                 Spacer(Modifier.height(16.dp))
// 
//                 // Encrypted banner
//                 Row(
//                     modifier = Modifier
//                         .fillMaxWidth()
//                         .clip(RoundedCornerShape(12.dp))
//                         .background(C.Dark)
//                         .padding(14.dp),
//                     verticalAlignment = Alignment.CenterVertically,
//                     horizontalArrangement = Arrangement.spacedBy(10.dp)
//                 ) {
//                     Icon(Icons.Default.Lock, null, Modifier.size(20.dp), tint = Color(0xFFFFD700))
//                     Column {
//                         Text(
//                             "Encrypted & Secure",
//                             style = MaterialTheme.typography.labelMedium.copy(
//                                 fontWeight = FontWeight.Bold,
//                                 color = Color.White,
//                                 fontSize = 13.sp
//                             )
//                         )
//                         Text(
//                             "Your documents are safe with EVA2Z blockchain security.",
//                             style = MaterialTheme.typography.bodySmall.copy(
//                                 color = Color.White.copy(alpha = 0.65f),
//                                 fontSize = 11.sp,
//                                 lineHeight = 15.sp
//                             )
//                         )
//                     }
//                 }
//             }
// 
//             // Continue button
//             Surface(color = C.Surface, shadowElevation = 4.dp) {
//                 Column(
//                     modifier = Modifier
//                         .fillMaxWidth()
//                         .windowInsetsPadding(WindowInsets.navigationBars)
//                         .padding(horizontal = 16.dp, vertical = 12.dp)
//                 ) {
//                     PrimaryButton("Continue", onContinue)
//                     Spacer(Modifier.height(4.dp))
//                     Text(
//                         "POWERED BY EVA2Z CONNECTIVITY",
//                         modifier = Modifier.fillMaxWidth(),
//                         textAlign = TextAlign.Center,
//                         style = MaterialTheme.typography.labelSmall.copy(
//                             fontSize = 9.sp,
//                             color = C.TextHint,
//                             letterSpacing = 0.8.sp
//                         )
//                     )
//                 }
//             }
//         }
//     }
// }
// 
// @Composable
// private fun DocumentUploadRow(
//     icon: ImageVector,
//     title: String,
//     subtitle: String,
//     isDone: Boolean,
//     onCamera: () -> Unit,
//     onGallery: () -> Unit,
//     onUpload: () -> Unit,
//     validatedByEva2z: Boolean = false
// ) {
//     Card(
//         modifier = Modifier.fillMaxWidth(),
//         shape = RoundedCornerShape(12.dp),
//         colors = CardDefaults.cardColors(containerColor = C.Surface),
//         elevation = CardDefaults.cardElevation(1.dp)
//     ) {
//         Column(modifier = Modifier.fillMaxWidth()) {
//             Row(
//                 modifier = Modifier
//                     .fillMaxWidth()
//                     .padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = if (isDone) 12.dp else 8.dp),
//                 verticalAlignment = Alignment.CenterVertically,
//                 horizontalArrangement = Arrangement.spacedBy(10.dp)
//             ) {
//                 Box(
//                     modifier = Modifier
//                         .size(38.dp)
//                         .background(if (isDone) C.GreenBg else C.PrimaryLight, RoundedCornerShape(9.dp)),
//                     contentAlignment = Alignment.Center
//                 ) {
//                     Icon(
//                         icon, null, Modifier.size(19.dp),
//                         tint = if (isDone) C.Green else C.Primary
//                     )
//                 }
//                 Column(modifier = Modifier.weight(1f)) {
//                     Text(
//                         title,
//                         style = MaterialTheme.typography.bodyMedium.copy(
//                             fontWeight = FontWeight.SemiBold,
//                             color = C.TextPrimary,
//                             fontSize = 13.5.sp
//                         )
//                     )
//                     if (validatedByEva2z) {
//                         Row(
//                             verticalAlignment = Alignment.CenterVertically,
//                             horizontalArrangement = Arrangement.spacedBy(3.dp)
//                         ) {
//                             Icon(Icons.Default.Verified, null, Modifier.size(11.dp), tint = C.Green)
//                             Text(
//                                 "Validate by EVA2Z",
//                                 style = MaterialTheme.typography.labelSmall.copy(
//                                     fontSize = 11.sp,
//                                     color = C.Green,
//                                     fontWeight = FontWeight.Medium
//                                 )
//                             )
//                         }
//                     } else {
//                         Text(
//                             subtitle,
//                             style = MaterialTheme.typography.bodySmall.copy(
//                                 color = C.TextSecondary,
//                                 fontSize = 11.5.sp
//                             )
//                         )
//                     }
//                 }
//                 if (isDone) {
//                     Icon(Icons.Default.CheckCircle, null, Modifier.size(22.dp), tint = C.Green)
//                 }
//             }
// 
//             if (!isDone) {
//                 HorizontalDivider(color = C.Divider, modifier = Modifier.padding(horizontal = 12.dp))
//                 Row(
//                     modifier = Modifier
//                         .fillMaxWidth()
//                         .padding(horizontal = 8.dp, vertical = 8.dp),
//                     horizontalArrangement = Arrangement.spacedBy(8.dp)
//                 ) {
//                     // Camera
//                     UploadActionBtn(Icons.Outlined.CameraAlt, "CAMERA", onClick = onCamera, modifier = Modifier.weight(1f))
//                     // Gallery
//                     UploadActionBtn(Icons.Outlined.Photo, "GALLERY", onClick = onGallery, modifier = Modifier.weight(1f))
//                     // Upload
//                     UploadActionBtn(
//                         Icons.Outlined.Upload,
//                         "UPLOAD",
//                         onClick = onUpload,
//                         filled = true,
//                         modifier = Modifier.weight(1f)
//                     )
//                 }
//             }
//         }
//     }
// }
// 
// @Composable
// private fun UploadActionBtn(
//     icon: ImageVector,
//     label: String,
//     onClick: () -> Unit,
//     filled: Boolean = false,
//     modifier: Modifier = Modifier
// ) {
//     Column(
//         modifier = modifier
//             .clip(RoundedCornerShape(8.dp))
//             .background(if (filled) C.UploadBg else C.InputBg)
//             .clickable { onClick() }
//             .padding(vertical = 8.dp),
//         horizontalAlignment = Alignment.CenterHorizontally,
//         verticalArrangement = Arrangement.spacedBy(4.dp)
//     ) {
//         Icon(
//             icon, null, Modifier.size(18.dp),
//             tint = if (filled) Color.White else C.TextSecondary
//         )
//         Text(
//             label,
//             style = MaterialTheme.typography.labelSmall.copy(
//                 fontSize = 9.5.sp,
//                 fontWeight = FontWeight.SemiBold,
//                 color = if (filled) Color.White else C.TextSecondary,
//                 letterSpacing = 0.3.sp
//             )
//         )
//     }
// }
// 
// // ─────────────────────────────────────────────
// //  STEP 5 — REVIEW DOCUMENT
// // ─────────────────────────────────────────────
// 
// @Composable
// private fun Step5ReviewDocument(
//     onBack: () -> Unit,
//     onConfirm: () -> Unit
// ) {
//     var docType     by remember { mutableStateOf("Registration Certificate (RC)") }
//     var ownerName   by remember { mutableStateOf("Aarav Sharma") }
//     var vehicleNum  by remember { mutableStateOf("MH 12 AB 1234") }
//     var expiryDate  by remember { mutableStateOf("12 Oct 2028") }
// 
//     Scaffold(
//         containerColor = C.Background,
//         topBar = { VehicleTopBar("Review Document", onBack) }
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
//                 verticalArrangement = Arrangement.spacedBy(14.dp)
//             ) {
//                 // Scanned document preview card
//                 Card(
//                     modifier = Modifier.fillMaxWidth(),
//                     shape = RoundedCornerShape(14.dp),
//                     colors = CardDefaults.cardColors(containerColor = C.Surface),
//                     elevation = CardDefaults.cardElevation(2.dp)
//                 ) {
//                     Box(modifier = Modifier.fillMaxWidth().height(160.dp)) {
//                         // Simulate scanned doc appearance
//                         Box(
//                             modifier = Modifier
//                                 .fillMaxSize()
//                                 .background(
//                                     Brush.verticalGradient(
//                                         listOf(Color(0xFFF5EFE8), Color(0xFFEDE5D8))
//                                     )
//                                 )
//                         )
//                         // Document lines simulation
//                         Column(
//                             modifier = Modifier
//                                 .fillMaxSize()
//                                 .padding(16.dp),
//                             verticalArrangement = Arrangement.spacedBy(7.dp)
//                         ) {
//                             repeat(2) {
//                                 Box(Modifier.fillMaxWidth(0.5f).height(10.dp).background(Color(0xFFBDB0A0).copy(alpha = 0.6f), RoundedCornerShape(3.dp)))
//                             }
//                             Spacer(Modifier.height(2.dp))
//                             repeat(6) { i ->
//                                 Box(
//                                     Modifier.fillMaxWidth(if (i % 2 == 0) 0.9f else 0.75f)
//                                         .height(7.dp)
//                                         .background(Color(0xFFCDC4B5).copy(alpha = 0.5f), RoundedCornerShape(2.dp))
//                                 )
//                             }
//                             Spacer(Modifier.height(4.dp))
//                             Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
//                                 Box(Modifier.size(30.dp).background(Color(0xFFBDB0A0).copy(alpha = 0.5f), CircleShape))
//                                 Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
//                                     Box(Modifier.width(70.dp).height(7.dp).background(Color(0xFFCDC4B5).copy(alpha = 0.5f), RoundedCornerShape(2.dp)))
//                                     Box(Modifier.width(50.dp).height(6.dp).background(Color(0xFFCDC4B5).copy(alpha = 0.4f), RoundedCornerShape(2.dp)))
//                                 }
//                             }
//                         }
//                         // SCANNED badge
//                         Box(
//                             modifier = Modifier
//                                 .align(Alignment.TopEnd)
//                                 .padding(10.dp)
//                                 .background(C.Green, RoundedCornerShape(6.dp))
//                                 .padding(horizontal = 8.dp, vertical = 3.dp)
//                         ) {
//                             Text(
//                                 "SCANNED",
//                                 style = MaterialTheme.typography.labelSmall.copy(
//                                     fontSize = 9.sp,
//                                     fontWeight = FontWeight.Bold,
//                                     color = Color.White,
//                                     letterSpacing = 0.4.sp
//                                 )
//                             )
//                         }
//                     }
//                 }
// 
//                 // Extracted Details card
//                 Card(
//                     modifier = Modifier.fillMaxWidth(),
//                     shape = RoundedCornerShape(14.dp),
//                     colors = CardDefaults.cardColors(containerColor = C.Surface),
//                     elevation = CardDefaults.cardElevation(1.dp)
//                 ) {
//                     Column(
//                         modifier = Modifier
//                             .fillMaxWidth()
//                             .padding(16.dp),
//                         verticalArrangement = Arrangement.spacedBy(0.dp)
//                     ) {
//                         Text(
//                             "Extracted Details",
//                             style = MaterialTheme.typography.titleSmall.copy(
//                                 fontWeight = FontWeight.Bold,
//                                 fontSize = 15.sp,
//                                 color = C.TextPrimary
//                             )
//                         )
//                         Spacer(Modifier.height(2.dp))
//                         Text(
//                             "Please verify if the information below is correct.",
//                             style = MaterialTheme.typography.bodySmall.copy(
//                                 color = C.TextSecondary,
//                                 fontSize = 12.sp
//                             )
//                         )
// 
//                         Spacer(Modifier.height(16.dp))
// 
//                         // Fields
//                         ExtractedField(
//                             label = "Document Type",
//                             value = docType,
//                             onValueChange = { docType = it },
//                             trailingIcon = Icons.Outlined.Description
//                         )
//                         HorizontalDivider(color = C.Divider, modifier = Modifier.padding(vertical = 10.dp))
// 
//                         ExtractedField(
//                             label = "Owner Name",
//                             value = ownerName,
//                             onValueChange = { ownerName = it },
//                             trailingIcon = Icons.Outlined.Person
//                         )
//                         HorizontalDivider(color = C.Divider, modifier = Modifier.padding(vertical = 10.dp))
// 
//                         ExtractedField(
//                             label = "Vehicle Number",
//                             value = vehicleNum,
//                             onValueChange = { vehicleNum = it },
//                             trailingIcon = Icons.Outlined.DirectionsCar
//                         )
//                         HorizontalDivider(color = C.Divider, modifier = Modifier.padding(vertical = 10.dp))
// 
//                         // Expiry Date (calendar icon)
//                         ExtractedField(
//                             label = "Expiry Date",
//                             value = expiryDate,
//                             onValueChange = { expiryDate = it },
//                             trailingIcon = Icons.Outlined.CalendarMonth,
//                             keyboardType = KeyboardType.Text
//                         )
//                     }
//                 }
//             }
// 
//             // Bottom buttons
//             Surface(color = C.Surface, shadowElevation = 4.dp) {
//                 Row(
//                     modifier = Modifier
//                         .fillMaxWidth()
//                         .windowInsetsPadding(WindowInsets.navigationBars)
//                         .padding(horizontal = 16.dp, vertical = 12.dp),
//                     horizontalArrangement = Arrangement.spacedBy(12.dp)
//                 ) {
//                     OutlinedButton(
//                         onClick = onBack,
//                         modifier = Modifier.weight(1f).height(50.dp),
//                         shape = RoundedCornerShape(12.dp),
//                         border = BorderStroke(1.5.dp, C.Border),
//                         colors = ButtonDefaults.outlinedButtonColors(contentColor = C.TextPrimary)
//                     ) {
//                         Text("Edit", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
//                     }
//                     Button(
//                         onClick = onConfirm,
//                         modifier = Modifier.weight(2f).height(50.dp),
//                         shape = RoundedCornerShape(12.dp),
//                         colors = ButtonDefaults.buttonColors(containerColor = C.Primary)
//                     ) {
//                         Text("Confirm Details", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Color.White)
//                     }
//                 }
//             }
//         }
//     }
// }
// 
// @Composable
// private fun ExtractedField(
//     label: String,
//     value: String,
//     onValueChange: (String) -> Unit,
//     trailingIcon: ImageVector,
//     keyboardType: KeyboardType = KeyboardType.Text
// ) {
//     Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
//         Text(
//             label,
//             style = MaterialTheme.typography.labelSmall.copy(
//                 fontSize = 11.sp,
//                 color = C.TextSecondary,
//                 fontWeight = FontWeight.Medium,
//                 letterSpacing = 0.2.sp
//             )
//         )
//         OutlinedTextField(
//             value = value,
//             onValueChange = onValueChange,
//             modifier = Modifier.fillMaxWidth(),
//             singleLine = true,
//             shape = RoundedCornerShape(10.dp),
//             trailingIcon = {
//                 Icon(trailingIcon, null, Modifier.size(19.dp), tint = C.TextHint)
//             },
//             colors = OutlinedTextFieldDefaults.colors(
//                 focusedBorderColor   = C.Primary,
//                 unfocusedBorderColor = C.Border,
//                 unfocusedContainerColor = C.InputBg,
//                 focusedContainerColor   = C.Surface,
//                 cursorColor = C.Primary
//             ),
//             textStyle = MaterialTheme.typography.bodyMedium.copy(
//                 fontSize = 14.sp,
//                 fontWeight = FontWeight.Medium,
//                 color = C.TextPrimary
//             ),
//             keyboardOptions = KeyboardOptions(keyboardType = keyboardType)
//         )
//     }
// }