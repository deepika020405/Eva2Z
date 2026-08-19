package com.developer.ev2z.ui.screens.weather

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Grain
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Umbrella
import androidx.compose.material.icons.filled.WbCloudy
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.developer.ev2z.navigation.PlatformBackHandler
import com.developer.ev2z.theme.AppPageBg
import kotlin.math.roundToInt

// ─── Neutrals ────────────────────────────────────────────────────────────────
private object WeatherColors {
    val Surface = Color(0xFFFFFFFF)
    val TextPrimary = Color(0xFF14181C)
    val TextSecondary = Color(0xFF6B7480)
    val TextMuted = Color(0xFF9AA3AD)
    val Hairline = Color(0xFFEDEFF2)
    val TrackNeutral = Color(0xFFE9ECF0)
    val ActionGreen = Color(0xFF00A859)
    val CardShadow = Color(0x0A0C1B2E)
}

/**
 * A soft, light colourway per weather kind. The whole screen is tinted from this — the
 * page wash, the hero card, the hourly strip — so it reads as today's sky without ever
 * going dark or heavy.
 *
 * [deep] is the only saturated tone; it carries text and icons so contrast stays strong
 * against the pale [heroTop] / [heroBottom] gradient.
 */
private data class WeatherPalette(
    val heroTop: Color,
    val heroBottom: Color,
    val accent: Color,
    val deep: Color,
    val pageWash: Color
)

private fun paletteFor(kind: WeatherKind, isDay: Boolean): WeatherPalette = when {
    !isDay && (kind == WeatherKind.CLEAR || kind == WeatherKind.PARTLY_CLOUDY) -> WeatherPalette(
        heroTop = Color(0xFFEFF1FD), heroBottom = Color(0xFFDDE2F8),
        accent = Color(0xFF5C6BC0), deep = Color(0xFF2A3785), pageWash = Color(0xFFF7F8FE)
    )
    kind == WeatherKind.CLEAR -> WeatherPalette(
        heroTop = Color(0xFFFFF9EA), heroBottom = Color(0xFFFFEAC0),
        accent = Color(0xFFE79A26), deep = Color(0xFF9A5B00), pageWash = Color(0xFFFFFCF4)
    )
    kind == WeatherKind.PARTLY_CLOUDY -> WeatherPalette(
        heroTop = Color(0xFFF0F7FF), heroBottom = Color(0xFFDAEAFD),
        accent = Color(0xFF3D86D4), deep = Color(0xFF17538F), pageWash = Color(0xFFF6FAFF)
    )
    kind == WeatherKind.CLOUDY -> WeatherPalette(
        heroTop = Color(0xFFF6F8FA), heroBottom = Color(0xFFE4EAEF),
        accent = Color(0xFF6E8494), deep = Color(0xFF3C4E5A), pageWash = Color(0xFFF9FBFC)
    )
    kind == WeatherKind.FOG -> WeatherPalette(
        heroTop = Color(0xFFF7F8F9), heroBottom = Color(0xFFE8ECEE),
        accent = Color(0xFF869AA5), deep = Color(0xFF48616D), pageWash = Color(0xFFFAFBFB)
    )
    kind == WeatherKind.DRIZZLE || kind == WeatherKind.RAIN -> WeatherPalette(
        heroTop = Color(0xFFEEF5FE), heroBottom = Color(0xFFD6E8FB),
        accent = Color(0xFF2F80ED), deep = Color(0xFF104A8E), pageWash = Color(0xFFF5F9FE)
    )
    kind == WeatherKind.SNOW -> WeatherPalette(
        heroTop = Color(0xFFF3FBFF), heroBottom = Color(0xFFDEF2FD),
        accent = Color(0xFF3FB6EE), deep = Color(0xFF01699F), pageWash = Color(0xFFF7FCFF)
    )
    kind == WeatherKind.THUNDERSTORM -> WeatherPalette(
        heroTop = Color(0xFFF4F2FC), heroBottom = Color(0xFFE2DEF7),
        accent = Color(0xFF7360C0), deep = Color(0xFF41328A), pageWash = Color(0xFFF9F8FD)
    )
    else -> WeatherPalette(
        heroTop = Color(0xFFF5F7F9), heroBottom = Color(0xFFE5ECF0),
        accent = Color(0xFF72899E), deep = Color(0xFF44596C), pageWash = Color(0xFFF9FBFC)
    )
}

