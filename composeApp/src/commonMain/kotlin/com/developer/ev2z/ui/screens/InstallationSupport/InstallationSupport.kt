package com.developer.ev2z.ui.screens.InstallationSupport

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.developer.ev2z.navigation.PlatformBackHandler
import com.developer.ev2z.ui.screens.social_media.openUrl
import kotlinx.coroutines.delay
import kotlinx.datetime.*
import kotlin.time.ExperimentalTime

private val BrandGreen = Color(0xFF34A853)
private val TextPrimary = Color(0xFF1A1A1A)
private val TextSecondary = Color(0xFF6B6B6B)
private val BorderDefault = Color(0xFFE0E0E0)
private val FieldBg = Color(0xFFF9FBF9)
private val ErrorRed = Color(0xFFE53935)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalTime::class)
@Composable
fun InstallationSupportScreen(
    onBack: () -> Unit
) {
    PlatformBackHandler(enabled = true) { onBack() }

    val viewModel = remember { InstallationSupportViewModel() }
    val formState by viewModel.state.collectAsState()

    var showForm by remember { mutableStateOf(false) }

    // Form fields
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var selectedProduct by remember { mutableStateOf("") }
    var selectedIssue by remember { mutableStateOf("") }
    var comments by remember { mutableStateOf("") }
    var appointmentDate by remember { mutableStateOf("") }
    var appointmentTime by remember { mutableStateOf("") }

    var nameError by remember { mutableStateOf(false) }
    var phoneError by remember { mutableStateOf(false) }
    var emailError by remember { mutableStateOf(false) }
    var productError by remember { mutableStateOf(false) }
    var issueError by remember { mutableStateOf(false) }

    var productExpanded by remember { mutableStateOf(false) }
    var issueExpanded by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val datePickerState = rememberDatePickerState()
    val timePickerState = rememberTimePickerState(initialHour = 10, initialMinute = 0, is24Hour = false)

    val products = listOf("GPS Tracker", "Parking TAG", "Other")
    val issueTypes = listOf(
        "Installation Help", "Device Not Working", "App Issue",
        "GPS Not Tracking", "Billing / Payment", "Other"
    )

    val scrollState = rememberScrollState()

    // Auto-scroll to form when it opens
    LaunchedEffect(showForm) {
        if (showForm) scrollState.animateScrollTo(scrollState.maxValue)
    }

    // Navigate to success screen when API call succeeds
    if (formState.isSuccess) {
        SupportSuccessScreen(onDone = onBack)
        return
    }

    // ── Date picker dialog ────────────────────────────────────────────────────
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val date = Instant.fromEpochMilliseconds(millis)
                            .toLocalDateTime(TimeZone.currentSystemDefault()).date
                        val d = date.dayOfMonth.toString().padStart(2, '0')
                        val m = date.monthNumber.toString().padStart(2, '0')
                        appointmentDate = "$d/$m/${date.year}"
                    }
                    showDatePicker = false
                }) { Text("OK", color = BrandGreen, fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel", color = TextSecondary) }
            },
            colors = DatePickerDefaults.colors(
                selectedDayContainerColor = BrandGreen,
                selectedDayContentColor = Color.White,
                todayDateBorderColor = BrandGreen,
                todayContentColor = BrandGreen
            )
        ) { DatePicker(state = datePickerState) }
    }

    // ── Time picker dialog ────────────────────────────────────────────────────
    if (showTimePicker) {
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val h = timePickerState.hour
                    val min = timePickerState.minute
                    val period = if (h < 12) "AM" else "PM"
                    val displayH = when { h == 0 -> 12; h > 12 -> h - 12; else -> h }
                    appointmentTime = "${displayH.toString().padStart(2, '0')}:${min.toString().padStart(2, '0')} $period"
                    showTimePicker = false
                }) { Text("OK", color = BrandGreen, fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("Cancel", color = TextSecondary) }
            },
            title = {
                Text("Select Appointment Time", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            },
            text = {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    TimePicker(
                        state = timePickerState,
                        colors = TimePickerDefaults.colors(
                            clockDialColor = Color(0xFFF0F7F2),
                            selectorColor = BrandGreen,
                            timeSelectorSelectedContainerColor = BrandGreen,
                            timeSelectorSelectedContentColor = Color.White,
                            timeSelectorUnselectedContainerColor = Color(0xFFF0F7F2),
                            timeSelectorUnselectedContentColor = TextPrimary
                        )
                    )
                }
            },
            shape = RoundedCornerShape(20.dp),
            containerColor = Color.White
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Installation Support", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text("GPS device setup guide", fontSize = 11.sp, color = TextSecondary)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color(0xFFF5F6FA)
    ) { padding ->

        Box(modifier = Modifier.fillMaxSize()) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // ── Installation Video Guides ─────────────────────────────────
            SupportFormCard {
                SupportSectionTitle(icon = Icons.Default.PlayCircle, title = "Installation Video Guides")
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Watch step-by-step installation guide for your vehicle",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    modifier = Modifier.padding(start = 2.dp)
                )
                Spacer(Modifier.height(16.dp))

                VideoGuideRow(
                    icon = Icons.Default.TwoWheeler,
                    title = "EV 2 Wheeler",
                    subtitle = "Electric 2W Vehicle",
                    onClick = { openUrl("https://youtu.be/sYjUGsugOtY?si=aP8lZJ4Lg5RPeo3f") }
                )
                Spacer(Modifier.height(12.dp))
                VideoGuideRow(
                    icon = Icons.Default.DirectionsCar,
                    title = "4 Wheeler Vehicle",
                    subtitle = "Car, SUV, Truck, Bus",
                    onClick = { openUrl("https://youtu.be/KrQRf6XZ-Ok?si=iODpWPEVbBtl1Ev5") }
                )
            }

            // ── Still facing issue? Banner ────────────────────────────────
            if (!showForm) {
                Card(
                    onClick = { showForm = true },
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color(0xFF2E7D32), Color(0xFF43A047))
                                )
                            )
                            .padding(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.SupportAgent,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = "Still facing the issue?",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = "Submit a support request and our team will assist you",
                                    fontSize = 12.sp,
                                    color = Color.White.copy(alpha = 0.85f),
                                    lineHeight = 18.sp
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowForward,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }
            }

            // ── Support Form (animated in) ────────────────────────────────
            AnimatedVisibility(
                visible = showForm,
                enter = fadeIn(tween(300)) + expandVertically(tween(400)),
                exit = fadeOut(tween(200)) + shrinkVertically(tween(300))
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {

                    // Form header with dismiss option
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.SupportAgent,
                                contentDescription = null,
                                tint = BrandGreen,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Support Request Form",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                        }
                        TextButton(
                            onClick = { showForm = false },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Close",
                                tint = TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("Hide", fontSize = 13.sp, color = TextSecondary)
                        }
                    }

                    // ── Personal Information ──────────────────────────────
                    SupportFormCard {
                        SupportSectionTitle(icon = Icons.Default.Person, title = "Personal Information")
                        Spacer(Modifier.height(18.dp))

                        SupportTextField(
                            label = "Full Name",
                            value = name,
                            onValueChange = { name = it; nameError = false },
                            placeholder = "Enter your full name",
                            leadingIcon = Icons.Default.Person,
                            isError = nameError,
                            errorText = "Name is required"
                        )
                        Spacer(Modifier.height(14.dp))
                        SupportTextField(
                            label = "Phone Number",
                            value = phone,
                            onValueChange = { if (it.length <= 15) { phone = it; phoneError = false } },
                            placeholder = "Enter 10-digit phone number",
                            leadingIcon = Icons.Default.Phone,
                            keyboardType = KeyboardType.Phone,
                            isError = phoneError,
                            errorText = "Enter a valid phone number"
                        )
                        Spacer(Modifier.height(14.dp))
                        SupportTextField(
                            label = "Email ID",
                            value = email,
                            onValueChange = { email = it; emailError = false },
                            placeholder = "Enter your email address",
                            leadingIcon = Icons.Default.MarkEmailRead,
                            keyboardType = KeyboardType.Email,
                            isError = emailError,
                            errorText = "Enter a valid email address"
                        )
                    }

                    // ── Issue Details ─────────────────────────────────────
                    SupportFormCard {
                        SupportSectionTitle(icon = Icons.Default.Build, title = "Issue Details")
                        Spacer(Modifier.height(18.dp))

                        SupportDropdown(
                            label = "Product",
                            value = selectedProduct,
                            placeholder = "Select your product",
                            options = products,
                            expanded = productExpanded,
                            onExpandedChange = { productExpanded = it },
                            onSelect = { selectedProduct = it; productExpanded = false; productError = false },
                            leadingIcon = Icons.Default.ShoppingBag,
                            isError = productError,
                            errorText = "Please select a product"
                        )
                        Spacer(Modifier.height(14.dp))
                        SupportDropdown(
                            label = "Issue / Support Type",
                            value = selectedIssue,
                            placeholder = "Select type of support",
                            options = issueTypes,
                            expanded = issueExpanded,
                            onExpandedChange = { issueExpanded = it },
                            onSelect = { selectedIssue = it; issueExpanded = false; issueError = false },
                            leadingIcon = Icons.Default.Warning,
                            isError = issueError,
                            errorText = "Please select an issue type"
                        )
                        Spacer(Modifier.height(14.dp))

                        Column(modifier = Modifier.fillMaxWidth()) {
                            SupportFieldLabel(label = "Comments", required = false)
                            Spacer(Modifier.height(6.dp))
                            OutlinedTextField(
                                value = comments,
                                onValueChange = { comments = it },
                                modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 120.dp),
                                placeholder = {
                                    Text("Describe your issue in detail...", color = Color(0xFFBBBBBB), fontSize = 14.sp)
                                },
                                maxLines = 8,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = BrandGreen,
                                    unfocusedBorderColor = BorderDefault,
                                    focusedContainerColor = FieldBg,
                                    unfocusedContainerColor = FieldBg,
                                    cursorColor = BrandGreen
                                ),
                                shape = RoundedCornerShape(14.dp)
                            )
                        }
                    }

                    // ── Appointment Date & Time ────────────────────────────
                    SupportFormCard {
                        SupportSectionTitle(icon = Icons.Default.Schedule, title = "Appointment Date & Time")
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Optional — book a slot for on-site assistance",
                            fontSize = 12.sp,
                            color = TextSecondary,
                            modifier = Modifier.padding(start = 2.dp)
                        )
                        Spacer(Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                SupportFieldLabel(label = "Date", required = false)
                                Spacer(Modifier.height(6.dp))
                                OutlinedTextField(
                                    value = appointmentDate,
                                    onValueChange = {},
                                    readOnly = true,
                                    modifier = Modifier.fillMaxWidth().clickable { showDatePicker = true },
                                    placeholder = { Text("DD/MM/YYYY", color = Color(0xFFBBBBBB), fontSize = 13.sp) },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.CalendarMonth, null,
                                            tint = if (appointmentDate.isNotEmpty()) BrandGreen else Color(0xFFCCCCCC),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    },
                                    trailingIcon = if (appointmentDate.isNotEmpty()) {
                                        { IconButton(onClick = { appointmentDate = "" }) {
                                            Icon(Icons.Default.Close, null, tint = Color(0xFFAAAAAA), modifier = Modifier.size(16.dp))
                                        }}
                                    } else null,
                                    singleLine = true,
                                    enabled = false,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        disabledBorderColor = if (appointmentDate.isNotEmpty()) BrandGreen else BorderDefault,
                                        disabledContainerColor = FieldBg,
                                        disabledTextColor = TextPrimary,
                                        disabledLeadingIconColor = if (appointmentDate.isNotEmpty()) BrandGreen else Color(0xFFCCCCCC)
                                    ),
                                    shape = RoundedCornerShape(14.dp)
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                SupportFieldLabel(label = "Time", required = false)
                                Spacer(Modifier.height(6.dp))
                                OutlinedTextField(
                                    value = appointmentTime,
                                    onValueChange = {},
                                    readOnly = true,
                                    modifier = Modifier.fillMaxWidth().clickable { showTimePicker = true },
                                    placeholder = { Text("HH:MM AM", color = Color(0xFFBBBBBB), fontSize = 13.sp) },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.AccessTime, null,
                                            tint = if (appointmentTime.isNotEmpty()) BrandGreen else Color(0xFFCCCCCC),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    },
                                    trailingIcon = if (appointmentTime.isNotEmpty()) {
                                        { IconButton(onClick = { appointmentTime = "" }) {
                                            Icon(Icons.Default.Close, null, tint = Color(0xFFAAAAAA), modifier = Modifier.size(16.dp))
                                        }}
                                    } else null,
                                    singleLine = true,
                                    enabled = false,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        disabledBorderColor = if (appointmentTime.isNotEmpty()) BrandGreen else BorderDefault,
                                        disabledContainerColor = FieldBg,
                                        disabledTextColor = TextPrimary,
                                        disabledLeadingIconColor = if (appointmentTime.isNotEmpty()) BrandGreen else Color(0xFFCCCCCC)
                                    ),
                                    shape = RoundedCornerShape(14.dp)
                                )
                            }
                        }
                    }

                    // ── Submit Button ─────────────────────────────────────
                    Button(
                        onClick = {
                            var valid = true
                            if (name.isBlank()) { nameError = true; valid = false }
                            if (phone.isBlank() || phone.length < 10) { phoneError = true; valid = false }
                            if (email.isBlank() || !isSupportEmailValid(email)) { emailError = true; valid = false }
                            if (selectedProduct.isBlank()) { productError = true; valid = false }
                            if (selectedIssue.isBlank()) { issueError = true; valid = false }
                            if (valid) {
                                viewModel.submitSupport(
                                    name = name,
                                    phone = phone,
                                    email = email,
                                    product = selectedProduct,
                                    issueType = selectedIssue,
                                    comment = comments,
                                    appointmentDate = appointmentDate,
                                    appointmentTime = appointmentTime
                                )
                            }
                        },
                        enabled = !formState.isLoading,
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BrandGreen,
                            contentColor = Color.White,
                            disabledContainerColor = BrandGreen.copy(alpha = 0.6f),
                            disabledContentColor = Color.White
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp)
                    ) {
                        if (formState.isLoading) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(22.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Send, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Submit Request", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
        } // end Column

        // ── Error toast ───────────────────────────────────────────────────
        formState.error?.let { errorMsg ->
            LaunchedEffect(errorMsg) {
                delay(3500)
                viewModel.clearError()
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .padding(padding),
                contentAlignment = Alignment.BottomCenter
            ) {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFB71C1C)),
                    elevation = CardDefaults.cardElevation(6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Error, null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Text(errorMsg, color = Color.White, fontSize = 14.sp)
                    }
                }
            }
        }

        } // end Box
    }
}

