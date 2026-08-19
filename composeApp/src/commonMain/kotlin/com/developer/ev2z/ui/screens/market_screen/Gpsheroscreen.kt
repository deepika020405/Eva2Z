package com.developer.ev2z.ui.screens.market_screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import com.developer.ev2z.util.VideoPlayerView
import com.developer.ev2z.theme.AppPageBg

// ── Neutrals ───────────────────────────────────────────────────────────────
private val Ink       = Color(0xFF16201A)
private val InkMid    = Color(0xFF5A6B5E)
private val InkLight  = Color(0xFF95A399)
private val Hairline  = Color(0xFFE7EEE8)
private val CardShadow = Color(0x0F14301F)
private val Hero_Bg   = AppPageBg

/**
 * One soft colourway per accent. [soft] backs an icon tile, [tint] carries the icon and
 * any figure that needs to stand out. Sections rotate through these so the page reads as
 * lightly colourful rather than a wall of one green.
 */
private data class Accent(val tint: Color, val soft: Color)

private val AGreen  = Accent(Color(0xFF00A859), Color(0xFFE9F7F0))
private val ABlue   = Accent(Color(0xFF2F80ED), Color(0xFFEAF2FD))
private val AAmber  = Accent(Color(0xFFE79A26), Color(0xFFFDF4E4))
private val AViolet = Accent(Color(0xFF7360C0), Color(0xFFF1EFFB))
private val ATeal   = Accent(Color(0xFF13A08F), Color(0xFFE5F6F3))
private val ARose   = Accent(Color(0xFFD94E8C), Color(0xFFFDECF3))

private val Brand     = AGreen.tint
private val BrandDeep = Color(0xFF00713C)
private val BrandSoft = AGreen.soft
private val Gold      = Color(0xFFF4B740)

/** Brand gradient used on the two call-to-action buttons. */
private val CtaBrush = Brush.horizontalGradient(listOf(Color(0xFF00B863), Color(0xFF00874A)))

// ── Responsive metrics ─────────────────────────────────────────────────────

private data class HeroDimens(
    val hPad: Dp,
    val contentMaxWidth: Dp,
    val sectionGap: Dp,
    val cardCorner: Dp,
    val cardPad: Dp,
    val topTitle: TextUnit,
    val microLabel: TextUnit,
    val sectionTitle: TextUnit,
    val headline: TextUnit,
    val headlineLine: TextUnit,
    val body: TextUnit,
    val ctaHeight: Dp,
    val ctaText: TextUnit,
    val statValue: TextUnit,
    val statLabel: TextUnit,
    val iconTile: Dp,
    val iconGlyph: Dp,
    val featureLabel: TextUnit,
    val featureColumns: Int,
    val whyTitle: TextUnit,
    val whyBody: TextUnit,
    val bloom: Dp,
    /** Benefit cards pair up once each one still gets a comfortable measure. */
    val whyTwoColumn: Boolean,
)

private fun heroDimensFor(maxWidth: Dp): HeroDimens {
    // 375dp ≈ a standard phone. Clamped so tablets don't balloon and small phones stay legible.
    val scale = (maxWidth / 375.dp).coerceIn(0.86f, 1.24f)
    fun sp(base: Float): TextUnit = (base * scale).sp
    fun d(base: Float): Dp = (base * scale).dp

    val isTablet = maxWidth >= 600.dp
    val isCompact = maxWidth < 360.dp

    return HeroDimens(
        hPad = when {
            isTablet -> 28.dp
            isCompact -> 14.dp
            else -> 18.dp
        },
        contentMaxWidth = if (isTablet) 760.dp else Dp.Unspecified,
        sectionGap = d(22f),
        cardCorner = d(24f),
        cardPad = d(if (isCompact) 16f else 20f),
        topTitle = sp(15f),
        microLabel = sp(10f),
        sectionTitle = sp(17f),
        // The headline is the one thing allowed to grow properly on a big screen.
        headline = sp(if (isCompact) 26f else 31f),
        headlineLine = sp(if (isCompact) 33f else 39f),
        body = sp(13f),
        ctaHeight = d(56f),
        ctaText = sp(15.5f),
        statValue = sp(19f),
        statLabel = sp(9.5f),
        iconTile = d(48f),
        iconGlyph = d(22f),
        featureLabel = sp(9.5f),
        // 10 features divide cleanly by 5; only a very narrow screen drops to 4.
        featureColumns = if (maxWidth < 340.dp) 4 else 5,
        whyTitle = sp(14.5f),
        whyBody = sp(12.5f),
        bloom = d(230f),
        whyTwoColumn = maxWidth >= 700.dp,
    )
}

