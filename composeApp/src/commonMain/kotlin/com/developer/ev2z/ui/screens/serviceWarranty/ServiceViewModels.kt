package com.developer.ev2z.ui.screens.serviceWarranty

import com.developer.ev2z.network.ApiService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Common lifecycle for the flow's ViewModels.
 *
 * These are plain classes owning a [CoroutineScope] — the convention documented
 * in CLAUDE.md and used by DocumentsViewModel — not androidx ViewModels. Each
 * one talks to [ApiService] directly, exactly as the rest of the app does, and
 * [dispose] cancels its scope when the screen leaves composition so an in-flight
 * call cannot write into a state nobody is showing. ServiceWarrantyFlow wires
 * that up.
 */
abstract class ServiceScopedViewModel {
    protected val viewModelScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    open fun dispose() {
        viewModelScope.cancel()
    }
}

/* ══════════════════════════ 1. DASHBOARD ══════════════════════════════════ */

data class ServiceDashboardState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val vehicles: List<ServiceVehicle> = emptyList(),
    val selectedVehicle: ServiceVehicle? = null,
    val summary: ServiceSummary = ServiceSummary(),
    val recentTickets: List<ServiceTicket> = emptyList(),
    val activeAllocation: ServiceTicket? = null,
    val errorMessage: String? = null,
)

class ServiceDashboardViewModel(
    private val apiService: ApiService = ApiService(),
) : ServiceScopedViewModel() {

    private val _state = MutableStateFlow(ServiceDashboardState())
    val state: StateFlow<ServiceDashboardState> = _state.asStateFlow()

    init {
        load(initial = true)
    }

    /**
     * Vehicles and the complaint list, each reported on separately — a failed
     * device call should not blank out tickets that did load, or the reverse.
     */
    private fun load(initial: Boolean) {
        viewModelScope.launch {
            val vehicles = try {
                apiService.fetchServiceVehicles().also {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        vehicles = it,
                        selectedVehicle = _state.value.selectedVehicle ?: it.firstOrNull(),
                        errorMessage = null,
                    )
                }
            } catch (e: Exception) {
                println("❌ Service dashboard vehicles failed: ${e.message}")
                _state.value = _state.value.copy(
                    isLoading = false,
                    errorMessage = "Couldn't load your vehicles. Pull to refresh or try again.",
                )
                _state.value.vehicles
            }

            try {
                val tickets = apiService.fetchServiceComplaints(vehicles)
                _state.value = _state.value.copy(
                    recentTickets = tickets.take(3),
                    activeAllocation = tickets.firstOrNull {
                        it.allocation != null && !it.status.isTerminal
                    },
                    summary = tickets.summarise(),
                    errorMessage = if (initial) _state.value.errorMessage else null,
                )
            } catch (e: Exception) {
                println("❌ Service dashboard complaints failed: ${e.message}")
                _state.value = _state.value.copy(
                    errorMessage = _state.value.errorMessage
                        ?: "Couldn't load your complaints. Pull to refresh or try again.",
                )
            } finally {
                _state.value = _state.value.copy(isRefreshing = false)
            }
        }
    }

    fun refresh() {
        if (_state.value.isRefreshing) return
        _state.value = _state.value.copy(isRefreshing = true)
        load(initial = false)
    }
}

/* ═══════════════════════ 2. RAISE COMPLAINT ═══════════════════════════════ */

/**
 * PhonePe's SDK returns the moment its own UI closes, but our server only learns
 * the payment settled when PhonePe's webhook reaches it. Re-posting the complaint
 * straight away lost that race — the server could not see SERVICE-… settle and
 * answered with a brand new charge (observed 2026-09-19, ~50ms after the SDK
 * result). So the confirmation waits before the first re-post, and waits again
 * before a single retry.
 *
 * One retry, not a polling loop: every post re-uploads the invoice and photos.
 */
private const val PAYMENT_SETTLE_DELAY_MS = 3_000L
private const val PAYMENT_RETRY_DELAY_MS = 5_000L

