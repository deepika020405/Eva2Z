package com.developer.ev2z.ui.screens.DriverScore

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*

// ─────────────────────────────────────────────────────────────────────────────
//  ScoreHeroCard — Clean white card with single green gradient strip
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun ScoreHeroCard(
    vehicle: VehicleScore,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Eva2zRadius.xl))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Eva2zColors.GreenGradientStart,
                        Eva2zColors.GreenGradientEnd
                    )
                )
            )
            .padding(Eva2zSpacing.xl)
    ) {
        // Subtle decorative circle — barely visible
        Box(
            modifier = Modifier
                .size(160.dp)
                .offset(x = 220.dp, y = (-50).dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.06f))
        )

        Column {
            // Period
            Text(
                text = vehicle.period,
                color = Color.White.copy(alpha = 0.70f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.3.sp
            )
            Spacer(Modifier.height(2.dp))

            // Name + plate
            Text(
                text = "${vehicle.vehicleEmoji}  ${vehicle.vehicleName}  ·  ${vehicle.vehiclePlate}",
                color = Color.White.copy(alpha = 0.90f),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(Modifier.height(Eva2zSpacing.lg))

            // Score row
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(Eva2zSpacing.md)
            ) {
                AnimatedScoreNumber(score = vehicle.overallScore)

                Column(modifier = Modifier.padding(bottom = Eva2zSpacing.sm)) {
                    // Grade chip — white semi-transparent
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(Eva2zRadius.pill))
                            .background(Color.White.copy(alpha = 0.20f))
                            .padding(horizontal = Eva2zSpacing.md, vertical = 3.dp)
                    ) {
                        Text(
                            text = vehicle.grade.label.uppercase(),
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    val sign = if (vehicle.monthOverMonthChange >= 0) "+${vehicle.monthOverMonthChange}" else "${vehicle.monthOverMonthChange}"
                    Text(
                        text = "$sign vs last month",
                        color = Color.White.copy(alpha = 0.75f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(Modifier.height(Eva2zSpacing.lg))

            // Progress bar — white track, white fill
            ScoreProgressBar(score = vehicle.overallScore, onGreenBg = true)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  AnimatedScoreNumber
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun AnimatedScoreNumber(score: Int) {
    val animated by animateIntAsState(
        targetValue = score,
        animationSpec = tween(1000, easing = EaseOutCubic),
        label = "score"
    )
    Row(verticalAlignment = Alignment.Bottom) {
        Text(
            text = "$animated",
            color = Color.White,
            fontSize = 60.sp,
            fontWeight = FontWeight.Black,
            lineHeight = 60.sp
        )
        Text(
            text = "/100",
            color = Color.White.copy(alpha = 0.55f),
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  GradePill — minimal, no heavy colors
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun GradePill(
    grade: ScoreGrade,
    modifier: Modifier = Modifier,
    onGreenBg: Boolean = true
) {
    val bg   = if (onGreenBg) Color.White.copy(alpha = 0.20f) else Eva2zColors.gradeColorLight(grade)
    val text = if (onGreenBg) Color.White else Eva2zColors.gradeColor(grade)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(Eva2zRadius.pill))
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 3.dp)
    ) {
        Text(
            text = grade.label.uppercase(),
            color = text,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  ScoreProgressBar — thin, minimal
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun ScoreProgressBar(score: Int, modifier: Modifier = Modifier, onGreenBg: Boolean = true) {
    val progress by animateFloatAsState(
        targetValue = score / 100f,
        animationSpec = tween(900, easing = EaseOutCubic),
        label = "progress"
    )
    val track = if (onGreenBg) Color.White.copy(alpha = 0.25f) else Eva2zColors.Border
    val fill  = if (onGreenBg) Color.White.copy(alpha = 0.90f) else Eva2zColors.Green

    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(5.dp)
                .clip(RoundedCornerShape(Eva2zRadius.pill))
                .background(track)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(Eva2zRadius.pill))
                    .background(fill)
            )
        }
        Spacer(Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            listOf("0", "25", "50", "75", "100").forEach {
                Text(
                    text = it,
                    color = if (onGreenBg) Color.White.copy(alpha = 0.50f) else Eva2zColors.TextMuted,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  MetricCard — flat white, hairline border, NO colored backgrounds
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun MetricCard(
    emoji: String,
    label: String,
    value: String,
    statusText: String,
    statusColor: Color,
    bgColor: Color,           // kept for API compat — ignored, always white
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(Eva2zRadius.lg))
            .background(Eva2zColors.Surface)
            .border(1.dp, Eva2zColors.Border, RoundedCornerShape(Eva2zRadius.lg))
            .padding(Eva2zSpacing.md)
    ) {
        Text(text = emoji, fontSize = 20.sp)
        Spacer(Modifier.height(Eva2zSpacing.sm))
        Text(
            text = label,
            color = Eva2zColors.TextMuted,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.2.sp
        )
        Text(
            text = value,
            color = Eva2zColors.TextPrimary,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = statusText,
            color = statusColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  TripItem — clean list row, white card
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun TripItem(trip: TripScore, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Eva2zRadius.lg))
            .background(Eva2zColors.Surface)
            .border(1.dp, Eva2zColors.Border, RoundedCornerShape(Eva2zRadius.lg))
            .padding(Eva2zSpacing.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon bg — very subtle green tint
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(Eva2zRadius.md))
                .background(Eva2zColors.GreenLight),
            contentAlignment = Alignment.Center
        ) {
            Text(text = trip.emoji, fontSize = 18.sp)
        }
        Spacer(Modifier.width(Eva2zSpacing.md))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = trip.tripName,
                color = Eva2zColors.TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "${trip.timestamp}  ·  ${trip.distanceKm} km",
                color = Eva2zColors.TextMuted,
                fontSize = 11.sp
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "${trip.score}",
                color = Eva2zColors.gradeColor(trip.grade),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "pts",
                color = Eva2zColors.TextMuted,
                fontSize = 9.sp
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  ScoreBreakdownBar — minimal bar, green fill only
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun ScoreBreakdownBar(param: ScoreParameter, modifier: Modifier = Modifier) {
    val progress by animateFloatAsState(
        targetValue = param.score / 100f,
        animationSpec = tween(800, easing = EaseOutCubic),
        label = "bar"
    )
    val fillColor = Eva2zColors.gradeColor(param.grade)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Eva2zRadius.md))
            .background(Eva2zColors.Surface)
            .border(1.dp, Eva2zColors.Border, RoundedCornerShape(Eva2zRadius.md))
            .padding(horizontal = Eva2zSpacing.lg, vertical = Eva2zSpacing.md)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${param.emoji}  ${param.label}",
                color = Eva2zColors.TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "${param.score}",
                color = fillColor,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(5.dp)
                .clip(RoundedCornerShape(Eva2zRadius.pill))
                .background(Eva2zColors.Border)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(Eva2zRadius.pill))
                    .background(fillColor)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  WeeklyTrendChart — clean bar chart, green only
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun WeeklyTrendChart(days: List<DailyScore>, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Eva2zRadius.xl))
            .background(Eva2zColors.Surface)
            .border(1.dp, Eva2zColors.Border, RoundedCornerShape(Eva2zRadius.xl))
            .padding(Eva2zSpacing.lg)
    ) {
        Text(
            text = "Weekly Trend",
            color = Eva2zColors.TextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(Eva2zSpacing.md))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            days.forEach { day ->
                val animated by animateFloatAsState(
                    targetValue = day.score / 100f,
                    animationSpec = tween(700, easing = EaseOutCubic),
                    label = "chart"
                )
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 2.dp),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(animated)
                                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                .background(
                                    if (day.isToday) Eva2zColors.Green
                                    else Eva2zColors.Green.copy(alpha = 0.30f)
                                )
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = day.dayLabel,
                        fontSize = 9.sp,
                        color = if (day.isToday) Eva2zColors.Green else Eva2zColors.TextMuted,
                        fontWeight = if (day.isToday) FontWeight.Bold else FontWeight.Normal,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  VehicleTabPill — minimal tabs
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun VehicleTabPill(
    vehicle: VehicleScore,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(Eva2zRadius.pill))
            .background(if (isSelected) Eva2zColors.Green else Eva2zColors.Surface)
            .border(
                1.dp,
                if (isSelected) Eva2zColors.Green else Eva2zColors.Border,
                RoundedCornerShape(Eva2zRadius.pill)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = Eva2zSpacing.lg, vertical = Eva2zSpacing.sm)
    ) {
        Text(
            text = "${vehicle.vehicleEmoji}  ${vehicle.vehicleName}",
            color = if (isSelected) Color.White else Eva2zColors.TextSecondary,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  SectionTitle — plain, no heavy styling
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun SectionTitle(text: String, subtitle: String? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = Eva2zSpacing.lg, bottom = Eva2zSpacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            color = Eva2zColors.TextPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )
        if (subtitle != null) {
            Spacer(Modifier.width(Eva2zSpacing.sm))
            Text(
                text = subtitle,
                color = Eva2zColors.TextMuted,
                fontSize = 12.sp
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  StatsSummaryRow — inside green hero card
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun StatsSummaryRow(
    totalTrips: Int,
    totalKm: Int,
    safeRatePercent: Int,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier.fillMaxWidth()) {
        listOf(
            "$totalTrips" to "Trips",
            "$totalKm" to "KMs",
            "$safeRatePercent%" to "Safe Rate"
        ).forEachIndexed { i, (value, label) ->
            if (i > 0) {
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(36.dp)
                        .background(Color.White.copy(alpha = 0.25f))
                        .align(Alignment.CenterVertically)
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = value,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = label,
                    color = Color.White.copy(alpha = 0.65f),
                    fontSize = 10.sp
                )
            }
        }
    }
}