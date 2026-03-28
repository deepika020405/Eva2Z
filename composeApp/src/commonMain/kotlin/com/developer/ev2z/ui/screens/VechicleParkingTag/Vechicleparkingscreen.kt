package com.developer.ev2z.ui.screens.VechicleParkingTag

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
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
import com.developer.ev2z.navigation.PlatformBackHandler
import kotlinx.coroutines.launch

// ─────────────────────────────────────────────────────────────────
// Design tokens
// ─────────────────────────────────────────────────────────────────

private val BrandGreen      = Color(0xFF34A853)
private val GreenLight      = Color(0xFFE8F8ED)
private val GreenMid        = Color(0xFFC4EDD0)
private val BackgroundColor = Color(0xFFF6F6F6)
private val CardColor       = Color.White
private val TextPrimary     = Color(0xFF1E1E1E)
private val TextSecondary   = Color(0xFF777777)
private val TextTertiary    = Color(0xFF9CA3AF)
private val BorderColor     = Color(0xFFEAECF0)
private val ErrorColor      = Color(0xFFEF4444)
private val SuccessBg       = Color(0xFFECFDF5)
private val SuccessText     = Color(0xFF059669)
private val WarningBg       = Color(0xFFFEF3C7)
private val WarningText     = Color(0xFFD97706)
private val DeleteRed       = Color(0xFFDC2626)
private val DeleteRedLight  = Color(0xFFFEF2F2)

