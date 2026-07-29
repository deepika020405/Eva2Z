package com.developer.ev2z.ui.screens.fuelPrice

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.math.roundToInt

// ─── Light-palette design tokens (Guardian Drive prototype alignment) ──────────
private object FuelColors {
    val Background = Color(0xFFFFFFFF)
    val PetrolBg = Color(0xFFEAF7EC)
    val PetrolAccent = Color(0xFF34A853)
    val DieselBg = Color(0xFFFDF3E4)
    val DieselAccent = Color(0xFFE59A2F)
    val TextPrimary = Color(0xFF1B1B1B)
    val TextSecondary = Color(0xFF8A8A8A)
    val LocationCardBg = Color(0xFFF6F7F6)
    val RowDivider = Color(0xFFF0F0F0)
    val ActionGreen = Color(0xFF3FA84A)
    val Down = Color(0xFFD64545)
}


private data class FuelDimens(
    val hPad: Dp,
    val contentMaxWidth: Dp,
    val cardGap: Dp,
    // Header
    val backButton: Dp,
    val backIcon: Dp,
    val titleSize: TextUnit,
    val subtitleSize: TextUnit,
    // Location selector
    val locCorner: Dp,
    val locPadH: Dp,
    val locPadV: Dp,
    val locPinIcon: Dp,
    val locLabelSize: TextUnit,
    val locValueSize: TextUnit,
    val locChevron: Dp,
    // Fuel card
    val cardCorner: Dp,
    val cardPad: Dp,
    val cardLabelSize: TextUnit,
    val cardPriceSize: TextUnit,
    val cardChangeSize: TextUnit,
    val pumpBadge: Dp,
    val pumpIcon: Dp,
    // Meta + section
    val metaSize: TextUnit,
    val sectionSize: TextUnit,
    // Nearby table
    val tableCorner: Dp,
    val tableHeaderSize: TextUnit,
    val tableRowSize: TextUnit,
    val tableRowPadV: Dp,
    // Layout switches
    val stackFuelCards: Boolean,
    val compactTableHeaders: Boolean,
)

private fun fuelDimensFor(maxWidth: Dp): FuelDimens {
    // 375dp ≈ a standard phone. Clamp so tablets don't balloon and tiny phones stay legible.
    val scale = (maxWidth / 375.dp).coerceIn(0.85f, 1.30f)
    fun sp(base: Float): TextUnit = (base * scale).sp
    fun d(base: Float): Dp = (base * scale).dp

    val isTablet = maxWidth >= 600.dp
    val isCompact = maxWidth < 360.dp
    val hPad = when {
        isTablet -> 28.dp
        isCompact -> 16.dp
        else -> 20.dp
    }

    return FuelDimens(
        hPad = hPad,
        contentMaxWidth = if (isTablet) 640.dp else Dp.Unspecified,
        cardGap = d(12f),
        backButton = d(34f),
        backIcon = d(22f),
        titleSize = sp(25f),
        subtitleSize = sp(13.5f),
        locCorner = d(14f),
        locPadH = d(14f),
        locPadV = d(13f),
        locPinIcon = d(20f),
        locLabelSize = sp(11f),
        locValueSize = sp(15f),
        locChevron = d(22f),
        cardCorner = d(16f),
        cardPad = d(14f),
        cardLabelSize = sp(14f),
        cardPriceSize = sp(21f),
        cardChangeSize = sp(12f),
        pumpBadge = d(26f),
        pumpIcon = d(14f),
        metaSize = sp(12f),
        sectionSize = sp(17f),
        tableCorner = d(14f),
        tableHeaderSize = sp(12f),
        tableRowSize = sp(14f),
        tableRowPadV = d(12f),
        // Side-by-side price cards get too cramped to read below ~340dp.
        stackFuelCards = maxWidth < 340.dp,
        compactTableHeaders = maxWidth < 400.dp,
    )
}

// Rows shown before the "Show all states" toggle kicks in.
private const val NEARBY_PREVIEW = 5

private fun formatRupees(value: Double): String {
    val cents = (value * 100).roundToInt()
    val rupees = cents / 100
    val paise = abs(cents % 100)
    return "₹$rupees.${paise.toString().padStart(2, '0')}"
}

