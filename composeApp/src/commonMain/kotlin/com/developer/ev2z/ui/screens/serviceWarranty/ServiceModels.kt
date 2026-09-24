package com.developer.ev2z.ui.screens.serviceWarranty

import androidx.compose.ui.graphics.Color
import com.developer.ev2z.theme.AppPageBg
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/* ═══════════════════════════════ PALETTE ═══════════════════════════════════
 *
 * This app has no MaterialTheme wrapper — EVA2ZTheme in theme/Theme.kt is fully
 * commented out, so MaterialTheme.colorScheme would resolve to Material 3's
 * default purple baseline. Every screen therefore paints its own colours, and
 * this object holds the ones used across the Service & Warranty flow. Values
 * match the palette the recent screens already use (HomeTheme.LightPalette,
 * NotificationsScreen, CreatePostScreen).
 */
object ServiceColors {
    val PageBg = AppPageBg
    val CardBg = Color.White

    val Brand = Color(0xFF00A844)        // brand green, same as Home / Community
    val BrandSoft = Color(0xFFDCFCE7)    // brand green tint for chips + icon wells

    val TextPrimary = Color(0xFF0F172A)
    val TextSecondary = Color(0xFF64748B)
    val TextMuted = Color(0xFF94A3B8)
    val BorderSoft = Color(0xFFE2E8F0)

    val Warning = Color(0xFFB45309)
    val WarningSoft = Color(0xFFFEF3C7)
    val Danger = Color(0xFFDC2626)
    val DangerSoft = Color(0xFFFEE2E2)
    val Info = Color(0xFF1D4ED8)
    val InfoSoft = Color(0xFFDBEAFE)
}

/* ═══════════════════════════════ DOMAIN ════════════════════════════════════ */

/**
 * A vehicle/device the complaint is raised against. Populated from the real
 * `get-user-devices` response — see `ApiService.fetchServiceVehicles()`.
 */
data class ServiceVehicle(
    val vehicleId: String,
    val registrationNumber: String,
    val model: String,
    val vin: String,
    val purchaseDate: String? = null,
    val warrantyExpiry: String? = null,
    val warrantyActive: Boolean = false,
)

/**
 * Stands in when `get-user-devices` returns nothing for the account. The complaint
 * still goes through — the server accepts a null `device_id` — this only keeps the
 * ticket displayable without pretending a registration exists.
 */
val UnlinkedVehicle = ServiceVehicle(
    vehicleId = "",
    registrationNumber = "Not linked",
    model = SERVICE_VEHICLE_TYPE,
    vin = "",
)

/**
 * The 12 serviceable components (FRD §4), with the numeric ids `component_id`
 * carries. There is no endpoint to read these from, so they live here.
 *
 * Tyre/Wheel, Horn and Other are wear-and-tear or unclassified items that do not
 * go through warranty validation, so they do not prompt for an invoice.
 */
val ServiceComponentCatalog: List<ServiceComponentItem> = listOf(
    ServiceComponentItem("battery", "Battery", serverId = "2"),
    ServiceComponentItem("motor", "Motor", serverId = "1"),
    ServiceComponentItem("controller", "Controller", serverId = "3"),
    ServiceComponentItem("charger", "Charger", serverId = "4"),
    ServiceComponentItem("display", "Display / Console", serverId = "5"),
    ServiceComponentItem("brakes", "Brakes", serverId = "6"),
    ServiceComponentItem("suspension", "Suspension", serverId = "7"),
    ServiceComponentItem("lights", "Lights", serverId = "8"),
    ServiceComponentItem("gps", "GPS Device", serverId = "9"),
    ServiceComponentItem("tyre", "Tyre / Wheel", requiresInvoice = false, serverId = "10"),
    ServiceComponentItem("horn", "Horn", requiresInvoice = false, serverId = "11"),
    ServiceComponentItem("other", "Other", requiresInvoice = false, serverId = "12"),
)

/** One-time service charge for a complaint raised by a non-EVA2Z customer. */
const val SERVICE_FEE_RUPEES = 299

/**
 * Every vehicle serviced through this flow is an EV scooty — there is no second
 * type to choose from, so the vehicle field states it outright instead of asking.
 * It is deliberately independent of `get-user-devices`: the type is known even when
 * the device list is empty or the call failed.
 */
