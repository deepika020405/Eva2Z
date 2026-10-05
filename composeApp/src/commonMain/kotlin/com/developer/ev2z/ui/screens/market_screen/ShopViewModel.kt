// commonMain/ui/screens/market_screen/ShopViewModel.kt
package com.developer.ev2z.ui.screens.market_screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.developer.ev2z.data.local.StoredOrder
import com.developer.ev2z.data.local.StoredOrderAddress
import com.developer.ev2z.data.local.StoredOrderItem
import com.developer.ev2z.phonepe.PaymentState
import com.developer.ev2z.repository.ProductRepository
import com.developer.ev2z.response.CartItemDto
import com.developer.ev2z.response.ProductDto
import com.developer.ev2z.response.WishlistProductWrapper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import com.developer.ev2z.phonepe.openPhonePeSdk
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

data class ShopUiState(
    val loading: Boolean = false,
    val products: List<ProductDto> = emptyList(),
    val wishlist: List<WishlistProductWrapper> = emptyList(),
    val cart: List<CartItemDto> = emptyList(),
    val cartCount: Int = 0,
    val totalPrice: String? = null,
    val addresses: List<BillingAddress> = emptyList(),
    val selectedAddress: BillingAddress? = null,
    val error: String? = null,
    val promoApplied: Boolean = false,
    val appliedPromoCode: String? = null,
    val originalAmount: String? = null,
    val finalAmount: String? = null,
    val discountApplied: Double? = null,
    val promoError: String? = null,
    // Smart promo banner: what the user already owns
    val hasGpsDevice: Boolean = false,
    val hasParkingTag: Boolean = false
)

data class BillingAddress(
    val id: Int,
    val fullName: String,
    val phone: String,
    val email: String,
    val address: String,
    val city: String,
    val state: String,
    val pincode: String
)

class ShopViewModel(
    private val repo: ProductRepository,
    private val tokenProvider: () -> String
) : ViewModel() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _state = MutableStateFlow(ShopUiState())
    val state: StateFlow<ShopUiState> = _state

    private val _paymentState = MutableStateFlow<PaymentState>(PaymentState.Idle)
    val paymentState: StateFlow<PaymentState> = _paymentState

    // The order the last successful payment produced. Held here rather than read back from
    // the store, because the store copy is dropped as soon as my-billing returns the order.
    private val _lastPlacedOrder = MutableStateFlow<StoredOrder?>(null)
    val lastPlacedOrder: StateFlow<StoredOrder?> = _lastPlacedOrder

    // The two ids create-orders hands back for the payment in flight. They are NOT
    // interchangeable: merchantOrderId is our own ORD-… id and the only one the server can
    // look the order up by, while phonepeOrderId is PhonePe's internal OMO… reference.
    // Both go back in the confirmation callback.
    private var pendingMerchantOrderId: String? = null
    private var pendingPhonePeOrderId: String? = null
    private val CACHE_VALIDITY = 2 * 60 * 1000 // 2 min

    // 🔥 In-memory cache
    private var cachedProducts: List<ProductDto>? = null
    private var cachedWishlist: List<WishlistProductWrapper>? = null
    private var cachedCart: List<CartItemDto>? = null
    private var cachedCartCount: Int = 0
    private var cachedTotalPrice: String? = null
    private var cachedHasGpsDevice: Boolean = false
    private var cachedHasParkingTag: Boolean = false
    private var lastLoadedAt: Long = 0L

    // Post-payment cart wipe; cart reads join it so items can't reappear mid-clear.
    private var cartClearJob: Job? = null