class GpsHeroScreen(
    val wiredProduct:    ShopProduct?,
    val wirelessProduct: ShopProduct?,
    val onAddToCart:     (ShopProduct, String, String) -> Unit
) : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.current

        val openFlow: () -> Unit = {
            navigator?.push(
                GpsTrackerFlowScreen(
                    wiredProduct    = wiredProduct,
                    wirelessProduct = wirelessProduct,
                    onAddToCart     = onAddToCart
                )
            )
        }

        BoxWithConstraints(modifier = Modifier.fillMaxSize().background(Hero_Bg)) {
            val d = heroDimensFor(maxWidth)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                HeroTopBar(d, onBack = { navigator?.pop() })

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = d.contentMaxWidth)
                        .align(Alignment.CenterHorizontally)
                ) {
                    Spacer(Modifier.height(d.sectionGap * 0.7f))
                    HeroCard(d, onBuy = openFlow)

                    Spacer(Modifier.height(d.sectionGap * 0.6f))
                    StatsStrip(d)

                    Spacer(Modifier.height(d.sectionGap))
                    VideoSection(d)

                    Spacer(Modifier.height(d.sectionGap))
                    FeaturesSection(d)

                    Spacer(Modifier.height(d.sectionGap))
                    WhySection(d)

                    Spacer(Modifier.height(d.sectionGap))
                    ClosingCta(d, onBuy = openFlow)

                    Spacer(Modifier.height(32.dp))
                }
            }
        }
    }
}

// ═════════════════════════════ TOP BAR ═════════════════════════════

@Composable
private fun HeroTopBar(d: HeroDimens, onBack: () -> Unit) {
    Surface(color = Color.White, shadowElevation = 0.dp, modifier = Modifier.fillMaxWidth()) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = d.hPad, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SquareIconButton(
                    icon = Icons.Default.ArrowBack,
                    description = "Back",
                    tint = Ink,
                    background = Color.White,
                    onClick = onBack
                )

                Column(
                    modifier = Modifier.weight(1f).padding(horizontal = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Eva2z GPS Tracker",
                        fontSize = d.topTitle,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.2).sp,
                        color = Ink,
                        maxLines = 1
                    )
                    Spacer(Modifier.height(2.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(1.5.dp)
                    ) {
                        repeat(5) {
                            Icon(
                                Icons.Default.Star, null,
                                tint = Gold,
                                modifier = Modifier.size(9.dp)
                            )
                        }
                        Text(
                            "  4.9 · 6,988 reviews",
                            fontSize = d.microLabel,
                            letterSpacing = 0.2.sp,
                            color = InkLight,
                            maxLines = 1
                        )
                    }
                }

                Box {
                    SquareIconButton(
                        icon = Icons.Default.ShoppingBag,
                        description = null,
                        tint = Brand,
                        background = BrandSoft,
                        onClick = null
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 2.dp, y = (-2).dp)
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Brand)
                            .border(1.5.dp, Color.White, CircleShape)
                    )
                }
            }
            HorizontalDivider(color = Hairline, thickness = 1.dp)
        }
    }
}

@Composable
private fun SquareIconButton(
    icon: ImageVector,
    description: String?,
    tint: Color,
    background: Color,
    onClick: (() -> Unit)?
) {
    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(RoundedCornerShape(13.dp))
            .background(background)
            .border(1.dp, Hairline, RoundedCornerShape(13.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, description, tint = tint, modifier = Modifier.size(19.dp))
    }
}

// ═════════════════════════════ HERO CARD ═════════════════════════════

