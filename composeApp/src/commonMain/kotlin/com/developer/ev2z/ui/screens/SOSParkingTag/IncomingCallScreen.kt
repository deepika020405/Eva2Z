package com.developer.ev2z.ui.screens.SOSParkingTag

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.developer.ev2z.network.ApiService
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ── Palette ───────────────────────────────────────────────────────────
private val RingBg         = Color(0xFF071A07)
private val RingBgMid      = Color(0xFF0C1F0C)
private val RingGreen      = Color(0xFF4A7D39)
private val RingGreenLight = Color(0xFF85B070)
private val RingGreenGlow  = Color(0xFF4A7D39)
private val DeclineRed     = Color(0xFFD32F2F)
private val AcceptGreen    = Color(0xFF2E7D32)

// ─────────────────────────────────────────────────────────────────────
// IncomingCallScreen
// Shows when a visitor calls the owner. Two actions:
//   • Accept → POST /call/accept  → onAccepted()
//   • Decline → POST /call/decline → onDeclined()
// ─────────────────────────────────────────────────────────────────────

@Composable
fun IncomingCallScreen(
    visitorName:  String,
    visitorPhone: String,
    roomId:       String,
    apiService:   ApiService,
    onAccepted:   () -> Unit,   // navigate to CallScreen
    onDeclined:   () -> Unit    // navigate back / to QuickReplyScreen
) {
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }
    var loadingAction by remember { mutableStateOf<String?>(null) }   // "accept" | "decline"
    var errorMsg by remember { mutableStateOf<String?>(null) }

    // ── Ripple pulse animation (3 staggered rings) ───────────────────
    val infiniteTransition = rememberInfiniteTransition(label = "rings")

    val ring1Scale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.55f,
        animationSpec = infiniteRepeatable(
            tween(2000, easing = EaseOutQuart),
            RepeatMode.Restart
        ), label = "ring1"
    )
    val ring1Alpha by infiniteTransition.animateFloat(
        initialValue = 0.45f, targetValue = 0f,
        animationSpec = infiniteRepeatable(
            tween(2000, easing = EaseOutQuart),
            RepeatMode.Restart
        ), label = "ring1a"
    )
    val ring2Scale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.55f,
        animationSpec = infiniteRepeatable(
            tween(2000, 400, easing = EaseOutQuart),
            RepeatMode.Restart
        ), label = "ring2"
    )
    val ring2Alpha by infiniteTransition.animateFloat(
        initialValue = 0.35f, targetValue = 0f,
        animationSpec = infiniteRepeatable(
            tween(2000, 400, easing = EaseOutQuart),
            RepeatMode.Restart
        ), label = "ring2a"
    )
    val ring3Scale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.55f,
        animationSpec = infiniteRepeatable(
            tween(2000, 800, easing = EaseOutQuart),
            RepeatMode.Restart
        ), label = "ring3"
    )
    val ring3Alpha by infiniteTransition.animateFloat(
        initialValue = 0.25f, targetValue = 0f,
        animationSpec = infiniteRepeatable(
            tween(2000, 800, easing = EaseOutQuart),
            RepeatMode.Restart
        ), label = "ring3a"
    )

    // ── Ringing dots ─────────────────────────────────────────────────
    var dotCount by remember { mutableStateOf(1) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(500)
            dotCount = (dotCount % 3) + 1
        }
    }
    val dots = ".".repeat(dotCount)

    // ── Auto-decline after 30s ────────────────────────────────────────
    LaunchedEffect(Unit) {
        delay(30_000)
        if (!isLoading) {
            scope.launch {
                runCatching { apiService.declineCall(roomId) }
                onDeclined()
            }
        }
    }

    fun onAcceptClick() {
        if (isLoading) return
        isLoading = true
        loadingAction = "accept"
        errorMsg = null
        scope.launch {
            val result = runCatching { apiService.acceptCall(roomId) }
            isLoading = false
            loadingAction = null
            if (result.isFailure) {
                errorMsg = "Could not accept call. Check your connection."
                return@launch
            }
            onAccepted()
        }
    }

    fun onDeclineClick() {
        if (isLoading) return
        isLoading = true
        loadingAction = "decline"
        scope.launch {
            runCatching { apiService.declineCall(roomId) }
            isLoading = false
            loadingAction = null
            onDeclined()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(RingBg, RingBgMid, Color(0xFF050F05))))
    ) {
        // Error snackbar
        AnimatedVisibility(
            visible = errorMsg != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
                .navigationBarsPadding()
        ) {
            errorMsg?.let { msg ->
                LaunchedEffect(msg) {
                    kotlinx.coroutines.delay(3000)
                    errorMsg = null
                }
                Snackbar(
                    containerColor = Color(0xFFB71C1C),
                    contentColor = Color.White,
                ) { Text(msg, fontSize = 13.sp) }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Spacer(Modifier.height(56.dp))

            // ── "Incoming Call" label ─────────────────────────────────
            Text(
                "Incoming Call",
                fontSize = 13.sp,
                color = RingGreenLight.copy(alpha = 0.7f),
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.2.sp
            )

            Spacer(Modifier.height(8.dp))

            // ── "Parking Alert" badge ─────────────────────────────────
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = RingGreen.copy(alpha = 0.18f)
            ) {
                Text(
                    "Parking Alert",
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp),
                    fontSize = 11.sp,
                    color = RingGreenLight,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.5.sp
                )
            }

            Spacer(Modifier.height(48.dp))

            // ── Ripple rings + avatar ─────────────────────────────────
            Box(contentAlignment = Alignment.Center) {
                // Ring 3 (outermost, most transparent)
                Box(
                    modifier = Modifier
                        .size(130.dp)
                        .scale(ring3Scale)
                        .clip(CircleShape)
                        .background(RingGreenGlow.copy(alpha = ring3Alpha))
                )
                // Ring 2
                Box(
                    modifier = Modifier
                        .size(130.dp)
                        .scale(ring2Scale)
                        .clip(CircleShape)
                        .background(RingGreenGlow.copy(alpha = ring2Alpha))
                )
                // Ring 1 (innermost)
                Box(
                    modifier = Modifier
                        .size(130.dp)
                        .scale(ring1Scale)
                        .clip(CircleShape)
                        .background(RingGreenGlow.copy(alpha = ring1Alpha))
                )
                // Avatar circle
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(RingGreenLight, RingGreen)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = visitorName.firstOrNull()?.uppercase() ?: "V",
                        fontSize = 46.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Spacer(Modifier.height(36.dp))

            // ── Caller name ───────────────────────────────────────────
            Text(
                visitorName,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(8.dp))

            // ── Phone number ──────────────────────────────────────────
            if (visitorPhone.isNotBlank()) {
                Text(
                    visitorPhone,
                    fontSize = 15.sp,
                    color = Color.White.copy(alpha = 0.55f)
                )
                Spacer(Modifier.height(12.dp))
            }

            // ── Ringing indicator ────────────────────────────────────
            Text(
                "Ringing$dots",
                fontSize = 14.sp,
                color = RingGreenLight.copy(alpha = 0.7f),
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.5.sp
            )

            Spacer(Modifier.weight(1f))

            // ── Accept / Decline buttons ──────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Decline (left)
                CallActionButton(
                    icon        = Icons.Filled.CallEnd,
                    label       = "Decline",
                    bgColor     = DeclineRed,
                    isLoading   = loadingAction == "decline",
                    enabled     = !isLoading,
                    onClick     = ::onDeclineClick
                )

                // Accept (right)
                CallActionButton(
                    icon        = Icons.Filled.Call,
                    label       = "Accept",
                    bgColor     = AcceptGreen,
                    isLoading   = loadingAction == "accept",
                    enabled     = !isLoading,
                    onClick     = ::onAcceptClick
                )
            }

            Spacer(Modifier.height(56.dp))
        }
    }
}

// ── Reusable action button (accept / decline) ─────────────────────────
@Composable
private fun CallActionButton(
    icon:      androidx.compose.ui.graphics.vector.ImageVector,
    label:     String,
    bgColor:   Color,
    isLoading: Boolean,
    enabled:   Boolean,
    onClick:   () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        IconButton(
            onClick  = onClick,
            enabled  = enabled,
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(if (enabled) bgColor else bgColor.copy(alpha = 0.4f))
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier    = Modifier.size(28.dp),
                    color       = Color.White,
                    strokeWidth = 2.5.dp
                )
            } else {
                Icon(
                    icon,
                    contentDescription = label,
                    tint     = Color.White,
                    modifier = Modifier.size(36.dp)
                )
            }
        }
        Text(
            label,
            fontSize    = 13.sp,
            color       = Color.White.copy(alpha = 0.65f),
            fontWeight  = FontWeight.Medium
        )
    }
}