const val SERVICE_VEHICLE_TYPE = "EV Scooty"

/**
 * Whether the person raising the complaint is on EVA2Z Connect.
 *
 * It is only ever an answer sent to the server — what it costs is the server's
 * call, made on the submission response (`payment_required` / `service_charge`),
 * so nothing here is shown to the user as a price.
 */
enum class ServiceCustomerType(
    val label: String,
    /**
     * What this flow expects to be charged, used for the submit button before the
     * server has said anything and as the amount for a `payment_required` response
     * that names no figure. The server's own quote replaces it as soon as there is
     * one — nothing else in the form states a price.
     */
    val expectedFeeRupees: Int,
    /** What `POST service/complaints` expects in `customer_type`. */
    val apiValue: String,
) {
    EVA2Z_CUSTOMER(
        label = "Yes, I'm an EVA2Z Connect customer",
        expectedFeeRupees = 0,
        apiValue = "existing",
    ),
    NON_CUSTOMER(
        label = "No, I'm not an EVA2Z Connect customer",
        expectedFeeRupees = SERVICE_FEE_RUPEES,
        apiValue = "new",
    );

    companion object {
        fun fromApi(raw: String?): ServiceCustomerType = when (raw?.trim()?.lowercase()) {
            "new", "non_customer", "guest" -> NON_CUSTOMER
            else -> EVA2Z_CUSTOMER
        }
    }
}

/**
 * A service-fee checkout the backend has created, ready to hand to PhonePe.
 *
 * [phonepeOrderId] is PhonePe's own OMO… reference and [merchantOrderId] is ours —
 * they are not interchangeable: the SDK resolves the token against the first, and
 * only the second can be used to confirm the payment with our server.
 */
data class ServicePaymentOrder(
    val merchantOrderId: String,
    val phonepeOrderId: String,
    val token: String,
    val amountRupees: Int,
    /** True when the criminal-verification endpoints stood in for the service ones. */
    val viaLegacyEndpoint: Boolean = false,
    /**
     * True when `POST service/complaints` issued this order itself.
     *
     * It then also owns the ticket: re-posting the complaint with
     * `merchant_order_id` is what turns the paid order into one. The payment
     * itself is still announced on `service/payment-callback` first — that route
     * was missing when this flow was written and is live now.
     */
    val confirmedByResubmit: Boolean = false,
)

/** Proof that the service charge was paid, carried on the ticket it unlocked. */
data class ServicePaymentReceipt(
    val merchantOrderId: String,
    /**
     * PhonePe's own OMO… reference (or the SDK's transaction id when it gives
     * one). Sent alongside [merchantOrderId] on the re-post so the server can
     * look the payment up either way.
     */
    val phonepeOrderId: String? = null,
    val amountRupees: Int,
    val paidAtMillis: Long,
)

/**
 * What `POST service/complaints` answered.
 *
 * The server owns the free-or-paid decision — the dropdown only declares which
 * kind of customer is asking. An EVA2Z Connect customer comes straight back as
 * [Created]; anyone else gets [PaymentRequired], pays, and posts the same
 * complaint again with the payment reference attached.
 */
sealed interface ComplaintSubmission {

    data class Created(val ticket: ServiceTicket) : ComplaintSubmission

    data class PaymentRequired(
        val amountRupees: Int,
        /** The server's own wording for the submit button, e.g. "Pay ₹299 & Submit". */
        val buttonText: String?,
        /**
         * The response's own `payment_status: "SUCCESS"` — the money is in and only
         * the ticket is missing. `service/complaints` owns the whole payment
         * lifecycle, so this field is the only signal there is: it means post again,
         * not open another checkout and not give up.
         */
        val settled: Boolean = false,
        /** The PhonePe checkout, when the server returned one alongside the amount. */
        val order: ServicePaymentOrder?,
    ) : ComplaintSubmission

    data class Rejected(val message: String) : ComplaintSubmission
}