@Composable
private fun HeroCard(d: HeroDimens, onBuy: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = d.hPad)
            .shadow(
                18.dp,
                RoundedCornerShape(d.cardCorner),
                ambientColor = BrandDeep.copy(alpha = 0.30f),
                spotColor = BrandDeep.copy(alpha = 0.30f)
            )
            .clip(RoundedCornerShape(d.cardCorner))
            .background(Brush.verticalGradient(listOf(Color.White, BrandSoft)))
            .clipToBounds()
    ) {
        // A soft bloom plus an oversized ghosted shield give the card depth, so it reads
        // as a surface catching light rather than a flat panel with a border.
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(d.bloom)
                .offset(x = d.bloom * 0.30f, y = -d.bloom * 0.36f)
                .background(
                    Brush.radialGradient(
                        listOf(Color.White.copy(alpha = 0.9f), Color.White.copy(alpha = 0f))
                    ),
                    CircleShape
                )
        )
        Icon(
            Icons.Default.Shield, null,
            tint = Brand,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(d.bloom)
                .offset(x = d.bloom * 0.26f, y = d.bloom * 0.26f)
                .alpha(0.07f)
        )

        Column(modifier = Modifier.padding(d.cardPad + 2.dp)) {
            PillBadge(
                text = "INDIA'S #1 GPS SECURITY PLATFORM",
                accent = AGreen,
                fontSize = d.microLabel * 0.92f,
                withDot = true
            )

            Spacer(Modifier.height(18.dp))

            Text(
                "Protect Your\nCar & Bike",
                fontSize = d.headline,
                fontWeight = FontWeight.Black,
                letterSpacing = (-0.9).sp,
                color = Ink,
                lineHeight = d.headlineLine
            )
            Text(
                "From Theft — Live",
                fontSize = d.headline,
                fontWeight = FontWeight.Black,
                letterSpacing = (-0.9).sp,
                color = Brand,
                lineHeight = d.headlineLine
            )

            Spacer(Modifier.height(11.dp))
            Text(
                "Real-time GPS tracking, engine cut-off & anti-theft alerts. Trusted by 5,000+ owners across India.",
                fontSize = d.body,
                color = InkMid,
                lineHeight = d.body * 1.55f
            )

            Spacer(Modifier.height(22.dp))
            GradientCta(d, label = "Buy Eva2z GPS", showArrow = true, onClick = onBuy)

            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Overlapping avatars, each ringed in white so they read as a stack.
                Box(modifier = Modifier.width(70.dp).height(30.dp)) {
                    listOf(
                        AGreen.tint to "R",
                        ATeal.tint to "A",
                        ABlue.tint to "P"
                    ).forEachIndexed { i, (color, label) ->
                        Box(
                            modifier = Modifier
                                .offset(x = (i * 19).dp)
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(2.dp, Color.White, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                label,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        "5,000+ happy owners",
                        fontSize = d.body * 0.94f,
                        fontWeight = FontWeight.SemiBold,
                        color = Ink
                    )
                    Text(
                        "across India",
                        fontSize = d.microLabel,
                        letterSpacing = 0.2.sp,
                        color = InkLight
                    )
                }
            }
        }
    }
}

@Composable
private fun GradientCta(
    d: HeroDimens,
    label: String,
    showArrow: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(d.ctaHeight)
            .shadow(10.dp, shape, spotColor = BrandDeep.copy(alpha = 0.45f))
            .clip(shape)
            .background(CtaBrush),
        shape = shape,
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
        elevation = ButtonDefaults.buttonElevation(0.dp)
    ) {
        Icon(Icons.Default.ShoppingCart, null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(9.dp))
        Text(
            label,
            fontSize = d.ctaText,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.2.sp
        )
        if (showArrow) {
            Spacer(Modifier.width(9.dp))
            Icon(Icons.Default.ArrowForward, null, modifier = Modifier.size(16.dp))
        }
    }
}

// ═════════════════════════════ STATS ═════════════════════════════

@Composable
private fun StatsStrip(d: HeroDimens) {
    // White card with the figures themselves carrying the colour — a solid green block
    // here would compete with the hero directly above it.
    val stats = listOf(
        Triple("5K+", "Secured", AGreen),
        Triple("4.9★", "Rating", AAmber),
        Triple("1 Yr", "Warranty", ABlue),
        Triple("24/7", "Support", AViolet)
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = d.hPad)
            .shadow(6.dp, RoundedCornerShape(18.dp), spotColor = CardShadow)
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White)
            .border(1.dp, Hairline, RoundedCornerShape(18.dp))
            .padding(vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        stats.forEachIndexed { i, (value, label, accent) ->
            if (i > 0) {
                Box(Modifier.width(1.dp).height(30.dp).background(Hairline))
            }
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    value,
                    fontSize = d.statValue,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-0.5).sp,
                    color = accent.tint,
                    maxLines = 1
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    label.uppercase(),
                    fontSize = d.statLabel,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp,
                    color = InkLight,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            }
        }
    }
}

// ═════════════════════════════ VIDEO ═════════════════════════════

