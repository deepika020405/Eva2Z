package com.developer.ev2z.ui.screens.fastag

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
// Toll and WarningAmber live in material-icons-extended (already on the classpath).
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Toll
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * FASTag block for HomeScreen. Dropped into the LazyColumn as its own `item { }`,
 * directly under the e-Challan card since both are "money and compliance".
 *
 * The important product point: this is NOT a recharge button. A recharge button is
 * something a dozen apps already have. What Eva2z has that they do not is the VRN of
 * every tracked vehicle, which means it can show balances across the fleet and flag
 * the low ones before a truck is stopped at a plaza. That is the reason to build this,
 * so the low-balance vehicles are what the section leads with.
 */

/** One row in the fleet FASTag list. */
data class FastagVehicleBalance(
    val vehicleId: String,
    val vehicleNumber: String,
    val issuerName: String?,
    val balancePaise: Long?,
    /** False when the issuer does not share balance — normal, not an error. */
    val balanceAvailable: Boolean = true,
    val lowBalanceThresholdPaise: Long = 50_000L,
    val lastCheckedLabel: String? = null,
    val isStale: Boolean = false
) {
    val isLow: Boolean
        get() = balanceAvailable && balancePaise != null && balancePaise < lowBalanceThresholdPaise
}

@Composable
fun FastagHomeSection(
    vehicles: List<FastagVehicleBalance>,
    onRecharge: (FastagVehicleBalance) -> Unit,
    onViewAll: () -> Unit,
    onLinkFastag: () -> Unit,
    hPad: Dp,
    modifier: Modifier = Modifier,
    /** How many rows to show inline before collapsing behind "View all". */
    maxInlineRows: Int = 3
) {
    Column(modifier = modifier.padding(horizontal = hPad)) {

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("FASTag", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
            if (vehicles.isNotEmpty()) {
                Surface(
                    onClick = onViewAll,
                    shape = RoundedCornerShape(20.dp),
                    color = FastagBlueLight
                ) {
                    Text(
                        "View all",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = FastagBlue,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        if (vehicles.isEmpty()) {
            FastagEmptyCard(onLinkFastag = onLinkFastag)
            return@Column
        }

        // Low balance first — the whole point of the section. Sort and count together
        // so neither runs on every recomposition.
        val (sorted, lowCount) = remember(vehicles) {
            val s = vehicles.sortedWith(
                compareByDescending<FastagVehicleBalance> { it.isLow }
                    .thenBy { it.balancePaise ?: Long.MAX_VALUE }
            )
            s to s.count { it.isLow }
        }

        if (lowCount > 0) {
            LowBalanceStrip(count = lowCount)
            Spacer(Modifier.height(10.dp))
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            sorted.take(maxInlineRows).forEach { v ->
                FastagVehicleRow(vehicle = v, onRecharge = { onRecharge(v) })
            }
        }

        if (sorted.size > maxInlineRows) {
            Spacer(Modifier.height(10.dp))
            Text(
                "View all ${sorted.size} vehicles",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = FastagBlue,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onViewAll)
                    .padding(horizontal = 4.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun LowBalanceStrip(count: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(WarnAmberLight)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.WarningAmber, null, tint = WarnAmber, modifier = Modifier.size(17.dp))
        Spacer(Modifier.width(9.dp))
        Text(
            if (count == 1) "1 vehicle is low on FASTag balance"
            else "$count vehicles are low on FASTag balance",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF8A5C0A)
        )
    }
}

@Composable
private fun FastagVehicleRow(
    vehicle: FastagVehicleBalance,
    onRecharge: () -> Unit
) {
    Card(
        onClick = onRecharge,
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(16.dp), ambientColor = ShadowColor, spotColor = ShadowColor),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(
            1.dp,
            if (vehicle.isLow) WarnAmber.copy(0.42f) else FastagBlue.copy(0.18f)
        )
    ) {
        Box(Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .align(Alignment.TopEnd)
                    .offset(x = 30.dp, y = (-38).dp)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                (if (vehicle.isLow) WarnAmber else FastagBlue).copy(0.14f),
                                Color.Transparent
                            )
                        ),
                        CircleShape
                    )
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 13.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .background(
                            Brush.linearGradient(
                                if (vehicle.isLow)
                                    listOf(WarnAmber.copy(0.20f), WarnAmber.copy(0.08f))
                                else
                                    listOf(FastagBlue.copy(0.18f), FastagBlue.copy(0.07f))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Toll,
                        null,
                        tint = if (vehicle.isLow) WarnAmber else FastagBlue,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(Modifier.width(12.dp))

                Column(Modifier.weight(1f)) {
                    Text(
                        vehicle.vehicleNumber,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(2.dp))
                    // Every row carries its own freshness stamp. A fleet manager
                    // must never have to wonder how old a number is.
                    Text(
                        buildString {
                            append(vehicle.issuerName ?: "No issuer linked")
                            vehicle.lastCheckedLabel?.let { append(" · $it") }
                        },
                        fontSize = 10.sp,
                        color = if (vehicle.isStale) WarnAmber else TextHint,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(Modifier.width(8.dp))

                Column(horizontalAlignment = Alignment.End) {
                    if (vehicle.balanceAvailable && vehicle.balancePaise != null) {
                        Text(
                            formatRupeesShort(vehicle.balancePaise),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (vehicle.isLow) WarnAmber else TextPrimary
                        )
                        if (vehicle.isLow) {
                            Spacer(Modifier.height(2.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(WarnAmber)
                                    .padding(horizontal = 7.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    "Low",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                            }
                        }
                    } else {
                        // Absent balance is designed for, not left as an empty gap.
                        Text(
                            "Balance\nnot shared",
                            fontSize = 10.sp,
                            color = MutedText,
                            lineHeight = 13.sp
                        )
                    }
                }

                Spacer(Modifier.width(10.dp))

                // No clickable here: the whole Card is already the tap target.
                // A nested one with the same action swallows the ripple and emits a
                // duplicate semantics node for TalkBack.
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (vehicle.isLow) WarnAmber else BrandGreen)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        "Recharge",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun FastagEmptyCard(onLinkFastag: () -> Unit) {
    Card(
        onClick = onLinkFastag,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, FastagBlue.copy(0.24f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(15.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(FastagBlue.copy(0.18f), FastagBlue.copy(0.07f))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Toll, null, tint = FastagBlue, modifier = Modifier.size(26.dp))
            }
            Spacer(Modifier.height(12.dp))
            Text(
                "No FASTags linked yet",
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TextPrimary
            )
            Spacer(Modifier.height(6.dp))
            // Sells the outcome the fleet manager cares about, rather than
            // describing an absence.
            Text(
                "Link a FASTag to see live balances across your fleet and get alerted " +
                        "before a vehicle is stopped at a plaza.",
                fontSize = 11.sp,
                color = TextSecondary,
                lineHeight = 16.sp,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(14.dp))
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(FastagBlue)
                    .padding(horizontal = 18.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Add, null, tint = Color.White, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    "Link a FASTag",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}