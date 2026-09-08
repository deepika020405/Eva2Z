@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.developer.ev2z.ui.screens.fastag

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.developer.ev2z.data.local.StorageManager
import kotlin.time.Clock

/**
 * State holder for [FastagRechargeScreen].
 *
 * Deliberately framework-free — no ViewModel, no coroutine scope — so it can be driven
 * from a ViewModel, a Molecule presenter, or `remember { }` in a screen. Wire the four
 * operations through [Callbacks] to whatever the data layer looks like.
 *
 * Three behaviours here are not cosmetic and should survive any refactor:
 *
 *  1. Editing an identifier after a successful verify DROPS the verification. Otherwise
 *     a user can verify plate A, change it to plate B, and pay against a token bound to A.
 *
 *  2. `referenceId` is generated once per PAYMENT ATTEMPT and reused for every network
 *     retry of that attempt. A declined payment gets a NEW one. Mixing these up either
 *     double-charges the user or locks them out of retrying — see [beginPaymentAttempt].
 *
 *  3. Amounts live as a rupee string in the UI and become paise exactly once, at submit.
 *     No Double, ever.
 */
class FastagRechargeStateHolder(
    private val callbacks: Callbacks,
    private val referenceIdStore: ReferenceIdStore,
    vehicleId: String? = null,
    vehicleNumber: String? = null
) {

    /**
     * Durable storage for the in-flight idempotency key.
     *
     * This is an interface rather than a field on purpose. An in-memory key does not
     * survive process death, so a user whose app is killed mid-payment comes back,
     * taps pay, mints a NEW key and gets charged twice for one recharge. Back this
     * with anything that survives a cold start — [StorageManagerReferenceIdStore] uses
     * the app's own StorageManager (SharedPreferences on Android, UserDefaults on iOS).
     *
     * [save] must complete before the network call is made, not after.
     */
    interface ReferenceIdStore {
        fun save(referenceId: String)
        fun load(): String?
        fun clear()
    }

    interface Callbacks {
        /** GET fastag/billers */
        fun loadBillers(onResult: (List<FastagBiller>) -> Unit, onError: (String) -> Unit)

        /** GET fastag/billers/{id}/params */
        fun loadBillerConfig(
            billerId: String,
            onResult: (FastagBillerConfig) -> Unit,
            onError: (String) -> Unit
        )

        /** POST fastag/validate */
        fun validate(
            billerId: String,
            customerParams: Map<String, String>,
            vehicleId: String?,
            onResult: (FastagValidation) -> Unit,
            onError: (String) -> Unit
        )

        /**
         * POST fastag/recharge with `Idempotency-Key: referenceId`.
         * Hands off to the aggregator's payment SDK on success.
         */
        fun startRecharge(
            referenceId: String,
            validationToken: String,
            amountPaise: Long,
            vehicleId: String?,
            onAccepted: (FastagTransaction) -> Unit,
            onError: (String) -> Unit
        )
    }

    var state by mutableStateOf(
        FastagRechargeUiState(vehicleId = vehicleId, vehicleNumber = vehicleNumber)
    )
        private set

    /** The accepted transaction, once the server has taken the recharge. */
    var acceptedTransaction by mutableStateOf<FastagTransaction?>(null)
        private set

    /**
     * Current payment attempt key. Read through from durable storage so a cold start
     * mid-payment resumes the same attempt instead of starting a second one.
     */
    private var pendingReferenceId: String?
        get() = referenceIdStore.load()
        set(value) {
            if (value == null) referenceIdStore.clear() else referenceIdStore.save(value)
        }

    // ───────────────────────────── Loading ─────────────────────────

    fun start() {
        if (state.billers.isNotEmpty() || state.billersLoading) return
        state = state.copy(billersLoading = true)
        callbacks.loadBillers(
            onResult = { billers -> state = state.copy(billers = billers, billersLoading = false) },
            onError = { msg -> state = state.copy(billersLoading = false, submitError = msg) }
        )
    }

    fun onSelectBiller(biller: FastagBiller) {
        // A new issuer means a new schema and a dead validation token.
        state = state.copy(
            selectedBiller = biller,
            billerConfig = null,
            configLoading = true,
            paramValues = emptyMap(),
            paramErrors = emptyMap(),
            validation = null,
            amountInput = "",
            amountError = null,
            submitError = null
        )
        callbacks.loadBillerConfig(
            billerId = biller.billerId,
            onResult = { config ->
                // Prefill the vehicle number when the schema actually asks for one.
                // Some issuers key on a Tag ID, in which case there is nothing to prefill.
                val prefill = buildMap {
                    val vrnParam = config.customerParams
                        .firstOrNull { it.name.equals("vehicleNumber", ignoreCase = true) }
                    val vrn = state.vehicleNumber
                    if (vrnParam != null && !vrn.isNullOrBlank()) {
                        put(vrnParam.name, stripHsrpPrefix(normalizeVrn(vrn)))
                    }
                }
                // No errors seeded from the prefill. Showing a red field the user has
                // never touched is hostile; if a stored plate is malformed they will
                // see it when they tap Verify.
                state = state.copy(
                    billerConfig = config,
                    configLoading = false,
                    paramValues = prefill,
                    paramErrors = emptyMap()
                )
            },
            onError = { msg -> state = state.copy(configLoading = false, submitError = msg) }
        )
    }

    fun onChangeIssuer() {
        state = state.copy(
            selectedBiller = null,
            billerConfig = null,
            paramValues = emptyMap(),
            paramErrors = emptyMap(),
            validation = null,
            amountInput = "",
            amountError = null
        )
    }

    // ──────────────────────── Identifier fields ────────────────────

    fun onParamChange(paramName: String, raw: String) {
        val param = state.billerConfig?.customerParams?.firstOrNull { it.name == paramName } ?: return
        val sanitized = sanitizeParamInput(param, raw)

        state = state.copy(
            paramValues = state.paramValues + (paramName to sanitized),
            paramErrors = state.paramErrors + (paramName to validateParam(param, sanitized)),
            // Any edit invalidates a previous verification. Without this the user could
            // verify one tag and pay against another.
            validation = null,
            amountError = null,
            submitError = null
        )
    }

    fun onVerify() {
        val biller = state.selectedBiller ?: return
        val config = state.billerConfig ?: return

        // Re-validate everything before hitting the network, including untouched fields.
        val errors = config.customerParams.associate { p ->
            p.name to validateParam(p, state.paramValues[p.name].orEmpty())
        }
        if (errors.values.any { it != null }) {
            state = state.copy(paramErrors = errors)
            return
        }

        // Trim on the way out. validateParam trims before checking, so an untrimmed
        // value passes validation and would otherwise be transmitted with the space.
        val payload = config.customerParams.associate { p ->
            p.name to state.paramValues[p.name].orEmpty().trim()
        }

        state = state.copy(validating = true, paramErrors = errors, submitError = null)
        callbacks.validate(
            billerId = biller.billerId,
            customerParams = payload,
            vehicleId = state.vehicleId,
            onResult = { result ->
                state = state.copy(validating = false, validation = result)
            },
            onError = { msg ->
                state = state.copy(
                    validating = false,
                    validation = FastagValidation(valid = false, failureMessage = msg)
                )
            }
        )
    }

    // ─────────────────────────── Amount ────────────────────────────

    fun onAmountChange(input: String) {
        val limits = state.validation?.amountLimits ?: state.billerConfig?.amountLimits
        val paise = rupeesToPaise(input)
        state = state.copy(
            amountInput = input,
            amountError = validateAmount(paise, limits),
            submitError = null
        )
    }

    // ─────────────────────────── Payment ───────────────────────────

    /**
     * Starts a NEW payment attempt. Call this from the pay button and from "Try again"
     * after a confirmed failure — both are new attempts and must not share a key.
     *
     * Do NOT call this to retry a network timeout. For that, call [retryCurrentAttempt],
     * which reuses the existing key so the server deduplicates instead of charging twice.
     */
    fun beginPaymentAttempt() {
        // Re-check the whole precondition set here rather than trusting that
        // onParamChange was the only mutation path. This is the last gate before money.
        val validation = state.validation ?: return
        if (!validation.valid) return
        val token = validation.validationToken ?: return
        val config = state.billerConfig ?: return
        if (config.customerParams.any { validateParam(it, state.paramValues[it.name].orEmpty()) != null }) return

        val amount = state.amountPaise ?: return
        if (state.amountError != null) return
        if (validateAmount(amount, validation.amountLimits ?: config.amountLimits) != null) return

        // Persist BEFORE the network call — a crash between here and the response must
        // not lose the key, or the retry becomes a second charge.
        val ref = newReferenceId()
        pendingReferenceId = ref
        submit(ref, token, amount)
    }

    /** Same attempt, same key — safe after a dropped connection. */
    fun retryCurrentAttempt() {
        val ref = pendingReferenceId ?: return beginPaymentAttempt()
        val token = state.validation?.validationToken ?: return
        val amount = state.amountPaise ?: return
        submit(ref, token, amount)
    }

    /** Call after a terminal FAILED, ABANDONED or SUCCESS so the next tap mints a fresh key. */
    fun clearPaymentAttempt() {
        pendingReferenceId = null
        acceptedTransaction = null
        state = state.copy(submitting = false)
    }

    private fun submit(referenceId: String, validationToken: String, amountPaise: Long) {
        state = state.copy(submitting = true, submitError = null)
        callbacks.startRecharge(
            referenceId = referenceId,
            validationToken = validationToken,
            amountPaise = amountPaise,
            vehicleId = state.vehicleId,
            onAccepted = { txn ->
                // Surface the transaction — the caller navigates to FastagStatusScreen
                // with it. Dropping it here would strand the user on the form.
                acceptedTransaction = txn
                state = state.copy(submitting = false)
            },
            onError = { msg -> state = state.copy(submitting = false, submitError = msg) }
        )
    }

    /**
     * ULID-ish: sortable timestamp prefix plus randomness. Any collision-resistant
     * scheme works — what matters is that it is generated once and persisted locally
     * BEFORE the network call, so an app restart mid-request does not mint a second key.
     */
    private fun newReferenceId(): String {
        val ts = Clock.System.now().toEpochMilliseconds().toString(36).uppercase()
        val rand = (1..10)
            .map { CROCKFORD.random() }
            .joinToString("")
        return "eva_rch_$ts$rand"
    }

    private companion object {
        val CROCKFORD = "0123456789ABCDEFGHJKMNPQRSTVWXYZ".toList()
    }
}

/**
 * Default [FastagRechargeStateHolder.ReferenceIdStore] backed by the app's
 * [StorageManager] — SharedPreferences on Android, NSUserDefaults on iOS.
 *
 * Both platform implementations write synchronously, which is what this needs: the key
 * has to be on disk before the recharge request leaves the device. An async write would
 * lose it if the process died in the window between the call and the flush — the exact
 * double-charge this class exists to prevent.
 */
class StorageManagerReferenceIdStore(
    private val storage: StorageManager,
    private val key: String = "fastag_pending_reference_id"
) : FastagRechargeStateHolder.ReferenceIdStore {

    override fun save(referenceId: String) {
        storage.saveString(key, referenceId)
    }

    override fun load(): String? = storage.getString(key, "").takeIf { it.isNotBlank() }

    override fun clear() {
        storage.remove(key)
    }
}