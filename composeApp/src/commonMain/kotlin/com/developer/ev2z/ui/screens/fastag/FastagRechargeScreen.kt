package com.developer.ev2z.ui.screens.fastag

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
// Most of these live in material-icons-extended (already on the classpath).
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.developer.ev2z.theme.AppPageBg
import com.developer.ev2z.ui.components.dismissKeyboardOnTap

/**
 * FASTag recharge.
 *
 * Single progressive screen rather than a wizard: a returning user with a linked tag
 * lands here with the issuer and identifier already filled and only touches the amount.
 * A first-time user walks down the page. Steps reveal as they are satisfied — see
 * [FastagRechargeUiState.step].
 *
 * Mandatory fields collected here:
 *   1. Issuer bank            -> billerId
 *   2. Customer identifier    -> customerParams, rendered from the server schema
 *   3. Amount                 -> validated against the issuer's own min/max
 *
 * Everything else on screen is either read-back (registered name, balance) or a
 * breakdown of what the user is about to be charged.
 *
 * This composable is stateless. Hoist [FastagRechargeUiState] into a state holder.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun FastagRechargeScreen(
    state: FastagRechargeUiState,
    onBack: () -> Unit,
    onSelectBiller: (FastagBiller) -> Unit,
    onParamChange: (paramName: String, value: String) -> Unit,
    onVerify: () -> Unit,
    onChangeIssuer: () -> Unit,
    onAmountChange: (String) -> Unit,
    onPay: () -> Unit,
    onOpenHistory: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var issuerSheetOpen by rememberSaveable { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current

    // Hand-rolled instead of Scaffold: the top bar and the pay bar each need their own
    // safe-area inset (status bar / home indicator + keyboard), and Scaffold's single
    // content-inset pass left both drawing under the iPhone's safe area.
    FastagResponsiveRoot(
        modifier = modifier
            .fillMaxSize()
            .background(AppPageBg)
    ) { dimens ->
        Column(Modifier.fillMaxSize()) {

            FastagTopBar(
                title = "FASTag recharge",
                onBack = onBack,
                onHistory = onOpenHistory
            )

            // weight(1f) — the scroll viewport is whatever is left between the two bars,
            // so it always has a bounded height and always scrolls, however tall the
            // content grows or however little screen is left above the keyboard.
            Column(
                modifier = Modifier
                    .weight(1f)
                    // Cap before fill: fillMaxWidth pins the width to the parent's, and a
                    // widthIn after it can no longer shrink that fixed constraint.
                    .fastagContentWidth(dimens)
                    .fillMaxWidth()
                    .align(Alignment.CenterHorizontally)
                    .verticalScroll(rememberScrollState())
                    .dismissKeyboardOnTap(focusManager)
                    .padding(horizontal = dimens.hPad)
                    .padding(top = 12.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {

                // ── Vehicle context ────────────────────────────────────
                if (state.vehicleNumber != null) {
                    VehicleContextCard(
                        vehicleNumber = state.vehicleNumber,
                        issuerName = state.selectedBiller?.name,
                        balancePaise = state.validation?.balancePaise
                    )
                }

                // ── Step 1 · issuer ────────────────────────────────────
                StepHeader(
                    index = 1,
                    title = "Select FASTag issuer",
                    done = state.selectedBiller != null
                )
                IssuerSelectorCard(
                    biller = state.selectedBiller,
                    loading = state.billersLoading,
                    onClick = { issuerSheetOpen = true }
                )

                // ── Step 2 · identifier ────────────────────────────────
                if (state.selectedBiller != null) {
                    StepHeader(
                        index = 2,
                        title = "Tag details",
                        done = state.validation?.valid == true
                    )

                    when {
                        state.configLoading -> LoadingRow("Loading fields for ${state.selectedBiller.name}")

                        state.billerConfig != null -> {
                            val locked = state.validation?.valid == true

                            // Rendered entirely from the server schema. There is no
                            // hardcoded field here by design — some issuers key on a
                            // Tag ID or wallet ID rather than a vehicle number.
                            state.billerConfig.customerParams.forEachIndexed { index, param ->
                                val isLast = index == state.billerConfig.customerParams.lastIndex
                                FastagTextField(
                                    value = state.paramValues[param.name].orEmpty(),
                                    onValueChange = { onParamChange(param.name, it) },
                                    label = param.label + if (param.optional) " (optional)" else "",
                                    placeholder = placeholderFor(param),
                                    helper = param.helperText,
                                    error = state.paramErrors[param.name],
                                    enabled = !locked && !state.validating,
                                    keyboardType = keyboardTypeFor(param),
                                    imeAction = if (isLast) ImeAction.Done else ImeAction.Next,
                                    onImeAction = {
                                        if (isLast) {
                                            focusManager.clearFocus()
                                            if (state.canVerify) onVerify()
                                        } else {
                                            focusManager.moveFocus(FocusDirection.Down)
                                        }
                                    },
                                    modifier = Modifier.padding(top = if (index == 0) 0.dp else 10.dp)
                                )
                            }

                            if (!locked) {
                                Spacer(Modifier.height(4.dp))
                                InfoBanner(
                                    icon = Icons.Default.Lock,
                                    tint = FastagBlue,
                                    background = FastagBlueLight,
                                    text = "We'll check these details with ${state.selectedBiller.name} " +
                                            "before you pay. No money moves yet."
                                )
                                Spacer(Modifier.height(10.dp))
                                PrimaryButton(
                                    text = if (state.validating) "Checking…" else "Verify tag",
                                    enabled = state.canVerify,
                                    loading = state.validating,
                                    onClick = {
                                        focusManager.clearFocus()
                                        keyboard?.hide()
                                        onVerify()
                                    }
                                )
                            }
                        }
                    }

                    // Validation failed — the usual cause is the wrong issuer, not a
                    // wrong plate, so that is the primary action offered.
                    val v = state.validation
                    if (v != null && !v.valid) {
                        Spacer(Modifier.height(4.dp))
                        ValidationErrorCard(
                            message = v.failureMessage
                                ?: "We couldn't find this tag with ${state.selectedBiller.name}.",
                            onChangeIssuer = {
                                onChangeIssuer()
                                issuerSheetOpen = true
                            }
                        )
                    }
                }

                // ── Step 3 · verified read-back ────────────────────────
                val verified = state.validation
                if (verified != null && verified.valid) {
                    VerifiedTagCard(
                        customerName = verified.customerName,
                        identifier = primaryIdentifier(state),
                        issuerName = state.selectedBiller?.name.orEmpty(),
                        balancePaise = verified.balancePaise,
                        balanceAvailable = verified.balanceAvailable,
                        onChangeIssuer = {
                            onChangeIssuer()
                            issuerSheetOpen = true
                        }
                    )

                    // ── Step 4 · amount ────────────────────────────────
                    StepHeader(index = 3, title = "Recharge amount", done = state.canPay)

                    AmountEntryCard(
                        amountInput = state.amountInput,
                        onAmountChange = onAmountChange,
                        error = state.amountError,
                        limits = verified.amountLimits ?: state.billerConfig?.amountLimits,
                        presets = verified.amountPresetsPaise.ifEmpty {
                            state.billerConfig?.amountPresetsPaise.orEmpty()
                        },
                        onPresetClick = { paise -> onAmountChange((paise / 100).toString()) },
                        onDone = {
                            focusManager.clearFocus()
                            keyboard?.hide()
                        }
                    )

                    // Three separate numbers, never collapsed into one — the amount
                    // credited to the tag, any fee, and what actually leaves the account.
                    if (state.amountPaise != null && state.amountError == null) {
                        PaymentBreakdownCard(
                            amountPaise = state.amountPaise!!,
                            feePaise = state.convenienceFeePaise,
                            totalPaise = state.totalPayablePaise ?: state.amountPaise!!
                        )
                    }
                }

                if (state.submitError != null) {
                    InfoBanner(
                        icon = Icons.Default.ErrorOutline,
                        tint = DangerRed,
                        background = DangerRedLight,
                        text = state.submitError
                    )
                }

                Spacer(Modifier.height(4.dp))
                SecurityFootnote()
            }

            FastagPayBar(
                state = state,
                dimens = dimens,
                onPay = {
                    focusManager.clearFocus()
                    keyboard?.hide()
                    onPay()
                }
            )
        }

        if (issuerSheetOpen) {
            IssuerPickerSheet(
                billers = state.billers,
                selected = state.selectedBiller,
                maxListHeight = dimens.sheetListMaxHeight,
                onSelect = {
                    onSelectBiller(it)
                    issuerSheetOpen = false
                },
                onDismiss = { issuerSheetOpen = false }
            )
        }
    }
}

// ───────────────────────────── Top bar ─────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FastagTopBar(
    title: String,
    onBack: () -> Unit,
    onHistory: () -> Unit
) {
    TopAppBar(
        // The inset is applied here rather than left to TopAppBarDefaults.windowInsets so
        // the bar clears the iPhone notch the same way every other screen in the app does.
        modifier = Modifier.statusBarsPadding(),
        windowInsets = WindowInsets(0, 0, 0, 0),
        title = {
            Text(
                title,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
            }
        },
        actions = {
            IconButton(onClick = onHistory) {
                Icon(Icons.Default.History, contentDescription = "Recharge history", tint = TextSecondary)
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = AppPageBg,
            titleContentColor = TextPrimary
        )
    )
}

// ─────────────────────── Vehicle context card ──────────────────────

@Composable
private fun VehicleContextCard(
    vehicleNumber: String,
    issuerName: String?,
    balancePaise: Long?
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(16.dp), ambientColor = ShadowColor, spotColor = ShadowColor),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, FastagBlue.copy(alpha = 0.24f))
    ) {
        Box(Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .align(Alignment.TopEnd)
                    .offset(x = 32.dp, y = (-40).dp)
                    .background(
                        Brush.radialGradient(listOf(FastagBlue.copy(0.14f), Color.Transparent)),
                        CircleShape
                    )
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GradientIconTile(
                    icon = Icons.Default.DirectionsCar,
                    tint = FastagBlue,
                    size = 46.dp
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        vehicleNumber,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        issuerName ?: "Select an issuer to continue",
                        fontSize = 11.sp,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (balancePaise != null) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Balance", fontSize = 10.sp, color = TextHint)
                        Text(
                            formatRupees(balancePaise),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (balancePaise < 50_000L) WarnAmber else TextPrimary
                        )
                    }
                }
            }
        }
    }
}

// ───────────────────────── Step header ─────────────────────────────

@Composable
private fun StepHeader(index: Int, title: String, done: Boolean) {
    Row(
        modifier = Modifier.padding(top = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(if (done) BrandGreen else BrandGreenLight),
            contentAlignment = Alignment.Center
        ) {
            if (done) {
                Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(14.dp))
            } else {
                Text(
                    index.toString(),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = BrandGreen
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        Text(title, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
    }
}

// ─────────────────────── Issuer selector ───────────────────────────

@Composable
private fun IssuerSelectorCard(
    biller: FastagBiller?,
    loading: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, if (biller != null) BrandGreen.copy(0.45f) else FieldBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (biller != null) {
                IssuerAvatar(biller.shortCode)
            } else {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .background(MutedIconBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.AccountBalance,
                        null,
                        tint = MutedIcon,
                        modifier = Modifier.size(19.dp)
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    if (biller != null) "Issuer" else "Choose your FASTag bank",
                    fontSize = 11.sp,
                    color = TextSecondary
                )
                Spacer(Modifier.height(1.dp))
                Text(
                    biller?.name ?: "Printed on the tag on your windscreen",
                    fontSize = 14.sp,
                    fontWeight = if (biller != null) FontWeight.Bold else FontWeight.Normal,
                    color = if (biller != null) TextPrimary else TextHint,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = BrandGreen
                )
            } else {
                Icon(
                    Icons.Default.ExpandMore,
                    contentDescription = "Change issuer",
                    tint = if (biller != null) BrandGreen else TextHint,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
private fun IssuerAvatar(shortCode: String, size: Dp = 38.dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(11.dp))
            .background(
                Brush.linearGradient(listOf(FastagBlue.copy(0.18f), FastagBlue.copy(0.07f)))
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            shortCode.take(2).uppercase(),
            fontSize = 13.sp,
            fontWeight = FontWeight.ExtraBold,
            color = FastagBlue
        )
    }
}

// ──────────────────── Issuer picker bottom sheet ───────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IssuerPickerSheet(
    billers: List<FastagBiller>,
    selected: FastagBiller?,
    maxListHeight: Dp,
    onSelect: (FastagBiller) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    // Plain remember, not rememberSaveable: a stale search term restored on the
    // next open would show a filtered list the user did not ask for.
    var query by remember { mutableStateOf("") }

    val filtered = remember(billers, query) {
        val q = query.trim()
        billers.filter { it.active }
            .filter { q.isEmpty() || it.name.contains(q, ignoreCase = true) }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = CardWhite,
        dragHandle = { BottomSheetDefaults.DragHandle(color = FieldBorder) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                // The sheet is a scrim over the whole window, so it owns its own bottom
                // inset — without it the last issuer sits under the home indicator.
                .navigationBarsPadding()
                .imePadding()
                .padding(bottom = 20.dp)
        ) {
            Text(
                "Select FASTag issuer",
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TextPrimary
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "The issuing bank is printed on the FASTag sticker on your windscreen.",
                fontSize = 12.sp,
                color = TextSecondary,
                lineHeight = 17.sp
            )
            Spacer(Modifier.height(14.dp))

            FastagTextField(
                value = query,
                onValueChange = { query = it },
                label = null,
                placeholder = "Search bank or issuer",
                helper = null,
                error = null,
                enabled = true,
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Search,
                onImeAction = {},
                leadingIcon = Icons.Default.Search
            )

            Spacer(Modifier.height(10.dp))

            if (filtered.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 36.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No issuers match \"$query\"", fontSize = 13.sp, color = TextHint)
                }
            } else {
                // Explicit height, not heightIn(max=). The sheet measures its content
                // with an infinite max height, and a lazy list under an infinite
                // constraint throws "Vertically scrollable component was measured with
                // an infinity maximum height constraints". heightIn caps the size but
                // does not remove the infinite constraint, so it still crashes.
                //
                // The height is now bounded by the window as well as by the row count:
                // seven 58dp rows plus the header overflowed a short screen, and the
                // rows past the bottom edge could not be reached or scrolled to.
                val rowHeight = 58.dp
                val listHeight = (rowHeight * filtered.size)
                    .coerceAtMost(maxListHeight)
                LazyColumn(
                    modifier = Modifier.height(listHeight),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    items(filtered, key = { it.billerId }) { biller ->
                        IssuerRow(
                            biller = biller,
                            selected = biller.billerId == selected?.billerId,
                            onClick = { onSelect(biller) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun IssuerRow(
    biller: FastagBiller,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) BrandGreenLight else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IssuerAvatar(biller.shortCode, size = 34.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                biller.name,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            // Surfaced up front so the missing balance later reads as expected,
            // not as a failure.
            if (!biller.supportsBalanceEnquiry) {
                Text(
                    "Balance not shared by this bank",
                    fontSize = 10.sp,
                    color = TextHint
                )
            }
        }
        if (selected) {
            Icon(Icons.Default.CheckCircle, null, tint = BrandGreen, modifier = Modifier.size(20.dp))
        }
    }
}

// ──────────────────────── Verified tag card ────────────────────────

@Composable
private fun VerifiedTagCard(
    customerName: String?,
    identifier: String?,
    issuerName: String,
    balancePaise: Long?,
    balanceAvailable: Boolean,
    onChangeIssuer: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, BrandGreen.copy(0.35f))
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.VerifiedUser,
                    null,
                    tint = BrandGreen,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    "Tag verified",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = BrandGreen
                )
                Spacer(Modifier.weight(1f))
                Text(
                    "Change",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onChangeIssuer)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }

            Spacer(Modifier.height(10.dp))

            // Registered name is the user's only chance to catch a wrong plate
            // before money moves. It is the most important line on this card.
            if (!customerName.isNullOrBlank()) {
                DetailRow("Registered name", customerName)
            }
            if (!identifier.isNullOrBlank()) {
                DetailRow("Tag / vehicle", identifier)
            }
            DetailRow("Issuer", issuerName)

            Spacer(Modifier.height(10.dp))
            FastagDivider(DividerSoft)
            Spacer(Modifier.height(10.dp))

            if (balanceAvailable && balancePaise != null) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Current balance", fontSize = 11.sp, color = TextSecondary)
                    Text(
                        formatRupees(balancePaise),
                        fontSize = 26.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (balancePaise < 50_000L) WarnAmber else TextPrimary
                    )
                }
            } else {
                // Absent balance must look deliberate — never an empty "₹--".
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MutedCardBg)
                        .padding(horizontal = 10.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Info,
                        null,
                        tint = MutedIcon,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "$issuerName doesn't share balance. You can still recharge.",
                        fontSize = 11.sp,
                        color = TextSecondary,
                        lineHeight = 15.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(label, fontSize = 12.sp, color = TextSecondary)
        Spacer(Modifier.width(12.dp))
        Text(
            value,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f, fill = false)
        )
    }
}

// ─────────────────────────── Amount entry ──────────────────────────

@Composable
private fun AmountEntryCard(
    amountInput: String,
    onAmountChange: (String) -> Unit,
    error: String?,
    limits: FastagAmountLimits?,
    presets: List<Long>,
    onPresetClick: (Long) -> Unit,
    onDone: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, if (error != null) DangerRed.copy(0.5f) else FieldBorder)
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 16.dp)) {

            // Big amount input — currency symbol is static, the field is digits only.
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    "₹",
                    fontSize = 30.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (amountInput.isEmpty()) TextHint else TextPrimary
                )
                Spacer(Modifier.width(4.dp))
                BasicTextField(
                    value = amountInput,
                    onValueChange = { raw ->
                        // Digits and a single decimal point, max two decimals.
                        val cleaned = raw.filter { it.isDigit() || it == '.' }
                        val parts = cleaned.split(".")
                        val normalized = when {
                            parts.size <= 1 -> parts.getOrElse(0) { "" }.take(7)
                            else -> parts[0].take(7) + "." + parts[1].take(2)
                        }
                        onAmountChange(normalized)
                    },
                    textStyle = TextStyle(
                        fontSize = 30.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary
                    ),
                    singleLine = true,
                    cursorBrush = SolidColor(BrandGreen),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = { onDone() }),
                    modifier = Modifier.weight(1f),
                    decorationBox = { inner ->
                        Box(contentAlignment = Alignment.CenterStart) {
                            if (amountInput.isEmpty()) {
                                Text(
                                    "0",
                                    fontSize = 30.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = TextHint
                                )
                            }
                            inner()
                        }
                    }
                )
            }

            Spacer(Modifier.height(6.dp))
            FastagDivider(if (error != null) DangerRed.copy(0.5f) else BrandGreen)

            if (error != null) {
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.ErrorOutline,
                        null,
                        tint = DangerRed,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(error, fontSize = 11.sp, color = DangerRed, fontWeight = FontWeight.Medium)
                }
            } else if (limits != null) {
                Spacer(Modifier.height(8.dp))
                // Limits come from the issuer's own config, never from app constants.
                Text(
                    "Minimum ${formatRupeesShort(limits.minPaise)} · " +
                            "Maximum ${formatRupeesShort(limits.maxPaise)}",
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }

            if (presets.isNotEmpty()) {
                Spacer(Modifier.height(14.dp))
                val selectedPaise = rupeesToPaise(amountInput)
                FlowRowPresets(
                    presets = presets,
                    selectedPaise = selectedPaise,
                    onClick = onPresetClick
                )
            }
        }
    }
}

/**
 * Simple wrapping chip row. Avoids FlowRow so this file has no dependency on the
 * experimental foundation-layout API — the preset count is small and bounded.
 */