private fun weatherIcon(kind: WeatherKind, isDay: Boolean): ImageVector = when (kind) {
    WeatherKind.CLEAR -> if (isDay) Icons.Default.WbSunny else Icons.Default.DarkMode
    WeatherKind.PARTLY_CLOUDY -> if (isDay) Icons.Default.WbCloudy else Icons.Default.NightsStay
    WeatherKind.CLOUDY -> Icons.Default.Cloud
    WeatherKind.FOG -> Icons.Default.BlurOn
    WeatherKind.DRIZZLE -> Icons.Default.Grain
    WeatherKind.RAIN -> Icons.Default.Umbrella
    WeatherKind.SNOW -> Icons.Default.AcUnit
    WeatherKind.THUNDERSTORM -> Icons.Default.Bolt
    WeatherKind.UNKNOWN -> Icons.Default.Cloud
}

private data class WeatherDimens(
    val hPad: Dp,
    val contentMaxWidth: Dp,
    val backButton: Dp,
    val backIcon: Dp,
    val titleSize: TextUnit,
    val subtitleSize: TextUnit,
    val heroCorner: Dp,
    val heroPad: Dp,
    val heroIcon: Dp,
    val heroGhost: Dp,
    val tempSize: TextUnit,
    val conditionSize: TextUnit,
    val heroMetaSize: TextUnit,
    val sectionSize: TextUnit,
    val cardCorner: Dp,
    val metricValueSize: TextUnit,
    val metricLabelSize: TextUnit,
    val metricIcon: Dp,
    val metricColumns: Int,
    val hourWidth: Dp,
    val hourIcon: Dp,
    val hourLabelSize: TextUnit,
    val hourTempSize: TextUnit,
    val dayRowPadV: Dp,
    val dayIcon: Dp,
    val dayLabelSize: TextUnit,
    val rangeBarWidth: Dp,
    /** Hero and details sit side by side once there's genuinely room for both. */
    val twoPane: Boolean,
)

private fun weatherDimensFor(maxWidth: Dp, maxHeight: Dp): WeatherDimens {
    // 375dp ≈ a standard phone. Clamp so tablets don't balloon and tiny phones stay legible.
    val scale = (maxWidth / 375.dp).coerceIn(0.85f, 1.25f)
    fun sp(base: Float): TextUnit = (base * scale).sp
    fun d(base: Float): Dp = (base * scale).dp

    val isTablet = maxWidth >= 600.dp
    val isCompact = maxWidth < 360.dp
    val twoPane = maxWidth >= 720.dp
    // A landscape phone has very little vertical room — the oversized hero would fill it.
    val shortViewport = maxHeight < 420.dp

    return WeatherDimens(
        hPad = when {
            isTablet -> 28.dp
            isCompact -> 16.dp
            else -> 20.dp
        },
        contentMaxWidth = when {
            twoPane -> 1040.dp
            isTablet -> 680.dp
            else -> Dp.Unspecified
        },
        backButton = d(40f),
        backIcon = d(20f),
        titleSize = sp(if (shortViewport) 20f else 24f),
        subtitleSize = sp(13f),
        heroCorner = d(28f),
        heroPad = d(if (shortViewport) 18f else 24f),
        heroIcon = d(if (shortViewport) 26f else 32f),
        heroGhost = d(if (shortViewport) 130f else 190f),
        tempSize = sp(if (shortViewport) 52f else 72f),
        conditionSize = sp(16f),
        heroMetaSize = sp(12.5f),
        sectionSize = sp(11.5f),
        cardCorner = d(20f),
        metricValueSize = sp(19f),
        metricLabelSize = sp(10.5f),
        metricIcon = d(16f),
        hourWidth = d(64f),
        hourIcon = d(21f),
        hourLabelSize = sp(11f),
        hourTempSize = sp(16f),
        dayRowPadV = d(14f),
        dayIcon = d(22f),
        dayLabelSize = sp(14f),
        rangeBarWidth = d(if (isCompact) 54f else 72f),
        // In two-pane the grid only gets half the width, so it stays at two across.
        metricColumns = when {
            twoPane -> 2
            maxWidth >= 600.dp -> 4
            maxWidth >= 460.dp -> 3
            else -> 2
        },
        twoPane = twoPane,
    )
}

