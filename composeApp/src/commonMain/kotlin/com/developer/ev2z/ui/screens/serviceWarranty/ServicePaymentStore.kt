package com.developer.ev2z.ui.screens.serviceWarranty

import com.developer.ev2z.data.local.StorageManager
import com.developer.ev2z.data.local.createStorageManager
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Service-charge receipts, kept on the device and keyed by ticket number.
 *
 * `service/my-complaints` answers a paid row with a null `merchant_order_id` and no
 * `paid_at`, so the only full account of a charge is the one the app held in memory
 * while it was being paid. That is why the detail screen showed the charge straight
 * after raising a complaint and nothing at all on the next launch: the receipt was
 * handed over as a navigation argument and died with the process.
 *
 * This is the record that survives. It is written the moment the ticket is filed and
 * read back by [ComplaintDetailViewModel] on every later visit, which is what keeps
 * "Service charge", "Paid on" and "Payment ref" on the card after a restart.
 *
 * Receipts live under the same storage the session does, so logging out clears them.
 */
object ServicePaymentStore {

    private const val KEY = "service_payment_receipts"

    /** Receipts are a few dozen bytes each; this is years of complaints. */
    private const val MAX_ENTRIES = 50

    // Lazy because on Android createStorageManager() needs the Context MainActivity
    // installs at startup — resolving it at class-init time would race that.
    private val storage: StorageManager by lazy { createStorageManager() }

    private val json = Json { ignoreUnknownKeys = true }

    /** The stored shape. [ServicePaymentReceipt] itself is not serializable. */
    @Serializable
    private data class Entry(
        val merchantOrderId: String,
        val phonepeOrderId: String? = null,
        val amountRupees: Int,
        val paidAtMillis: Long,
    )

    /**
     * Records that [receipt] paid for [ticketId].
     *
     * Overwrites any earlier receipt for the same ticket — a second charge on one
     * complaint means the later one is the live reference.
     */
    fun save(ticketId: String, receipt: ServicePaymentReceipt) {
        val id = ticketId.trim()
        if (id.isEmpty()) return

        val entry = Entry(
            merchantOrderId = receipt.merchantOrderId,
            phonepeOrderId = receipt.phonepeOrderId,
            amountRupees = receipt.amountRupees,
            paidAtMillis = receipt.paidAtMillis,
        )

        // Oldest receipts drop out first, so the entry just paid is never the one
        // evicted however long the list has grown.
        val merged = (read() + (id to entry)).let { all ->
            if (all.size <= MAX_ENTRIES) all
            else all.entries
                .sortedByDescending { it.value.paidAtMillis }
                .take(MAX_ENTRIES)
                .associate { it.key to it.value }
        }

        runCatching { storage.saveString(KEY, json.encodeToString(merged)) }
            .onFailure { println("⚠️ Couldn't store the receipt for $id: ${it.message}") }
    }

    /** The charge recorded against [ticketId], or null if this device never paid it. */
    fun receiptFor(ticketId: String): ServicePaymentReceipt? {
        val entry = read()[ticketId.trim()] ?: return null
        return ServicePaymentReceipt(
            merchantOrderId = entry.merchantOrderId,
            phonepeOrderId = entry.phonepeOrderId,
            amountRupees = entry.amountRupees,
            paidAtMillis = entry.paidAtMillis,
        )
    }

    // A store that cannot be parsed is treated as empty rather than fatal: a lost
    // receipt costs one line on a card, and throwing here would take the screen down.
    private fun read(): Map<String, Entry> = runCatching {
        storage.getString(KEY, "")
            .takeIf { it.isNotBlank() }
            ?.let { json.decodeFromString<Map<String, Entry>>(it) }
    }.getOrElse {
        println("⚠️ Couldn't read stored service receipts: ${it.message}")
        null
    } ?: emptyMap()
}