/** One of the serviceable components (FRD §4). */
data class ServiceComponentItem(
    val id: String,
    val label: String,
    /** When true an invoice must be attached before the complaint can be filed. */
    val requiresInvoice: Boolean = true,
    /**
     * The numeric id `POST service/complaints` wants in `component_id`. There is no
     * endpoint to read these from, so they are held in [ServiceComponentCatalog];
     * without one the slug [id] is sent and the server has to resolve it.
     */
    val serverId: String? = null,
) {
    /** What goes on the wire as `component_id`. */
    val apiId: String get() = serverId ?: id
}

/**
 * Ticket lifecycle — the four statuses the service backend actually reports.
 * Anything it sends that isn't one of these collapses onto the nearest stage in
 * `resolveStatus`, so the app never shows a stage the admin panel cannot set.
 */
enum class TicketStatus(val label: String, val stepIndex: Int) {
    SUBMITTED("Submitted", 0),
    OPEN("Open", 1),
    IN_PROGRESS("In progress", 2),
    COMPLETED("Completed", 3);

    val isTerminal: Boolean get() = this == COMPLETED

    companion object {
        /** The stages drawn by the stepper, in order. */
        val stepperStages = listOf(SUBMITTED, OPEN, IN_PROGRESS, COMPLETED)
    }
}

enum class ServicePriority(val label: String) {
    LOW("Low"), NORMAL("Normal"), HIGH("High"), URGENT("Urgent")
}

enum class AttachmentKind { INVOICE, PHOTO, VIDEO }

data class ServiceAttachment(
    val id: String,
    val name: String,
    val sizeBytes: Long,
    val kind: AttachmentKind,
    /** Only held locally until upload; not retained for tickets fetched back. */
    val bytes: ByteArray? = null,
    /** Absolute URL of the stored copy, once the server has echoed one back. */
    val url: String? = null,
    /** The server's `mime_type`, so a PDF invoice isn't described as an image. */
    val mimeType: String? = null,
) {
    // ByteArray breaks data-class equality, so compare by identity fields only.
    override fun equals(other: Any?): Boolean =
        this === other || (other is ServiceAttachment && other.id == id)

    override fun hashCode(): Int = id.hashCode()
}

data class ServiceAllocation(
    val technicianName: String,
    val technicianPhone: String? = null,
    val serviceCenterName: String,
    val serviceCenterAddress: String? = null,
    val allocationDateMillis: Long,
    val expectedServiceDateMillis: Long? = null,
    val priority: ServicePriority = ServicePriority.NORMAL,
)

data class TimelineEntry(
    val title: String,
    val note: String? = null,
    val timestampMillis: Long,
)

data class ServiceTicket(
    /** The SR-… ticket number, and what every screen keys and searches on. */
    val ticketId: String,
    /**
     * The numeric row id. `GET service/my-complaints/{id}` takes this, not
     * [ticketId] — null only for a ticket the server never echoed an id for.
     */
    val serverId: String? = null,
    val vehicle: ServiceVehicle,
    val component: ServiceComponentItem,
    val issueTitle: String,
    val issueDescription: String,
    val errorCode: String? = null,
    val status: TicketStatus,
    val attachments: List<ServiceAttachment> = emptyList(),
    val allocation: ServiceAllocation? = null,
    val timeline: List<TimelineEntry> = emptyList(),
    val customerType: ServiceCustomerType = ServiceCustomerType.EVA2Z_CUSTOMER,
    /** Non-null only when a service charge was collected for this ticket. */
    val payment: ServicePaymentReceipt? = null,
    val createdAtMillis: Long,
    val lastUpdatedMillis: Long,
)

/** Filter tabs on the My Complaints screen — one per [TicketStatus], plus "All". */
enum class ComplaintFilter(val label: String) {
    ALL("All"),
    SUBMITTED("Submitted"),
    OPEN("Open"),
    IN_PROGRESS("In progress"),
    COMPLETED("Completed");

    fun matches(ticket: ServiceTicket): Boolean = when (this) {
        ALL -> true
        SUBMITTED -> ticket.status == TicketStatus.SUBMITTED
        OPEN -> ticket.status == TicketStatus.OPEN
        IN_PROGRESS -> ticket.status == TicketStatus.IN_PROGRESS
        COMPLETED -> ticket.status == TicketStatus.COMPLETED
    }
}