@Composable
fun WeatherScreen(
    uiState: WeatherUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onRefresh: () -> Unit = {},
    onRequestLocation: () -> Unit = {},
    onOpenLocationSettings: () -> Unit = {},
    onDismissLocationIssue: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Device back goes home instead of falling through to the app's exit prompt.
    PlatformBackHandler(enabled = true) { onBack() }

    uiState.locationIssue?.let { issue ->
        WeatherLocationIssueDialog(
            issue = issue,
            onDismiss = onDismissLocationIssue,
            onOpenSettings = onOpenLocationSettings,
            onRetry = onRequestLocation
        )
    }

    val current = uiState.current
    val palette = paletteFor(
        current?.condition?.kind ?: WeatherKind.PARTLY_CLOUDY,
        current?.isDay ?: true
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            // Today's colour bleeds down from the top and resolves into the app's page
            // tint, so the screen has depth without a hard band anywhere.
            .background(
                Brush.verticalGradient(
                    0f to palette.pageWash,
                    0.38f to AppPageBg,
                    1f to AppPageBg
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        when {
            uiState.needsLocation -> WeatherLocationGate(
                palette = palette,
                onBack = onBack,
                onAllow = onRequestLocation,
                modifier = Modifier.align(Alignment.Center)
            )

            current == null && (uiState.isLoading || uiState.isLocating) -> {
                Column(
                    modifier = Modifier.align(Alignment.Center).padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(color = palette.accent, strokeWidth = 2.5.dp)
                    Spacer(Modifier.height(18.dp))
                    Text(
                        text = if (uiState.isLocating) {
                            "Finding your location"
                        } else {
                            "Loading the forecast"
                        },
                        color = WeatherColors.TextSecondary,
                        fontSize = 14.sp,
                        letterSpacing = 0.2.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }

            current == null -> {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = uiState.error ?: "Couldn't load the weather",
                        color = WeatherColors.TextSecondary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(18.dp))
                    Button(
                        onClick = onRetry,
                        colors = ButtonDefaults.buttonColors(containerColor = palette.accent),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Try again", color = Color.White, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = onBack) {
                        Text("Go back", color = WeatherColors.TextSecondary)
                    }
                }
            }

            else -> WeatherContent(
                uiState = uiState,
                current = current,
                palette = palette,
                onBack = onBack,
                onRefresh = onRefresh
            )
        }
    }
}

@Composable
private fun WeatherContent(
    uiState: WeatherUiState,
    current: CurrentWeather,
    palette: WeatherPalette,
    onBack: () -> Unit,
    onRefresh: () -> Unit
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val d = weatherDimensFor(maxWidth, maxHeight)

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            // Generous bottom inset: this screen renders inside the home tab, so the app's
            // own bottom navigation bar sits over the end of the list.
            contentPadding = PaddingValues(bottom = 96.dp)
        ) {
            item {
                WeatherSection(d) {
                    Spacer(Modifier.height(8.dp))
                    WeatherHeader(
                        dimens = d,
                        palette = palette,
                        locationLabel = uiState.locationLabel,
                        refreshing = uiState.isRefreshing,
                        onBack = onBack,
                        onRefresh = onRefresh
                    )
                    Spacer(Modifier.height(20.dp))
                }
            }

            if (d.twoPane) {
                // Wide screens: today's headline and its details share one row instead of
                // leaving a long empty gutter down both sides.
                item {
                    WeatherSection(d) {
                        // Both panes size to their own content and top-align. Stretching
                        // them to a shared intrinsic height would buy nothing here and
                        // makes the row depend on nested intrinsic measurement.
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(18.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(Modifier.weight(1f)) {
                                WeatherHero(d, palette, current)
                            }
                            Column(Modifier.weight(1f)) {
                                SectionLabel("Today's details", d)
                                Spacer(Modifier.height(12.dp))
                                MetricGrid(d, current)
                            }
                        }
                    }
                }
            } else {
                item { WeatherSection(d) { WeatherHero(d, palette, current) } }
            }

            if (uiState.hourly.isNotEmpty()) {
                item {
                    WeatherSection(d, padded = false) {
                        Spacer(Modifier.height(30.dp))
                        SectionLabel(
                            "Next 24 hours", d,
                            Modifier.padding(horizontal = d.hPad)
                        )
                        Spacer(Modifier.height(14.dp))
                        HourlyStrip(d, uiState.hourly)
                    }
                }
            }

            if (!d.twoPane) {
                item {
                    WeatherSection(d) {
                        Spacer(Modifier.height(30.dp))
                        SectionLabel("Today's details", d)
                        Spacer(Modifier.height(14.dp))
                        MetricGrid(d, current)
                    }
                }
            }

            if (uiState.daily.isNotEmpty()) {
                item {
                    WeatherSection(d) {
                        Spacer(Modifier.height(30.dp))
                        SectionLabel("7-day forecast", d)
                        Spacer(Modifier.height(14.dp))
                        DailyList(d, palette, uiState.daily)
                    }
                }
            }

            item {
                WeatherSection(d) {
                    Spacer(Modifier.height(22.dp))
                    Text(
                        text = "Updated ${current.observedAt}  ·  Open-Meteo",
                        fontSize = d.metricLabelSize,
                        letterSpacing = 0.3.sp,
                        color = WeatherColors.TextMuted
                    )
                }
            }
        }
    }
}