@Composable
private fun FlowRowPresets(
    presets: List<Long>,
    selectedPaise: Long?,
    onClick: (Long) -> Unit
) {
    // Four chips are unreadable on a 320dp-wide phone, so the row count follows the
    // window rather than being fixed.
    val perRow = LocalFastagDimens.current.presetsPerRow
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        presets.chunked(perRow).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { paise ->
                    val isSelected = selectedPaise == paise
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isSelected) BrandGreen else BrandGreenLight)
                            .clickable { onClick(paise) }
                            .padding(vertical = 9.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            formatRupeesShort(paise),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color.White else BrandGreenDark
                        )
                    }
                }
                repeat(perRow - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

// ─────────────────────── Payment breakdown ─────────────────────────

@Composable
private fun PaymentBreakdownCard(
    amountPaise: Long,
    feePaise: Long,
    totalPaise: Long
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, FieldBorder)
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            BreakdownRow("Recharge amount", formatRupees(amountPaise), bold = false)
            Spacer(Modifier.height(6.dp))
            BreakdownRow("Convenience fee", formatRupees(feePaise), bold = false)
            Spacer(Modifier.height(9.dp))
            FastagDivider(DividerSoft)
            Spacer(Modifier.height(9.dp))
            BreakdownRow("Total payable", formatRupees(totalPaise), bold = true)
        }
    }
}