// ── Video guide row ───────────────────────────────────────────────────────────

@Composable
private fun VideoGuideRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F6FA)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .background(BrandGreen.copy(alpha = 0.12f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = BrandGreen, modifier = Modifier.size(26.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Text(subtitle, fontSize = 12.sp, color = TextSecondary)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(Icons.Default.PlayCircle, null, tint = BrandGreen, modifier = Modifier.size(16.dp))
                Text("Watch", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = BrandGreen)
            }
        }
    }
}

// ── Reusable card wrapper ─────────────────────────────────────────────────────

@Composable
private fun SupportFormCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp), content = content)
    }
}

// ── Section title ─────────────────────────────────────────────────────────────

@Composable
private fun SupportSectionTitle(icon: ImageVector, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .background(BrandGreen.copy(alpha = 0.12f), RoundedCornerShape(11.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = BrandGreen, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(10.dp))
        Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
    }
}

// ── Field label ───────────────────────────────────────────────────────────────

@Composable
private fun SupportFieldLabel(label: String, required: Boolean = true) {
    Row(modifier = Modifier.padding(start = 2.dp)) {
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color(0xFF444444))
        if (required) Text(" *", fontSize = 13.sp, color = ErrorRed, fontWeight = FontWeight.Medium)
    }
}

// ── Text field ────────────────────────────────────────────────────────────────

