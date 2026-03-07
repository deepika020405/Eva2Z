package com.developer.ev2z.ui.screens.VechicleParkingTag


import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ─────────────────────────────────────────────────────────────────
// Design tokens — matches your app's BrandGreen, CardColor etc.
// ─────────────────────────────────────────────────────────────────

private val BrandGreen      = Color(0xFF34A853)   // same as HomeScreen.kt
private val GreenLight      = Color(0xFFE8F8ED)
private val GreenMid        = Color(0xFFC4EDD0)
private val BackgroundColor = Color(0xFFF6F6F6)   // same as HomeScreen.kt
private val CardColor       = Color.White
private val TextPrimary     = Color(0xFF1E1E1E)   // same as HomeScreen.kt
private val TextSecondary   = Color(0xFF777777)   // same as HomeScreen.kt
private val TextTertiary    = Color(0xFF9CA3AF)
private val BorderColor     = Color(0xFFEAECF0)
private val ErrorColor      = Color(0xFFEF4444)
private val SuccessBg       = Color(0xFFECFDF5)
private val SuccessText     = Color(0xFF059669)
private val WarningBg       = Color(0xFFFEF3C7)
private val WarningText     = Color(0xFFD97706)

// ─────────────────────────────────────────────────────────────────
// ParkingScreen  — entry point
// Called from HomeNavGraph just like DriverScoreFlowScreen etc.
// ─────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParkingScreen(
    viewModel: ParkingViewModel,
    onBackPressed: () -> Unit
) {
    val vehicleType        by viewModel.vehicleType.collectAsState()
    val vehicleNumber      by viewModel.vehicleNumber.collectAsState()
    val vehicleTypeError   by viewModel.vehicleTypeError.collectAsState()
    val vehicleNumberError by viewModel.vehicleNumberError.collectAsState()
    val uiState            by viewModel.uiState.collectAsState()
    val history            by viewModel.history.collectAsState()
    val focusManager       = LocalFocusManager.current

    // Load history on enter
    LaunchedEffect(Unit) { viewModel.loadHistory() }

    Scaffold(
        containerColor = BackgroundColor,
        topBar = {
            ParkingTopBar(onBackPressed = onBackPressed)
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {


            // ── Form Section Label ──────────────────────────────
            SectionLabel("Park a Vehicle")

            // ── Form Card ───────────────────────────────────────
            ParkingFormCard(
                vehicleType        = vehicleType,
                vehicleNumber      = vehicleNumber,
                vehicleTypeError   = vehicleTypeError,
                vehicleNumberError = vehicleNumberError,
                isLoading          = uiState is ParkingUiState.Loading,
                isFormValid        = vehicleType.isNotBlank() && vehicleNumber.isNotBlank(),
                onVehicleTypeChange   = viewModel::onVehicleTypeChange,
                onVehicleNumberChange = viewModel::onVehicleNumberChange,
                onChipSelect          = viewModel::selectVehicleType,
                onSubmit = {
                    focusManager.clearFocus()
                    viewModel.submitParking()
                },
                onImeNext = { focusManager.moveFocus(FocusDirection.Down) }
            )

            // ── Success / Error Banners ─────────────────────────
            AnimatedVisibility(
                visible = uiState !is ParkingUiState.Idle &&
                        uiState !is ParkingUiState.Loading,
                enter   = fadeIn() + expandVertically(),
                exit    = fadeOut() + shrinkVertically()
            ) {
                when (val s = uiState) {
                    is ParkingUiState.Success ->
                        ParkingSuccessBanner(
                            response    = s.response,
                            onParkAnother = viewModel::clearForm
                        )
                    is ParkingUiState.Error ->
                        ParkingErrorBanner(
                            message   = s.message,
                            onDismiss = viewModel::resetState
                        )
                    else -> Unit
                }
            }

            // ── Recent Parkings ─────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SectionLabel("Recent Parkings")
                Text(
                    "View All",
                    fontSize   = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color      = BrandGreen
                )
            }

            RecentParkingsCard(history = history)

            Spacer(Modifier.height(16.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// Top Bar  — same style as your other screens (e.g. GPSDataScreen)
// ─────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ParkingTopBar(onBackPressed: () -> Unit) {
    Surface(color = CardColor, shadowElevation = 1.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Back button — same as your app's pattern
            IconButton(
                onClick = onBackPressed,
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(BackgroundColor)
            ) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint     = TextPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Vehicle Parking",
                    fontSize   = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color      = TextPrimary
                )
                Text(
                    "Register your parking slot",
                    fontSize = 12.sp,
                    color    = TextSecondary
                )
            }

        }
    }
}



// ─────────────────────────────────────────────────────────────────
// Section Label  — same as your "Quick Actions", "Upcoming Features"
// ─────────────────────────────────────────────────────────────────

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        fontSize   = 16.sp,
        fontWeight = FontWeight.Bold,
        color      = TextPrimary,
        modifier   = Modifier.padding(horizontal = 2.dp)
    )
}