private fun formatChange(value: Double): String {
    val cents = (abs(value) * 100).roundToInt()
    val rupees = cents / 100
    val paise = cents % 100
    return "₹$rupees.${paise.toString().padStart(2, '0')}"
}


@Composable
fun FuelPriceScreen(
    uiState: FuelPriceUiState,
    onBack: () -> Unit,
    onStateSelected: (String) -> Unit,
    onRetry: () -> Unit,
    onRequestLocation: () -> Unit = {},
    onOpenLocationSettings: () -> Unit = {},
    onDismissLocationIssue: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showLocationPicker by remember { mutableStateOf(false) }
    var showAllStates by remember { mutableStateOf(false) }

    uiState.locationIssue?.let { issue ->
        LocationIssueDialog(
            issue = issue,
            onDismiss = onDismissLocationIssue,
            onOpenSettings = onOpenLocationSettings,
            onRetry = onRequestLocation
        )
    }

    if (showLocationPicker) {
        LocationPickerDialog(
            uiState = uiState,
            onDismiss = { showLocationPicker = false },
            onStateSelected = { state ->
                showLocationPicker = false
                onStateSelected(state)
            }
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(FuelColors.Background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        when {
            // Ask before showing anything — rates are only useful once we know where you are.
            uiState.needsLocationPermission -> {
                LocationPermissionGate(
                    onBack = onBack,
                    onAllow = onRequestLocation,
                    onPickManually = { showLocationPicker = true },
                    canPickManually = uiState.availableStates.isNotEmpty(),
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            uiState.isLocating && uiState.current == null -> {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(color = FuelColors.PetrolAccent)
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "Finding your location…",
                        color = FuelColors.TextSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            }

            uiState.isLoading && uiState.current == null -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = FuelColors.PetrolAccent
                )
            }

            uiState.error != null && uiState.current == null -> {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = uiState.error ?: "Couldn't load fuel prices",
                        color = FuelColors.TextSecondary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = onRetry,
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = FuelColors.ActionGreen,
                            contentColor = Color.White
                        )
                    ) {
                        Text("Retry", fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            else -> BoxWithConstraints(Modifier.fillMaxSize()) {
                val d = fuelDimensFor(maxWidth)

                // The whole screen scrolls — header included — so nothing is stranded
                // off-screen on short displays or in landscape.
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = d.contentMaxWidth)
                            .padding(horizontal = d.hPad)
                    ) {
                        Spacer(Modifier.height(12.dp))
                        FuelPriceHeader(dimens = d, onBackClick = onBack)

                        Spacer(Modifier.height(20.dp))
                        LocationSelector(
                            dimens = d,
                            location = uiState.selectedLocationLabel,
                            detected = uiState.locationStatus == FuelLocationStatus.DETECTED,
                            onClick = { showLocationPicker = true }
                        )

                        // Offer GPS whenever the shown state didn't come from a fix.
                        if (uiState.locationStatus != FuelLocationStatus.DETECTED) {
                            UseMyLocationRow(
                                dimens = d,
                                locating = uiState.isLocating,
                                onClick = onRequestLocation
                            )
                        }

                        Spacer(Modifier.height(16.dp))
                        FuelPriceCards(dimens = d, current = uiState.current)

                        uiState.current?.let { price ->
                            Spacer(Modifier.height(10.dp))
                            Text(
                                text = "Updated ${price.updatedAt}   ${price.source}",
                                fontSize = d.metaSize,
                                color = FuelColors.TextSecondary
                            )
                        }

                        val nearby = uiState.nearbyStates
                        if (nearby.isNotEmpty()) {
                            val visible = if (showAllStates) nearby else nearby.take(NEARBY_PREVIEW)

                            Spacer(Modifier.height(24.dp))
                            Text(
                                text = if (showAllStates) "All States" else "Nearby States",
                                fontSize = d.sectionSize,
                                fontWeight = FontWeight.SemiBold,
                                color = FuelColors.TextPrimary
                            )
                            Spacer(Modifier.height(8.dp))
                            NearbyStatesTable(
                                dimens = d,
                                states = visible,
                                onStateClick = onStateSelected
                            )

                            if (nearby.size > NEARBY_PREVIEW) {
                                ShowAllStatesToggle(
                                    dimens = d,
                                    expanded = showAllStates,
                                    totalCount = nearby.size,
                                    onToggle = { showAllStates = !showAllStates }
                                )
                            }
                        }

                        Spacer(Modifier.height(24.dp))
                    }
                }
            }
        }
    }
}