/**
 * Wraps a block in the screen's centred content column, so every section lines up on
 * the same edges no matter how wide the display is. [padded] is off for the hourly
 * strip, which bleeds to the screen edge and pads its own scrolling content instead.
 */
@Composable
private fun WeatherSection(
    dimens: WeatherDimens,
    padded: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = dimens.contentMaxWidth)
            .then(if (padded) Modifier.padding(horizontal = dimens.hPad) else Modifier),
        content = content
    )
}

/** Small tracked caps — quieter than a heading, which lets the data carry the page. */
@Composable
private fun SectionLabel(text: String, dimens: WeatherDimens, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        fontSize = dimens.sectionSize,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.4.sp,
        color = WeatherColors.TextMuted,
        modifier = modifier
    )
}

/** Big numeral with the degree mark set small and raised, the way print sets it. */
@Composable
private fun TemperatureText(
    value: Double,
    size: TextUnit,
    color: Color,
    weight: FontWeight,
    modifier: Modifier = Modifier
) {
    val whole = formatTemperature(value).removeSuffix("°")
    Text(
        text = buildAnnotatedString {
            append(whole)
            withStyle(
                SpanStyle(
                    fontSize = size * 0.42f,
                    baselineShift = BaselineShift(0.42f),
                    fontWeight = FontWeight.Medium
                )
            ) { append("°") }
        },
        fontSize = size,
        fontWeight = weight,
        letterSpacing = (-1.5).sp,
        color = color,
        maxLines = 1,
        modifier = modifier
    )
}

@Composable
private fun WeatherHeader(
    dimens: WeatherDimens,
    palette: WeatherPalette,
    locationLabel: String,
    refreshing: Boolean,
    onBack: () -> Unit,
    onRefresh: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircleAction(
            size = dimens.backButton,
            iconSize = dimens.backIcon,
            icon = Icons.AutoMirrored.Filled.ArrowBack,
            description = "Back",
            tint = WeatherColors.TextPrimary,
            onClick = onBack
        )

        Column(modifier = Modifier.weight(1f).padding(horizontal = 14.dp)) {
            Text(
                text = "Weather",
                fontSize = dimens.titleSize,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.4).sp,
                color = WeatherColors.TextPrimary,
                maxLines = 1
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = palette.accent,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = locationLabel.ifBlank { "Your location" },
                    fontSize = dimens.subtitleSize,
                    letterSpacing = 0.1.sp,
                    color = WeatherColors.TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        if (refreshing) {
            Box(
                modifier = Modifier.size(dimens.backButton),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(dimens.backIcon),
                    color = palette.accent,
                    strokeWidth = 2.dp
                )
            }
        } else {
            CircleAction(
                size = dimens.backButton,
                iconSize = dimens.backIcon,
                icon = Icons.Default.Refresh,
                description = "Refresh weather",
                tint = WeatherColors.TextPrimary,
                onClick = onRefresh
            )
        }
    }
}

/** White disc with a hairline edge — reads as a control without shouting. */
@Composable
private fun CircleAction(
    size: Dp,
    iconSize: Dp,
    icon: ImageVector,
    description: String,
    tint: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(size)
            .shadow(3.dp, CircleShape, spotColor = WeatherColors.CardShadow)
            .clip(CircleShape)
            .background(WeatherColors.Surface)
            .border(1.dp, WeatherColors.Hairline, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        IconButton(onClick = onClick, modifier = Modifier.size(size)) {
            Icon(
                imageVector = icon,
                contentDescription = description,
                tint = tint,
                modifier = Modifier.size(iconSize)
            )
        }
    }
}

