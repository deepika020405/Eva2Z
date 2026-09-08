package com.developer.ev2z.ui.screens.fastag

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * The seam between the FASTag UI and whatever serves it.
 *
 * Four operations, mapping one-to-one onto the four backend endpoints the feature needs:
 * biller list, biller params, validate, recharge. [FastagRechargeStateHolder.Callbacks]
 * adapts these into the callback shape the state holder wants.
 *
 * **Never call a BBPS aggregator directly from the app.** Every one of these must hit
 * `gps.eva2z.in`, which holds the aggregator keys and receives the webhooks. Keys in an
 * APK are keys in the hands of anyone who runs `jadx` on the release build.
 */
interface FastagDataSource {

    /** GET fastag/billers */
    suspend fun billers(): Result<List<FastagBiller>>

    /** GET fastag/billers/{billerId}/params */
    suspend fun billerConfig(billerId: String): Result<FastagBillerConfig>

    /** POST fastag/validate */
    suspend fun validate(
        billerId: String,
        customerParams: Map<String, String>,
        vehicleId: String?
    ): Result<FastagValidation>

    /** POST fastag/recharge, with `Idempotency-Key: referenceId`. */
    suspend fun startRecharge(
        referenceId: String,
        validationToken: String,
        amountPaise: Long,
        vehicleId: String?
    ): Result<FastagTransaction>

    /** The vehicles shown in the Home FASTag section, newest balance first. */
    suspend fun linkedVehicles(): Result<List<FastagVehicleBalance>>
}

/**
 * Local data source used until the FASTag endpoints exist on `gps.eva2z.in`.
 *
 * It serves the real issuer list and the real dynamic-schema behaviour so the whole
 * form, its validation and its error states can be exercised end to end. What it
 * deliberately does NOT do is fake a payment: [startRecharge] returns a plain
 * "not live yet" message rather than a synthetic SUCCESS. A screen that says money
 * moved when it did not is the single worst bug this feature could ship with.
 *
 * Replace this with a Ktor-backed implementation once the endpoints land — nothing
 * outside this file needs to change.
 */
class LocalFastagDataSource : FastagDataSource {

    override suspend fun billers(): Result<List<FastagBiller>> {
        delay(250)
        return Result.success(FastagSampleData.billers)
    }

    override suspend fun billerConfig(billerId: String): Result<FastagBillerConfig> {
        delay(200)
        // Most issuers key on the plate; a couple key on the tag itself. Serving both
        // shapes here is what keeps a hardcoded "Vehicle Number" field from creeping in.
        val config = when (billerId) {
            "BLR_PAYTM", "BLR_AIRTEL" -> FastagSampleData.tagIdConfig.copy(billerId = billerId)
            else -> FastagSampleData.vrnConfig.copy(billerId = billerId)
        }
        return Result.success(config)
    }

    override suspend fun validate(
        billerId: String,
        customerParams: Map<String, String>,
        vehicleId: String?
    ): Result<FastagValidation> {
        delay(600)
        val biller = FastagSampleData.billers.firstOrNull { it.billerId == billerId }
        val config = billerConfig(billerId).getOrNull()
        return Result.success(
            FastagValidation(
                valid = true,
                customerName = null,      // only the issuer can supply this
                balancePaise = null,      // and the balance
                balanceAvailable = biller?.supportsBalanceEnquiry == true,
                amountLimits = config?.amountLimits,
                amountPresetsPaise = config?.amountPresetsPaise.orEmpty(),
                validationToken = "local-preview-token"
            )
        )
    }

    override suspend fun startRecharge(
        referenceId: String,
        validationToken: String,
        amountPaise: Long,
        vehicleId: String?
    ): Result<FastagTransaction> {
        delay(300)
        // No synthetic success, and no synthetic failure either — this is an honest
        // "the rails aren't connected", not a payment outcome.
        return Result.failure(
            IllegalStateException(
                "FASTag recharge isn't live yet. Your details are correct and nothing " +
                        "has been charged — we'll enable payments shortly."
            )
        )
    }

    override suspend fun linkedVehicles(): Result<List<FastagVehicleBalance>> =
        Result.success(emptyList())
}

/**
 * Adapts a [FastagDataSource] to the callback interface the state holder consumes,
 * launching each call on [scope]. Kept separate so the state holder stays free of
 * coroutines and the data source stays free of Compose.
 */
class FastagCallbacks(
    private val source: FastagDataSource,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main)
) : FastagRechargeStateHolder.Callbacks {

    override fun loadBillers(
        onResult: (List<FastagBiller>) -> Unit,
        onError: (String) -> Unit
    ) {
        scope.launch {
            source.billers().fold(onResult) { onError(it.readable("We couldn't load the issuer list.")) }
        }
    }

    override fun loadBillerConfig(
        billerId: String,
        onResult: (FastagBillerConfig) -> Unit,
        onError: (String) -> Unit
    ) {
        scope.launch {
            source.billerConfig(billerId)
                .fold(onResult) { onError(it.readable("We couldn't load this issuer's details.")) }
        }
    }

    override fun validate(
        billerId: String,
        customerParams: Map<String, String>,
        vehicleId: String?,
        onResult: (FastagValidation) -> Unit,
        onError: (String) -> Unit
    ) {
        scope.launch {
            source.validate(billerId, customerParams, vehicleId)
                .fold(onResult) { onError(it.readable("We couldn't reach the issuer just now.")) }
        }
    }

    override fun startRecharge(
        referenceId: String,
        validationToken: String,
        amountPaise: Long,
        vehicleId: String?,
        onAccepted: (FastagTransaction) -> Unit,
        onError: (String) -> Unit
    ) {
        scope.launch {
            source.startRecharge(referenceId, validationToken, amountPaise, vehicleId)
                .fold(onAccepted) { onError(it.readable("We couldn't start this recharge.")) }
        }
    }
}

/**
 * "Error" and "Failed" are banned from this feature's copy — both are meaningless to a
 * user and frightening when money is involved. Fall back to a plain sentence when the
 * throwable has nothing worth showing.
 */
private fun Throwable.readable(fallback: String): String =
    message?.takeIf { it.isNotBlank() } ?: fallback