@Composable
private fun BreakdownRow(label: String, value: String, bold: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            fontSize = if (bold) 14.sp else 12.sp,
            fontWeight = if (bold) FontWeight.ExtraBold else FontWeight.Normal,
            color = if (bold) TextPrimary else TextSecondary
        )
        Text(
            value,
            fontSize = if (bold) 14.sp else 12.sp,
            fontWeight = if (bold) FontWeight.ExtraBold else FontWeight.Medium,
            color = TextPrimary
        )
    }
}

// ────────────────────────── Sticky pay bar ─────────────────────────

@Composable
private fun FastagPayBar(
    state: FastagRechargeUiState,
    dimens: FastagDimens,
    onPay: () -> Unit
) {
    Surface(
        color = CardWhite,
        shadowElevation = 12.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                // union, not two paddings: when the keyboard is up it already covers the
                // home indicator, so adding both would float the button off the keyboard.
                .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fastagContentWidth(dimens)
                    .fillMaxWidth()
                    .padding(horizontal = dimens.hPad, vertical = 12.dp)
            ) {
                if (state.step == FastagStep.READY_TO_PAY) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Total payable", fontSize = 11.sp, color = TextSecondary)
                            Text(
                                formatRupees(state.totalPayablePaise ?: 0L),
                                fontSize = 19.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = TextPrimary
                            )
                        }
                        state.selectedBiller?.let {
                            Text(
                                it.name,
                                fontSize = 11.sp,
                                color = TextHint,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.widthIn(max = 150.dp)
                            )
                        }
                    }
                }

                PrimaryButton(
                    text = when {
                        state.submitting -> "Please wait…"
                        state.step == FastagStep.READY_TO_PAY ->
                            "Proceed to pay ${formatRupees(state.totalPayablePaise ?: 0L)}"
                        state.step == FastagStep.VERIFIED -> "Enter an amount"
                        state.step == FastagStep.ENTER_IDENTIFIER -> "Verify tag to continue"
                        else -> "Select an issuer to continue"
                    },
                    enabled = state.canPay,
                    loading = state.submitting,
                    onClick = onPay
                )
            }
        }
    }
}

