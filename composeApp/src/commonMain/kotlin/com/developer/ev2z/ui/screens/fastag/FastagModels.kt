package com.developer.ev2z.ui.screens.fastag

/**
 * FASTag domain models for Eva2z Connect.
 *
 * Design note that matters more than anything else in this file:
 * [FastagBillerConfig.customerParams] is a SCHEMA, not a fixed set of fields.
 * Most issuers ask for a vehicle registration number, but several ask for a Tag ID,
 * a Wallet ID, or a registered mobile number instead. The recharge screen renders
 * whatever this list contains. Never hardcode a "Vehicle Number" field.
 *
 * Everything here is pure Kotlin so it compiles for both Android and iOS from
 * commonMain — no java.*, no platform formatting.
 */

// ───────────────────────────── Biller ──────────────────────────────

data class FastagBiller(
    val billerId: String,
    val name: String,
    /** Two-letter code shown in the avatar tile, e.g. "IC" for ICICI. */
    val shortCode: String,
    /** False when the issuer never returns a balance. Drives UI copy, not an error. */
    val supportsBalanceEnquiry: Boolean = true,
    val active: Boolean = true
)

// ───────────────────── Dynamic parameter schema ────────────────────

enum class FastagParamType { ALPHANUMERIC, NUMERIC, ALPHABETIC }

data class FastagBillerParam(
    /** Machine name sent back to the server, e.g. "vehicleNumber", "tagId". */
    val name: String,
    /** Human label rendered on the field. Comes from the server. */
    val label: String,
    val dataType: FastagParamType = FastagParamType.ALPHANUMERIC,
    /** Server-supplied pattern. Client-side check is a UX nicety; server re-validates. */
    val regex: String? = null,
    val minLength: Int = 1,
    val maxLength: Int = 32,
    val optional: Boolean = false,
    val helperText: String? = null
)

data class FastagAmountLimits(val minPaise: Long, val maxPaise: Long)

data class FastagBillerConfig(
    val billerId: String,
    /** Bump forces the client to discard a cached schema. */
    val paramsVersion: String,
    val customerParams: List<FastagBillerParam>,
    val amountLimits: FastagAmountLimits,
    val amountPresetsPaise: List<Long>
)

// ───────────────────────── Validation result ───────────────────────

data class FastagValidation(
    val valid: Boolean,
    val customerName: String? = null,
    val balancePaise: Long? = null,
    /** False = issuer does not share balance. This is normal, not a failure. */
    val balanceAvailable: Boolean = false,
    val amountLimits: FastagAmountLimits? = null,
    val amountPresetsPaise: List<Long> = emptyList(),
    /** Binds issuer + identifier to the recharge call so the client can't swap them. */
    val validationToken: String? = null,
    val failureMessage: String? = null
)

// ───────────────────────── Transaction state ───────────────────────

/**
 * Mirrors the server state machine. Note that FAILED and DEEMED are very different
 * things to a user: FAILED means nothing was debited, DEEMED means money left their
 * account. The UI must never conflate them.
 */
enum class FastagTxnStatus {
    INITIATED,
    PENDING,
    SUCCESS,
    FAILED,
    DEEMED,
    REFUND_PENDING,
    REFUNDED,
    ABANDONED,
    EXPIRED;

    val isTerminal: Boolean
        get() = this == SUCCESS || this == FAILED || this == REFUNDED ||
                this == ABANDONED || this == EXPIRED

    /** True when the user's money has left their account and is not yet back. */
    val moneyIsOut: Boolean
        get() = this == DEEMED || this == REFUND_PENDING || this == SUCCESS
}

