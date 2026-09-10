package com.developer.ev2z.ui.screens.CriminalVerification

import com.developer.ev2z.data.local.UserPrefs
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * One submitted verification, kept on device so the user can come back later and
 * check a status with the candidate id that the server generated for them.
 */
@Serializable
data class BgvHistoryEntry(
    val candidateId: Int,
    val name: String = "",
    val phone: String = "",
    val amount: Int = 0,
    val merchantOrderId: String? = null,
    val paymentDone: Boolean = false,
    val status: String = BgvStatus.VERIFICATION_STARTED.name,
    val createdAtMs: Long = 0L
)

private const val KEY_BGV_HISTORY = "bgv_history"
private const val MAX_ENTRIES = 30

class BgvHistoryStore(private val prefs: UserPrefs) {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    fun getAll(): List<BgvHistoryEntry> {
        val raw = prefs.getString(KEY_BGV_HISTORY, "")
        if (raw.isBlank()) return emptyList()
        return runCatching {
            json.decodeFromString<List<BgvHistoryEntry>>(raw)
        }.getOrElse { emptyList() }
    }

    /**
     * Adds the entry, or refreshes the one already stored for this candidate id.
     * Merging keeps payment facts (amount, order id, paid flag, submitted-at) that a
     * later status refresh does not know about.
     */
    fun upsert(entry: BgvHistoryEntry): List<BgvHistoryEntry> {
        val existing = getAll()
        val old = existing.firstOrNull { it.candidateId == entry.candidateId }

        val merged = if (old == null) entry else entry.copy(
            name            = entry.name.ifBlank { old.name },
            phone           = entry.phone.ifBlank { old.phone },
            amount          = if (entry.amount > 0) entry.amount else old.amount,
            merchantOrderId = entry.merchantOrderId ?: old.merchantOrderId,
            paymentDone     = entry.paymentDone || old.paymentDone,
            createdAtMs     = if (entry.createdAtMs > 0) entry.createdAtMs else old.createdAtMs
        )

        val updated = listOf(merged) +
                existing.filter { it.candidateId != entry.candidateId }.take(MAX_ENTRIES - 1)

        save(updated)
        return updated
    }

    fun updateStatus(candidateId: Int, status: String): List<BgvHistoryEntry> {
        val updated = getAll().map {
            if (it.candidateId == candidateId) it.copy(status = status) else it
        }
        save(updated)
        return updated
    }

    fun remove(candidateId: Int): List<BgvHistoryEntry> {
        val updated = getAll().filter { it.candidateId != candidateId }
        save(updated)
        return updated
    }

    private fun save(entries: List<BgvHistoryEntry>) {
        prefs.saveString(KEY_BGV_HISTORY, json.encodeToString(entries))
    }
}