// ───────────────────────── Reusable pieces ─────────────────────────

@Composable
private fun PrimaryButton(
    text: String,
    enabled: Boolean,
    loading: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled && !loading,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 54.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = BrandGreen,
            contentColor = Color.White,
            disabledContainerColor = MutedCardBg,
            disabledContentColor = MutedText
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = Color.White
            )
            Spacer(Modifier.width(10.dp))
        }
        Text(text, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
private fun FastagTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String?,
    placeholder: String,
    helper: String?,
    error: String?,
    enabled: Boolean,
    keyboardType: KeyboardType,
    imeAction: ImeAction,
    onImeAction: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null
) {
    var focused by remember { mutableStateOf(false) }
    val borderColor = when {
        error != null -> DangerRed
        focused -> FieldBorderFocus
        else -> FieldBorder
    }

    Column(modifier = modifier.fillMaxWidth()) {
        if (label != null) {
            Text(label, fontSize = 12.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(6.dp))
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(if (enabled) CardWhite else MutedCardBg)
                .border(
                    width = if (focused || error != null) 1.5.dp else 1.dp,
                    color = borderColor,
                    shape = RoundedCornerShape(14.dp)
                )
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (leadingIcon != null) {
                Icon(leadingIcon, null, tint = TextHint, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(10.dp))
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                enabled = enabled,
                singleLine = true,
                textStyle = TextStyle(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (enabled) TextPrimary else TextSecondary
                ),
                cursorBrush = SolidColor(BrandGreen),
                keyboardOptions = KeyboardOptions(
                    keyboardType = keyboardType,
                    imeAction = imeAction
                ),
                keyboardActions = KeyboardActions(
                    onNext = { onImeAction() },
                    onDone = { onImeAction() },
                    onSearch = { onImeAction() }
                ),
                modifier = Modifier
                    .weight(1f)
                    .onFocusChanged { focused = it.isFocused },
                decorationBox = { inner ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (value.isEmpty()) {
                            Text(placeholder, fontSize = 15.sp, color = TextHint)
                        }
                        inner()
                    }
                }
            )
            if (value.isNotEmpty() && enabled) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Clear",
                    tint = TextHint,
                    modifier = Modifier
                        .size(18.dp)
                        .clickable { onValueChange("") }
                )
            }
        }

        val sub = error ?: helper
        if (sub != null) {
            Spacer(Modifier.height(5.dp))
            Text(
                sub,
                fontSize = 11.sp,
                color = if (error != null) DangerRed else TextHint,
                lineHeight = 15.sp
            )
        }
    }
}