@Composable
private fun VideoSection(d: HeroDimens) {
    SectionCard(d) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "See It In Action",
                    fontSize = d.sectionTitle,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.3).sp,
                    color = Ink
                )
                Text(
                    "Watch Eva2z GPS live demo",
                    fontSize = d.body * 0.92f,
                    color = InkLight
                )
            }
            Spacer(Modifier.width(8.dp))
            PillBadge("LIVE DEMO", ARose, d.microLabel, withDot = true)
        }

        // 16:9 rather than a fixed height — the frame then fits every screen width
        // instead of letterboxing on small phones and shrinking on tablets.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(16.dp))
                .background(BrandSoft)
                .border(1.dp, Hairline, RoundedCornerShape(16.dp))
        ) {
            VideoPlayerView(
                url = "https://www.youtube.com/watch?v=gMR4ZqFlplg",
                modifier = Modifier.fillMaxSize()
            )
        }

        Spacer(Modifier.height(11.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(Icons.Default.Link, null, tint = InkLight, modifier = Modifier.size(13.dp))
            Text("Features Video Clip", fontSize = d.microLabel, color = InkLight)
        }
    }
}

// ═════════════════════════════ FEATURES ═════════════════════════════

@Composable
private fun FeaturesSection(d: HeroDimens) {
    // Rotating accents give the grid rhythm; a single green repeated ten times reads flat.
    val features = listOf(
        Triple(Icons.Default.LocationOn, "Live Location", AGreen),
        Triple(Icons.Default.LocalGasStation, "Geo-fencing", ABlue),
        Triple(Icons.Default.History, "Trip History", AViolet),
        Triple(Icons.Default.PowerOff, "Engine Cut Off", ARose),
        Triple(Icons.Default.NotificationsActive, "Anti-Theft", AAmber),
        Triple(Icons.Default.Description, "Doc Vault", ATeal),
        Triple(Icons.Default.Assessment, "Daily Reports", ABlue),
        Triple(Icons.Default.Verified, "Aadhaar Verify", AGreen),
        Triple(Icons.Default.VerifiedUser, "Criminal Check", AViolet),
        Triple(Icons.Default.Speed, "Driving Score", AAmber),
    )

    SectionCard(d) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "All Features",
                fontSize = d.sectionTitle,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.3).sp,
                color = Ink
            )
            PillBadge("${features.size} FEATURES", AGreen, d.microLabel, withDot = false)
        }

        features.chunked(d.featureColumns).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 7.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.Top
            ) {
                rowItems.forEach { (icon, label, accent) ->
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(d.iconTile)
                                .clip(RoundedCornerShape(15.dp))
                                .background(accent.soft),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                icon, null,
                                tint = accent.tint,
                                modifier = Modifier.size(d.iconGlyph)
                            )
                        }
                        Spacer(Modifier.height(7.dp))
                        Text(
                            label,
                            fontSize = d.featureLabel,
                            color = InkMid,
                            textAlign = TextAlign.Center,
                            lineHeight = d.featureLabel * 1.35f,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                // Keeps a short final row aligned with the full ones above it.
                repeat(d.featureColumns - rowItems.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

// ═════════════════════════════ WHY EVA2Z ═════════════════════════════

private data class Reason(
    val icon: ImageVector,
    val accent: Accent,
    val title: String,
    val desc: String
)

@Composable
private fun WhySection(d: HeroDimens) {
    val reasons = listOf(
        Reason(
            Icons.Default.FlashOn, AAmber,
            "Instant Alerts",
            "Get notified when your vehicle moves, ignition starts, or geofence is crossed."
        ),
        Reason(
            Icons.Default.BatteryFull, AGreen,
            "30-Day Battery Backup",
            "6000mAh battery keeps tracking even when vehicle power is cut — no blind spots."
        ),
        Reason(
            Icons.Default.HeadsetMic, ABlue,
            "24/7 Expert Support",
            "Our team is always available to help you track, protect, and manage your vehicle."
        )
    )

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = d.hPad)) {
        Text(
            "WHY CHOOSE EVA2Z",
            fontSize = d.microLabel,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp,
            color = InkLight
        )
        Spacer(Modifier.height(12.dp))

        if (d.whyTwoColumn) {
            // Wide screens: pair the cards so the copy keeps a readable measure instead
            // of stretching one sentence across the whole width.
            reasons.chunked(2).forEach { pair ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    pair.forEach { reason ->
                        Box(Modifier.weight(1f)) { WhyCard(d, reason) }
                    }
                    repeat(2 - pair.size) { Spacer(Modifier.weight(1f)) }
                }
                Spacer(Modifier.height(12.dp))
            }
        } else {
            reasons.forEach { reason ->
                WhyCard(d, reason)
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun WhyCard(d: HeroDimens, reason: Reason) {
    val (icon, accent, title, desc) = reason
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(5.dp, RoundedCornerShape(20.dp), spotColor = CardShadow)
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White)
            .border(1.dp, Hairline, RoundedCornerShape(20.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(d.iconTile)
                .clip(RoundedCornerShape(15.dp))
                .background(accent.soft),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = accent.tint, modifier = Modifier.size(d.iconGlyph))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                fontSize = d.whyTitle,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.2).sp,
                color = Ink
            )
            Spacer(Modifier.height(5.dp))
            Text(desc, fontSize = d.whyBody, color = InkMid, lineHeight = d.whyBody * 1.5f)
        }
    }
}

