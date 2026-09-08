package com.developer.ev2z.ui.screens.fastag

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.developer.ev2z.data.local.createStorageManager

/**
 * Single entry point for the FASTag feature — the only thing the nav graph needs to know
 * about. Owns the recharge → status hop internally so HomeTab stays a one-line branch
 * next to the existing fuel-price and weather screens.
 *
 * @param vehicleNumber prefilled into the issuer's vehicle-number field when its schema
 *   asks for one. Null is fine and common: several issuers key on a Tag ID instead.
 */
@Composable
fun FastagFlow(
    onExit: () -> Unit,
    vehicleId: String? = null,
    vehicleNumber: String? = null,
    dataSource: FastagDataSource = remember { LocalFastagDataSource() }
) {
    val holder = remember(vehicleId, vehicleNumber) {
        FastagRechargeStateHolder(
            callbacks = FastagCallbacks(dataSource),
            referenceIdStore = StorageManagerReferenceIdStore(createStorageManager()),
            vehicleId = vehicleId,
            vehicleNumber = vehicleNumber
        )
    }

    LaunchedEffect(holder) { holder.start() }

    // Snapshotted once the server accepts the recharge; the status screen renders from
    // this and never from a payment-SDK callback.
    var statusTxn by remember { mutableStateOf<FastagTransaction?>(null) }

    LaunchedEffect(holder.acceptedTransaction) {
        holder.acceptedTransaction?.let { statusTxn = it }
    }

    val txn = statusTxn
    if (txn != null) {
        FastagStatusScreen(
            txn = txn,
            onDone = {
                // Terminal state consumed — the next tap must mint a fresh key.
                holder.clearPaymentAttempt()
                statusTxn = null
                onExit()
            },
            onRetry = {
                // A confirmed failure is a NEW attempt, so drop the old key and send
                // the user back to the form rather than resubmitting behind their back.
                holder.clearPaymentAttempt()
                statusTxn = null
            }
        )
    } else {
        FastagRechargeScreen(
            state = holder.state,
            onBack = onExit,
            onSelectBiller = holder::onSelectBiller,
            onParamChange = holder::onParamChange,
            onVerify = holder::onVerify,
            onChangeIssuer = holder::onChangeIssuer,
            onAmountChange = holder::onAmountChange,
            onPay = holder::beginPaymentAttempt
        )
    }
}