data class FastagTransaction(
    val referenceId: String,
    val status: FastagTxnStatus,
    /** Credited to the tag. */
    val amountPaise: Long,
    val convenienceFeePaise: Long = 0L,
    val vehicleId: String? = null,
    /** VRN for display. Null for issuers that don't key on a vehicle number. */
    val displayIdentifier: String? = null,
    val billerName: String,
    val issuerRefId: String? = null,
    val newBalancePaise: Long? = null,
    val initiatedAtMillis: Long,
    val completedAtMillis: Long? = null,
    val failureReason: String? = null,
    val refundExpectedByMillis: Long? = null
) {
    /**
     * What actually left the customer's instrument.
     *
     * Computed, NOT a constructor default. As a defaulted val it would be frozen at
     * construction and `copy(convenienceFeePaise = …)` would leave it stale — a receipt
     * whose total does not match its own line items.
     */
    val totalDebitedPaise: Long
        get() = amountPaise + convenienceFeePaise
}

// ───────────────────────── Screen UI state ─────────────────────────

/** One step of the recharge form. The screen reveals them progressively. */
enum class FastagStep { PICK_ISSUER, ENTER_IDENTIFIER, VERIFIED, READY_TO_PAY }

data class FastagRechargeUiState(
    val vehicleId: String? = null,
    val vehicleNumber: String? = null,

    val billers: List<FastagBiller> = emptyList(),
    val billersLoading: Boolean = false,

    val selectedBiller: FastagBiller? = null,
    val billerConfig: FastagBillerConfig? = null,
    val configLoading: Boolean = false,

    /** paramName -> raw user input. */
    val paramValues: Map<String, String> = emptyMap(),
    /** paramName -> inline error, or null when valid. */
    val paramErrors: Map<String, String?> = emptyMap(),

    val validating: Boolean = false,
    val validation: FastagValidation? = null,

    /** Rupees as typed. Converted to paise only at submit. */
    val amountInput: String = "",
    val amountError: String? = null,
    val convenienceFeePaise: Long = 0L,

    val submitting: Boolean = false,
    val submitError: String? = null
) {
    val step: FastagStep
        get() = when {
            selectedBiller == null -> FastagStep.PICK_ISSUER
            validation?.valid != true -> FastagStep.ENTER_IDENTIFIER
            amountPaise == null || amountError != null -> FastagStep.VERIFIED
            else -> FastagStep.READY_TO_PAY
        }

    val amountPaise: Long?
        get() = rupeesToPaise(amountInput)

    val totalPayablePaise: Long?
        get() = amountPaise?.let { it + convenienceFeePaise }

    val canVerify: Boolean
        get() = selectedBiller != null &&
                billerConfig != null &&
                !validating &&
                billerConfig.customerParams.all { p ->
                    val v = paramValues[p.name].orEmpty().trim()
                    (p.optional || v.isNotEmpty()) && paramErrors[p.name] == null
                }

    val canPay: Boolean
        get() = step == FastagStep.READY_TO_PAY && !submitting
}

// ─────────────────────────── Field helpers ─────────────────────────

/**
 * Uppercases and strips separators. Runs on every keystroke, so it must never remove
 * a character the user legitimately typed.
 *
 * It deliberately does NOT strip the `IND` prefix from HSRP plates. Doing so silently
 * corrupts real plates — "INDORE1234" becomes "ORE1234", and "IN D1234" becomes "1234"
 * because removing the space manufactures a false prefix. Users type the plate, not the
 * blue HSRP strip; handle a pasted `IND` prefix in [stripHsrpPrefix] on submit instead,
 * where it can be applied once with a guard rather than on every character.
 */
fun normalizeVrn(input: String): String =
    input.uppercase().replace(Regex("[^A-Z0-9]"), "")

/**
 * One-shot `IND` removal for pasted values. Applied only when what follows still looks
 * like a plate, so genuine "IND…" plates survive.
 */
fun stripHsrpPrefix(value: String): String {
    if (!value.startsWith("IND")) return value
    val rest = value.removePrefix("IND")
    return if (looksLikeVrn(rest)) rest else value
}

/** Eva2z-side plate patterns. Used for prefill sanity only — the biller's regex wins. */
private val VRN_STANDARD = Regex("^[A-Z]{2}[0-9]{1,2}[A-Z]{0,3}[0-9]{4}$")
private val VRN_BH = Regex("^[0-9]{2}BH[0-9]{4}[A-Z]{1,2}$")