@OptIn(ExperimentalTime::class)
    fun loadData(forceRefresh: Boolean = false) {
        val now = Clock.System.now().toEpochMilliseconds()

        // ✅ USE CACHE
        if (
            !forceRefresh &&
            cachedProducts != null &&
            cachedWishlist != null &&
            cachedCart != null &&
            now - lastLoadedAt < CACHE_VALIDITY
        ) {
            _state.value = _state.value.copy(
                loading = false,
                products = cachedProducts!!,
                wishlist = cachedWishlist!!,
                cart = cachedCart!!,
                cartCount = cachedCartCount,
                totalPrice = cachedTotalPrice,
                hasGpsDevice = cachedHasGpsDevice,
                hasParkingTag = cachedHasParkingTag,
                error = null
            )
            return
        }

        _state.value = _state.value.copy(loading = true)
        val token = tokenProvider()

        scope.launch {
            try {
                cartClearJob?.join()   // don't read a cart that is mid-clear after payment
                val products = repo.getProducts(token)
                val wishlist = repo.getWishlist(token)
                val cartResp = repo.getCart(token)
                val count = cartResp.cart.sumOf { it.quantity }

                // Determine what the user already owns for the smart promo banner
                val devicesResp = repo.getMyDevices(token)
                val hasGps = devicesResp.devices.isNotEmpty()
                val parkingResp = repo.getUserParkingList()
                val hasParking = (parkingResp.data?.size ?: 0) > 0

                // 🔥 SAVE CACHE
                cachedProducts = products
                cachedWishlist = wishlist
                cachedCart = cartResp.cart
                cachedCartCount = count
                cachedTotalPrice = cartResp.totalPrice
                cachedHasGpsDevice = hasGps
                cachedHasParkingTag = hasParking
                lastLoadedAt = Clock.System.now().toEpochMilliseconds()

                _state.value = _state.value.copy(
                    loading = false,
                    products = products,
                    wishlist = wishlist,
                    cart = cartResp.cart,
                    cartCount = count,
                    totalPrice = cartResp.totalPrice,
                    hasGpsDevice = hasGps,
                    hasParkingTag = hasParking,
                    error = null
                )

            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    loading = false,
                    error = e.message ?: "Unknown Error"
                )
            }
        }
    }
    fun toggleWishlist(productId: Int) {
        val token = tokenProvider()
        scope.launch {
            val isWishlisted = _state.value.wishlist.any { it.product_id == productId }
            if (isWishlisted) repo.removeWishlist(token, productId)
            else repo.addWishlist(token, productId)

            val wishlist = repo.getWishlist(token)
            _state.value = _state.value.copy(wishlist = wishlist)

            cachedWishlist = wishlist // 🔥
        }
    }

    fun isFavorite(productId: Int): Boolean {
        return _state.value.wishlist.any { it.product_id == productId }
    }

//    fun refreshCart() {
//        val token = tokenProvider()
//        scope.launch {
//            val cartResp = repo.getCart(token)
//            val count = cartResp.cart.sumOf { it.quantity }
//
//            _state.value = _state.value.copy(
//                cart = cartResp.cart,
//                cartCount = count,
//                totalPrice = cartResp.totalPrice
//            )
//
//            cachedCart = cartResp.cart
//            cachedCartCount = count
//            cachedTotalPrice = cartResp.totalPrice
//        }
//    }

    fun refreshCart() {
        scope.launch {
            cartClearJob?.join()   // never re-read the cart mid-clear
            fetchCart()
        }
    }

    /** Reads the server cart into state + cache. Callers own the loading flag. */
    private suspend fun fetchCart() {
        val cartResp = repo.getCart(tokenProvider())
        val count = cartResp.cart.sumOf { it.quantity }

        _state.value = _state.value.copy(
            cart = cartResp.cart,
            cartCount = count,
            totalPrice = cartResp.totalPrice
        )

        cachedCart = cartResp.cart
        cachedCartCount = count
        cachedTotalPrice = cartResp.totalPrice
    }

