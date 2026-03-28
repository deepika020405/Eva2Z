package com.developer.ev2z.ui.screens.SOSParkingTag


import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.developer.ev2z.navigation.PlatformBackHandler

// ─────────────────────────────────────────────────────────────────
// QuickReplyScreen — Screen 2
// ─────────────────────────────────────────────────────────────────

@Composable
fun QuickReplyScreen(
    alertId: Int,
    viewModel: SosTagViewModel,
    onConfirmed: () -> Unit,   // Navigate to ConfirmationScreen on success
    onBackPressed: () -> Unit
) {
    PlatformBackHandler(enabled = true) { onBackPressed() }

    val selectedReply  by viewModel.selectedReply.collectAsState()
    val customMessage  by viewModel.customMessage.collectAsState()
    val replyState     by viewModel.replyState.collectAsState()
    val currentAlert   by viewModel.currentAlert.collectAsState()

    // Navigate to confirmation when reply succeeds
    LaunchedEffect(replyState) {
        if (replyState is SosReplyUiState.Success) {
            onConfirmed()
        }
    }

    Scaffold(
        containerColor = BackgroundColor,
        topBar = { QuickReplyTopBar(onBackPressed = onBackPressed) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {

            // ── Header ────────────────────────────────────────
            Text(
                "Choose a quick reply",
                fontSize   = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color      = TextPrimary
            )
            Text(
                "Notify the person who reported your vehicle.",
                fontSize = 14.sp,
                color    = TextSecondary,
                lineHeight = 20.sp
            )

            Spacer(Modifier.height(4.dp))

            // ── Quick Reply Options ───────────────────────────
            QuickReplyOption.values().forEach { option ->
                QuickReplyOptionCard(
                    option   = option,
                    selected = selectedReply == option,
                    onClick  = { viewModel.selectReply(option) }
                )
            }

            // ── Custom Message Input (animated) ───────────────
            AnimatedVisibility(
                visible = selectedReply == QuickReplyOption.CUSTOM,
                enter   = fadeIn() + expandVertically(),
                exit    = fadeOut() + shrinkVertically()
            ) {
                CustomMessageInput(
                    value         = customMessage,
                    onValueChange = viewModel::onCustomMessageChange
                )
            }

            // ── Error Banner ──────────────────────────────────
            AnimatedVisibility(visible = replyState is SosReplyUiState.Error) {
                if (replyState is SosReplyUiState.Error) {
                    ReplyErrorBanner(
                        message   = (replyState as SosReplyUiState.Error).message,
                        onDismiss = viewModel::resetReplyState
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // ── Send Reply Button ─────────────────────────────
            val isLoading = replyState is SosReplyUiState.Loading
            Button(
                onClick  = { viewModel.submitReply(alertId) },
                enabled  = viewModel.isReplyValid && !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape    = RoundedCornerShape(50),
                colors   = ButtonDefaults.buttonColors(
                    containerColor         = SosOrange,
                    disabledContainerColor = SosOrange.copy(alpha = 0.35f)
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier    = Modifier.size(20.dp),
                        color       = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(10.dp))
                    Text("Sending…", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                } else {
                    Icon(Icons.Default.Send, null, modifier = Modifier.size(18.dp), tint = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text("Send Response", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }

            // ── Divider ───────────────────────────────────────
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier              = Modifier.fillMaxWidth()
            ) {
                Divider(modifier = Modifier.weight(1f), color = BorderColor)
                Text("Need to talk directly?", fontSize = 12.sp, color = TextTertiary)
                Divider(modifier = Modifier.weight(1f), color = BorderColor)
            }

            // ── Call Reporter (Masked) ────────────────────────
            Button(
                onClick  = { /* TODO: initiate masked call via backend */ },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape    = RoundedCornerShape(50),
                colors   = ButtonDefaults.buttonColors(containerColor = SosOrange),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
            ) {
                Icon(Icons.Default.Call, null, modifier = Modifier.size(20.dp), tint = Color.White)
                Spacer(Modifier.width(8.dp))
                Text("Call Reporter (Masked)", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }

            // Privacy note
            Text(
                "YOUR NUMBER REMAINS PRIVATE",
                fontSize      = 10.sp,
                color         = TextTertiary,
                fontWeight    = FontWeight.SemiBold,
                letterSpacing = 0.8.sp,
                modifier      = Modifier.fillMaxWidth(),
                textAlign     = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(Modifier.height(16.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// Top Bar
// ─────────────────────────────────────────────────────────────────

@Composable
private fun QuickReplyTopBar(onBackPressed: () -> Unit) {
    Surface(color = CardColor, shadowElevation = 1.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            IconButton(
                onClick  = onBackPressed,
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(BackgroundColor)
            ) {
                Icon(
                    Icons.Default.ArrowBack, null,
                    tint = TextPrimary, modifier = Modifier.size(20.dp)
                )
            }
            Text(
                "Respond to the Alert",
                fontSize   = 17.sp,
                fontWeight = FontWeight.Bold,
                color      = TextPrimary
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// Quick Reply Option Card
// ─────────────────────────────────────────────────────────────────

@Composable
private fun QuickReplyOptionCard(
    option: QuickReplyOption,
    selected: Boolean,
    onClick: () -> Unit
) {
    val icon: ImageVector = when (option) {
        QuickReplyOption.COMING_SOON -> Icons.Outlined.DirectionsWalk
        QuickReplyOption.MOVING_NOW  -> Icons.Outlined.DirectionsCar
        QuickReplyOption.SORRY       -> Icons.Outlined.SentimentSatisfied
        QuickReplyOption.CUSTOM      -> Icons.Outlined.Edit
    }

    Card(
        modifier  = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape     = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(if (selected) 3.dp else 1.dp),
        colors    = CardDefaults.cardColors(
            containerColor = if (selected) SosOrangeLight else CardColor
        ),
        border = if (selected)
            BorderStroke(1.5.dp, SosOrange)
        else
            BorderStroke(1.dp, BorderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Icon box
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        if (selected) SosOrange else BackgroundColor
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon, null,
                    tint     = if (selected) Color.White else TextSecondary,
                    modifier = Modifier.size(22.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    option.title,
                    fontSize   = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color      = if (selected) TextPrimary else TextPrimary
                )
                Text(
                    option.subtitle,
                    fontSize = 12.sp,
                    color    = TextSecondary
                )
            }

            Icon(
                if (selected) Icons.Default.CheckCircle else Icons.Default.ChevronRight,
                null,
                tint     = if (selected) SosOrange else TextTertiary,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// Custom Message Input
// ─────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomMessageInput(
    value: String,
    onValueChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            "YOUR MESSAGE",
            fontSize      = 11.sp,
            fontWeight    = FontWeight.SemiBold,
            color         = TextSecondary,
            letterSpacing = 0.4.sp
        )
        OutlinedTextField(
            value         = value,
            onValueChange = onValueChange,
            placeholder   = { Text("Type your custom message…", color = TextTertiary) },
            modifier      = Modifier
                .fillMaxWidth()
                .heightIn(min = 100.dp),
            shape  = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor      = SosOrange,
                unfocusedBorderColor    = BorderColor,
                focusedContainerColor   = CardColor,
                unfocusedContainerColor = BackgroundColor,
                cursorColor             = SosOrange
            ),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction      = ImeAction.Default
            ),
            maxLines = 5
        )
    }
}

// ─────────────────────────────────────────────────────────────────
// Error Banner
// ─────────────────────────────────────────────────────────────────

@Composable
private fun ReplyErrorBanner(message: String, onDismiss: () -> Unit) {
    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(12.dp),
        colors    = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
        border    = BorderStroke(1.dp, ErrorRed.copy(alpha = 0.4f))
    ) {
        Row(
            modifier              = Modifier.padding(14.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(Icons.Default.ErrorOutline, null, tint = ErrorRed, modifier = Modifier.size(20.dp))
            Text(message, fontSize = 13.sp, color = TextSecondary, modifier = Modifier.weight(1f))
            IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Default.Close, null, tint = TextTertiary, modifier = Modifier.size(16.dp))
            }
        }
    }
}