/**
 * How many times a complaint is re-posted once the server reports the charge as
 * SUCCESS but still has not filed the ticket (observed 2026-09-21: the third post
 * flipped payment_status to SUCCESS, and the app gave up on that very answer).
 * Every post re-uploads the invoice and photos, hence a small number.
 */
private const val SETTLED_REPOST_ATTEMPTS = 3

data class RaiseComplaintState(
    val isLoading: Boolean = true,
    val isSubmitting: Boolean = false,
    /** True from "create the order" until PhonePe hands a result back. */
    val isPreparingPayment: Boolean = false,
    /** True while waiting for the server to see a finished payment settle. */
    val isConfirmingPayment: Boolean = false,
    val vehicles: List<ServiceVehicle> = emptyList(),
    val components: List<ServiceComponentItem> = emptyList(),
    val draft: ComplaintDraft = ComplaintDraft(),
    /** Set once the user has pressed Submit at least once, so errors aren't shown early. */
    val showValidation: Boolean = false,
    /** A checkout waiting to be opened; the screen consumes it exactly once. */
    val pendingPayment: ServicePaymentOrder? = null,
    /** Set once the service charge is confirmed, so a retry never charges twice. */
    val paidReceipt: ServicePaymentReceipt? = null,
    /** The server's own wording for the submit button, once it has told us. */
    val serverButtonText: String? = null,
    /** The charge the server quoted on `payment_required`, once it has quoted one. */
    val serverQuotedRupees: Int? = null,
    val submittedTicketId: String? = null,
    /**
     * Set when the service charge was paid but the server still would not file the
     * complaint. The money is gone, so the screen finishes the flow on this rather
     * than leaving a paid user staring at an error on a filled-in form.
     */
    val paidWithoutTicket: ServicePaymentReceipt? = null,
    val errorMessage: String? = null,
) {
    val inlineError: String? get() = if (showValidation) draft.validationError else null

    val isBusy: Boolean get() = isSubmitting || isPreparingPayment || isConfirmingPayment

    /** The button stays disabled until every required field is filled in. */
    val canSubmit: Boolean get() = !isBusy && draft.isComplete

    /**
     * What is still missing, so a disabled button always says why it is disabled.
     * Muted rather than red until the user has actually tried to submit.
     */
    val pendingRequirement: String? get() = if (isBusy) null else draft.validationError

    /**
     * What the complaint is expected to cost. There is no endpoint that quotes a
     * charge ahead of submission — `service/complaints` only reveals it in its
     * answer — so this is the customer type's expected fee until the server has
     * quoted one, and the server's figure from then on.
     */
    val chargeRupees: Int get() = serverQuotedRupees ?: draft.customerType?.expectedFeeRupees ?: 0

    /**
     * Carries the charge the moment the form is complete, so nobody reaches a
     * PhonePe checkout without the button having said so first. The server's own
     * wording wins as soon as it has sent one.
     */
    val submitLabel: String get() = when {
        isPreparingPayment -> "Opening PhonePe…"
        isConfirmingPayment -> "Confirming payment…"
        isSubmitting -> "Submitting…"
        paidReceipt != null -> "Submit Complaint"
        serverButtonText != null -> serverButtonText
        chargeRupees > 0 -> "Pay ₹$chargeRupees & Submit"
        else -> "Submit Complaint"
    }
}