// ═════════════════════════════ CLOSING CTA ═════════════════════════════

@Composable
private fun ClosingCta(d: HeroDimens, onBuy: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = d.hPad)
            .shadow(
                16.dp,
                RoundedCornerShape(d.cardCorner),
                ambientColor = BrandDeep.copy(alpha = 0.28f),
                spotColor = BrandDeep.copy(alpha = 0.28f)
            )
            .clip(RoundedCornerShape(d.cardCorner))
            .background(Brush.verticalGradient(listOf(BrandSoft, Color(0xFFDCF0E6))))
            .clipToBounds()
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .size(d.bloom)
                .offset(y = -d.bloom * 0.42f)
                .background(
                    Brush.radialGradient(
                        listOf(Color.White.copy(alpha = 0.95f), Color.White.copy(alpha = 0f))
                    ),
                    CircleShape
                )
        )

        Column(
            modifier = Modifier.padding(d.cardPad + 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .shadow(10.dp, CircleShape, spotColor = BrandDeep.copy(alpha = 0.35f))
                    .clip(CircleShape)
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Shield, null, tint = Brand, modifier = Modifier.size(31.dp))
            }

            Spacer(Modifier.height(18.dp))
            Text(
                "Ready to Secure\nYour Vehicle?",
                fontSize = d.headline * 0.8f,
                fontWeight = FontWeight.Black,
                letterSpacing = (-0.7).sp,
                color = Ink,
                lineHeight = d.headlineLine * 0.82f,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(9.dp))
            Text(
                "Join 5,000+ vehicle owners. Get real-time GPS, anti-theft & engine cut-off today.",
                fontSize = d.body,
                color = InkMid,
                lineHeight = d.body * 1.5f,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(22.dp))
            GradientCta(d, label = "Buy Eva2z GPS Now", showArrow = false, onClick = onBuy)

            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                listOf(
                    Icons.Default.Security to "Secure Pay",
                    Icons.Default.LocalShipping to "Free Delivery",
                    Icons.Default.Replay to "Easy Return"
                ).forEach { (icon, label) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(icon, null, tint = BrandDeep, modifier = Modifier.size(13.dp))
                        Text(
                            label,
                            fontSize = d.microLabel,
                            color = InkMid,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

// ═════════════════════════════ SHARED ═════════════════════════════

/** White panel with a hairline edge and a soft lift — the page's default container. */
@Composable
private fun SectionCard(d: HeroDimens, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = d.hPad)
            .shadow(6.dp, RoundedCornerShape(d.cardCorner - 4.dp), spotColor = CardShadow)
            .clip(RoundedCornerShape(d.cardCorner - 4.dp))
            .background(Color.White)
            .border(1.dp, Hairline, RoundedCornerShape(d.cardCorner - 4.dp))
            .padding(d.cardPad),
        content = content
    )
}

@Composable
private fun PillBadge(
    text: String,
    accent: Accent,
    fontSize: TextUnit,
    withDot: Boolean
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(accent.soft)
            .padding(horizontal = 11.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (withDot) {
            Box(Modifier.size(6.dp).clip(CircleShape).background(accent.tint))
        }
        Text(
            text,
            fontSize = fontSize,
            fontWeight = FontWeight.Bold,
            color = accent.tint,
            letterSpacing = 0.9.sp,
            maxLines = 1
        )
    }
}