// Side by side normally; stacked on very narrow phones so the rate never truncates.
@Composable
private fun FuelPriceCards(dimens: FuelDimens, current: FuelPrice?) {
    // Falls back to naming the price's scope when the API gives no previous-day
    // rate to compare against.
    val scopeCaption = "Today's rate"

    val petrol: @Composable (Modifier) -> Unit = { mod ->
        FuelPriceCard(
            dimens = dimens,
            label = "Petrol",
            price = formatRupees(current?.petrolPrice ?: 0.0),
            changeValue = current?.petrolDeltaToday,
            fallbackCaption = scopeCaption,
            bgColor = FuelColors.PetrolBg,
            accentColor = FuelColors.PetrolAccent,
            modifier = mod
        )
    }
    val diesel: @Composable (Modifier) -> Unit = { mod ->
        FuelPriceCard(
            dimens = dimens,
            label = "Diesel",
            price = formatRupees(current?.dieselPrice ?: 0.0),
            changeValue = current?.dieselDeltaToday,
            fallbackCaption = scopeCaption,
            bgColor = FuelColors.DieselBg,
            accentColor = FuelColors.DieselAccent,
            modifier = mod
        )
    }

    if (dimens.stackFuelCards) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(dimens.cardGap)
        ) {
            petrol(Modifier.fillMaxWidth())
            diesel(Modifier.fillMaxWidth())
        }
    } else {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(dimens.cardGap)
        ) {
            petrol(Modifier.weight(1f).fillMaxHeight())
            diesel(Modifier.weight(1f).fillMaxHeight())
        }
    }
}

@Composable
private fun ShowAllStatesToggle(
    dimens: FuelDimens,
    expanded: Boolean,
    totalCount: Int,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(dimens.tableCorner))
            .clickable(onClick = onToggle)
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (expanded) "Show fewer states" else "Show all $totalCount states",
            fontSize = dimens.tableRowSize,
            fontWeight = FontWeight.Medium,
            color = FuelColors.ActionGreen
        )
        Spacer(Modifier.width(4.dp))
        Icon(
            imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
            contentDescription = null,
            tint = FuelColors.ActionGreen,
            modifier = Modifier.size(dimens.locChevron)
        )
    }
}

@Composable
private fun FuelPriceHeader(dimens: FuelDimens, onBackClick: () -> Unit) {
    Column {
        IconButton(onClick = onBackClick, modifier = Modifier.size(dimens.backButton)) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = FuelColors.TextPrimary,
                modifier = Modifier.size(dimens.backIcon)
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Fuel Prices",
            fontSize = dimens.titleSize,
            fontWeight = FontWeight.Bold,
            color = FuelColors.TextPrimary
        )
        Text(
            text = "Daily rates near you",
            fontSize = dimens.subtitleSize,
            color = FuelColors.TextSecondary
        )
    }
}

// Explains why the location tap did nothing, and sends the user to the exact place
// they can fix it — the phone's location toggle, or this app's permission page.
@Composable
private fun LocationIssueDialog(
    issue: LocationIssue,
    onDismiss: () -> Unit,
    onOpenSettings: () -> Unit,
    onRetry: () -> Unit
) {
    val title = when (issue) {
        LocationIssue.SERVICE_OFF -> "Location is off"
        LocationIssue.PERMISSION_DENIED -> "Allow location access"
        LocationIssue.NO_FIX -> "Couldn't find your location"
    }
    val message = when (issue) {
        LocationIssue.SERVICE_OFF ->
            "Your phone's location is switched off, so we can't tell which state you're in. Turn it on to see today's rates for your area."
        LocationIssue.PERMISSION_DENIED ->
            "Eva2z needs location access to show fuel rates for the state you're in. You can allow it from app settings."
        LocationIssue.NO_FIX ->
            "We couldn't get a location fix just now. Move to an open area and try again, or pick your state manually."
    }
    val confirmLabel = if (issue == LocationIssue.NO_FIX) "Try again" else "Open settings"

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(FuelColors.PetrolBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = FuelColors.PetrolAccent,
                    modifier = Modifier.size(26.dp)
                )
            }
        },
        title = {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                color = FuelColors.TextPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Text(
                text = message,
                color = FuelColors.TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = { if (issue == LocationIssue.NO_FIX) { onDismiss(); onRetry() } else onOpenSettings() },
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = FuelColors.ActionGreen,
                    contentColor = Color.White
                )
            ) {
                Text(confirmLabel, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Not now", color = FuelColors.TextSecondary)
            }
        },
        containerColor = FuelColors.Background
    )
}