fun looksLikeVrn(value: String): Boolean =
    VRN_STANDARD.matches(value) || VRN_BH.matches(value)

/** Applies the normalisation appropriate to a param's declared type. */
fun sanitizeParamInput(param: FastagBillerParam, raw: String): String {
    val trimmed = raw.take(param.maxLength.coerceAtLeast(1))
    return when {
        param.name.equals("vehicleNumber", ignoreCase = true) -> normalizeVrn(trimmed)
        param.dataType == FastagParamType.NUMERIC -> trimmed.filter { it.isDigit() }
        param.dataType == FastagParamType.ALPHABETIC -> trimmed.filter { it.isLetter() }
        else -> trimmed
    }
}

/**
 * Client-side field validation. Returns null when valid.
 * Deliberately permissive on the regex: a plate that fails the pattern but is genuinely
 * valid must not be hard-blocked, so an unmatched regex warns rather than errors when
 * the length is otherwise plausible.
 */
fun validateParam(param: FastagBillerParam, raw: String): String? {
    val v = raw.trim()
    if (v.isEmpty()) return if (param.optional) null else "${param.label} is required"
    if (v.length < param.minLength) return "Enter at least ${param.minLength} characters"
    if (v.length > param.maxLength) return "Cannot be longer than ${param.maxLength} characters"
    val pattern = param.regex
    if (!pattern.isNullOrBlank()) {
        val ok = runCatching { Regex(pattern).matches(v) }.getOrDefault(true)
        if (!ok) return "This doesn't look right. Check and try again."
    }
    return null
}

fun validateAmount(paise: Long?, limits: FastagAmountLimits?): String? {
    if (paise == null) return null            // nothing typed yet — not an error
    if (paise <= 0L) return "Enter an amount"
    if (limits == null) return null
    if (paise < limits.minPaise) return "Minimum recharge is ${formatRupees(limits.minPaise)}"
    if (paise > limits.maxPaise) return "Maximum recharge is ${formatRupees(limits.maxPaise)}"
    return null
}

// ────────────────────────── Money formatting ───────────────────────

/** Largest amount this feature will parse: ₹10,00,000. Guards against Long overflow. */
private const val MAX_PARSEABLE_PAISE = 100_000_000L

/**
 * Rupee string -> paise. Returns null for blank, signed, overflowing or unparseable
 * input rather than 0, so "nothing typed" and "typed zero" stay distinguishable.
 *
 * Signs are rejected outright: `toLongOrNull` happily accepts "-100", which would flow
 * into the formatter and render "₹-,100". Fractions beyond two digits are ROUNDED, not
 * truncated — silently dropping a paise off a payment amount is not acceptable.
 */
fun rupeesToPaise(input: String): Long? {
    val cleaned = input.replace(",", "").replace(" ", "").trim()
    if (cleaned.isEmpty()) return null
    if (cleaned.any { !it.isDigit() && it != '.' }) return null   // rejects + and -

    val parts = cleaned.split(".")
    if (parts.size > 2) return null

    val whole = parts[0].ifEmpty { "0" }.toLongOrNull() ?: return null
    if (whole < 0 || whole > MAX_PARSEABLE_PAISE / 100) return null

    val fracDigits = parts.getOrElse(1) { "" }
    val frac = when {
        fracDigits.isEmpty() -> 0L
        else -> {
            // Round on the third decimal rather than truncating.
            val padded = fracDigits.padEnd(3, '0')
            val hundredths = padded.take(2).toLongOrNull() ?: return null
            val thousandths = padded[2].digitToIntOrNull() ?: return null
            if (thousandths >= 5) hundredths + 1 else hundredths
        }
    }
    val total = whole * 100 + frac
    return if (total in 0..MAX_PARSEABLE_PAISE) total else null
}