@OptIn(ExperimentalTime::class)
class RaiseComplaintViewModel(
    private val apiService: ApiService = ApiService(),
    private val preselectedVehicleId: String? = null,
) : ServiceScopedViewModel() {

    private val _state = MutableStateFlow(RaiseComplaintState())
    val state: StateFlow<RaiseComplaintState> = _state.asStateFlow()

    /**
     * The checkout currently open in PhonePe. Held outside the state so the screen
     * can clear [RaiseComplaintState.pendingPayment] the moment it launches the SDK
     * — otherwise a recomposition would open a second checkout for the same order.
     */
    private var activeOrder: ServicePaymentOrder? = null

    init {
        viewModelScope.launch {
            try {
                val vehicles = apiService.fetchServiceVehicles()
                val components = apiService.serviceComponents()
                _state.value = _state.value.copy(
                    isLoading = false,
                    vehicles = vehicles,
                    components = components,
                    draft = _state.value.draft.copy(
                        vehicle = vehicles.firstOrNull { it.vehicleId == preselectedVehicleId }
                            ?: vehicles.firstOrNull(),
                    ),
                )
            } catch (e: Exception) {
                println("❌ Raise complaint load failed: ${e.message}")
                _state.value = _state.value.copy(
                    isLoading = false,
                    components = ServiceComponentCatalog,
                    errorMessage = "Couldn't load your vehicles.",
                )
            }
        }
    }

    private fun edit(block: (ComplaintDraft) -> ComplaintDraft) {
        _state.value = _state.value.copy(draft = block(_state.value.draft))
    }

    fun selectCustomerType(type: ServiceCustomerType) = edit { it.copy(customerType = type) }

    fun selectVehicle(vehicle: ServiceVehicle) = edit { it.copy(vehicle = vehicle) }

    fun selectComponent(component: ServiceComponentItem) = edit { it.copy(component = component) }

    fun updateDescription(text: String) =
        edit { it.copy(issueDescription = text.take(ComplaintDraft.MAX_DESCRIPTION)) }

    fun updateErrorCode(text: String) = edit { it.copy(errorCode = text.take(24)) }

    fun attachInvoice(attachment: ServiceAttachment) {
        if (!acceptSize(attachment)) return
        edit { it.copy(invoice = attachment) }
    }

    fun removeInvoice() = edit { it.copy(invoice = null) }

    fun attachMedia(attachment: ServiceAttachment) {
        if (!acceptSize(attachment)) return
        val current = _state.value.draft.media
        if (current.size >= ComplaintDraft.MAX_MEDIA) {
            _state.value = _state.value.copy(
                errorMessage = "You can attach up to ${ComplaintDraft.MAX_MEDIA} files.",
            )
            return
        }
        edit { it.copy(media = current + attachment) }
    }

    fun removeMedia(id: String) =
        edit { draft -> draft.copy(media = draft.media.filterNot { it.id == id }) }

    private fun acceptSize(attachment: ServiceAttachment): Boolean {
        if (attachment.sizeBytes > ComplaintDraft.MAX_ATTACHMENT_BYTES) {
            _state.value = _state.value.copy(
                errorMessage = "${attachment.name} is larger than 15 MB.",
            )
            return false
        }
        return true
    }

    /**
     * Always posts the complaint first — the server decides whether money is owed.
     * An EVA2Z Connect customer's complaint is filed on that first call; anyone else
     * gets a `payment_required` answer, pays, and the complaint is posted again with
     * the reference attached.
     */
    fun submit() {
        val current = _state.value
        if (current.isBusy) return

        _state.value = current.copy(showValidation = true)
        if (current.draft.validationError != null) return

        postComplaint()
    }

    private fun postComplaint() {
        if (_state.value.isSubmitting) return
        _state.value = _state.value.copy(isSubmitting = true)

        viewModelScope.launch {
            try {
                var retried = false
                var attempts = 0

                while (true) {
                    val result = apiService.submitServiceComplaint(
                        draft = _state.value.draft,
                        payment = _state.value.paidReceipt,
                    )

                    when (result) {
                        is ComplaintSubmission.Created -> {
                            // The ticket exists, so the form has done its job and is
                            // emptied — nothing that was just filed stays behind to be
                            // submitted a second time. The receipt is kept because the
                            // success dialog still names the charge that was paid.
                            _state.value = _state.value.copy(
                                isSubmitting = false,
                                isConfirmingPayment = false,
                                draft = ComplaintDraft(vehicle = _state.value.draft.vehicle),
                                showValidation = false,
                                serverButtonText = null,
                                serverQuotedRupees = null,
                                submittedTicketId = result.ticket.ticketId,
                            )
                            return@launch
                        }

                        is ComplaintSubmission.PaymentRequired -> {
                            val paid = _state.value.paidReceipt
                                ?: run {
                                    // Nothing paid yet — this is the server quoting the
                                    // charge, which is the normal first answer.
                                    val buttonHadWarned = _state.value.chargeRupees > 0
                                    _state.value = _state.value.copy(
                                        isSubmitting = false,
                                        serverButtonText = result.buttonText,
                                        serverQuotedRupees = result.amountRupees,
                                    )
                                    // A charge the button never mentioned — because the
                                    // app expected this customer type to be free — is not
                                    // sprung on the user as a checkout. The button now
                                    // carries the server's quote, and the next tap pays.
                                    if (!buttonHadWarned) return@launch

                                    startServicePayment(result.amountRupees, result.order)
                                    return@launch
                                }

                            // Asked again after paying. Two different situations, and
                            // the response's own payment_status tells them apart.
                            _state.value = _state.value.copy(isConfirmingPayment = true)

                            if (result.settled) {
                                // The server has the money and just has not turned it
                                // into a ticket yet. Nothing to confirm and nothing to
                                // pay — only this endpoint can finish it, so it is
                                // asked again. Never reopen the checkout here.
                                if (attempts < SETTLED_REPOST_ATTEMPTS) {
                                    attempts++
                                    println(
                                        "💳 ${paid.merchantOrderId} is SUCCESS but has no ticket yet " +
                                            "— re-posting ($attempts/$SETTLED_REPOST_ATTEMPTS) " +
                                            "in ${PAYMENT_RETRY_DELAY_MS}ms."
                                    )
                                    delay(PAYMENT_RETRY_DELAY_MS)
                                    continue
                                }
                            } else if (!retried) {
                                retried = true

                                // Still PENDING, so the money has not been seen at all.
                                // Tell the server outright down both callback routes;
                                // either may refuse a SERVICE-… order, and either way
                                // the re-post below is still attempted.
                                val accepted = apiService.settleServicePayment(paid)
                                println(
                                    "💳 ${paid.merchantOrderId} not settled yet " +
                                        "(callback accepted=$accepted) — retrying the " +
                                        "re-post in ${PAYMENT_RETRY_DELAY_MS}ms."
                                )

                                delay(PAYMENT_RETRY_DELAY_MS)
                                continue
                            }

                            // The money is taken and there is nothing further the app
                            // can do, so the form is finished with rather than left
                            // sitting on an error: the draft is cleared and the screen
                            // hands over to My Complaints, where the ticket will show
                            // up as soon as the server files it.
                            println("❌ ${paid.merchantOrderId} still unsettled after the retry.")
                            _state.value = _state.value.copy(
                                isSubmitting = false,
                                isConfirmingPayment = false,
                                draft = ComplaintDraft(),
                                showValidation = false,
                                paidWithoutTicket = paid,
                            )
                            return@launch
                        }

                        is ComplaintSubmission.Rejected -> {
                            _state.value = _state.value.copy(
                                isSubmitting = false,
                                isConfirmingPayment = false,
                                errorMessage = result.message,
                            )
                            return@launch
                        }
                    }
                }
            } catch (e: Exception) {
                println("❌ Complaint submit failed: ${e.message}")
                _state.value = _state.value.copy(
                    isSubmitting = false,
                    isConfirmingPayment = false,
                    // Say the money is safe — otherwise a paid user assumes it vanished.
                    errorMessage = if (_state.value.paidReceipt != null) {
                        "Your payment went through but the complaint couldn't be filed. " +
                            "Tap submit again — you won't be charged twice."
                    } else {
                        // The repository already retried and checked the list, so
                        // reaching here means the upload genuinely did not complete.
                        "Couldn't upload your complaint. Check your connection and try again."
                    },
                )
            }
        }
    }

    /**
     * [readyOrder] is the checkout the submission response carried, if any. Without
     * one the order has to be created separately before PhonePe can be opened.
     */
    private fun startServicePayment(amountRupees: Int, readyOrder: ServicePaymentOrder?) {
        _state.value = _state.value.copy(isPreparingPayment = true)

        if (readyOrder != null) {
            activeOrder = readyOrder
            _state.value = _state.value.copy(pendingPayment = readyOrder)
            return
        }

        viewModelScope.launch {
            try {
                val order = apiService.buildServicePaymentOrder(amountRupees)
                activeOrder = order
                _state.value = _state.value.copy(pendingPayment = order)
            } catch (e: Exception) {
                println("❌ Service fee order failed: ${e.message}")
                activeOrder = null
                _state.value = _state.value.copy(
                    isPreparingPayment = false,
                    errorMessage = "Couldn't start the ₹$amountRupees payment. Please try again.",
                )
            }
        }
    }

    /** Called by the screen the instant it hands the order to the PhonePe SDK. */
    fun paymentLaunched() {
        _state.value = _state.value.copy(pendingPayment = null)
    }

    /**
     * PhonePe's success result only means the checkout UI closed — the order is not
     * settled until our server says so.
     *
     * For an order `service/complaints` issued itself, re-posting the complaint with
     * `merchant_order_id` is what files the ticket — but the payment is still
     * announced first. `service/payment-callback` 404'd when this screen was built
     * and is live now; without that call the server was left to notice the money on
     * its own, which is how a paid complaint ended up settled with no ticket.
     */
    fun onPaymentResult(success: Boolean, transactionId: String?, message: String?) {
        val order = activeOrder
        if (order == null) {
            _state.value = _state.value.copy(isPreparingPayment = false)
            return
        }

        if (!success) {
            activeOrder = null
            _state.value = _state.value.copy(
                isPreparingPayment = false,
                errorMessage = message?.takeIf { it.isNotBlank() }
                    ?: "Payment cancelled. Your complaint has not been submitted.",
            )
            return
        }

        val receipt = ServicePaymentReceipt(
            merchantOrderId = order.merchantOrderId,
            // The SDK's transaction id when it gives one — it is often null,
            // hence the order's own OMO… reference as the fallback.
            phonepeOrderId = transactionId?.takeIf { it.isNotBlank() } ?: order.phonepeOrderId,
            amountRupees = order.amountRupees,
            paidAtMillis = Clock.System.now().toEpochMilliseconds(),
        )

        viewModelScope.launch {
            val confirmed = if (order.confirmedByResubmit) {
                // Announce the payment down every route that will take this order,
                // then let the re-post below file the ticket. A route refusing a
                // SERVICE-… order is not a failure — the re-post carries
                // merchant_order_id and is the proof either way, so the user is
                // never stopped here over money that has already left their account.
                apiService.settleServicePayment(receipt)
                true
            } else try {
                apiService.confirmServicePayment(order, transactionId)
            } catch (e: Exception) {
                println("❌ Service payment confirmation failed: ${e.message}")
                false
            }

            if (!confirmed) {
                activeOrder = null
                _state.value = _state.value.copy(
                    isPreparingPayment = false,
                    errorMessage = "We couldn't confirm your ₹${order.amountRupees} payment. " +
                        "Please check with support before paying again.",
                )
                return@launch
            }

            activeOrder = null
            _state.value = _state.value.copy(
                isPreparingPayment = false,
                isConfirmingPayment = true,
                paidReceipt = receipt,
            )

            // Let PhonePe's webhook reach our server before asking it whether the
            // money arrived; posting immediately is what made it answer with a
            // second charge instead of the ticket.
            delay(PAYMENT_SETTLE_DELAY_MS)

            // Second post — same complaint, now carrying the payment reference.
            postComplaint()
        }
    }

    fun consumeSubmission() {
        _state.value = _state.value.copy(submittedTicketId = null)
    }

    /**
     * Clears the paid-but-unfiled hand-off. The receipt itself is kept so a second
     * submit in this session re-posts the same reference instead of opening a
     * fresh checkout for money already taken.
     */
    fun consumePaidWithoutTicket() {
        _state.value = _state.value.copy(paidWithoutTicket = null)
    }

    fun dismissError() {
        _state.value = _state.value.copy(errorMessage = null)
    }
}