/**
 * Material 3 renamed Divider to HorizontalDivider in 1.2.0. Wrapped here so a project
 * on M3 1.1.x only has to change this one line to `Divider(color = color)`.
 */
@Composable
private fun FastagDivider(color: Color) {
    HorizontalDivider(color = color)
}

@Composable
private fun InfoBanner(
    icon: ImageVector,
    tint: Color,
    background: Color,
    text: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(background)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(15.dp))
        Spacer(Modifier.width(9.dp))
        Text(text, fontSize = 11.sp, color = TextSecondary, lineHeight = 16.sp)
    }
}

@Composable
private fun ValidationErrorCard(
    message: String,
    onChangeIssuer: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = DangerRedLight),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(
                message,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF8F322D),
                lineHeight = 18.sp
            )
            Spacer(Modifier.height(4.dp))
            // Says plainly that no money moved — the first thing a user needs to know.
            Text(
                "This usually means the tag was issued by a different bank. " +
                        "Nothing has been charged.",
                fontSize = 11.sp,
                color = Color(0xFF7D3A36),
                lineHeight = 16.sp
            )
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(DangerRed)
                    .clickable(onClick = onChangeIssuer)
                    .padding(horizontal = 14.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.SwapHoriz,
                    null,
                    tint = Color.White,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    "Try a different issuer",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun LoadingRow(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(18.dp),
            strokeWidth = 2.dp,
            color = BrandGreen
        )
        Spacer(Modifier.width(10.dp))
        Text(text, fontSize = 12.sp, color = TextSecondary)
    }
}

