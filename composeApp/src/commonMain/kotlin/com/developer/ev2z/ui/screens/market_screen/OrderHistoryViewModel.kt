// commonMain/ui/screens/market_screen/OrderHistoryViewModel.kt
package com.developer.ev2z.ui.screens.market_screen

import com.developer.ev2z.data.local.OrderHistoryStore
import com.developer.ev2z.data.local.createStorageManager
import com.developer.ev2z.repository.OrderRepository
import com.developer.ev2z.repository.OrderUi
import com.developer.ev2z.util.ApiProvider
import com.developer.ev2z.util.AppTokenProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class OrderHistoryState(
    val loading: Boolean = false,
    /** A refresh over an already-populated list — keeps the list on screen. */
    val refreshing: Boolean = false,
    val orders: List<OrderUi> = emptyList(),
    /** Order count and lifetime spend as reported by `my-billing`. */
    val totalOrders: Int = 0,
    val grandTotal: Int = 0,
    /** True when `my-billing` could not be read; what's shown is local-only. */
    val serverUnreachable: Boolean = false
)

/**
 * Backs Shop → My Orders.
 *
 * History comes from `my-billing`. An order paid for a moment ago may not be there yet, so
 * the local record placed by [ShopViewModel] fills the gap and [syncAfterOrder] re-checks
 * the server for a short while until it shows up.
 */
class OrderHistoryViewModel(
    private val repo: OrderRepository,
    private val tokenProvider: () -> String
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _state = MutableStateFlow(OrderHistoryState())
    val state: StateFlow<OrderHistoryState> = _state

    private var loadJob: Job? = null
    private var syncJob: Job? = null

    fun load() {
        if (busy()) return

        val known = _state.value.orders.ifEmpty { repo.getLocalOrders() }

        // Locally-recorded orders render instantly; the server list folds in when it arrives.
        _state.value = _state.value.copy(
            loading = known.isEmpty(),
            refreshing = known.isNotEmpty(),
            orders = known
        )

        loadJob = scope.launch { fetch() }
    }

    /** Explicit refresh from the top bar — always hits the server. */
    fun refresh() {
        if (busy()) return
        _state.value = _state.value.copy(refreshing = true)
        loadJob = scope.launch { fetch() }
    }

    private fun busy() = loadJob?.isActive == true || syncJob?.isActive == true

    private suspend fun fetch() {
        val result = repo.getOrders(tokenProvider())
        _state.value = OrderHistoryState(
            loading = false,
            refreshing = false,
            orders = result.orders,
            totalOrders = result.totalOrders,
            grandTotal = result.grandTotal,
            serverUnreachable = !result.serverReachable
        )
    }

    /**
     * Called right after a payment succeeds. The order is already in local history, so this
     * only chases the server copy — it stops as soon as the order is no longer local-only,
     * and gives up quietly after a few tries (the local record keeps showing either way).
     */
    fun syncAfterOrder(localOrderId: String?) {
        syncJob?.cancel()
        syncJob = scope.launch {
            loadJob?.join()

            _state.value = _state.value.copy(
                refreshing = true,
                orders = _state.value.orders.ifEmpty { repo.getLocalOrders() }
            )
            fetch()

            for (wait in SYNC_DELAYS_MS) {
                val stillPending = localOrderId == null || _state.value.orders
                    .any { it.orderId == localOrderId && it.isLocalOnly }
                if (!stillPending) break

                delay(wait)
                fetch()
            }
        }
    }

    private companion object {
        /** Back-off while waiting for the payment callback to register the order. */
        val SYNC_DELAYS_MS = listOf(4_000L, 8_000L, 15_000L)
    }
}

/** Single instance so the Shop tab and the payment result screen see the same list. */
object OrderHistoryProvider {

    val store: OrderHistoryStore by lazy { OrderHistoryStore(createStorageManager()) }

    val viewModel: OrderHistoryViewModel by lazy {
        OrderHistoryViewModel(
            repo = OrderRepository(ApiProvider.apiService, store),
            tokenProvider = { AppTokenProvider.getToken() }
        )
    }
}