@Composable
private fun LocationPermissionGate(
    onBack: () -> Unit,
    onAllow: () -> Unit,
    onPickManually: () -> Unit,
    canPickManually: Boolean,
    modifier: Modifier = Modifier
) {
    Box(Modifier.fillMaxSize()) {
        IconButton(
            onClick = onBack,
            modifier = Modifier.align(Alignment.TopStart).padding(start = 8.dp, top = 12.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = FuelColors.TextPrimary
            )
        }

        Column(
            modifier = modifier
                .verticalScroll(rememberScrollState())
                .widthIn(max = 420.dp)
                .padding(horizontal = 32.dp, vertical = 56.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(FuelColors.PetrolBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = FuelColors.PetrolAccent,
                    modifier = Modifier.size(34.dp)
                )
            }
            Spacer(Modifier.height(20.dp))
            Text(
                text = "Turn on location",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = FuelColors.TextPrimary,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Allow location access so we can show today's petrol and diesel rates for the state you're in.",
                fontSize = 14.sp,
                color = FuelColors.TextSecondary,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onAllow,
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = FuelColors.ActionGreen,
                    contentColor = Color.White
                )
            ) {
                Text("Allow location", fontWeight = FontWeight.SemiBold)
            }
            if (canPickManually) {
                Spacer(Modifier.height(4.dp))
                TextButton(onClick = onPickManually) {
                    Text("Choose a state instead", color = FuelColors.TextSecondary)
                }
            }
        }
    }
}

@Composable
private fun UseMyLocationRow(dimens: FuelDimens, locating: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .padding(top = 8.dp)
            .clip(RoundedCornerShape(20.dp))
            .clickable(enabled = !locating, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (locating) {
            CircularProgressIndicator(
                modifier = Modifier.size(14.dp),
                color = FuelColors.ActionGreen,
                strokeWidth = 2.dp
            )
        } else {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = null,
                tint = FuelColors.ActionGreen,
                modifier = Modifier.size(16.dp)
            )
        }
        Spacer(Modifier.width(6.dp))
        Text(
            text = if (locating) "Finding your location…" else "Use my current location",
            fontSize = dimens.metaSize,
            fontWeight = FontWeight.Medium,
            color = FuelColors.ActionGreen
        )
    }
}

@Composable
private fun LocationSelector(
    dimens: FuelDimens,
    location: String,
    detected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(dimens.locCorner))
            .background(FuelColors.LocationCardBg)
            .clickable(onClick = onClick)
            .padding(horizontal = dimens.locPadH, vertical = dimens.locPadV),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.LocationOn,
            contentDescription = null,
            tint = FuelColors.PetrolAccent,
            modifier = Modifier.size(dimens.locPinIcon)
        )
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (detected) "Your location" else "Location",
                fontSize = dimens.locLabelSize,
                color = FuelColors.TextSecondary
            )
            Text(
                text = location,
                fontSize = dimens.locValueSize,
                fontWeight = FontWeight.Medium,
                color = FuelColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.width(8.dp))
        Icon(
            imageVector = Icons.Default.KeyboardArrowDown,
            contentDescription = "Change location",
            tint = FuelColors.TextSecondary,
            modifier = Modifier.size(dimens.locChevron)
        )
    }
}

