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
import com.developer.ev2z.navigation.PlatformBackHandler

// ── Light Green Palette ───────────────────────────────────────────
private val LGBackground     = Color(0xFFF2FAF4)   // page bg
private val LGSurface        = Color(0xFFE8F5E9)   // card / chip bg
private val LGPrimary        = Color(0xFF43A047)   // buttons, icons
private val LGPrimaryLight   = Color(0xFFC8E6C9)   // icon circle bg
private val LGPrimaryDark    = Color(0xFF2E7D32)   // pressed / accent
private val LGDivider        = Color(0xFFA5D6A7)
private val LGTextPrimary    = Color(0xFF1B3A1F)
private val LGTextSecondary  = Color(0xFF5A7A5E)
private val LGTopBarBg       = Color(0xFFFFFFFF)

// ─────────────────────────────────────────────────────────────────
// ConfirmationScreen — Chat Closed · Light Green Theme
// ─────────────────────────────────────────────────────────────────

@Composable
fun ConfirmationScreen(
    alertId: Int,
    viewModel: SosTagViewModel,
    onClose: () -> Unit
) {
    // System back → close the overlay and return to home immediately.
    // Composed AFTER the home pager's Voyager handlers so it wins LIFO priority
    // and a single back press is enough.
    PlatformBackHandler(enabled = true) { onClose() }

    val replyState by viewModel.replyState.collectAsState()
    val isSuccess  = replyState is SosReplyUiState.Success

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    val iconScale by animateFloatAsState(
        targetValue   = if (visible) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness    = Spring.StiffnessMedium
        ),
        label = "iconScale"
    )

    Scaffold(
        containerColor = LGBackground,
        topBar = {
            ClosedChatTopBar(
                title = if (isSuccess) "Response Confirmed" else "Session Closed"
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {

            // ── Top Content ──────────────────────────────────
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Spacer(Modifier.height(16.dp))

                // ── Animated Icon Circle ─────────────────────
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .scale(iconScale)
                        .clip(CircleShape)
                        .background(LGPrimaryLight),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isSuccess) Icons.Default.CheckCircle
                        else Icons.Default.LockClock,
                        contentDescription = null,
                        tint     = LGPrimary,
                        modifier = Modifier.size(58.dp)
                    )
                }

                // ── Title & Subtitle ─────────────────────────
                AnimatedVisibility(
                    visible = visible,
                    enter   = fadeIn(tween(delayMillis = 300)) + slideInVertically()
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = if (isSuccess) "Response Sent Successfully"
                            else "This Chat Has Been Closed",
                            fontSize   = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color      = LGTextPrimary,
                            textAlign  = TextAlign.Center
                        )
                        Text(
                            text = if (isSuccess)
                                "Your message has been delivered.\nThe reporter has been notified."
                            else
                                "This conversation has ended and\nis no longer available for responses.",
                            fontSize   = 14.sp,
                            color      = LGTextSecondary,
                            textAlign  = TextAlign.Center,
                            lineHeight = 21.sp
                        )
                    }
                }

                // ── Info Card ────────────────────────────────
                AnimatedVisibility(
                    visible = visible,
                    enter   = fadeIn(tween(delayMillis = 500)) + expandVertically()
                ) {
                    Card(
                        modifier  = Modifier.fillMaxWidth(),
                        shape     = RoundedCornerShape(20.dp),
                        colors    = CardDefaults.cardColors(containerColor = LGSurface),
                        elevation = CardDefaults.cardElevation(0.dp)
                    ) {
                        Column(
                            modifier            = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            if (isSuccess) {
                                // ── Success rows ──────────────
                                ChatInfoRow(
                                    icon      = Icons.Default.MarkEmailRead,
                                    iconColor = LGPrimary,
                                    label     = "Delivery Status",
                                    value     = "Message delivered to the reporter."
                                )
                                HorizontalDivider(color = LGDivider, thickness = 0.8.dp)
                                ChatInfoRow(
                                    icon      = Icons.Default.Lock,
                                    iconColor = LGPrimaryDark,
                                    label     = "Chat Status",
                                    value     = "This chat is now closed. No further replies are needed."
                                )
                            }
                        }
                    }
                }

                // ── "Chat Closed" Badge ──────────────────────
                AnimatedVisibility(
                    visible = visible,
                    enter   = fadeIn(tween(delayMillis = 650))
                ) {
                    Row(
                        modifier            = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(LGPrimaryLight)
                            .padding(horizontal = 14.dp, vertical = 7.dp),
                        verticalAlignment   = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Default.Lock, null,
                            tint     = LGPrimary,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            "Chat Closed",
                            fontSize   = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color      = LGPrimary
                        )
                    }
                }
            }

            // ── Bottom Area ───────────────────────────────────
            AnimatedVisibility(
                visible = visible,
                enter   = fadeIn(tween(delayMillis = 750)) + slideInVertically { it / 2 }
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier            = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick   = onClose,
                        modifier  = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape     = RoundedCornerShape(50),
                        colors    = ButtonDefaults.buttonColors(containerColor = LGPrimary),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                    ) {
                        Icon(
                            Icons.Default.Close, null,
                            tint     = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Close",
                            fontWeight = FontWeight.Bold,
                            fontSize   = 15.sp,
                            color      = Color.White
                        )
                    }

                    Text(
                        text      = "You may safely close this screen.",
                        fontSize  = 12.sp,
                        color     = LGTextSecondary,
                        textAlign = TextAlign.Center,
                        modifier  = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// ChatInfoRow
// ─────────────────────────────────────────────────────────────────

@Composable
private fun ChatInfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    label: String,
    value: String
) {
    Row(
        verticalAlignment     = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector        = icon,
            contentDescription = null,
            tint               = iconColor,
            modifier           = Modifier
                .size(20.dp)
                .padding(top = 2.dp)
        )
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(label, fontSize = 11.sp, color = LGTextSecondary, fontWeight = FontWeight.SemiBold)
            Text(value, fontSize = 13.sp, color = LGTextPrimary,   lineHeight = 18.sp)
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// Top Bar — No close button (chat is already closed)
// ─────────────────────────────────────────────────────────────────

@Composable
private fun ClosedChatTopBar(title: String) {
    Surface(
        color            = LGTopBarBg,
        shadowElevation  = 1.dp,
        tonalElevation   = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            // ── Green dot indicator ──
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(LGPrimary)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                title,
                fontSize   = 17.sp,
                fontWeight = FontWeight.Bold,
                color      = LGTextPrimary
            )
        }
    }
}