// ─────────────────────────────────────────────────────────────────
// Form Card  — same Card style as HomeQuickActionsSection
// ─────────────────────────────────────────────────────────────────

@Composable
private fun ParkingFormCard(
    vehicleType: String,
    vehicleNumber: String,
    vehicleTypeError: String?,
    vehicleNumberError: String?,
    isLoading: Boolean,
    isFormValid: Boolean,
    onVehicleTypeChange: (String) -> Unit,
    onVehicleNumberChange: (String) -> Unit,
    onChipSelect: (String) -> Unit,
    onSubmit: () -> Unit,
    onImeNext: () -> Unit
) {
//    val quickTypes = listOf("Honda City", "Maruti Swift", "Hyundai i20", "Tata Nexon", "VEGUS EV")

    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(24.dp),       // matches your HomeScreen cards
        elevation = CardDefaults.cardElevation(4.dp), // same elevation
        colors    = CardDefaults.cardColors(containerColor = CardColor)
    ) {
        Column(
            modifier            = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {

            // ── Vehicle Type Field ────────────────────────────
            Eva2zInputField(
                value           = vehicleType,
                onValueChange   = onVehicleTypeChange,
                label           = "VEHICLE TYPE",
                placeholder     = "e.g. Honda City",
                icon            = Icons.Outlined.DirectionsCar,
                errorText       = vehicleTypeError,
                imeAction       = ImeAction.Next,
                keyboardActions = KeyboardActions(onNext = { onImeNext() })
            )

            // ── Quick select chips ────────────────────────────
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState())
            ) {
//                quickTypes.forEach { type ->
//                    VehicleTypeChip(
//                        label    = type,
//                        selected = vehicleType.equals(type, ignoreCase = true),
//                        onClick  = { onChipSelect(type) }
//                    )
//                }
            }

            Divider(color = BorderColor)

            // ── Vehicle Number Field ──────────────────────────
            Eva2zInputField(
                value           = vehicleNumber,
                onValueChange   = onVehicleNumberChange,
                label           = "VEHICLE NUMBER",
                placeholder     = "e.g. DL01AB1234",
                icon            = Icons.Outlined.Tag,
                errorText       = vehicleNumberError,
                imeAction       = ImeAction.Done,
                capitalization  = KeyboardCapitalization.Characters,
                keyboardActions = KeyboardActions(onDone = {
                    if (isFormValid) onSubmit()
                })
            )

            // ── Tip row ───────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(GreenLight)
                    .padding(11.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    Icons.Default.Info,
                    null,
                    tint     = BrandGreen,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    "Enter registration number exactly as on RC (e.g. DL01AB1234)",
                    fontSize   = 12.sp,
                    color      = Color(0xFF2D6A3F),
                    lineHeight = 18.sp
                )
            }

            // ── Submit Button — same style as your app's primary buttons ──
            Button(
                onClick  = onSubmit,
                enabled  = isFormValid && !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape  = RoundedCornerShape(50),    // pill — same as your "Unlock Documents" btn
                colors = ButtonDefaults.buttonColors(
                    containerColor         = BrandGreen,
                    disabledContainerColor = BrandGreen.copy(alpha = 0.35f)
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier    = Modifier.size(20.dp),
                        color       = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "Registering…",
                        fontWeight = FontWeight.Bold,
                        fontSize   = 15.sp
                    )
                } else {
                    Text(
                        " Register Parking Slot",
                        fontWeight = FontWeight.Bold,
                        fontSize   = 15.sp
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// Input Field  — OutlinedTextField matching your app style
// ─────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Eva2zInputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    icon: ImageVector,
    errorText: String?          = null,
    imeAction: ImeAction        = ImeAction.Next,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.Words,
    keyboardActions: KeyboardActions       = KeyboardActions.Default
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {

        Text(
            label,
            fontSize     = 11.sp,
            fontWeight   = FontWeight.SemiBold,
            color        = TextSecondary,
            letterSpacing = 0.4.sp
        )

        OutlinedTextField(
            value         = value,
            onValueChange = onValueChange,
            placeholder   = {
                Text(placeholder, fontSize = 14.sp, color = TextTertiary)
            },
            leadingIcon = {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(GreenLight),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        icon, null,
                        tint     = BrandGreen,
                        modifier = Modifier.size(16.dp)
                    )
                }
            },
            trailingIcon = if (value.isNotBlank()) ({
                Icon(
                    Icons.Default.CheckCircle, null,
                    tint     = BrandGreen,
                    modifier = Modifier.size(20.dp)
                )
            }) else null,
            isError  = errorText != null,
            modifier = Modifier.fillMaxWidth(),
            shape    = RoundedCornerShape(14.dp),
            colors   = OutlinedTextFieldDefaults.colors(
                focusedBorderColor      = BrandGreen,
                unfocusedBorderColor    = BorderColor,
                errorBorderColor        = ErrorColor,
                focusedContainerColor   = CardColor,
                unfocusedContainerColor = BackgroundColor,
                errorContainerColor     = Color(0xFFFEF2F2),
                focusedTextColor        = TextPrimary,
                unfocusedTextColor      = TextPrimary,
                cursorColor             = BrandGreen
            ),
            keyboardOptions = KeyboardOptions(
                capitalization = capitalization,
                keyboardType   = KeyboardType.Text,
                imeAction      = imeAction
            ),
            keyboardActions = keyboardActions,
            singleLine      = true,
            textStyle       = LocalTextStyle.current.copy(
                fontSize   = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color      = TextPrimary
            )
        )

        errorText?.let { err ->
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    Icons.Default.Error, null,
                    tint     = ErrorColor,
                    modifier = Modifier.size(13.dp)
                )
                Text(err, fontSize = 12.sp, color = ErrorColor)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// Quick-select chip
// ─────────────────────────────────────────────────────────────────

@Composable
private fun VehicleTypeChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) GreenLight else BackgroundColor)
            .border(
                width = 1.5.dp,
                color = if (selected) BrandGreen else BorderColor,
                shape = RoundedCornerShape(20.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 7.dp)
    ) {
        Text(
            label,
            fontSize   = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color      = if (selected) BrandGreen else TextSecondary
        )
    }
}