// ─────────────────────────────────────────────────────────────────
// ParkingScreen — entry point
// ─────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParkingScreen(
    viewModel: ParkingViewModel,
    onBackPressed: () -> Unit
) {

    val vehicleType         by viewModel.vehicleType.collectAsState()
    val vehicleNumber       by viewModel.vehicleNumber.collectAsState()
    val phone               by viewModel.phone.collectAsState()
    val alternatePhone      by viewModel.alternatePhone.collectAsState()
    val vehicleTypeError    by viewModel.vehicleTypeError.collectAsState()
    val vehicleNumberError  by viewModel.vehicleNumberError.collectAsState()
    val phoneError          by viewModel.phoneError.collectAsState()
    val alternatePhoneError by viewModel.alternatePhoneError.collectAsState()
    val uiState             by viewModel.uiState.collectAsState()
    val history             by viewModel.history.collectAsState()
    val registeredPhone     by viewModel.registeredPhone.collectAsState()
    val focusManager        = LocalFocusManager.current

    // ── Delete confirmation state ─────────────────────────────────
    var deleteTargetId   by remember { mutableStateOf<Int?>(null) }
    var deleteTargetName by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope             = rememberCoroutineScope()


    // ✅ Success hone par form clear karo
    LaunchedEffect(uiState) {
        if (uiState is ParkingUiState.Success) {
            viewModel.clearFormFields()
            focusManager.clearFocus()
        }
    }

    PlatformBackHandler(enabled = true) { onBackPressed() }

    LaunchedEffect(Unit) { viewModel.loadHistory() }

    Scaffold(
        containerColor = BackgroundColor,
        snackbarHost   = { SnackbarHost(snackbarHostState) },
        topBar         = { ParkingTopBar(onBackPressed = onBackPressed) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SectionLabel("Register a Vehicle")

            // ── Registered Number Banner ──────────────────────────
            if (registeredPhone.isNotBlank()) {
                Surface(
                    shape    = RoundedCornerShape(12.dp),
                    color    = Color(0xFFF0FDF4),
                    border   = BorderStroke(1.dp, Color(0xFFBBF7D0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(GreenLight),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Person, null, tint = BrandGreen, modifier = Modifier.size(16.dp))
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Your Registered Number",
                                fontSize      = 10.sp,
                                fontWeight    = FontWeight.SemiBold,
                                color         = Color(0xFF15803D),
                                letterSpacing = 0.3.sp
                            )
                            Text(
                                "+91 ${registeredPhone.chunked(5).joinToString(" ")}",
                                fontSize   = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color      = BrandGreen
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFDCFCE7)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(Icons.Default.Lock, null, tint = BrandGreen, modifier = Modifier.size(10.dp))
                                Text("Protected", fontSize = 9.sp, color = BrandGreen, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }

            // ✅ Form sirf tab dikhao jab success nahi hai
            AnimatedVisibility(
                visible = uiState !is ParkingUiState.Success,
                enter   = fadeIn() + expandVertically(),
                exit    = fadeOut() + shrinkVertically()
            ) {
                ParkingFormCard(
                    vehicleType         = vehicleType,
                    vehicleNumber       = vehicleNumber,
                    phone               = phone,
                    alternatePhone      = alternatePhone,
                    vehicleTypeError    = vehicleTypeError,
                    vehicleNumberError  = vehicleNumberError,
                    phoneError          = phoneError,
                    alternatePhoneError = alternatePhoneError,
                    isLoading           = uiState is ParkingUiState.Loading,
                    isFormValid         = viewModel.isFormValid,
                    onVehicleTypeChange    = viewModel::onVehicleTypeChange,
                    onVehicleNumberChange  = viewModel::onVehicleNumberChange,
                    onPhoneChange          = viewModel::onPhoneChange,
                    onAlternatePhoneChange = viewModel::onAlternatePhoneChange,
                    onChipSelect           = viewModel::selectVehicleType,
                    onSubmit = {
                        focusManager.clearFocus()
                        viewModel.submitParking()
                    },
                    onImeNext = { focusManager.moveFocus(FocusDirection.Down) }
                )
            }

            // ── Success / Error Banners ───────────────────────
            AnimatedVisibility(
                visible = uiState !is ParkingUiState.Idle && uiState !is ParkingUiState.Loading,
                enter   = fadeIn() + expandVertically(),
                exit    = fadeOut() + shrinkVertically()
            ) {
                when (val s = uiState) {
                    is ParkingUiState.Success ->
                        // ✅ onGoHome callback added
                        ParkingSuccessBanner(
                            response      = s.response,
                            onParkAnother = {
                                viewModel.clearForm()
                            },
                            onGoHome      = onBackPressed  // ✅ home pe jaao
                        )
                    is ParkingUiState.Error ->
                        ParkingErrorBanner(message = s.message, onDismiss = viewModel::resetState)
                    else -> Unit
                }
            }

            // ── Recent Parkings header ────────────────────────
            Row(
                modifier              = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                SectionLabel("Recent SOS Registration")
                Text("View All", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = BrandGreen)
            }

            RecentParkingsCard(
                history  = history,
                onDelete = { id, name ->
                    deleteTargetId   = id
                    deleteTargetName = name
                }
            )

            Spacer(Modifier.height(16.dp))
        }
    }

    // ── Delete Confirmation Dialog ────────────────────────────────
    if (deleteTargetId != null) {
        AlertDialog(
            onDismissRequest = { deleteTargetId = null },
            shape = RoundedCornerShape(20.dp),
            icon = {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(DeleteRedLight),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Delete, null, tint = DeleteRed, modifier = Modifier.size(24.dp))
                }
            },
            title = {
                Text("Delete Registration", fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
            },
            text = {
                Text(
                    "Remove \"$deleteTargetName\" from your registered vehicles? This cannot be undone.",
                    fontSize  = 14.sp,
                    color     = TextSecondary,
                    textAlign = TextAlign.Center
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val id = deleteTargetId ?: return@Button
                        viewModel.deleteVehicle(
                            vehicleId = id,
                            onSuccess = {
                                scope.launch {
                                    snackbarHostState.showSnackbar("Vehicle removed successfully")
                                }
                            },
                            onError = { msg ->
                                scope.launch {
                                    snackbarHostState.showSnackbar(msg)
                                }
                            }
                        )
                        deleteTargetId = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DeleteRed),
                    shape  = RoundedCornerShape(10.dp)
                ) { Text("Delete") }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { deleteTargetId = null },
                    shape   = RoundedCornerShape(10.dp)
                ) { Text("Cancel") }
            }
        )
    }
}

