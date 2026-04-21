package com.developer.ev2z.ui.screens.SOSParkingTag

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.developer.ev2z.calling.VoiceCallManager
import kotlinx.coroutines.delay

// ── Palette ──────────────────────────────────────────────────────────
private val CallBg         = Color(0xFF0A1A0A)
private val CallBgMid      = Color(0xFF0F1F0F)
private val CallGreen      = Color(0xFF4A7D39)
private val CallGreenLight = Color(0xFF85B070)
private val CallSurfaceAlt = Color(0xFF1C2B1C)
private val ActiveGreen    = Color(0xFF4A7D39)
private val EndCallRed     = Color(0xFFD32F2F)

@Composable
fun CallScreen(
    visitorName: String,
    visitorPhone: String,
    onEnd: () -> Unit,
    voiceCallManager: VoiceCallManager? = null,
    isConnected: Boolean = false
) {
    // ── Timer — starts only after connected ────────────────────────
    var elapsedSeconds by remember { mutableStateOf(0) }
    LaunchedEffect(isConnected) {
        if (!isConnected) return@LaunchedEffect
        while (true) { delay(1000); elapsedSeconds++ }
    }
    val timerText = "${(elapsedSeconds / 60).toString().padStart(2, '0')}:${(elapsedSeconds % 60).toString().padStart(2, '0')}"

    // ── Connecting dots animation ──────────────────────────────────
    var dotCount by remember { mutableStateOf(1) }
    LaunchedEffect(isConnected) {
        if (isConnected) return@LaunchedEffect
        while (true) { delay(500); dotCount = (dotCount % 3) + 1 }
    }

    // ── Toggles ────────────────────────────────────────────────────
    var isMuted   by remember { mutableStateOf(false) }
    var isSpeaker by remember { mutableStateOf(false) }

    // ── Pulse ring ─────────────────────────────────────────────────
    val pulse = rememberInfiniteTransition(label = "pulse")
    val pulseScale by pulse.animateFloat(
        initialValue = 1f, targetValue = 1.14f,
        animationSpec = infiniteRepeatable(
            tween(900, easing = EaseInOut), RepeatMode.Reverse
        ), label = "scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(CallBg, CallBgMid, Color(0xFF081208))))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Spacer(Modifier.height(52.dp))

            // ── Subtitle ──────────────────────────────────────────
            Text(
                if (isConnected) "Parking Alert · In Call"
                else "Parking Alert · Connecting" + ".".repeat(dotCount),
                fontSize = 12.sp,
                color = CallGreenLight.copy(alpha = 0.6f),
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.sp
            )

            Spacer(Modifier.height(32.dp))

            // ── Pulsing avatar ────────────────────────────────────
            Box(contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(116.dp)
                        .scale(pulseScale)
                        .clip(CircleShape)
                        .background(CallGreen.copy(alpha = 0.12f))
                )
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(listOf(CallGreenLight, CallGreen))
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        visitorName.firstOrNull()?.uppercase() ?: "V",
                        fontSize = 38.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Spacer(Modifier.height(28.dp))

            // ── Name ──────────────────────────────────────────────
            Text(
                visitorName,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(Modifier.height(6.dp))

            // ── Phone ─────────────────────────────────────────────
            Text(
                visitorPhone,
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.55f)
            )

            Spacer(Modifier.height(16.dp))

            // ── Timer / connecting pill ───────────────────────────
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color.White.copy(alpha = 0.08f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    val dotAlpha by pulse.animateFloat(
                        initialValue = 1f, targetValue = 0.2f,
                        animationSpec = infiniteRepeatable(
                            tween(600), RepeatMode.Reverse
                        ), label = "dot"
                    )
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(
                                (if (isConnected) CallGreenLight else Color(0xFFFFB74D))
                                    .copy(alpha = dotAlpha)
                            )
                    )
                    Text(
                        if (isConnected) timerText else "Connecting...",
                        fontSize = 15.sp,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            // ── Mute + Speaker ────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CallButton(
                    icon        = if (isMuted) Icons.Filled.MicOff else Icons.Filled.Mic,
                    label       = if (isMuted) "Unmute" else "Mute",
                    isActive    = isMuted,
                    size        = 64.dp,
                    iconSize    = 28.dp,
                    activeColor = ActiveGreen,
                    onClick     = {
                        isMuted = !isMuted
                        voiceCallManager?.setMuted(isMuted)
                    }
                )

                CallButton(
                    icon        = if (isSpeaker) Icons.Filled.VolumeUp else Icons.Filled.VolumeOff,
                    label       = if (isSpeaker) "Speaker On" else "Speaker",
                    isActive    = isSpeaker,
                    size        = 64.dp,
                    iconSize    = 28.dp,
                    activeColor = ActiveGreen,
                    onClick     = {
                        isSpeaker = !isSpeaker
                        voiceCallManager?.setSpeaker(isSpeaker)
                    }
                )
            }

            Spacer(Modifier.height(44.dp))

            // ── End call ──────────────────────────────────────────
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                IconButton(
                    onClick = onEnd,
                    modifier = Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .background(EndCallRed)
                ) {
                    Icon(
                        Icons.Filled.CallEnd,
                        contentDescription = "End call",
                        tint = Color.White,
                        modifier = Modifier.size(34.dp)
                    )
                }
                Text(
                    "End Call",
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.55f),
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(Modifier.height(52.dp))
        }
    }
}

// ── Generic call action button ────────────────────────────────────────
@Composable
private fun CallButton(
    icon: ImageVector,
    label: String,
    isActive: Boolean,
    size: Dp,
    iconSize: Dp,
    activeColor: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(if (isActive) activeColor else CallSurfaceAlt)
        ) {
            Icon(
                icon,
                contentDescription = label,
                tint = Color.White,
                modifier = Modifier.size(iconSize)
            )
        }
        Text(
            label,
            fontSize = 12.sp,
            color = Color.White.copy(alpha = 0.6f),
            fontWeight = FontWeight.Medium
        )
    }
}