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
    )
}

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
    modifier: Modifier = Modifier
) {
    var showLocationPicker by remember { mutableStateOf(false) }

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
            uiState.isLoading && uiState.current == null -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = FuelColors.PetrolAccent
                )
            }

            uiState.error != null && uiState.current == null -> {
                Column(
                    modifier = Modifier.align(Alignment.Center).padding(24.dp),
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
                val contentWidth = Modifier
                    .fillMaxWidth()
                    .widthIn(max = d.contentMaxWidth)

                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Scrollable content — fills available space above the pinned CTA.
                    Column(
                        modifier = contentWidth
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = d.hPad)
                    ) {
                        Spacer(Modifier.height(12.dp))
                        FuelPriceHeader(dimens = d, onBackClick = onBack)

                        Spacer(Modifier.height(20.dp))
                        LocationSelector(
                            dimens = d,
                            location = uiState.selectedLocationLabel,
                            onClick = { showLocationPicker = true }
                        )

                        Spacer(Modifier.height(16.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(IntrinsicSize.Min),
                            horizontalArrangement = Arrangement.spacedBy(d.cardGap)
                        ) {
                            // Falls back to naming the price's scope when the API gives no
                            // previous-day rate to compare against.
                            val scopeCaption = "Today's rate"

                            FuelPriceCard(
                                dimens = d,
                                label = "Petrol",
                                price = formatRupees(uiState.current?.petrolPrice ?: 0.0),
                                changeValue = uiState.current?.petrolDeltaToday,
                                fallbackCaption = scopeCaption,
                                bgColor = FuelColors.PetrolBg,
                                accentColor = FuelColors.PetrolAccent,
                                modifier = Modifier.weight(1f).fillMaxHeight()
                            )
                            FuelPriceCard(
                                dimens = d,
                                label = "Diesel",
                                price = formatRupees(uiState.current?.dieselPrice ?: 0.0),
                                changeValue = uiState.current?.dieselDeltaToday,
                                fallbackCaption = scopeCaption,
                                bgColor = FuelColors.DieselBg,
                                accentColor = FuelColors.DieselAccent,
                                modifier = Modifier.weight(1f).fillMaxHeight()
                            )
                        }

                        uiState.current?.let { price ->
                            Spacer(Modifier.height(10.dp))
                            Text(
                                text = "Updated ${price.updatedAt}   ${price.source}",
                                fontSize = d.metaSize,
                                color = FuelColors.TextSecondary
                            )
                        }

                        if (uiState.nearbyStates.isNotEmpty()) {
                            Spacer(Modifier.height(24.dp))
                            Text(
                                text = "Nearby States",
                                fontSize = d.sectionSize,
                                fontWeight = FontWeight.SemiBold,
                                color = FuelColors.TextPrimary
                            )
                            Spacer(Modifier.height(8.dp))
                            NearbyStatesTable(dimens = d, states = uiState.nearbyStates)
                        }

                        Spacer(Modifier.height(24.dp))
                    }
                }
            }
        }
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

@Composable
private fun LocationSelector(dimens: FuelDimens, location: String, onClick: () -> Unit) {
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
            Text(text = "Location", fontSize = dimens.locLabelSize, color = FuelColors.TextSecondary)
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
private fun NearbyStatesTable(dimens: FuelDimens, states: List<NearbyStatePrice>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(dimens.tableCorner))
            .background(FuelColors.Background)
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            Text(text = "", modifier = Modifier.weight(1.2f))
            Text(
                text = "P",
                modifier = Modifier.weight(0.8f),
                textAlign = TextAlign.End,
                fontSize = dimens.tableHeaderSize,
                color = FuelColors.TextSecondary
            )
            Text(
                text = "D",
                modifier = Modifier.weight(0.8f),
                textAlign = TextAlign.End,
                fontSize = dimens.tableHeaderSize,
                color = FuelColors.TextSecondary
            )
        }

        states.forEachIndexed { index, row ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = dimens.tableRowPadV),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = row.stateName,
                    modifier = Modifier.weight(1.2f),
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