@Composable
private fun SupportTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    leadingIcon: ImageVector,
    keyboardType: KeyboardType = KeyboardType.Text,
    isError: Boolean = false,
    errorText: String = ""
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        SupportFieldLabel(label = label)
        Spacer(Modifier.height(6.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(placeholder, color = Color(0xFFBBBBBB), fontSize = 14.sp) },
            leadingIcon = {
                Icon(
                    leadingIcon, null,
                    tint = when { isError -> ErrorRed; value.isNotEmpty() -> BrandGreen; else -> Color(0xFFCCCCCC) },
                    modifier = Modifier.size(20.dp)
                )
            },
            singleLine = true,
            isError = isError,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = BrandGreen,
                unfocusedBorderColor = BorderDefault,
                errorBorderColor = ErrorRed,
                focusedContainerColor = FieldBg,
                unfocusedContainerColor = FieldBg,
                errorContainerColor = Color(0xFFFFF0F0),
                cursorColor = BrandGreen
            ),
            shape = RoundedCornerShape(14.dp)
        )
        if (isError) {
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 4.dp)) {
                Icon(Icons.Default.Info, null, tint = ErrorRed, modifier = Modifier.size(12.dp))
                Spacer(Modifier.width(4.dp))
                Text(errorText, color = ErrorRed, fontSize = 11.sp)
            }
        }
    }
}