/* ═══════════════════════ 3. MY COMPLAINTS ═════════════════════════════════ */

data class MyComplaintsState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val query: String = "",
    val filter: ComplaintFilter = ComplaintFilter.ALL,
    val allTickets: List<ServiceTicket> = emptyList(),
    /** Set only when the fetch failed; an empty list on its own is not an error. */
    val errorMessage: String? = null,
) {
    /** Tickets after filter + search, newest first. */
    val tickets: List<ServiceTicket>
        get() = allTickets
            .filter { filter.matches(it) }
            .search(query)
            .sortedByDescending { it.lastUpdatedMillis }

    fun countFor(target: ComplaintFilter): Int =
        if (target == ComplaintFilter.ALL) allTickets.size
        else allTickets.count { target.matches(it) }
}

class MyComplaintsViewModel(
    private val apiService: ApiService = ApiService(),
    initialFilter: ComplaintFilter = ComplaintFilter.ALL,
) : ServiceScopedViewModel() {

    private val _state = MutableStateFlow(MyComplaintsState(filter = initialFilter))
    val state: StateFlow<MyComplaintsState> = _state.asStateFlow()

    init {
        load(initial = true)
    }

    /** Re-reads `service/my-complaints`. [initial] only decides which spinner shows. */
    private fun load(initial: Boolean) {
        _state.value = _state.value.copy(
            isLoading = initial,
            isRefreshing = !initial,
            errorMessage = null,
        )
        viewModelScope.launch {
            try {
                // Devices first: a complaint row carries only a device_id, so
                // without them a ticket cannot name the vehicle it was raised for.
                val vehicles = runCatching { apiService.fetchServiceVehicles() }.getOrDefault(emptyList())
                _state.value = _state.value.copy(
                    allTickets = apiService.fetchServiceComplaints(vehicles),
                    errorMessage = null,
                )
            } catch (e: Exception) {
                println("❌ my-complaints load failed: ${e.message}")
                _state.value = _state.value.copy(
                    // Anything already fetched stays on screen; the message only
                    // explains why it may be out of date.
                    errorMessage = "Couldn't load your complaints. Check your connection and try again.",
                )
            } finally {
                _state.value = _state.value.copy(isLoading = false, isRefreshing = false)
            }
        }
    }

    fun refresh() {
        if (_state.value.isLoading || _state.value.isRefreshing) return
        load(initial = false)
    }

    fun updateQuery(query: String) {
        _state.value = _state.value.copy(query = query)
    }

    fun setFilter(filter: ComplaintFilter) {
        _state.value = _state.value.copy(filter = filter)
    }
}