//    fun addToCart(add: String, productId: Int, quantity: Int = 1, plan_type: String, device_type: String) {
//        val token = tokenProvider()
//        scope.launch {
//            try {
//                val ok = repo.addToCart(token, add, productId, quantity, plan_type, device_type)
//                if (!ok) println("❌ Failed to add to cart")
//            } catch (e: Exception) {
//                println("❌ addToCart error: ${e.message}")
//            }
//        }
//    }

    fun addToCart(
        add: String,
        productId: Int,
        quantity: Int = 1,
        plan_type: String,
        device_type: String,
        onSuccess: (() -> Unit)? = null,
        onError: (() -> Unit)? = null
    ) {
        val token = tokenProvider()

        // Products 10 and 11 are the plan-based GPS trackers: the cart API refuses them
        // with 422 {"message":"plan_type must be selected for this product."} when the
        // plan is empty. The shop listing's Add button has no plan picker, so fall back
        // to the device-only plan — exactly the price the card shows. The details screen
        // still sends its own choice, and that one wins.
        val finalPlanType =
            if (plan_type.isBlank() && (productId == 10 || productId == 11)) "without_recharge"
            else plan_type

        scope.launch {
            try {
                val ok = repo.addToCart(token, add, productId, quantity, finalPlanType, device_type)
                if (ok) {
                    refreshCart()
                    onSuccess?.invoke()
                } else {
                    println("❌ Failed to add to cart")
                    onError?.invoke()
                }
            } catch (e: Exception) {
                println("❌ addToCart error: ${e.message}")
                onError?.invoke()
            }
        }
    }

    fun loadCart() {
        _state.value = _state.value.copy(loading = true)

        scope.launch {
            try {
                // A post-payment clear may still be in flight — waiting for it stops the
                // paid-for items from flashing back into the cart.
                cartClearJob?.join()
                fetchCart()
                _state.value = _state.value.copy(loading = false)

            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    loading = false,
                    error = e.message
                )
            }
        }
    }

    fun updateQuantity(productId: Int, newQty: Int, plan_type: String?, device_type: String?) {
        val token = tokenProvider()

        scope.launch {
            val finalPlan = normalizedPlanType(productId, plan_type)
            val finalDevice = normalizedDeviceType(device_type)


            try {
                val ok = repo.updateCartQuantity(
                    token = token,
                    productId = productId,
                    quantity = newQty,
                    plan_type = finalPlan,
                    device_type = finalDevice
                )

                if (ok) refreshCart()
                else println("❌ Failed to update quantity")

            } catch (e: Exception) {
                println("❌ updateQuantity error: ${e.message}")
            }
        }
    }

    fun loadBillingAddresses() {
        val token = tokenProvider()

        scope.launch {
            try {
                _state.value = _state.value.copy(loading = true)

                println("🔄 Loading Billing Addresses...")

                val list = repo.getBillingAddresses(token)
                println("📦 Raw Billing Count = ${list.size}")

                val mapped = list.sortedByDescending { it.id }.map {
                    BillingAddress(
                        id = it.id,
                        fullName = it.fname ?: "",
                        phone = it.phone ?: "",
                        email = it.email ?: "",
                        address = it.address ?: "",
                        city = it.city ?: "",
                        state = it.state ?: "",
                        pincode = it.pincode ?: ""
                    )
                }

                _state.value = _state.value.copy(
                    addresses = mapped,
                    selectedAddress = mapped.firstOrNull()
                )

            } catch (e: Exception) {
                println("❌ loadBillingAddresses ERROR: ${e.message}")
                _state.value = _state.value.copy(error = e.message)
            } finally {
                _state.value = _state.value.copy(loading = false)
            }
        }
    }

    fun selectAddress(address: BillingAddress) {
        _state.value = _state.value.copy(selectedAddress = address)
    }

    fun addNewAddress(
        fname: String,
        phone: String,
        email: String,
        address: String,
        city: String,
        state: String,
        pincode: String,
        onDone: () -> Unit
    ) {
        val token = tokenProvider()

        scope.launch {
            val ok = repo.addBillingAddress(
                token, fname, phone, email, address, city, state, pincode
            )

            if (ok) {
                println("✔ Address Added Successfully")
                loadBillingAddresses()
                onDone()
            } else {
                println("❌ Failed to Add Address")
            }
        }
    }

    /**
     * Edit a saved delivery address.
     *
     * The backend has no update route: `POST gps-billing` always INSERTs — it ignores a
     * billing_id and hands back a fresh row (verified 2026-09-29). So an edit is saved as
     * a new address first and the old row removed only once that succeeded, which also
     * means a failure can never leave the user with no address at all. The row id changes,
     * and loadBillingAddresses() selects the newest row, so the edited one stays selected.
     */
    fun updateAddress(
        oldId: Int,
        fname: String,
        phone: String,
        email: String,
        address: String,
        city: String,
        state: String,
        pincode: String,
        onDone: () -> Unit
    ) {
        val token = tokenProvider()

        scope.launch {
            val saved = repo.addBillingAddress(
                token, fname, phone, email, address, city, state, pincode
            )

            if (!saved) {
                println("❌ updateAddress: saving the edited address failed, old one kept")
                return@launch
            }

            val removed = repo.deleteBillingAddress(token, oldId)
            if (!removed) println("⚠️ updateAddress: old address $oldId could not be removed")

            loadBillingAddresses()
            onDone()
        }
    }

    fun applyPromo(code: String) {
        val token = tokenProvider()
        val amount = _state.value.totalPrice ?: "0"
        val upper = code.trim().uppercase()

        if (!upper.startsWith("WELCOME") && !upper.startsWith("REFER")) {
            _state.value = _state.value.copy(
                promoApplied = false,
                promoError = "Please enter a valid coupon code"
            )
            return
        }

        scope.launch {
            try {
                val resp = repo.applyPromo(token, amount, upper)

                if (!resp.success) {
                    _state.value = _state.value.copy(
                        promoApplied = false,
                        promoError = "Please enter a valid coupon code"
                    )
                    return@launch
                }

                _state.value = _state.value.copy(
                    promoApplied = true,
                    appliedPromoCode = resp.appliedCode,
                    originalAmount = resp.originalAmount,
                    finalAmount = resp.finalAmount.toString(),
                    discountApplied = resp.discountApplied,
                    totalPrice = resp.finalAmount.toString(),
                    promoError = null
                )

            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    promoApplied = false,
                    promoError = "Please enter a valid coupon code"
                )
            }
        }
    }

    fun clearPromoError() {
        _state.value = _state.value.copy(promoError = null)
    }

    fun removeFromCart(item: CartItemDto) {
        val token = tokenProvider()

        scope.launch {
            val finalPlan = normalizedPlanType(item.product_id, item.plan_type)
            val finalDevice = normalizedDeviceType(item.device_type)

            try {
                val ok = repo.addToCart(
                    token = token,
                    add = "remove",
                    productId = item.product_id,
                    quantity = 0,
                    plan_type = finalPlan,
                    device_type = finalDevice
                )

                if (ok) refreshCart()
                else println("❌ Failed to remove item")

            } catch (e: Exception) {
                println("❌ removeFromCart error = ${e.message}")
            }
        }
    }

    fun deleteBillingAddress(billingId: Int) {
        val token = tokenProvider()

        scope.launch {
            val ok = repo.deleteBillingAddress(token, billingId)

            if (ok) {
                println("🗑️ Billing address deleted")
                loadBillingAddresses()
            } else {
                println("❌ Failed to delete billing address")
            }
        }
    }

    // ✅ FIXED: Payment initiation with proper state management
    fun initiatePhonePePayment() {
        val s = _state.value
        val token = tokenProvider()
        val amount = (s.finalAmount ?: s.totalPrice ?: "0").toDouble()
        val billingId = s.selectedAddress?.id
        val cartItems = s.cart

        if (billingId == null) {
            _paymentState.value = PaymentState.Failed(
                reason = "Please select delivery address",
                amount = 0
            )
            return
        }

        if (cartItems.isEmpty()) {
            _paymentState.value = PaymentState.Failed(
                reason = "Cart is empty",
                amount = 0
            )
            return
        }

        println("🚀 Starting payment initiation...")
        _paymentState.value = PaymentState.Processing

        scope.launch {
            try {
                // ✅ 1. Create order on backend
                println("📝 Creating order on backend...")
                val resp = repo.createPhonePeOrder(
                    token = token,
                    amount = amount,
                    billingId = billingId,
                    cart = cartItems
                )

                val phonepeOrderId = resp["phonepeOrderId"]?.jsonPrimitive?.content
                    ?: throw Exception("phonepeOrderId missing")

                val merchantOrderId = resp["merchantOrderId"]?.jsonPrimitive?.content
                    ?: throw Exception("merchantOrderId missing")

                val rawResponseStr = resp["order"]?.jsonObject
                    ?.get("raw_response")?.jsonPrimitive?.content
                    ?: throw Exception("order.raw_response missing")

                val orderToken = Json.parseToJsonElement(rawResponseStr)
                    .jsonObject["token"]?.jsonPrimitive?.content
                    ?: throw Exception("token missing in raw_response")

                println("✅ Order Created: token=${orderToken.take(50)}..., merchantOrderId=$merchantOrderId, phonepeOrderId=$phonepeOrderId")
                pendingMerchantOrderId = merchantOrderId
                pendingPhonePeOrderId = phonepeOrderId

                // ✅ 2. Launch PhonePe SDK
                println("📱 Launching PhonePe SDK...")
                openPhonePeSdk(orderToken, phonepeOrderId) { result ->
                    handlePaymentResult(result)
                }

            } catch (e: Exception) {
                println("❌ Order creation failed: ${e.message}")
                _paymentState.value = PaymentState.Failed(
                    reason = e.message ?: "Order creation failed",
                    amount = 0
                )
            }
        }
    }

    // ✅ FIXED: Payment result handler
    //
    // The SDK result only says the user finished the checkout UI — it is not proof that the
    // payment settled. The server decides that, and only once phonepe/callback reaches it:
    // until then the order sits at PENDING and my-billing (which filters on SUCCESS) will
    // not return it. So nothing that assumes a paid order — recording it, clearing the cart,
    // showing the success screen — may happen before that call comes back.
    private fun handlePaymentResult(result: com.developer.ev2z.phonepe.PaymentResult) {

        val amountInt =
            (_state.value.finalAmount ?: _state.value.totalPrice ?: "0")
                .toDouble()
                .toInt()

        val merchantOrderId = pendingMerchantOrderId
        val phonepeOrderId = pendingPhonePeOrderId.orEmpty()

        if (!result.success) {
            // Tell the server as well, so the row settles as FAILED rather than lingering
            // at PENDING and looking like an unconfirmed payment during reconciliation.
            if (merchantOrderId != null) {
                scope.launch {
                    runCatching {
                        repo.confirmPhonePePayment(merchantOrderId, phonepeOrderId, success = false)
                    }.onFailure { println("❌ Failed-payment callback failed — ${it.message}") }
                }
            }
            _paymentState.value = PaymentState.Failed(
                reason = result.message ?: "Payment failed",
                amount = amountInt
            )
            return
        }

        if (merchantOrderId == null) {
            println("❌ Payment succeeded but merchantOrderId is missing — cannot confirm")
            _paymentState.value = PaymentState.Failed(
                reason = "Payment could not be confirmed. If money was debited it will be " +
                        "reconciled automatically — please check My Orders before paying again.",
                amount = amountInt
            )
            return
        }

        _paymentState.value = PaymentState.Processing

        scope.launch {
            if (!confirmWithRetry(merchantOrderId, phonepeOrderId)) {
                // Money may well have moved, so the cart stays exactly as it is: emptying it
                // here would leave the user with neither the items nor an order to point at.
                println("❌ Payment confirmation failed — leaving cart intact")
                _paymentState.value = PaymentState.Failed(
                    reason = "We couldn't confirm your payment. If money was debited it will " +
                            "be reconciled automatically — please check My Orders before paying again.",
                    amount = amountInt
                )
                return@launch
            }

            println("✅ Payment confirmed with server (merchantOrderId=$merchantOrderId)")
            // Record before clearCart() — the cart is the only source of the ordered items.
            recordPlacedOrder(result.transactionId, amountInt)
            _paymentState.value = PaymentState.Success(
                txnId = result.transactionId,
                amount = amountInt
            )
            clearCart()
        }
    }

    // The user has already been charged by the time this runs, so a dropped connection must
    // not be what decides whether the order exists. Retry a few times before giving up.
    private suspend fun confirmWithRetry(
        merchantOrderId: String,
        phonepeOrderId: String,
        attempts: Int = 3
    ): Boolean {
        repeat(attempts) { attempt ->
            val ok = runCatching {
                repo.confirmPhonePePayment(merchantOrderId, phonepeOrderId, success = true)
            }.getOrElse { e ->
                println("❌ Confirm attempt ${attempt + 1} threw — ${e.message}")
                false
            }
            if (ok) return true
            println("⚠️ Confirm attempt ${attempt + 1}/$attempts did not succeed")
            if (attempt < attempts - 1) delay(1500L * (attempt + 1))
        }
        return false
    }

    // ✅ Clear cart after successful payment
    //
    // The server does not empty the cart when an order is created, so clearing only local
    // state made the paid-for items reappear the next time CartScreen called loadCart().
    // Remove each line item server-side, then reconcile with what the server actually has.
    private fun clearCart() {
        val token = tokenProvider()
        val orderedItems = _state.value.cart

        // Optimistic: hide the items and the checkout bar right away.
        _state.value = _state.value.copy(
            cart = emptyList(),
            cartCount = 0,
            totalPrice = "0",
            originalAmount = null,
            finalAmount = null,
            promoApplied = false,
            appliedPromoCode = null,
            discountApplied = null,
            promoError = null
        )
        cachedCart = emptyList()
        cachedCartCount = 0
        cachedTotalPrice = "0"
        lastLoadedAt = 0L   // next loadData() must re-read the cart, not serve the cache

        cartClearJob = scope.launch {
            orderedItems.forEach { item ->
                try {
                    repo.addToCart(
                        token = token,
                        add = "remove",
                        productId = item.product_id,
                        quantity = 0,
                        plan_type = normalizedPlanType(item.product_id, item.plan_type),
                        device_type = normalizedDeviceType(item.device_type)
                    )
                } catch (e: Exception) {
                    println("❌ clearCart: failed to remove ${item.product_id} — ${e.message}")
                }
            }
            println("✅ Cart cleared on server (${orderedItems.size} items)")
            runCatching { fetchCart() }
                .onFailure { println("❌ clearCart: cart re-read failed — ${it.message}") }
        }
    }

    // The cart API only accepts plan_type for the two plan-based products, and expects an
    // empty string otherwise — same normalisation add/update/remove already use.
    private fun normalizedPlanType(productId: Int, planType: String?): String =
        if (productId == 10 || productId == 11) {
            when (planType?.lowercase()) {
                "with_recharge" -> "with_recharge"
                "without_recharge" -> "without_recharge"
                else -> ""
            }
        } else ""

    private fun normalizedDeviceType(deviceType: String?): String =
        when (deviceType?.lowercase()) {
            "android" -> "android"
            "ios" -> "ios"
            else -> ""
        }

    // ── Order history ────────────────────────────────────────────
    // my-billing only lists the order once the payment callback settles server-side, so a
    // successful payment is written to local history here; OrderRepository merges it with
    // the server list and drops it once the server returns the same order.
    @OptIn(ExperimentalTime::class)
    private fun recordPlacedOrder(transactionId: String?, amount: Int) {
        val s = _state.value
        if (s.cart.isEmpty()) {
            println("⚠️ recordPlacedOrder skipped — cart already empty")
            return
        }

        val placedAt = Clock.System.now().toEpochMilliseconds()
        val orderId = "EVZ${(placedAt % 1_000_000).toString().padStart(6, '0')}"

        val order = StoredOrder(
            orderId = orderId,
            transactionId = transactionId?.takeIf { it.isNotBlank() },
            merchantOrderId = pendingMerchantOrderId?.takeIf { it.isNotBlank() },
            placedAt = placedAt,
            totalAmount = amount.toString(),
            items = s.cart.map { item ->
                StoredOrderItem(
                    productId = item.product_id,
                    name = item.product?.name ?: "Item",
                    imageUrl = item.product?.image.orEmpty(),
                    quantity = item.quantity,
                    price = item.product?.sale_price ?: item.price,
                    planType = item.plan_type
                )
            },
            address = s.selectedAddress?.let {
                StoredOrderAddress(
                    name = it.fullName,
                    phone = it.phone,
                    address = it.address,
                    city = it.city,
                    state = it.state,
                    pincode = it.pincode
                )
            }
        )

        OrderHistoryProvider.store.saveOrder(order)
        _lastPlacedOrder.value = order
        pendingMerchantOrderId = null
        pendingPhonePeOrderId = null

        // Pull the order history now and keep re-checking briefly, so My Orders shows the
        // server's copy (with its real order id) as soon as the backend has it.
        OrderHistoryProvider.viewModel.syncAfterOrder(orderId)
    }

    // ✅ Reset payment state (for retry)
    fun resetPaymentState() {
        println("🔄 Resetting payment state to Idle")
        _paymentState.value = PaymentState.Idle
    }
}