// ── Dropdown ──────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SupportDropdown(
    label: String,
    value: String,
    placeholder: String,
    options: List<String>,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onSelect: (String) -> Unit,
    leadingIcon: ImageVector,
    isError: Boolean = false,
    errorText: String = ""
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        SupportFieldLabel(label = label)
        Spacer(Modifier.height(6.dp))
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = onExpandedChange,
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = {},
                readOnly = true,
                modifier = Modifier.fillMaxWidth().menuAnchor(),
                placeholder = { Text(placeholder, color = Color(0xFFBBBBBB), fontSize = 14.sp) },
                leadingIcon = {
                    Icon(
                        leadingIcon, null,
                        tint = when { isError -> ErrorRed; value.isNotEmpty() -> BrandGreen; else -> Color(0xFFCCCCCC) },
                        modifier = Modifier.size(20.dp)
                    )
                },
                trailingIcon = {
                    Icon(
                        if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        null,
                        tint = if (value.isNotEmpty()) BrandGreen else Color(0xFFAAAAAA),
                        modifier = Modifier.size(22.dp)
                    )
                },
                singleLine = true,
                isError = isError,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BrandGreen,
                    unfocusedBorderColor = BorderDefault,
                    errorBorderColor = ErrorRed,
                    focusedContainerColor = FieldBg,
                    unfocusedContainerColor = FieldBg,
                    errorContainerColor = Color(0xFFFFF0F0),
                    cursorColor = BrandGreen
                ),
                shape = RoundedCornerShape(14.dp)
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { onExpandedChange(false) },
                modifier = Modifier.background(Color.White)
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                option,
                                fontSize = 14.sp,
                                color = if (value == option) BrandGreen else TextPrimary,
                                fontWeight = if (value == option) FontWeight.SemiBold else FontWeight.Normal
                            )
                        },
                        onClick = { onSelect(option) },
                        leadingIcon = {
                            if (value == option) Icon(Icons.Default.Check, null, tint = BrandGreen, modifier = Modifier.size(16.dp))
                            else Spacer(Modifier.size(16.dp))
                        }
                    )
                }
            }
        }
        if (isError) {
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 4.dp)) {
                Icon(Icons.Default.Info, null, tint = ErrorRed, modifier = Modifier.size(12.dp))
                Spacer(Modifier.width(4.dp))
                Text(errorText, color = ErrorRed, fontSize = 11.sp)
            }
        }
    }
}

// ── Success screen ────────────────────────────────────────────────────────────

@Composable
private fun SupportSuccessScreen(onDone: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(horizontal = 40.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .background(BrandGreen.copy(alpha = 0.10f), RoundedCornerShape(48.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.CheckCircle, null, tint = BrandGreen, modifier = Modifier.size(56.dp))
            }
            Text("Request Submitted!", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Text(
                "Our support team will review your request and get back to you within 24 hours.",
                fontSize = 14.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = onDone,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandGreen, contentColor = Color.White)
            ) {
                Text("Back to Home", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun isSupportEmailValid(email: String): Boolean =
    Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$").matches(email)