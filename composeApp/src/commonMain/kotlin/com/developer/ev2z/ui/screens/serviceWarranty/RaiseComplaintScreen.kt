package com.developer.ev2z.ui.screens.serviceWarranty

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.developer.ev2z.navigation.PlatformBackHandler
import com.developer.ev2z.phonepe.openPhonePeSdk
import com.developer.ev2z.util.rememberImagePicker
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * SCREEN 2 — Raise Complaint (FRD §3 / §4).
 * Vehicle → component → description → invoice → optional media → submit.
 *
 * Attachments reuse the app's existing [rememberImagePicker] (camera or gallery)
 * rather than introducing a new expect/actual picker, so no new platform code is
 * added. That limits invoices to image formats — PDF upload needs a document
 * picker and is the one FRD item not covered here.
 */
@OptIn(ExperimentalTime::class)
@Composable
fun RaiseComplaintScreen(
    viewModel: RaiseComplaintViewModel,
    onBack: () -> Unit,
    onComplaintCreated: (ticketId: String) -> Unit,
    /** Paid, but the server would not file the ticket — hand over to the list. */
    onPaidWithoutTicket: () -> Unit = onBack,
) {
    val state by viewModel.state.collectAsState()
    val snackbarHost = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showSuccessFor by remember { mutableStateOf<String?>(null) }

    PlatformBackHandler(true) { onBack() }

    val invoicePicker = rememberImagePicker { bytes ->
        if (bytes != null) {
            viewModel.attachInvoice(
                ServiceAttachment(
                    id = "inv-${Clock.System.now().toEpochMilliseconds()}",
                    name = "invoice.jpg",
                    sizeBytes = bytes.size.toLong(),
                    kind = AttachmentKind.INVOICE,
                    bytes = bytes,
                )
            )
        }
    }

    val mediaPicker = rememberImagePicker { bytes ->
        if (bytes != null) {
            val index = state.draft.media.size + 1
            viewModel.attachMedia(
                ServiceAttachment(
                    id = "media-${Clock.System.now().toEpochMilliseconds()}",
                    name = "photo_$index.jpg",
                    sizeBytes = bytes.size.toLong(),
                    kind = AttachmentKind.PHOTO,
                    bytes = bytes,
                )
            )
        }
    }

    // A non-EVA2Z customer pays the service charge before the complaint is filed.
    // The order is consumed immediately so a recomposition can't open a second
    // checkout for money that is already being collected.
    LaunchedEffect(state.pendingPayment) {
        val order = state.pendingPayment ?: return@LaunchedEffect
        viewModel.paymentLaunched()
        openPhonePeSdk(order.token, order.phonepeOrderId) { result ->
            viewModel.onPaymentResult(result.success, result.transactionId, result.message)
        }
    }

    LaunchedEffect(state.submittedTicketId) {
        state.submittedTicketId?.let { showSuccessFor = it }
    }
    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            scope.launch { snackbarHost.showSnackbar(it) }
            viewModel.dismissError()
        }
    }

    // The chrome is laid out here instead of through ServiceScaffold: this form is
    // the only screen in the flow with a pinned footer, and leaning on Scaffold's
    // padding maths left the last card sitting underneath the submit bar on iOS.
    // A plain Column hands the list exactly `height - topBar - footer` instead.
    BoxWithConstraints(
        Modifier.fillMaxSize().background(ServiceColors.PageBg),
    ) {
        // Sizing follows the viewport rather than fixed dp: an iPhone SE gets
        // tighter gutters and a shorter description box, larger phones get roomier
        // ones, and the form stops widening past a readable measure on a tablet.
        val compact = maxHeight < 700.dp
        val gutter = (maxWidth * 0.045f).coerceIn(12.dp, 22.dp)
        val cardGap = if (compact) 10.dp else 14.dp

        Column(
            Modifier
                .fillMaxHeight()
                .widthIn(max = 560.dp)
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .statusBarsPadding(),
        ) {

            RaiseComplaintTopBar(onBack = onBack, gutter = gutter)

            if (state.isLoading) {
                Box(
                    Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center,
                ) { LoadingState(label = "Loading vehicles and components…") }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentPadding = PaddingValues(
                        start = gutter,
                        end = gutter,
                        top = 8.dp,
                        bottom = 16.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(cardGap),
                ) {
                    /* ──────────────────── 1. customer type ─────────────────── */
                    item(key = "customer-type") {
                        ServiceCard(title = "1. Are you an EVA2Z Connect customer?") {
                            CustomerTypeDropdown(
                                selected = state.draft.customerType,
                                isError = state.showValidation && state.draft.customerType == null,
                                // Switching after paying would misrepresent the charge, so the
                                // choice is frozen once money has changed hands.
                                enabled = state.paidReceipt == null && !state.isBusy,
                                onSelect = viewModel::selectCustomerType,
                            )
                            // Nothing is said about money until the server has
                            // actually charged for it — a paid receipt is the one
                            // fact the app knows for certain.
                            AnimatedVisibility(state.paidReceipt != null) {
                                Column {
                                    Spacer(Modifier.height(12.dp))
                                    state.paidReceipt?.let { receipt ->
                                        InlineBanner(
                                            text = "₹${receipt.amountRupees} service charge paid. " +
                                                "Submit your complaint to finish.",
                                            tone = BannerTone.SUCCESS,
                                            icon = Icons.Filled.Verified,
                                        )
                                    }
                                }
                            }
                        }
                    }

                    /* ────────────────────── 2. vehicle ────────────────────── */
                    item(key = "vehicle") {
                        // "EV Scooty" is always the answer, so the field shows it filled in
                        // whatever the device list did — empty, failed, or several entries.
                        // Only the registration behind it is ever up for choosing.
                        val multipleVehicles = state.vehicles.size > 1
                        ServiceCard(title = "2. Vehicle") {
                            PreselectedVehicleRow(
                                vehicle = state.draft.vehicle,
                                expandable = multipleVehicles,
                                vehicles = state.vehicles,
                                onSelect = viewModel::selectVehicle,
                            )
                            AnimatedVisibility(state.draft.vehicle != null) {
                                Column {
                                    Spacer(Modifier.height(12.dp))
                                    Box(
                                        Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color(0xFFF8FAFC))
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                    ) {
                                        Column {
                                            state.draft.vehicle?.let { v ->
                                                LabelValueRow("Vehicle type", SERVICE_VEHICLE_TYPE)
                                                LabelValueRow("Registration No.", v.registrationNumber)
                                                LabelValueRow("Model", v.model)
                                                LabelValueRow("VIN / Chassis", v.vin)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    /* ───────────────────── 2. component ───────────────────── */
                    item(key = "component") {
                        ServiceCard(title = "3. Affected component") {
                            ComponentChipGrid(
                                components = state.components,
                                selected = state.draft.component,
                                onSelect = viewModel::selectComponent,
                            )
                        }
                    }

                    /* ──────────────────── 3. description ──────────────────── */
                    item(key = "issue") {
                        ServiceCard(title = "4. Describe the issue") {
                            val missing = state.showValidation && state.draft.issueDescription.isBlank()
                            OutlinedTextField(
                                value = state.draft.issueDescription,
                                onValueChange = viewModel::updateDescription,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = if (compact) 92.dp else 112.dp),
                                placeholder = {
                                    Text(
                                        "What is happening, when did it start, and how often?",
                                        fontSize = 13.sp,
                                        color = ServiceColors.TextMuted,
                                    )
                                },
                                supportingText = {
                                    Text(
                                        "${state.draft.issueDescription.length} / ${ComplaintDraft.MAX_DESCRIPTION}",
                                        fontSize = 11.sp,
                                        color = ServiceColors.TextMuted,
                                    )
                                },
                                isError = missing,
                                shape = RoundedCornerShape(12.dp),
                                textStyle = androidx.compose.ui.text.TextStyle(
                                    fontSize = 14.sp,
                                    color = ServiceColors.TextPrimary,
                                ),
                                colors = serviceFieldColors(),
                                keyboardOptions = KeyboardOptions(
                                    capitalization = KeyboardCapitalization.Sentences,
                                    imeAction = ImeAction.Default,
                                ),
                            )
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(
                                value = state.draft.errorCode,
                                onValueChange = viewModel::updateErrorCode,
                                modifier = Modifier.fillMaxWidth(),
                                label = {
                                    Text("Error code on display (optional)", fontSize = 12.sp)
                                },
                                placeholder = { Text("e.g. E-1042", fontSize = 13.sp, color = ServiceColors.TextMuted) },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                textStyle = androidx.compose.ui.text.TextStyle(
                                    fontSize = 14.sp,
                                    color = ServiceColors.TextPrimary,
                                ),
                                colors = serviceFieldColors(),
                                keyboardOptions = KeyboardOptions(
                                    capitalization = KeyboardCapitalization.Characters,
                                    imeAction = ImeAction.Done,
                                ),
                            )
                        }
                    }

                    /* ─────────────────────── 4. invoice ───────────────────── */
                    item(key = "invoice") {
                        ServiceCard(
                            title = if (state.draft.invoiceRequired) "5. Upload invoice (recommended)"
                            else "5. Upload invoice (optional)",
                        ) {
                            if (state.draft.invoiceRequired) {
                                InlineBanner(
                                    text = "Attach the invoice to get warranty validated for " +
                                        "${state.draft.component?.label ?: "this component"} — " +
                                        "you can still submit without it.",
                                    tone = BannerTone.INFO,
                                    icon = Icons.Filled.ErrorOutline,
                                )
                                Spacer(Modifier.height(12.dp))
                            }

                            val invoice = state.draft.invoice
                            if (invoice == null) {
                                DashedDropZone(
                                    label = "Upload purchase invoice",
                                    hint = "Photo or gallery image · up to 15 MB",
                                    icon = Icons.Filled.UploadFile,
                                    // Recommended, not required — warranty review may stall
                                    // without it, but that is the server's call to make.
                                    isError = false,
                                    onClick = { invoicePicker.launch() },
                                )
                            } else {
                                AttachmentRow(attachment = invoice, onRemove = viewModel::removeInvoice)
                            }
                        }
                    }

                    /* ──────────────────────── 5. media ────────────────────── */
                    item(key = "media") {
                        ServiceCard(title = "6. Photos of the problem (optional)") {
                            Text(
                                "A clear photo helps our technician diagnose it faster.",
                                fontSize = 12.sp,
                                color = ServiceColors.TextSecondary,
                                lineHeight = 17.sp,
                            )
                            Spacer(Modifier.height(12.dp))
                            state.draft.media.forEach { media ->
                                AttachmentRow(
                                    attachment = media,
                                    onRemove = { viewModel.removeMedia(media.id) },
                                    modifier = Modifier.padding(bottom = 8.dp),
                                )
                            }
                            if (state.draft.media.size < ComplaintDraft.MAX_MEDIA) {
                                OutlinedButton(
                                    onClick = { mediaPicker.launch() },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = ServiceColors.Brand,
                                    ),
                                ) {
                                    Icon(
                                        Icons.Filled.AddPhotoAlternate,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text("Add photo", fontSize = 14.sp)
                                }
                            }
                        }
                    }
                }

                SubmitBar(
                    inlineError = state.inlineError,
                    hint = state.pendingRequirement,
                    paidReceipt = state.paidReceipt,
                    isBusy = state.isBusy,
                    enabled = state.canSubmit,
                    label = state.submitLabel,
                    gutter = gutter,
                    compact = compact,
                    onSubmit = viewModel::submit,
                )
            }
        }

        // Floated over the footer so an error message never covers the button.
        SnackbarHost(
            snackbarHost,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = gutter, vertical = 12.dp),
        ) { data ->
            Snackbar(
                containerColor = ServiceColors.TextPrimary,
                contentColor = Color.White,
                shape = RoundedCornerShape(12.dp),
            ) { Text(data.visuals.message, fontSize = 13.sp) }
        }
    }

    /* ─────────────────────────── success dialog ─────────────────────────── */
    showSuccessFor?.let { ticketId ->
        AlertDialog(
            onDismissRequest = { },
            containerColor = ServiceColors.CardBg,
            shape = RoundedCornerShape(20.dp),
            icon = {
                Icon(
                    Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = ServiceColors.Brand,
                    modifier = Modifier.size(40.dp),
                )
            },
            title = {
                Text(
                    "Complaint submitted",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = ServiceColors.TextPrimary,
                )
            },
            text = {
                Column {
                    Text(
                        "Your service request has been created. Save this ticket ID for reference.",
                        fontSize = 13.sp,
                        color = ServiceColors.TextSecondary,
                        lineHeight = 19.sp,
                    )
                    Spacer(Modifier.height(12.dp))
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(ServiceColors.BrandSoft)
                            .padding(12.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            ticketId,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = ServiceColors.Brand,
                        )
                    }
                    state.paidReceipt?.let { receipt ->
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Service charge of ₹${receipt.amountRupees} paid · " +
                                "Ref ${receipt.merchantOrderId}",
                            fontSize = 12.sp,
                            color = ServiceColors.Brand,
                            lineHeight = 17.sp,
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "We'll notify you once the invoice is verified and a technician is assigned.",
                        fontSize = 12.sp,
                        color = ServiceColors.TextMuted,
                        lineHeight = 17.sp,
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSuccessFor = null
                        viewModel.consumeSubmission()
                        onComplaintCreated(ticketId)
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ServiceColors.Brand,
                        contentColor = Color.White,
                    ),
                ) { Text("Track complaint", fontSize = 14.sp) }
            },
            dismissButton = {
                TextButton(onClick = {
                    showSuccessFor = null
                    viewModel.consumeSubmission()
                    onBack()
                }) { Text("Done", fontSize = 14.sp, color = ServiceColors.TextSecondary) }
            },
        )
    }

    /* ──────────────────── paid, but the ticket isn't there ──────────────── */
    // Same hand-off as a successful submit — form cleared, on to My Complaints —
    // but it cannot show a ticket number, because the server did not create one.
    // Saying "submitted" here would be a lie the user only discovers on the list.
    state.paidWithoutTicket?.let { receipt ->
        AlertDialog(
            onDismissRequest = { },
            containerColor = ServiceColors.CardBg,
            shape = RoundedCornerShape(20.dp),
            icon = {
                Icon(
                    Icons.Filled.Verified,
                    contentDescription = null,
                    tint = ServiceColors.Brand,
                    modifier = Modifier.size(40.dp),
                )
            },
            title = {
                Text(
                    "Payment received",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = ServiceColors.TextPrimary,
                )
            },
            text = {
                Column {
                    Text(
                        "₹${receipt.amountRupees} has been paid for this service request. " +
                            "We're still confirming it with our servers, so the ticket may " +
                            "take a few minutes to appear under My Complaints.",
                        fontSize = 13.sp,
                        color = ServiceColors.TextSecondary,
                        lineHeight = 19.sp,
                    )
                    Spacer(Modifier.height(12.dp))
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(ServiceColors.BrandSoft)
                            .padding(12.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            receipt.merchantOrderId,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = ServiceColors.Brand,
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Keep this payment reference. Please don't pay again — if the " +
                            "complaint hasn't appeared shortly, share this reference with support.",
                        fontSize = 12.sp,
                        color = ServiceColors.TextMuted,
                        lineHeight = 17.sp,
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.consumePaidWithoutTicket()
                        onPaidWithoutTicket()
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ServiceColors.Brand,
                        contentColor = Color.White,
                    ),
                ) { Text("View my complaints", fontSize = 14.sp) }
            },
            dismissButton = {
                TextButton(onClick = {
                    viewModel.consumePaidWithoutTicket()
                    onBack()
                }) { Text("Done", fontSize = 14.sp, color = ServiceColors.TextSecondary) }
            },
        )
    }
}

/* ───────────────────────────── private pieces ──────────────────────────── */

/**
 * Matches the flat chevron-left header the rest of the flow gets from
 * [ServiceScaffold], but as a plain row this screen lays out itself — the status
 * bar inset is applied by the caller, which is the convention every
 * iOS-verified screen in this app follows.
 */
@Composable
private fun RaiseComplaintTopBar(onBack: () -> Unit, gutter: androidx.compose.ui.unit.Dp) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(ServiceColors.PageBg)
            .height(56.dp)
            .padding(end = gutter),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = "Back",
                tint = ServiceColors.TextPrimary,
            )
        }
        Text(
            "Raise Complaint",
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = ServiceColors.TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * The pinned footer: inline error, the charge being collected, and submit.
 *
 * It applies its own navigation-bar and IME insets — the home indicator on iOS
 * and the gesture bar on Android stay clear of the button, and the whole bar
 * rides above the keyboard while a field is focused. Because it is a sibling of
 * the list rather than an overlay, the space it takes is space the list never
 * tries to draw into.
 */
@Composable
private fun SubmitBar(
    inlineError: String?,
    hint: String?,
    paidReceipt: ServicePaymentReceipt?,
    isBusy: Boolean,
    enabled: Boolean,
    label: String,
    gutter: androidx.compose.ui.unit.Dp,
    compact: Boolean,
    onSubmit: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = ServiceColors.CardBg,
        shadowElevation = 8.dp,
    ) {
        Column(
            Modifier
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = gutter, vertical = if (compact) 10.dp else 16.dp),
        ) {
            // Red once a submit has been refused, muted grey while the form is
            // simply not finished yet — either way it names the missing field.
            (inlineError ?: hint)?.let {
                Text(
                    it,
                    fontSize = 12.sp,
                    color = if (inlineError != null) ServiceColors.Danger else ServiceColors.TextMuted,
                )
                Spacer(Modifier.height(8.dp))
            }

            // The only charge ever stated here is one the server has already
            // collected — what a complaint costs is its decision, made when the
            // form is submitted, and it arrives in the button's own label.
            paidReceipt?.let { receipt ->
                Row(
                    Modifier.fillMaxWidth().padding(bottom = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "Service charge",
                        fontSize = 12.sp,
                        color = ServiceColors.TextSecondary,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        "₹${receipt.amountRupees} paid",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = ServiceColors.Brand,
                    )
                }
            }

            Button(
                onClick = onSubmit,
                enabled = enabled,
                modifier = Modifier.fillMaxWidth().height(if (compact) 46.dp else 50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ServiceColors.Brand,
                    contentColor = Color.White,
                    disabledContainerColor = ServiceColors.BorderSoft,
                    disabledContentColor = ServiceColors.TextMuted,
                ),
            ) {
                if (isBusy) {
                    CircularProgressIndicator(
                        Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = Color.White,
                    )
                    Spacer(Modifier.width(10.dp))
                }
                Text(label, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun serviceFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = ServiceColors.Brand,
    unfocusedBorderColor = ServiceColors.BorderSoft,
    errorBorderColor = ServiceColors.Danger,
    cursorColor = ServiceColors.Brand,
    focusedLabelColor = ServiceColors.Brand,
    unfocusedLabelColor = ServiceColors.TextSecondary,
    focusedContainerColor = Color.White,
    unfocusedContainerColor = Color.White,
)

/**
 * Decides the price of the whole flow, so it sits above everything else and is the
 * first thing [ComplaintDraft.validationError] complains about.
 */
@Composable
private fun CustomerTypeDropdown(
    selected: ServiceCustomerType?,
    isError: Boolean,
    enabled: Boolean,
    onSelect: (ServiceCustomerType) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = enabled) { expanded = true },
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(0.dp),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (isError) ServiceColors.Danger else ServiceColors.BorderSoft,
            ),
        ) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Filled.Badge,
                    contentDescription = null,
                    tint = if (selected != null) ServiceColors.Brand else ServiceColors.TextSecondary,
                    modifier = Modifier.size(19.dp),
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    selected?.label ?: "Select an option",
                    fontSize = 14.sp,
                    color = if (selected != null) ServiceColors.TextPrimary else ServiceColors.TextMuted,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (enabled) {
                    Icon(
                        Icons.Filled.KeyboardArrowDown,
                        contentDescription = null,
                        tint = ServiceColors.TextSecondary,
                    )
                }
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(Color.White),
        ) {
            ServiceCustomerType.entries.forEach { type ->
                DropdownMenuItem(
                    text = {
                        Text(
                            type.label,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = ServiceColors.TextPrimary,
                        )
                    },
                    onClick = {
                        onSelect(type)
                        expanded = false
                    },
                )
            }
        }
    }
}