// ─────────────────────────────────────────────────────────────────
// Top Bar
// ─────────────────────────────────────────────────────────────────

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
            IconButton(
                onClick  = onBackPressed,
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(BackgroundColor)
            ) {
                Icon(
                    Icons.Default.ArrowBack, contentDescription = "Back",
                    tint = TextPrimary, modifier = Modifier.size(20.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("Vehicle SOS Registration", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text("Register your SOS parking Tag", fontSize = 12.sp, color = TextSecondary)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// Section Label
// ─────────────────────────────────────────────────────────────────

@Composable
private fun SectionLabel(text: String) {
    Text(
        text, fontSize = 16.sp, fontWeight = FontWeight.Bold,
        color = TextPrimary, modifier = Modifier.padding(horizontal = 2.dp)
    )
}

// ─────────────────────────────────────────────────────────────────
// Form Card
// ─────────────────────────────────────────────────────────────────

@Composable
private fun ParkingFormCard(
    vehicleType: String,
    vehicleNumber: String,
    phone: String,
    alternatePhone: String,
    vehicleTypeError: String?,
    vehicleNumberError: String?,
    phoneError: String?,
    alternatePhoneError: String?,
    isLoading: Boolean,
    isFormValid: Boolean,
    onVehicleTypeChange: (String) -> Unit,
    onVehicleNumberChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit,
    onAlternatePhoneChange: (String) -> Unit,
    onChipSelect: (String) -> Unit,
    onSubmit: () -> Unit,
    onImeNext: () -> Unit
) {
    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(4.dp),
        colors    = CardDefaults.cardColors(containerColor = CardColor)
    ) {
        Column(
            modifier            = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
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

            Divider(color = BorderColor)

            Eva2zInputField(
                value           = vehicleNumber,
                onValueChange   = onVehicleNumberChange,
                label           = "VEHICLE NUMBER",
                placeholder     = "e.g. DL01AB1234",
                icon            = Icons.Outlined.Tag,
                errorText       = vehicleNumberError,
                imeAction       = ImeAction.Next,
                capitalization  = KeyboardCapitalization.Characters,
                keyboardActions = KeyboardActions(onNext = { onImeNext() })
            )

            Divider(color = BorderColor)

            Eva2zInputField(
                value           = phone,
                onValueChange   = onPhoneChange,
                label           = "ALTERNATE NUMBER 1 *",
                placeholder     = "e.g. 9876543210",
                icon            = Icons.Outlined.Phone,
                errorText       = phoneError,
                imeAction       = ImeAction.Next,
                capitalization  = KeyboardCapitalization.None,
                keyboardType    = KeyboardType.Phone,
                keyboardActions = KeyboardActions(onNext = { onImeNext() })
            )

            Divider(color = BorderColor)

            Eva2zInputField(
                value           = alternatePhone,
                onValueChange   = onAlternatePhoneChange,
                label           = "ALTERNATE NUMBER 2",
                placeholder     = "e.g. 9876543210",
                icon            = Icons.Outlined.PhoneAndroid,
                errorText       = alternatePhoneError,
                imeAction       = ImeAction.Done,
                capitalization  = KeyboardCapitalization.None,
                keyboardType    = KeyboardType.Phone,
                keyboardActions = KeyboardActions(onDone = {
                    if (isFormValid) onSubmit()
                })
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(GreenLight)
                    .padding(11.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(Icons.Default.Info, null, tint = BrandGreen, modifier = Modifier.size(16.dp))
                Text(
                    "Enter registration number exactly as on RC (e.g. DL01AB1234)",
                    fontSize = 12.sp, color = Color(0xFF2D6A3F), lineHeight = 18.sp
                )
            }

            Button(
                onClick  = onSubmit,
                enabled  = isFormValid && !isLoading,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape    = RoundedCornerShape(50),
                colors   = ButtonDefaults.buttonColors(
                    containerColor         = BrandGreen,
                    disabledContainerColor = BrandGreen.copy(alpha = 0.35f)
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                    Spacer(Modifier.width(10.dp))
                    Text("Registering…", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                } else {
                    Text("Register SOS Parking Slot", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// Reusable Input Field
// ─────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Eva2zInputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    icon: ImageVector,
    errorText: String?                     = null,
    imeAction: ImeAction                   = ImeAction.Next,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.Words,
    keyboardType: KeyboardType             = KeyboardType.Text,
    keyboardActions: KeyboardActions       = KeyboardActions.Default
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
            color = TextSecondary, letterSpacing = 0.4.sp
        )
        OutlinedTextField(
            value         = value,
            onValueChange = onValueChange,
            placeholder   = { Text(placeholder, fontSize = 14.sp, color = TextTertiary) },
            leadingIcon = {
                Box(
                    modifier = Modifier.size(32.dp).clip(RoundedCornerShape(9.dp)).background(GreenLight),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, null, tint = BrandGreen, modifier = Modifier.size(16.dp))
                }
            },
            trailingIcon = if (value.isNotBlank()) ({
                Icon(Icons.Default.CheckCircle, null, tint = BrandGreen, modifier = Modifier.size(20.dp))
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
                keyboardType   = keyboardType,
                imeAction      = imeAction
            ),
            keyboardActions = keyboardActions,
            singleLine      = true,
            textStyle       = LocalTextStyle.current.copy(
                fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary
            )
        )
        errorText?.let { err ->
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(Icons.Default.Error, null, tint = ErrorColor, modifier = Modifier.size(13.dp))
                Text(err, fontSize = 12.sp, color = ErrorColor)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// Quick-select chip
// ─────────────────────────────────────────────────────────────────

@Composable
private fun VehicleTypeChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) GreenLight else BackgroundColor)
            .border(1.5.dp, if (selected) BrandGreen else BorderColor, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 7.dp)
    ) {
        Text(
            label, fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color      = if (selected) BrandGreen else TextSecondary
        )
    }
}

// ─────────────────────────────────────────────────────────────────
// Success Banner — ✅ onGoHome added + form hidden
// ─────────────────────────────────────────────────────────────────

@Composable
private fun ParkingSuccessBanner(
    response: ParkingResponse,
    onParkAnother: () -> Unit,
    onGoHome: () -> Unit          // ✅ NEW
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(16.dp),
        colors   = CardDefaults.cardColors(containerColor = SuccessBg),
        border   = BorderStroke(1.dp, GreenMid)
    ) {
        Column(
            modifier            = Modifier.padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ── Success icon + title ──
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFFDCFCE7)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.CheckCircle, null, tint = SuccessText, modifier = Modifier.size(36.dp))
            }

            Text(
                "Vehicle Registered Successfully!",
                fontSize   = 16.sp,
                fontWeight = FontWeight.Bold,
                color      = SuccessText,
                textAlign  = TextAlign.Center
            )

            response.message?.let {
                Text(it, fontSize = 13.sp, color = TextSecondary, textAlign = TextAlign.Center)
            }

            // ── Registration details ──
            response.data?.let { data ->
                Divider(color = GreenMid)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFF0FDF4))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    data.vehicle_type?.let    { BannerRow("Vehicle Type",      it) }
                    data.vehicle_number?.let  { BannerRow("Vehicle Number",    it) }
                    data.phone?.let           { BannerRow("Mobile",            it) }
                    data.alternate_phone?.let { BannerRow("Alternate Mobile",  it) }
                    data.status?.let          { BannerRow("Status",            it) }
                    data.id?.let              { BannerRow("Ticket #",          "#$it") }
                }
            }

            Divider(color = GreenMid)

            // ✅ Back to Homepage button
            Button(
                onClick  = onGoHome,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape    = RoundedCornerShape(50),
                colors   = ButtonDefaults.buttonColors(containerColor = BrandGreen),
                elevation = ButtonDefaults.buttonElevation(4.dp)
            ) {
                Icon(Icons.Default.Home, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Back to Homepage", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }

            // Register Another button
            OutlinedButton(
                onClick  = onParkAnother,
                shape    = RoundedCornerShape(50),
                border   = BorderStroke(1.5.dp, BrandGreen),
                modifier = Modifier.fillMaxWidth().height(46.dp)
            ) {
                Icon(Icons.Default.Add, null, tint = BrandGreen, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Register Another Vehicle", color = BrandGreen, fontWeight = FontWeight.SemiBold)
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
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(14.dp),
        colors   = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
        border   = BorderStroke(1.dp, ErrorColor.copy(alpha = 0.35f))
    ) {
        Row(
            modifier              = Modifier.padding(16.dp),
            verticalAlignment     = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(Icons.Default.ErrorOutline, null, tint = ErrorColor, modifier = Modifier.size(22.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Error", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = ErrorColor)
                Spacer(Modifier.height(2.dp))
                Text(message, fontSize = 13.sp, color = TextSecondary, lineHeight = 18.sp)
            }
            IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Default.Close, null, tint = TextTertiary, modifier = Modifier.size(16.dp))
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// Recent Parkings Card
// ─────────────────────────────────────────────────────────────────

@Composable
private fun RecentParkingsCard(
    history: List<ParkingData>,
    onDelete: (id: Int, name: String) -> Unit
) {
    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(4.dp),
        colors    = CardDefaults.cardColors(containerColor = CardColor)
    ) {
        Column(
            modifier            = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (history.isEmpty()) {
                Row(
                    modifier              = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Icon(Icons.Outlined.DirectionsCar, null, tint = TextTertiary, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("No recent registration", fontSize = 12.sp, color = TextTertiary)
                }
            } else {
                history.take(5).forEachIndexed { i, item ->
                    if (i > 0) Divider(color = BorderColor, thickness = 0.5.dp)
                    ParkingHistoryRow(
                        item     = item,
                        onDelete = {
                            item.id?.let { id -> onDelete(id, item.vehicle_type ?: "Vehicle") }
                        }
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// History Row
// ─────────────────────────────────────────────────────────────────

@Composable
private fun ParkingHistoryRow(
    item: ParkingData,
    onDelete: () -> Unit = {}
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier              = Modifier.fillMaxWidth(),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(GreenLight),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.LocalParking, null, tint = BrandGreen, modifier = Modifier.size(20.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    item.vehicle_type ?: "Unknown",
                    fontSize = 13.sp, fontWeight = FontWeight.Bold,
                    color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                Text(item.vehicle_number ?: "–", fontSize = 11.sp, color = TextSecondary, maxLines = 1)
            }

            // ✅ FIXED — status null ho toh "Registered" + green dikhao
            val isActive     = item.status?.lowercase() == "active"
            val isNullStatus = item.status.isNullOrBlank()

            val statusLabel = when {
                isNullStatus -> "Registered"
                isActive     -> "Active"
                else         -> item.status!!
            }
            val statusBg    = if (isNullStatus || isActive) GreenLight else WarningBg
            val statusColor = if (isNullStatus || isActive) BrandGreen else WarningText

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(statusBg)
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    statusLabel,
                    fontSize   = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color      = statusColor
                )
            }

            // Delete button
            IconButton(
                onClick  = onDelete,
                modifier = Modifier
                    .size(30.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(DeleteRedLight)
            ) {
                Icon(Icons.Default.Delete, null, tint = DeleteRed, modifier = Modifier.size(14.dp))
            }
        }

        Row(
            modifier              = Modifier.fillMaxWidth().padding(start = 50.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                item.phone?.let {
                    Text("📞 $it", fontSize = 11.sp, color = TextTertiary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                item.alternate_phone?.let {
                    Text("📞 $it", fontSize = 11.sp, color = TextTertiary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            Text(
                (item.parked_at ?: item.created_at ?: "–").take(10),
                fontSize = 10.sp, color = TextTertiary, maxLines = 1
            )
        }
    }
}
// ─────────────────────────────────────────────────────────────────
// Banner Row helper
// ─────────────────────────────────────────────────────────────────

@Composable
private fun BannerRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 13.sp, color = TextSecondary)
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
    }
}