@Composable
private fun GradientIconTile(
    icon: ImageVector,
    tint: Color,
    size: Dp
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(14.dp))
            .background(Brush.linearGradient(listOf(tint.copy(0.18f), tint.copy(0.07f)))),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(size * 0.5f))
    }
}

@Composable
private fun SecurityFootnote() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.Lock,
            null,
            tint = TextHint,
            modifier = Modifier.size(12.dp)
        )
        Spacer(Modifier.width(5.dp))
        Text(
            "Payments handled by our licensed partner. Eva2z never stores card or UPI details.",
            fontSize = 10.sp,
            color = TextHint,
            textAlign = TextAlign.Center,
            lineHeight = 14.sp
        )
    }
}

// ─────────────────────────── Small helpers ─────────────────────────

private fun keyboardTypeFor(param: FastagBillerParam): KeyboardType = when {
    param.name.equals("mobileNumber", ignoreCase = true) -> KeyboardType.Phone
    param.dataType == FastagParamType.NUMERIC -> KeyboardType.Number
    else -> KeyboardType.Text
}

private fun placeholderFor(param: FastagBillerParam): String = when {
    param.name.equals("vehicleNumber", ignoreCase = true) -> "MH12AB1234"
    param.name.equals("mobileNumber", ignoreCase = true) -> "10-digit mobile number"
    else -> "Enter ${param.label.lowercase()}"
}

/** Best identifier to show back to the user, whatever the schema keyed on. */
private fun primaryIdentifier(state: FastagRechargeUiState): String? =
    state.paramValues["vehicleNumber"]?.takeIf { it.isNotBlank() }
        ?: state.vehicleNumber
        ?: state.billerConfig?.customerParams
            ?.firstOrNull { !it.optional }
            ?.let { state.paramValues[it.name] }
            ?.takeIf { it.isNotBlank() }