/* ═══════════════════════ 4. COMPLAINT DETAIL ══════════════════════════════ */

data class ComplaintDetailState(
    val isLoading: Boolean = true,
    val ticket: ServiceTicket? = null,
    val errorMessage: String? = null,
)

class ComplaintDetailViewModel(
    private val apiService: ApiService = ApiService(),
    private val ticketId: String,
) : ServiceScopedViewModel() {

    private val _state = MutableStateFlow(ComplaintDetailState())
    val state: StateFlow<ComplaintDetailState> = _state.asStateFlow()

    init {
        load()
    }

    /**
     * Pulls `service/my-complaints/{id}` for the remarks, documents and device the
     * list response leaves out. The list is read first because the detail route is
     * keyed by the numeric row id, which only the list carries.
     */
    private fun load() {
        viewModelScope.launch {
            val ticket = try {
                val vehicles = runCatching { apiService.fetchServiceVehicles() }.getOrDefault(emptyList())
                apiService.fetchServiceComplaint(ticketId, vehicles)
            } catch (e: Exception) {
                println("❌ Complaint $ticketId detail failed: ${e.message}")
                null
            }
            _state.value = ComplaintDetailState(
                isLoading = false,
                ticket = ticket ?: _state.value.ticket,
                errorMessage = if (ticket == null && _state.value.ticket == null) {
                    "Couldn't load this service request. Check your connection and try again."
                } else null,
            )
        }
    }

    fun retry() = load()
}