/** Indian digit grouping: 1,00,000 not 100,000. Sign-safe. */
private fun groupIndian(value: Long): String {
    val negative = value < 0
    val s = if (value < 0) (-value).toString() else value.toString()
    val body = if (s.length <= 3) s else {
        val last3 = s.takeLast(3)
        val grouped = s.dropLast(3).reversed().chunked(2).joinToString(",").reversed()
        "$grouped,$last3"
    }
    return if (negative) "-$body" else body
}

/** "₹1,234.50" — always two decimals, always from paise. */
fun formatRupees(paise: Long): String {
    val negative = paise < 0
    val abs = if (paise < 0) -paise else paise
    val body = "${groupIndian(abs / 100)}.${(abs % 100).toInt().toString().padStart(2, '0')}"
    return if (negative) "-₹$body" else "₹$body"
}

/** "₹500" — no decimals, for compact chips and buttons. */
fun formatRupeesShort(paise: Long): String = "₹${groupIndian(paise / 100)}"

// ─────────────────────── Sample data for previews ──────────────────

object FastagSampleData {

    val billers = listOf(
        FastagBiller("BLR_ICICI", "ICICI Bank FASTag", "IC"),
        FastagBiller("BLR_PAYTM", "Paytm Payments Bank FASTag", "PY"),
        FastagBiller("BLR_IDFC", "IDFC FIRST Bank FASTag", "ID"),
        FastagBiller("BLR_HDFC", "HDFC Bank FASTag", "HD"),
        FastagBiller("BLR_AXIS", "Axis Bank FASTag", "AX", supportsBalanceEnquiry = false),
        FastagBiller("BLR_SBI", "SBI FASTag", "SB"),
        FastagBiller("BLR_AIRTEL", "Airtel Payments Bank FASTag", "AI"),
        FastagBiller("BLR_KOTAK", "Kotak Mahindra Bank FASTag", "KO"),
        FastagBiller("BLR_BOB", "Bank of Baroda FASTag", "BO"),
        FastagBiller("BLR_INDUS", "IndusInd Bank FASTag", "IN"),
        FastagBiller("BLR_FEDERAL", "Federal Bank FASTag", "FE"),
        FastagBiller("BLR_EQUITAS", "Equitas Small Finance Bank FASTag", "EQ")
    )

    /** Typical VRN-keyed issuer. */
    val vrnConfig = FastagBillerConfig(
        billerId = "BLR_ICICI",
        paramsVersion = "2026-08-14",
        customerParams = listOf(
            FastagBillerParam(
                name = "vehicleNumber",
                label = "Vehicle registration number",
                dataType = FastagParamType.ALPHANUMERIC,
                regex = "^[A-Z0-9]{8,11}$",
                minLength = 8,
                maxLength = 11,
                helperText = "As printed on the RC, without spaces"
            )
        ),
        amountLimits = FastagAmountLimits(minPaise = 10_000L, maxPaise = 2_000_000L),
        amountPresetsPaise = listOf(20_000L, 50_000L, 100_000L, 200_000L)
    )

    /**
     * A Tag-ID keyed issuer. Keep this around and test with it — it is the schema that
     * exposes any hardcoded "Vehicle Number" assumption in the UI.
     */
    val tagIdConfig = FastagBillerConfig(
        billerId = "BLR_OTHER",
        paramsVersion = "2026-08-14",
        customerParams = listOf(
            FastagBillerParam(
                name = "tagId",
                label = "Tag ID",
                dataType = FastagParamType.NUMERIC,
                minLength = 16,
                maxLength = 16,
                helperText = "16-digit number on the back of the tag"
            ),
            FastagBillerParam(
                name = "mobileNumber",
                label = "Registered mobile number",
                dataType = FastagParamType.NUMERIC,
                regex = "^[6-9][0-9]{9}$",
                minLength = 10,
                maxLength = 10
            )
        ),
        amountLimits = FastagAmountLimits(minPaise = 10_000L, maxPaise = 1_000_000L),
        amountPresetsPaise = listOf(20_000L, 50_000L, 100_000L)
    )
}