/** Counters behind the dashboard tiles and the My Complaints filter chips. */
fun List<ServiceTicket>.summarise(): ServiceSummary = ServiceSummary(
    submitted = count { it.status == TicketStatus.SUBMITTED },
    open = count { it.status == TicketStatus.OPEN },
    inProgress = count { it.status == TicketStatus.IN_PROGRESS },
    completed = count { it.status == TicketStatus.COMPLETED },
)

/** Free-text search across ticket id, component, issue text and vehicle (FRD §6). */
fun List<ServiceTicket>.search(query: String): List<ServiceTicket> {
    val q = query.trim().lowercase()
    if (q.isEmpty()) return this
    return filter { t ->
        t.ticketId.lowercase().contains(q) ||
            t.component.label.lowercase().contains(q) ||
            t.issueDescription.lowercase().contains(q) ||
            t.vehicle.registrationNumber.lowercase().contains(q) ||
            t.vehicle.vin.lowercase().contains(q)
    }
}

data class ServiceSummary(
    val submitted: Int = 0,
    val open: Int = 0,
    val inProgress: Int = 0,
    val completed: Int = 0,
) {
    /** Everything that is not finished yet — what the dashboard tiles count. */
    val active: Int get() = submitted + open + inProgress

    /** Adds two counter sets together. */
    operator fun plus(other: ServiceSummary): ServiceSummary = ServiceSummary(
        submitted = submitted + other.submitted,
        open = open + other.open,
        inProgress = inProgress + other.inProgress,
        completed = completed + other.completed,
    )
}

/** In-flight complaint being composed on the Raise Complaint screen. */
data class ComplaintDraft(
    val customerType: ServiceCustomerType? = null,
    val vehicle: ServiceVehicle? = null,
    val component: ServiceComponentItem? = null,
    val issueDescription: String = "",
    val errorCode: String = "",
    val invoice: ServiceAttachment? = null,
    val media: List<ServiceAttachment> = emptyList(),
) {
    val invoiceRequired: Boolean get() = component?.requiresInvoice == true

    /** True once every field the server needs has been filled in. */
    val isComplete: Boolean get() = validationError == null

    /**
     * First blocking validation problem, or null when the draft can be submitted.
     *
     * Kept to only what the complaint genuinely cannot be filed without. Rules the
     * server does not enforce do not belong here — a length minimum or a missing
     * device would just be the app refusing a submission the backend would accept.
     */
    val validationError: String?
        get() = when {
            customerType == null -> "Tell us whether you are an EVA2Z Connect customer."
            component == null -> "Select the component that has the issue."
            issueDescription.isBlank() -> "Describe the issue before submitting."
            else -> null
        }

    companion object {
        const val MAX_DESCRIPTION = 600
        const val MAX_MEDIA = 5
        const val MAX_ATTACHMENT_BYTES = 15L * 1024 * 1024
    }
}

/* ═════════════════════════════ FORMATTING ══════════════════════════════════ */

private val IST = TimeZone.of("Asia/Kolkata")

private val MONTHS = listOf(
    "Jan", "Feb", "Mar", "Apr", "May", "Jun",
    "Jul", "Aug", "Sep", "Oct", "Nov", "Dec",
)

@OptIn(ExperimentalTime::class)
fun Long.asServiceDate(): String {
    val dt = Instant.fromEpochMilliseconds(this).toLocalDateTime(IST)
    return "${dt.day} ${MONTHS[dt.month.ordinal]} ${dt.year}"
}

@OptIn(ExperimentalTime::class)
fun Long.asServiceDateTime(): String {
    val dt = Instant.fromEpochMilliseconds(this).toLocalDateTime(IST)
    val hour24 = dt.hour
    val hour12 = when {
        hour24 % 12 == 0 -> 12
        else -> hour24 % 12
    }
    val minute = dt.minute.toString().padStart(2, '0')
    val meridiem = if (hour24 < 12) "AM" else "PM"
    return "${dt.day} ${MONTHS[dt.month.ordinal]} ${dt.year}, $hour12:$minute $meridiem"
}

fun Long.asFileSize(): String = when {
    this >= 1024 * 1024 -> {
        val mb = this * 10 / (1024 * 1024)
        "${mb / 10}.${mb % 10} MB"
    }
    this >= 1024 -> "${this / 1024} KB"
    else -> "$this B"
}