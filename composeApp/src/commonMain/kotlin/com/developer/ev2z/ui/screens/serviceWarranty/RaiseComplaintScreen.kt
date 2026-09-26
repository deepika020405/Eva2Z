package com.developer.ev2z.ui.screens.serviceWarranty

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import kotlinx.coroutines.launch
import kotlin.time.ExperimentalTime

/**
 * SCREEN 2 — Raise Complaint (FRD §3 / §4).
 * Vehicle type → problem → description → terms → pay ₹[SERVICE_FEE_RUPEES] → submit.
 *
 * The form no longer asks who is asking or which component is affected: every
 * complaint carries the same service charge, and the problem is typed rather than
 * picked off a list. Nothing is uploaded from here either — the invoice and photo
 * sections are gone, so the whole form is four short cards.
 */
@OptIn(ExperimentalTime::class)
@Composable
fun RaiseComplaintScreen(
    viewModel: RaiseComplaintViewModel,
    onBack: () -> Unit,
    /**
     * [payment] is the receipt for the charge, when one was collected — handed on so
     * the ticket's own screen can show what was paid before the server reports it.
     */
    onComplaintCreated: (ticketId: String, payment: ServicePaymentReceipt?) -> Unit,
    /** Paid, but the server would not file the ticket — hand over to the list. */
    onPaidWithoutTicket: () -> Unit = onBack,
) {
    val state by viewModel.state.collectAsState()
    val snackbarHost = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showSuccessFor by remember { mutableStateOf<String?>(null) }

    PlatformBackHandler(true) { onBack() }

    // The service charge is collected before the complaint is filed. The order is
    // consumed immediately so a recomposition can't open a second checkout for
    // money that is already being collected.
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
                ) { LoadingState(label = "Loading your vehicles…") }
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
                    /* ─────────────────────── 1. vehicle ──────────────────────── */
                    item(key = "vehicle") {
                        ServiceCard(title = "1. Vehicle") {
                            // Nothing is preselected, so the question has to be asked
                            // out loud — a filled-in answer got walked past.
                            val missingType = state.showValidation && state.draft.vehicleType == null
                            Text(
                                "Which vehicle is this complaint for?",
                                fontSize = 12.sp,
                                color = if (missingType) ServiceColors.Danger
                                else ServiceColors.TextSecondary,
                            )
                            Spacer(Modifier.height(10.dp))
                            // Asked rather than read off the device list: the tracker
                            // knows nothing about what it is fitted to, and a petrol
                            // two-wheeler may have no device on the account at all.
                            ServiceVehicleType.entries.forEachIndexed { index, type ->
                                if (index > 0) Spacer(Modifier.height(8.dp))
                                VehicleTypeOption(
                                    type = type,
                                    selected = state.draft.vehicleType == type,
                                    isError = missingType,
                                    // Switching after paying would misrepresent what
                                    // the charge was raised for.
                                    enabled = state.paidReceipt == null && !state.isBusy,
                                    onSelect = { viewModel.selectVehicleType(type) },
                                )
                            }

                            // Only surfaced when the account holds more than one
                            // device, and then only to choose which one the ticket is
                            // filed against — a single device is picked silently.
                            if (state.vehicles.size > 1) {
                                Spacer(Modifier.height(10.dp))
                                VehiclePickerRow(
                                    vehicle = state.draft.vehicle,
                                    vehicles = state.vehicles,
                                    onSelect = viewModel::selectVehicle,
                                )
                            }

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

                    /* ─────────────────────── 2. problem ──────────────────────── */
                    item(key = "problem") {
                        ServiceCard(title = "2. Problem") {
                            val missing = state.showValidation && state.draft.problem.isBlank()
                            OutlinedTextField(
                                value = state.draft.problem,
                                onValueChange = viewModel::updateProblem,
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = {
                                    Text(
                                        "e.g. Battery not charging",
                                        fontSize = 13.sp,
                                        color = ServiceColors.TextMuted,
                                    )
                                },
                                supportingText = {
                                    Text(
                                        "${state.draft.problem.length} / ${ComplaintDraft.MAX_PROBLEM}",
                                        fontSize = 11.sp,
                                        color = ServiceColors.TextMuted,
                                    )
                                },
                                isError = missing,
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                textStyle = androidx.compose.ui.text.TextStyle(
                                    fontSize = 14.sp,
                                    color = ServiceColors.TextPrimary,
                                ),
                                colors = serviceFieldColors(),
                                keyboardOptions = KeyboardOptions(
                                    capitalization = KeyboardCapitalization.Sentences,
                                    imeAction = ImeAction.Next,
                                ),
                            )
                        }
                    }

                    /* ──────────────────── 3. description ──────────────────── */
                    item(key = "issue") {
                        ServiceCard(title = "3. Describe the issue") {
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

                    /* ──────────────── 4. service charge & terms ──────────────── */
                    item(key = "charge") {
                        ServiceCard(title = "4. Service charge") {
                            // The figure is the server's `service_charge` once it has
                            // stated one; the published fee stands in until then,
                            // because no endpoint quotes it ahead of the submission.
                            val charge = state.chargeRupees.takeIf { it > 0 }
                                ?: SERVICE_FEE_RUPEES
                            ChargeRow(
                                amountRupees = charge,
                                quoted = state.chargeIsFromServer,
                                paid = state.paidReceipt != null,
                            )
                            // The complaint is on record before the money is: the
                            // server files it and asks for the charge in one answer, so
                            // say so — otherwise a cancelled payment looks like nothing
                            // happened and the user submits the same fault twice.
                            state.unpaidTicket?.let { ticket ->
                                Spacer(Modifier.height(10.dp))
                                InlineBanner(
                                    text = "Complaint ${ticket.ticketId} is already filed. " +
                                        "Only the ₹$charge charge is outstanding — pay it to " +
                                        "complete the request; don't submit this form again.",
                                    tone = BannerTone.WARNING,
                                )
                            }
                            Spacer(Modifier.height(12.dp))
                            TermsCheckRow(
                                amountRupees = charge,
                                checked = state.draft.termsAccepted,
                                isError = state.showValidation && !state.draft.termsAccepted,
                                enabled = !state.isBusy,
                                onCheckedChange = viewModel::setTermsAccepted,
                            )
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
                        "We'll notify you once a technician is assigned.",
                        fontSize = 12.sp,
                        color = ServiceColors.TextMuted,
                        lineHeight = 17.sp,
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val receipt = state.paidReceipt
                        showSuccessFor = null
                        viewModel.consumeSubmission()
                        onComplaintCreated(ticketId, receipt)
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

            // Only ever a charge the server has already collected — what is still
            // owed is stated in the form's own charge card and in the button label.
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
 * One of the two kinds of vehicle this flow services — an EV or a petrol
 * two-wheeler. A radio row rather than a dropdown: with exactly two answers, both
 * are worth showing at once.
 *
 * Both start white with an empty ring. Green is the answer to "I chose this", never
 * a default: a preselected option is read as a question already answered, and the
 * user then does not realise they had a choice.
 */
@Composable
private fun VehicleTypeOption(
    type: ServiceVehicleType,
    selected: Boolean,
    isError: Boolean,
    enabled: Boolean,
    onSelect: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onSelect),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) ServiceColors.BrandSoft else Color.White,
        ),
        elevation = CardDefaults.cardElevation(0.dp),
        border = BorderStroke(
            1.dp,
            when {
                selected -> ServiceColors.Brand.copy(alpha = 0.55f)
                isError -> ServiceColors.Danger
                else -> ServiceColors.BorderSoft
            },
        ),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Filled.TwoWheeler,
                contentDescription = null,
                tint = if (selected) ServiceColors.Brand else ServiceColors.TextSecondary,
                modifier = Modifier.size(19.dp),
            )
            Spacer(Modifier.width(10.dp))
            Text(
                type.label,
                fontSize = 14.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (selected) ServiceColors.TextPrimary else ServiceColors.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (selected) {
                Icon(
                    Icons.Filled.CheckCircle,
                    contentDescription = "Selected",
                    tint = ServiceColors.Brand,
                    modifier = Modifier.size(18.dp),
                )
            } else {
                // An empty ring, drawn rather than iconised so it lines up with the
                // filled check that replaces it. Painted in the mid grey rather than
                // the card's own border tint — at BorderSoft it was too faint to read
                // as something waiting to be tapped.
                Box(
                    Modifier
                        .size(18.dp)
                        .border(
                            1.5.dp,
                            if (isError) ServiceColors.Danger else ServiceColors.TextMuted,
                            CircleShape,
                        ),
                )
            }
        }
    }
}

/**
 * Which device the ticket is filed against, shown only when the account holds
 * more than one — `service/complaints` keys a complaint by the tracker's IMEI, so
 * with several devices the choice is the user's to make.
 */
@Composable
private fun VehiclePickerRow(
    vehicle: ServiceVehicle?,
    vehicles: List<ServiceVehicle>,
    onSelect: (ServiceVehicle) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFFF8FAFC))
                .clickable { expanded = true }
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Filing against",
                fontSize = 12.sp,
                color = ServiceColors.TextSecondary,
                modifier = Modifier.weight(1f),
            )
            Text(
                vehicle?.registrationNumber ?: "Select a device",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = ServiceColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Icon(
                Icons.Filled.KeyboardArrowDown,
                contentDescription = null,
                tint = ServiceColors.TextSecondary,
                modifier = Modifier.size(18.dp),
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(Color.White),
        ) {
            vehicles.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Text(
                            option.registrationNumber,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = ServiceColors.TextPrimary,
                        )
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

/**
 * The charge this complaint carries. Stated before the checkout opens, so the
 * amount is never a surprise sprung by PhonePe.
 *
 * [quoted] is true once the figure is the server's own `service_charge` rather than
 * the app's published ₹[SERVICE_FEE_RUPEES] placeholder — the line underneath says
 * which it is, because the two have not always agreed.
 */
@Composable
private fun ChargeRow(amountRupees: Int, quoted: Boolean, paid: Boolean) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(ServiceColors.BrandSoft)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                "Complaint service charge",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = ServiceColors.TextPrimary,
            )
            Text(
                when {
                    paid -> "Paid — your complaint is being filed"
                    quoted -> "Confirmed by EVA2Z for this complaint"
                    else -> "Payable once; confirmed when you submit"
                },
                fontSize = 11.sp,
                color = ServiceColors.TextSecondary,
                lineHeight = 15.sp,
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(
            if (paid) "₹$amountRupees paid" else "₹$amountRupees",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = ServiceColors.Brand,
        )
    }
}

/** The last thing [ComplaintDraft.validationError] asks for before the charge. */
@Composable
private fun TermsCheckRow(
    amountRupees: Int,
    checked: Boolean,
    isError: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(enabled = enabled) { onCheckedChange(!checked) },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = { if (enabled) onCheckedChange(it) },
            enabled = enabled,
            colors = CheckboxDefaults.colors(
                checkedColor = ServiceColors.Brand,
                uncheckedColor = if (isError) ServiceColors.Danger else ServiceColors.TextMuted,
                checkmarkColor = Color.White,
            ),
        )
        Spacer(Modifier.width(4.dp))
        Text(
            "I agree to the service terms & conditions and to the " +
                "₹$amountRupees service charge for this complaint.",
            fontSize = 12.sp,
            color = if (isError) ServiceColors.Danger else ServiceColors.TextSecondary,
            lineHeight = 17.sp,
            modifier = Modifier.weight(1f).padding(end = 4.dp),
        )
    }
}