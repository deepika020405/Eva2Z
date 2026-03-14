package com.developer.ev2z.ui.screens.SOSParkingTag


import androidx.compose.animation.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ─────────────────────────────────────────────────────────────────
// ConfirmationScreen — Screen 3: Response Sent
// ─────────────────────────────────────────────────────────────────

@Composable
fun ConfirmationScreen(
    alertId: Int,
    viewModel: SosTagViewModel,
    onViewAlertDetails: () -> Unit,
    onClose: () -> Unit
) {
    val replyState by viewModel.replyState.collectAsState()
    val successMessage = (replyState as? SosReplyUiState.Success)?.message
        ?: "Response sent successfully!"

    // Animate checkmark on entry
    var checkVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { checkVisible = true }

    val checkScale by animateFloatAsState(
        targetValue    = if (checkVisible) 1f else 0f,
        animationSpec  = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness    = Spring.StiffnessMedium
        ),
        label = "checkScale"
    )

    Scaffold(
        containerColor = BackgroundColor,
        topBar = { ConfirmationTopBar(onClose = onClose) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Spacer(Modifier.height(24.dp))

                // ── Animated Checkmark ────────────────────────
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .scale(checkScale)
                        .clip(CircleShape)
                        .background(SuccessLight),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.CheckCircle, null,
                        tint     = SuccessGreen,
                        modifier = Modifier.size(56.dp)
                    )
                }

                // ── Title ─────────────────────────────────────
                AnimatedVisibility(
                    visible = checkVisible,
                    enter   = fadeIn(animationSpec = tween(delayMillis = 300)) + slideInVertically()
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "Response Sent",
                            fontSize   = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color      = TextPrimary
                        )
                        Text(
                            "The person who reported the issue has\nreceived your response.",
                            fontSize   = 14.sp,
                            color      = TextSecondary,
                            textAlign  = TextAlign.Center,
                            lineHeight = 21.sp
                        )
                    }
                }

                // ── Mail Illustration Card ────────────────────
                AnimatedVisibility(
                    visible = checkVisible,
                    enter   = fadeIn(animationSpec = tween(delayMillis = 500)) + expandVertically()
                ) {
                    Card(
                        modifier  = Modifier
                            .fillMaxWidth()
                            .height(130.dp),
                        shape     = RoundedCornerShape(20.dp),
                        colors    = CardDefaults.cardColors(containerColor = SosOrangeLight),
                        elevation = CardDefaults.cardElevation(0.dp)
                    ) {
                        Box(
                            modifier         = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            // Mail with arrow icon (like in design)
                            Row(
                                verticalAlignment     = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy((-8).dp)
                            ) {
                                Icon(
                                    Icons.Default.MarkEmailRead, null,
                                    tint     = SosOrange,
                                    modifier = Modifier.size(64.dp)
                                )
                            }
                        }
                    }
                }
            }

            // ── Bottom Buttons ────────────────────────────────
            AnimatedVisibility(
                visible = checkVisible,
                enter   = fadeIn(animationSpec = tween(delayMillis = 700)) + slideInVertically { it / 2 }
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier            = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick  = onViewAlertDetails,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape    = RoundedCornerShape(50),
                        colors   = ButtonDefaults.buttonColors(containerColor = SosOrange),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                    ) {
                        Text(
                            "View Alert Details",
                            fontWeight = FontWeight.Bold,
                            fontSize   = 15.sp
                        )
                    }

                    OutlinedButton(
                        onClick  = onClose,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape    = RoundedCornerShape(50),
                        colors   = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color(0xFFEFF0F3)
                        ),
                        border   = null
                    ) {
                        Text(
                            "Close",
                            fontWeight = FontWeight.SemiBold,
                            fontSize   = 15.sp,
                            color      = TextSecondary
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// Top Bar — X close button
// ─────────────────────────────────────────────────────────────────

@Composable
private fun ConfirmationTopBar(onClose: () -> Unit) {
    Surface(color = BackgroundColor, shadowElevation = 0.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick  = onClose,
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFEFF0F3))
            ) {
                Icon(
                    Icons.Default.Close, null,
                    tint     = TextPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }

            Text(
                "Confirmation",
                fontSize   = 17.sp,
                fontWeight = FontWeight.Bold,
                color      = TextPrimary
            )

            // Spacer to center title
            Spacer(Modifier.size(38.dp))
        }
    }
}