@Composable
private fun WeatherHero(
    dimens: WeatherDimens,
    palette: WeatherPalette,
    current: CurrentWeather
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 18.dp,
                shape = RoundedCornerShape(dimens.heroCorner),
                ambientColor = palette.deep.copy(alpha = 0.35f),
                spotColor = palette.deep.copy(alpha = 0.35f)
            )
            .clip(RoundedCornerShape(dimens.heroCorner))
            .background(Brush.verticalGradient(listOf(palette.heroTop, palette.heroBottom)))
            .clipToBounds()
    ) {
        // A soft bloom in the top-right corner gives the flat gradient somewhere to
        // catch the light, so the card reads as a surface rather than a swatch.
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(dimens.heroGhost)
                .offset(x = dimens.heroGhost * 0.28f, y = -dimens.heroGhost * 0.34f)
                .background(
                    Brush.radialGradient(
                        listOf(Color.White.copy(alpha = 0.72f), Color.White.copy(alpha = 0f))
                    ),
                    CircleShape
                )
        )
        // The condition icon, blown up and ghosted, bleeding off the corner.
        Icon(
            imageVector = weatherIcon(current.condition.kind, current.isDay),
            contentDescription = null,
            tint = palette.accent,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(dimens.heroGhost)
                .offset(x = dimens.heroGhost * 0.24f, y = -dimens.heroGhost * 0.20f)
                .alpha(0.13f)
        )

        Column(modifier = Modifier.padding(dimens.heroPad)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = weatherIcon(current.condition.kind, current.isDay),
                    contentDescription = current.condition.label,
                    tint = palette.accent,
                    modifier = Modifier.size(dimens.heroIcon)
                )
                Spacer(Modifier.width(9.dp))
                Text(
                    text = current.condition.label,
                    fontSize = dimens.conditionSize,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.1.sp,
                    color = palette.deep,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.height(6.dp))
            // Light weight at this size is what separates a display numeral from a
            // heading — bold would read as a price tag.
            TemperatureText(
                value = current.temperature,
                size = dimens.tempSize,
                color = WeatherColors.TextPrimary,
                weight = FontWeight.Light
            )

            current.feelsLike?.let {
                Text(
                    text = "Feels like ${formatTemperature(it)}",
                    fontSize = dimens.heroMetaSize,
                    letterSpacing = 0.2.sp,
                    color = WeatherColors.TextSecondary
                )
            }

            val stats = buildList {
                current.todayHigh?.let { add("High" to formatTemperature(it)) }
                current.todayLow?.let { add("Low" to formatTemperature(it)) }
                current.rainChanceToday?.let { add("Rain" to "$it%") }
            }
            if (stats.isNotEmpty()) {
                Spacer(Modifier.height(18.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(palette.deep.copy(alpha = 0.12f))
                )
                Spacer(Modifier.height(14.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    stats.forEachIndexed { index, (label, value) ->
                        if (index > 0) {
                            Box(
                                Modifier
                                    .height(26.dp)
                                    .width(1.dp)
                                    .background(palette.deep.copy(alpha = 0.10f))
                            )
                        }
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = if (index == 0) {
                                Alignment.Start
                            } else {
                                Alignment.CenterHorizontally
                            }
                        ) {
                            Text(
                                text = label.uppercase(),
                                fontSize = dimens.metricLabelSize,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.1.sp,
                                color = palette.deep.copy(alpha = 0.55f)
                            )
                            Spacer(Modifier.height(3.dp))
                            Text(
                                text = value,
                                fontSize = dimens.heroMetaSize * 1.25f,
                                fontWeight = FontWeight.SemiBold,
                                color = palette.deep
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HourlyStrip(dimens: WeatherDimens, hours: List<HourlyForecast>) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = dimens.hPad),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(hours) { hour ->
            // Each card is tinted by its own hour's conditions, so the strip shows the
            // day's shape at a glance — warm now, blue when the rain arrives.
            val p = paletteFor(hour.condition.kind, hour.isDay)
            Column(
                modifier = Modifier
                    .width(dimens.hourWidth)
                    .shadow(
                        if (hour.isNow) 8.dp else 2.dp,
                        RoundedCornerShape(dimens.cardCorner),
                        spotColor = if (hour.isNow) {
                            p.deep.copy(alpha = 0.35f)
                        } else {
                            WeatherColors.CardShadow
                        }
                    )
                    .clip(RoundedCornerShape(dimens.cardCorner))
                    .background(
                        if (hour.isNow) {
                            Brush.verticalGradient(listOf(p.heroTop, p.heroBottom))
                        } else {
                            Brush.verticalGradient(
                                listOf(WeatherColors.Surface, WeatherColors.Surface)
                            )
                        }
                    )
                    .border(
                        1.dp,
                        if (hour.isNow) Color.Transparent else WeatherColors.Hairline,
                        RoundedCornerShape(dimens.cardCorner)
                    )
                    .padding(vertical = 14.dp, horizontal = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = hour.label.uppercase(),
                    fontSize = dimens.hourLabelSize,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.6.sp,
                    color = if (hour.isNow) p.deep else WeatherColors.TextMuted,
                    maxLines = 1
                )
                Spacer(Modifier.height(11.dp))
                Icon(
                    imageVector = weatherIcon(hour.condition.kind, hour.isDay),
                    contentDescription = hour.condition.label,
                    tint = p.accent,
                    modifier = Modifier.size(dimens.hourIcon)
                )
                Spacer(Modifier.height(11.dp))
                TemperatureText(
                    value = hour.temperature,
                    size = dimens.hourTempSize,
                    color = WeatherColors.TextPrimary,
                    weight = FontWeight.SemiBold
                )
                // Only worth the extra height when rain is actually plausible.
                if ((hour.rainChance ?: 0) > 0) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "${hour.rainChance}%",
                        fontSize = dimens.hourLabelSize,
                        fontWeight = FontWeight.Medium,
                        color = RainBlue,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

private val RainBlue = Color(0xFF2F80ED)

// Each reading gets its own soft tint — the grid stays scannable because colour, not
// just position, tells the tiles apart.
private data class Metric(
    val icon: ImageVector,
    val label: String,
    val value: String,
    val tint: Color
)

@Composable
private fun MetricGrid(dimens: WeatherDimens, current: CurrentWeather) {
    val metrics = buildList {
        current.feelsLike?.let {
            add(Metric(Icons.Default.Thermostat, "Feels like", formatTemperature(it), Color(0xFFF4703C)))
        }
        current.humidity?.let {
            add(Metric(Icons.Default.Opacity, "Humidity", "$it%", Color(0xFF17A9C8)))
        }
        current.windSpeed?.let {
            add(Metric(Icons.Default.Air, "Wind", "${formatOneDecimal(it)} km/h", Color(0xFF13A08F)))
        }
        current.rainChanceToday?.let {
            add(Metric(Icons.Default.Umbrella, "Rain chance", "$it%", Color(0xFF2F80ED)))
        }
        current.uvIndexMax?.let {
            add(Metric(Icons.Default.LightMode, "UV index", formatOneDecimal(it), Color(0xFFE59A2F)))
        }
        current.pressure?.let {
            add(Metric(Icons.Default.Compress, "Pressure", "${it.roundToInt()} hPa", Color(0xFF7360C0)))
        }
        current.sunrise?.takeIf { it.isNotBlank() }?.let {
            add(Metric(Icons.Default.WbTwilight, "Sunrise", it, Color(0xFFF08C00)))
        }
        current.sunset?.takeIf { it.isNotBlank() }?.let {
            add(Metric(Icons.Default.NightsStay, "Sunset", it, Color(0xFFD94E8C)))
        }
    }
    if (metrics.isEmpty()) return

    // Chunked Rows rather than a LazyVerticalGrid — this sits inside a LazyColumn,
    // where a nested lazy grid on the same axis isn't allowed.
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        metrics.chunked(dimens.metricColumns).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                row.forEach { metric ->
                    MetricTile(dimens, metric, Modifier.weight(1f).fillMaxHeight())
                }
                // Keeps a short final row aligned with the ones above it.
                repeat(dimens.metricColumns - row.size) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun MetricTile(dimens: WeatherDimens, metric: Metric, modifier: Modifier = Modifier) {
    // White card with a hairline edge and its colour carried by the icon alone — a wall
    // of tinted blocks competes with the hero for attention.
    Column(
        modifier = modifier
            .shadow(3.dp, RoundedCornerShape(dimens.cardCorner), spotColor = WeatherColors.CardShadow)
            .clip(RoundedCornerShape(dimens.cardCorner))
            .background(WeatherColors.Surface)
            .border(1.dp, WeatherColors.Hairline, RoundedCornerShape(dimens.cardCorner))
            .padding(horizontal = 14.dp, vertical = 13.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(dimens.metricIcon + 12.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(metric.tint.copy(alpha = 0.11f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = metric.icon,
                    contentDescription = null,
                    tint = metric.tint,
                    modifier = Modifier.size(dimens.metricIcon - 2.dp)
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = metric.label.uppercase(),
                fontSize = dimens.metricLabelSize,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.9.sp,
                color = WeatherColors.TextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = metric.value,
            fontSize = dimens.metricValueSize,
            fontWeight = FontWeight.Medium,
            letterSpacing = (-0.3).sp,
            color = WeatherColors.TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun DailyList(
    dimens: WeatherDimens,
    palette: WeatherPalette,
    days: List<DailyForecast>
) {
    // The range bars are only comparable if every row is measured against the same
    // scale, so the week's own extremes define the track.
    val weekLow = days.mapNotNull { it.low }.minOrNull()
    val weekHigh = days.mapNotNull { it.high }.maxOrNull()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(5.dp, RoundedCornerShape(dimens.cardCorner), spotColor = WeatherColors.CardShadow)
            .clip(RoundedCornerShape(dimens.cardCorner))
            .background(WeatherColors.Surface)
            .border(1.dp, WeatherColors.Hairline, RoundedCornerShape(dimens.cardCorner))
    ) {
        days.forEachIndexed { index, day ->
            if (index > 0) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp)
                        .height(1.dp)
                        .background(WeatherColors.Hairline)
                )
            }
            val p = paletteFor(day.condition.kind, isDay = true)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (day.isToday) palette.heroTop.copy(alpha = 0.45f) else Color.Transparent
                    )
                    .padding(horizontal = 14.dp, vertical = dimens.dayRowPadV),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = day.label,
                        fontSize = dimens.dayLabelSize,
                        fontWeight = if (day.isToday) FontWeight.Bold else FontWeight.Medium,
                        letterSpacing = 0.1.sp,
                        color = if (day.isToday) palette.deep else WeatherColors.TextPrimary,
                        maxLines = 1
                    )
                    Text(
                        text = day.dateLabel,
                        fontSize = dimens.metricLabelSize,
                        letterSpacing = 0.3.sp,
                        color = WeatherColors.TextMuted,
                        maxLines = 1
                    )
                }

                Icon(
                    imageVector = weatherIcon(day.condition.kind, isDay = true),
                    contentDescription = day.condition.label,
                    tint = p.accent,
                    modifier = Modifier.size(dimens.dayIcon)
                )

                Box(modifier = Modifier.width(40.dp), contentAlignment = Alignment.CenterEnd) {
                    if ((day.rainChance ?: 0) > 0) {
                        Text(
                            text = "${day.rainChance}%",
                            fontSize = dimens.metricLabelSize,
                            fontWeight = FontWeight.SemiBold,
                            color = RainBlue,
                            maxLines = 1
                        )
                    }
                }

                Spacer(Modifier.width(10.dp))
                Text(
                    text = day.low?.let { formatTemperature(it) } ?: "—",
                    fontSize = dimens.dayLabelSize,
                    fontWeight = FontWeight.Normal,
                    color = WeatherColors.TextMuted,
                    maxLines = 1
                )
                Spacer(Modifier.width(8.dp))
                TemperatureRangeBar(
                    low = day.low,
                    high = day.high,
                    weekLow = weekLow,
                    weekHigh = weekHigh,
                    accent = p.accent,
                    width = dimens.rangeBarWidth
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = day.high?.let { formatTemperature(it) } ?: "—",
                    fontSize = dimens.dayLabelSize,
                    fontWeight = FontWeight.Bold,
                    color = WeatherColors.TextPrimary,
                    maxLines = 1
                )
            }
        }
    }
}

/**
 * Where this day's low–high sits inside the whole week's span. Turns seven pairs of
 * numbers into a shape you can read down the column without doing arithmetic.
 */
@Composable
private fun TemperatureRangeBar(
    low: Double?,
    high: Double?,
    weekLow: Double?,
    weekHigh: Double?,
    accent: Color,
    width: Dp
) {
    val track = Modifier
        .width(width)
        .height(5.dp)
        .clip(CircleShape)
        .background(WeatherColors.TrackNeutral)

    // A flat week (or a day with no reading) has no meaningful span to plot — show the
    // empty track rather than a bar that implies a range we don't have.
    val span = if (weekLow != null && weekHigh != null) weekHigh - weekLow else 0.0
    if (low == null || high == null || weekLow == null || span <= 0.0) {
        Box(track)
        return
    }

    val startFrac = ((low - weekLow) / span).coerceIn(0.0, 1.0).toFloat()
    val endFrac = ((high - weekLow) / span).coerceIn(0.0, 1.0).toFloat()
    // Weights must stay strictly positive, and a same-low-as-high day still needs a
    // visible dot rather than a zero-width sliver.
    val lead = startFrac.coerceAtLeast(0.0001f)
    val fill = (endFrac - startFrac).coerceAtLeast(0.12f)
    val trail = (1f - startFrac - fill).coerceAtLeast(0.0001f)

    Row(modifier = track) {
        Spacer(Modifier.weight(lead))
        Box(
            Modifier
                .weight(fill)
                .fillMaxHeight()
                .clip(CircleShape)
                .background(
                    Brush.horizontalGradient(listOf(accent.copy(alpha = 0.45f), accent))
                )
        )
        Spacer(Modifier.weight(trail))
    }
}

@Composable
private fun WeatherLocationGate(
    palette: WeatherPalette,
    onBack: () -> Unit,
    onAllow: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(30.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(84.dp)
                .shadow(14.dp, CircleShape, spotColor = palette.deep.copy(alpha = 0.3f))
                .clip(CircleShape)
                .background(Brush.verticalGradient(listOf(palette.heroTop, palette.heroBottom))),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = null,
                tint = palette.accent,
                modifier = Modifier.size(36.dp)
            )
        }
        Spacer(Modifier.height(22.dp))
        Text(
            text = "Weather for where you are",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.3).sp,
            color = WeatherColors.TextPrimary,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = "Allow location so we can show current conditions and the forecast for your area.",
            fontSize = 14.sp,
            letterSpacing = 0.1.sp,
            color = WeatherColors.TextSecondary,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(26.dp))
        Button(
            onClick = onAllow,
            colors = ButtonDefaults.buttonColors(containerColor = palette.accent),
            shape = RoundedCornerShape(15.dp),
            contentPadding = PaddingValues(horizontal = 26.dp, vertical = 13.dp)
        ) {
            Text(
                "Allow location",
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.2.sp
            )
        }
        Spacer(Modifier.height(6.dp))
        TextButton(onClick = onBack) {
            Text("Not now", color = WeatherColors.TextSecondary)
        }
    }
}

@Composable
private fun WeatherLocationIssueDialog(
    issue: WeatherLocationIssue,
    onDismiss: () -> Unit,
    onOpenSettings: () -> Unit,
    onRetry: () -> Unit
) {
    val title = when (issue) {
        WeatherLocationIssue.SERVICE_OFF -> "Location is off"
        WeatherLocationIssue.PERMISSION_DENIED -> "Allow location access"
        WeatherLocationIssue.NO_FIX -> "Couldn't find your location"
    }
    val message = when (issue) {
        WeatherLocationIssue.SERVICE_OFF ->
            "Your phone's location is switched off, so we can't tell where to show the weather for. Turn it on to see local conditions."
        WeatherLocationIssue.PERMISSION_DENIED ->
            "Eva2z needs location access to show the weather for your area. You can allow it from app settings."
        WeatherLocationIssue.NO_FIX ->
            "We couldn't get a location fix just now. Move to an open area and try again."
    }
    val confirmLabel = if (issue == WeatherLocationIssue.NO_FIX) "Try again" else "Open settings"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                title,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.2).sp,
                color = WeatherColors.TextPrimary
            )
        },
        text = {
            Text(message, fontSize = 14.sp, color = WeatherColors.TextSecondary)
        },
        confirmButton = {
            Button(
                onClick = {
                    // onOpenSettings clears the issue itself — it needs to still be set
                    // to know which settings page to open, so it isn't dismissed first.
                    if (issue == WeatherLocationIssue.NO_FIX) {
                        onDismiss()
                        onRetry()
                    } else {
                        onOpenSettings()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = WeatherColors.ActionGreen),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(confirmLabel, color = Color.White, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = WeatherColors.TextSecondary)
            }
        },
        containerColor = WeatherColors.Surface,
        shape = RoundedCornerShape(22.dp)
    )
}