@Composable
private fun FuelPriceCard(
    dimens: FuelDimens,
    label: String,
    price: String,
    changeValue: Double?,
    fallbackCaption: String,
    bgColor: Color,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(dimens.cardCorner))
            .background(bgColor)
            .padding(dimens.cardPad)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                fontSize = dimens.cardLabelSize,
                fontWeight = FontWeight.Medium,
                color = FuelColors.TextPrimary
            )
            Box(
                modifier = Modifier
                    .size(dimens.pumpBadge)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.LocalGasStation,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(dimens.pumpIcon)
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = price,
            fontSize = dimens.cardPriceSize,
            fontWeight = FontWeight.Bold,
            color = FuelColors.TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(4.dp))

        val changeText = when {
            changeValue == null -> fallbackCaption
            changeValue > 0.0 -> "▲ ${formatChange(changeValue)}"
            changeValue < 0.0 -> "▼ ${formatChange(changeValue)}"
            else -> "No Change"
        }
        val changeColor = when {
            changeValue == null -> FuelColors.TextSecondary
            changeValue > 0.0 -> accentColor
            changeValue < 0.0 -> FuelColors.Down
            else -> FuelColors.TextSecondary
        }
        Text(
            text = changeText,
            fontSize = dimens.cardChangeSize,
            fontWeight = FontWeight.Medium,
            color = changeColor,
            maxLines = 1
        )
    }
}

@Composable
private fun NearbyStatesTable(
    dimens: FuelDimens,
    states: List<NearbyStatePrice>,
    onStateClick: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(dimens.tableCorner))
            .background(FuelColors.Background)
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            Text(text = "", modifier = Modifier.weight(1.4f))
            Text(
                text = if (dimens.compactTableHeaders) "P" else "Petrol",
                modifier = Modifier.weight(0.8f),
                textAlign = TextAlign.End,
                fontSize = dimens.tableHeaderSize,
                color = FuelColors.TextSecondary
            )
            Text(
                text = if (dimens.compactTableHeaders) "D" else "Diesel",
                modifier = Modifier.weight(0.8f),
                textAlign = TextAlign.End,
                fontSize = dimens.tableHeaderSize,
                color = FuelColors.TextSecondary
            )
        }

        states.forEachIndexed { index, row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onStateClick(row.stateName) }
                    .padding(vertical = dimens.tableRowPadV),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = row.stateName,
                    modifier = Modifier.weight(1.4f),
                    fontSize = dimens.tableRowSize,
                    color = FuelColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = formatRupees(row.petrolPrice),
                    modifier = Modifier.weight(0.8f),
                    textAlign = TextAlign.End,
                    fontSize = dimens.tableRowSize,
                    color = FuelColors.TextPrimary,
                    maxLines = 1
                )
                Text(
                    text = formatRupees(row.dieselPrice),
                    modifier = Modifier.weight(0.8f),
                    textAlign = TextAlign.End,
                    fontSize = dimens.tableRowSize,
                    color = FuelColors.TextPrimary,
                    maxLines = 1
                )
            }
            if (index != states.lastIndex) {
                HorizontalDivider(color = FuelColors.RowDivider, thickness = 1.dp)
            }
        }
    }
}


@Composable
private fun LocationPickerDialog(
    uiState: FuelPriceUiState,
    onDismiss: () -> Unit,
    onStateSelected: (String) -> Unit
) {
    var query by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close", color = FuelColors.PetrolAccent) }
        },
        title = { Text("Select State", fontWeight = FontWeight.SemiBold) },
        text = {
            Column {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search state", color = FuelColors.TextSecondary) },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = FuelColors.TextSecondary)
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = FuelColors.PetrolAccent,
                        cursorColor = FuelColors.PetrolAccent
                    )
                )
                Spacer(Modifier.height(8.dp))

                val states = uiState.availableStates.filter { it.contains(query, ignoreCase = true) }
                LazyColumn(modifier = Modifier.heightIn(max = 320.dp)) {
                    items(states, key = { it }) { state ->
                        PickerRow(
                            label = state,
                            selected = state == uiState.selectedState,
                            onClick = { onStateSelected(state) }
                        )
                    }
                }
            }
        }
    )
}

@Composable
private fun PickerRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        text = label,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        color = if (selected) FuelColors.PetrolAccent else FuelColors.TextPrimary,
        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
    )
}