// ─────────────────────────────────────────────────────────────────
// Success Banner
// ─────────────────────────────────────────────────────────────────

@Composable
private fun ParkingSuccessBanner(
    response: ParkingResponse,
    onParkAnother: () -> Unit
) {
    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = SuccessBg),
        border    = BorderStroke(1.dp, GreenMid)
    ) {
        Column(
            modifier                = Modifier.padding(18.dp),
            horizontalAlignment     = Alignment.CenterHorizontally,
            verticalArrangement     = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    Icons.Default.CheckCircle, null,
                    tint     = SuccessText,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    "Vehicle Parked Successfully!",
                    fontSize   = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color      = SuccessText
                )
            }

            response.data?.let { data ->
                Divider(color = GreenMid)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    data.vehicle_type?.let   { BannerRow("Type",     it) }
                    data.vehicle_number?.let { BannerRow("Number",   it) }
                    data.parked_at?.let      { BannerRow("Parked At", it) }
                    data.status?.let         { BannerRow("Status",   it) }
                    data.id?.let             { BannerRow("Ticket #", "#$it") }
                }
            }

            response.message?.let {
                Text(
                    it,
                    fontSize  = 13.sp,
                    color     = TextSecondary,
                    textAlign = TextAlign.Center
                )
            }

            OutlinedButton(
                onClick  = onParkAnother,
                shape    = RoundedCornerShape(50),
                border   = BorderStroke(1.5.dp, BrandGreen),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Park Another Vehicle",
                    color      = BrandGreen,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// Error Banner
// ─────────────────────────────────────────────────────────────────

@Composable
private fun ParkingErrorBanner(message: String, onDismiss: () -> Unit) {
    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(14.dp),
        colors    = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
        border    = BorderStroke(1.dp, ErrorColor.copy(alpha = 0.35f))
    ) {
        Row(
            modifier              = Modifier.padding(16.dp),
            verticalAlignment     = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                Icons.Default.ErrorOutline, null,
                tint     = ErrorColor,
                modifier = Modifier.size(22.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Error",
                    fontSize   = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color      = ErrorColor
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    message,
                    fontSize   = 13.sp,
                    color      = TextSecondary,
                    lineHeight = 18.sp
                )
            }
            IconButton(
                onClick  = onDismiss,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    Icons.Default.Close, null,
                    tint     = TextTertiary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// Recent Parkings Card
// ─────────────────────────────────────────────────────────────────

@Composable
private fun RecentParkingsCard(history: List<ParkingData>) {
    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(4.dp),
        colors    = CardDefaults.cardColors(containerColor = CardColor)
    ) {
        Column(
            modifier            = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (history.isEmpty()) {
                Row(
                    modifier              = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Outlined.DirectionsCar, null,
                        tint     = TextTertiary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "No recent parkings",
                        fontSize = 13.sp,
                        color    = TextTertiary
                    )
                }
            } else {
                history.take(5).forEachIndexed { i, item ->
                    if (i > 0) Divider(color = BorderColor)
                    ParkingHistoryRow(item = item)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// Single history row
// ─────────────────────────────────────────────────────────────────

@Composable
private fun ParkingHistoryRow(item: ParkingData) {
    Row(
        modifier              = Modifier.fillMaxWidth(),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Icon box — same style as ModernFeatureItem
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(GreenLight),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.LocalParking, null,
                tint     = BrandGreen,
                modifier = Modifier.size(22.dp)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                item.vehicle_type ?: "Unknown",
                fontSize   = 13.sp,
                fontWeight = FontWeight.Bold,
                color      = TextPrimary,
                maxLines   = 1,
                overflow   = TextOverflow.Ellipsis
            )
            Text(
                item.vehicle_number ?: "–",
                fontSize = 12.sp,
                color    = TextSecondary,
                modifier = Modifier.padding(top = 2.dp)
            )
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                item.parked_at ?: item.created_at ?: "–",
                fontSize = 11.sp,
                color    = TextTertiary
            )
            val isActive = item.status?.lowercase() == "active"
            Box(
                modifier = Modifier
                    .padding(top = 4.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isActive) GreenLight else WarningBg)
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    if (isActive) "Active" else (item.status ?: "–"),
                    fontSize   = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color      = if (isActive) BrandGreen else WarningText
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// Shared banner row helper
// ─────────────────────────────────────────────────────────────────

@Composable
private fun BannerRow(label: String, value: String) {
    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 13.sp, color = TextSecondary)
        Text(
            value,
            fontSize   = 13.sp,
            fontWeight = FontWeight.Bold,
            color      = TextPrimary
        )
    }
}