package com.developer.ev2z.navigation

// ===============================================================================
// ========================= NEW UI BOTTOM NAV BAR (COMMENTED OUT) ===============
// ===============================================================================
// This file contains the new 5-tab bottom nav bar with custom painters.
// The old CustomBottomNavBar is in HomeNavGraph.kt.
// To restore the new UI, uncomment this file and remove CustomBottomNavBar from HomeNavGraph.kt.

/*

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import cafe.adriel.voyager.navigator.tab.Tab

// ─── Palette ──────────────────────────────────────────────────────────────────
private val GreenPrimary = Color(0xFF2DB84B)
private val GreenActive  = Color(0xFF239B3E)
private val GreenSoft    = Color(0xFF34D058)
private val TextMuted    = Color(0xFFB0B8C1)
private val NavBg        = Color.White
private val DividerColor = Color(0xFFEEEEEE)

// ─── Public bar (matches existing call site signature) ──────────────────────
@Composable
fun CustomBottomNavBar(
    tabs: List<Tab>,
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit
) {
    val centerIndex = 2 // Vault sits in the middle of the 5 tabs
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(NavBg)
    ) {
        // Top hairline
        Box(
            Modifier
                .fillMaxWidth()
                .height(0.5.dp)
                .background(DividerColor)
                .align(Alignment.TopCenter)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(56.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom
        ) {
            tabs.forEachIndexed { index, tab ->
                if (index == centerIndex) {
                    CenterVaultTab(
                        label = tab.options.title,
                        onClick = { onTabSelected(index) }
                    )
                } else {
                    RegularTab(
                        index = index,
                        label = tab.options.title,
                        isSelected = selectedIndex == index,
                        onClick = { onTabSelected(index) }
                    )
                }
            }
        }
    }
}

// ─── Center raised vault button ──────────────────────────────────────────────
@Composable
private fun RowScope.CenterVaultTab(label: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom
    ) {
        Box(
            modifier = Modifier
                .offset(y = (-12).dp)
                .size(56.dp)
                .shadow(
                    elevation = 12.dp,
                    shape = RoundedCornerShape(18.dp),
                    ambientColor = GreenPrimary.copy(0.35f),
                    spotColor = GreenPrimary.copy(0.45f)
                )
                .clip(RoundedCornerShape(18.dp))
                .background(
                    Brush.linearGradient(listOf(GreenSoft, GreenPrimary))
                )
                .zIndex(2f),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = remember { documentIconPainter() },
                contentDescription = label,
                tint = Color.Unspecified,
                modifier = Modifier.size(28.dp)
            )
        }
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = GreenActive,
            modifier = Modifier
                .offset(y = (-10).dp)
                .padding(bottom = 4.dp)
        )
    }
}

// ─── Regular tab (Home / MyQR / Shop / Profile) ──────────────────────────────
@Composable
private fun RowScope.RegularTab(
    index: Int,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val tint = if (isSelected) GreenActive else TextMuted
    Column(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(bottom = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom
    ) {
        Icon(
            painter = remember(index, isSelected) { navIconPainter(index, tint) },
            contentDescription = label,
            tint = Color.Unspecified,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = tint
        )
    }
}

// ─── Painters ────────────────────────────────────────────────────────────────

private fun navIconPainter(index: Int, color: Color): Painter = when (index) {
    0 -> homeIconPainter(color)
    1 -> qrIconPainter(color)
    3 -> shopIconPainter(color)
    4 -> profileIconPainter(color)
    else -> documentIconPainter()
}

private fun documentIconPainter(): Painter = object : Painter() {
    override val intrinsicSize = Size(24f, 24f)
    override fun DrawScope.onDraw() {
        val w = size.width
        val h = size.height
        val foldX = w * 0.62f
        val foldY = h * 0.33f

        val body = Path().apply {
            moveTo(w * 0.18f, h * 0.08f)
            lineTo(foldX,     h * 0.08f)
            lineTo(w * 0.85f, foldY)
            lineTo(w * 0.85f, h * 0.92f)
            cubicTo(w * 0.85f, h * 0.92f, w * 0.85f, h * 0.95f, w * 0.82f, h * 0.95f)
            lineTo(w * 0.18f, h * 0.95f)
            cubicTo(w * 0.15f, h * 0.95f, w * 0.15f, h * 0.92f, w * 0.15f, h * 0.92f)
            close()
        }
        drawPath(body, Color.White)

        val fold = Path().apply {
            moveTo(foldX,     h * 0.08f)
            lineTo(foldX,     foldY)
            lineTo(w * 0.85f, foldY)
            close()
        }
        drawPath(fold, Color.White.copy(alpha = 0.55f))

        val sw = size.minDimension * 0.065f
        drawLine(
            color = GreenPrimary,
            start = Offset(w * 0.28f, h * 0.57f),
            end   = Offset(w * 0.72f, h * 0.57f),
            strokeWidth = sw,
            cap = StrokeCap.Round
        )
        drawLine(
            color = GreenPrimary,
            start = Offset(w * 0.28f, h * 0.70f),
            end   = Offset(w * 0.58f, h * 0.70f),
            strokeWidth = sw,
            cap = StrokeCap.Round
        )
    }
}

private fun homeIconPainter(color: Color): Painter = object : Painter() {
    override val intrinsicSize = Size(24f, 24f)
    override fun DrawScope.onDraw() {
        val w = size.width
        val h = size.height
        val house = Path().apply {
            moveTo(w * 0.5f,  h * 0.10f)
            lineTo(w * 0.05f, h * 0.45f)
            lineTo(w * 0.15f, h * 0.45f)
            lineTo(w * 0.15f, h * 0.90f)
            lineTo(w * 0.38f, h * 0.90f)
            lineTo(w * 0.38f, h * 0.62f)
            lineTo(w * 0.62f, h * 0.62f)
            lineTo(w * 0.62f, h * 0.90f)
            lineTo(w * 0.85f, h * 0.90f)
            lineTo(w * 0.85f, h * 0.45f)
            lineTo(w * 0.95f, h * 0.45f)
            close()
        }
        drawPath(house, color)
    }
}

private fun qrIconPainter(color: Color): Painter = object : Painter() {
    override val intrinsicSize = Size(24f, 24f)
    override fun DrawScope.onDraw() {
        val w = size.width
        val h = size.height
        val sw = size.minDimension * 0.075f
        drawRoundRect(
            color = color,
            topLeft = Offset(w * 0.08f, h * 0.30f),
            size = Size(w * 0.84f, h * 0.58f),
            cornerRadius = CornerRadius(w * 0.08f),
            style = Stroke(width = sw)
        )
        drawCircle(
            color = color,
            radius = w * 0.165f,
            center = Offset(w * 0.5f, h * 0.59f),
            style = Stroke(width = sw)
        )
        val bump = Path().apply {
            moveTo(w * 0.37f, h * 0.30f)
            lineTo(w * 0.37f, h * 0.20f)
            cubicTo(w * 0.37f, h * 0.16f, w * 0.40f, h * 0.14f, w * 0.44f, h * 0.14f)
            lineTo(w * 0.56f, h * 0.14f)
            cubicTo(w * 0.60f, h * 0.14f, w * 0.63f, h * 0.16f, w * 0.63f, h * 0.20f)
            lineTo(w * 0.63f, h * 0.30f)
        }
        drawPath(bump, color, style = Stroke(width = sw, cap = StrokeCap.Round))
    }
}

private fun shopIconPainter(color: Color): Painter = object : Painter() {
    override val intrinsicSize = Size(24f, 24f)
    override fun DrawScope.onDraw() {
        val w = size.width
        val h = size.height
        val sw = size.minDimension * 0.075f
        listOf(h * 0.28f, h * 0.50f, h * 0.72f).forEach { y ->
            drawLine(
                color = color,
                start = Offset(w * 0.12f, y),
                end   = Offset(w * 0.88f, y),
                strokeWidth = sw,
                cap = StrokeCap.Round
            )
        }
    }
}

private fun profileIconPainter(color: Color): Painter = object : Painter() {
    override val intrinsicSize = Size(24f, 24f)
    override fun DrawScope.onDraw() {
        val w = size.width
        val h = size.height
        val sw = size.minDimension * 0.075f
        drawCircle(
            color = color,
            radius = w * 0.195f,
            center = Offset(w * 0.5f, h * 0.34f),
            style = Stroke(width = sw)
        )
        val shoulders = Path().apply {
            moveTo(w * 0.08f, h * 0.92f)
            cubicTo(
                w * 0.08f, h * 0.68f,
                w * 0.27f, h * 0.57f,
                w * 0.50f, h * 0.57f
            )
            cubicTo(
                w * 0.73f, h * 0.57f,
                w * 0.92f, h * 0.68f,
                w * 0.92f, h * 0.92f
            )
        }
        drawPath(
            shoulders,
            color,
            style = Stroke(width = sw, cap = StrokeCap.Round)
        )
    }
}

*/