/**
 * The vehicle field, always pre-filled with [SERVICE_VEHICLE_TYPE].
 *
 * The type is a constant of this flow, so it is printed unconditionally — an empty
 * device list or a failed `get-user-devices` only costs the registration line, never
 * the "EV Scooty" itself. [expandable] is true only when the account holds more than
 * one device, and then this doubles as the picker for which registration to file
 * against; the type line does not change either way.
 */
@Composable
private fun PreselectedVehicleRow(
    vehicle: ServiceVehicle?,
    expandable: Boolean = false,
    vehicles: List<ServiceVehicle> = emptyList(),
    onSelect: (ServiceVehicle) -> Unit = {},
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = expandable) { expanded = true },
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = ServiceColors.BrandSoft),
            elevation = CardDefaults.cardElevation(0.dp),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                ServiceColors.Brand.copy(alpha = 0.35f),
            ),
        ) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Filled.TwoWheeler,
                    contentDescription = null,
                    tint = ServiceColors.Brand,
                    modifier = Modifier.size(19.dp),
                )
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        SERVICE_VEHICLE_TYPE,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ServiceColors.TextPrimary,
                    )
                    Text(
                        when {
                            // Said here rather than at submit: no device is not a
                            // blocker, but the user should not find out afterwards.
                            vehicle == null -> "No device linked · you can still raise this"
                            expandable -> "${vehicle.registrationNumber} · Tap to change"
                            else -> "${vehicle.registrationNumber} · Selected automatically"
                        },
                        fontSize = 11.sp,
                        color = if (vehicle == null) ServiceColors.TextSecondary else ServiceColors.Brand,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Icon(
                    if (expandable) Icons.Filled.KeyboardArrowDown else Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = ServiceColors.Brand,
                    modifier = Modifier.size(18.dp),
                )
            }
        }

        if (expandable) {
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(Color.White),
            ) {
                vehicles.forEach { option ->
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text(
                                    option.registrationNumber,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = ServiceColors.TextPrimary,
                                )
                                Text(
                                    "$SERVICE_VEHICLE_TYPE · VIN ${option.vin.takeLast(6)}",
                                    fontSize = 11.sp,
                                    color = ServiceColors.TextSecondary,
                                )
                            }
                        },
                        onClick = {
                            onSelect(option)
                            expanded = false
                        },
                    )
                }
            }
        }
    }
}


@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ComponentChipGrid(
    components: List<ServiceComponentItem>,
    selected: ServiceComponentItem?,
    onSelect: (ServiceComponentItem) -> Unit,
) {
    // FlowRow keeps the 12 components readable without a nested scrollable grid.
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        components.forEach { component ->
            val isSelected = component.id == selected?.id
            Box(
                Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isSelected) ServiceColors.Brand else Color(0xFFF1F5F9))
                    .clickable { onSelect(component) }
                    .padding(horizontal = 12.dp, vertical = 9.dp),
            ) {
                Text(
                    component.label,
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isSelected) Color.White else ServiceColors.TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}