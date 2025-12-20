// commonMain/ui/screens/market_screen/ShopViewModel.kt
package com.developer.ev2z.ui.screens.market_screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.developer.ev2z.phonepe.PaymentState
import com.developer.ev2z.repository.ProductRepository
import com.developer.ev2z.response.CartItemDto
import com.developer.ev2z.response.ProductDto
import com.developer.ev2z.response.WishlistProductWrapper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import com.developer.ev2z.phonepe.openPhonePeSdk

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
    val promoError: String? = null
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

    fun loadData() {
        _state.value = _state.value.copy(loading = true)
        val token = tokenProvider()

        scope.launch {
            try {
                val products = repo.getProducts(token)
                val wishlist = repo.getWishlist(token)
                val cartResp = repo.getCart(token)
                val count = cartResp.cart.sumOf { it.quantity }

                _state.value = ShopUiState(
                    loading = false,
                    products = products,
                    wishlist = wishlist,
                    cart = cartResp.cart,
                    cartCount = count,
                    totalPrice = cartResp.totalPrice
                )

            } catch (e: Exception) {
                _state.value = ShopUiState(
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
        }
    }

    fun isFavorite(productId: Int): Boolean {
        return _state.value.wishlist.any { it.product_id == productId }
    }

    fun refreshCart() {
        val token = tokenProvider()
        scope.launch {
            try {
                val cartResp = repo.getCart(token)
                val count = cartResp.cart.sumOf { it.quantity }
                _state.value = _state.value.copy(
                    cart = cartResp.cart,
                    cartCount = count,
                    totalPrice = cartResp.totalPrice
                )
            } catch (e: Exception) {
                println("❌ refreshCart error: ${e.message}")
            }
        }
    }

    fun addToCart(add: String, productId: Int, quantity: Int = 1, plan_type: String, device_type: String) {
        val token = tokenProvider()
        scope.launch {
            try {
                val ok = repo.addToCart(token, add, productId, quantity, plan_type, device_type)
                if (!ok) println("❌ Failed to add to cart")
            } catch (e: Exception) {
                println("❌ addToCart error: ${e.message}")
            }
        }
    }

    fun loadCart() {
        val token = tokenProvider()
        _state.value = _state.value.copy(loading = true)

        scope.launch {
            try {
                val cartResp = repo.getCart(token)
                val count = cartResp.cart.sumOf { it.quantity }

                _state.value = _state.value.copy(
                    loading = false,
                    cart = cartResp.cart,
                    cartCount = count,
                    totalPrice = cartResp.totalPrice
                )

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
            val finalPlan = if (productId == 10 || productId == 11) {
                when (plan_type?.lowercase()) {
                    "with_recharge" -> "with_recharge"
                    "without_recharge" -> "without_recharge"
                    else -> ""
                }
            } else ""

            val finalDevice = when (device_type?.lowercase()) {
                "android" -> "android"
                "ios" -> "ios"
                else -> ""
            }

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
            val finalPlan = if (item.product_id == 10 || item.product_id == 11) {
                when (item.plan_type?.lowercase()) {
                    "with_recharge" -> "with_recharge"
                    "without_recharge" -> "without_recharge"
                    else -> ""
                }
            } else ""

            val finalDevice = when (item.device_type?.lowercase()) {
                "android" -> "android"
                "ios" -> "ios"
                else -> ""
            }

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

        if (billingId == null) {
            _paymentState.value = PaymentState.Failed("Please select delivery address")
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
                    billingId = billingId
                )

                val orderToken = resp["orderToken"]?.jsonPrimitive?.content
                    ?: throw Exception("orderToken missing")

                val phonepeOrderId = resp["phonepeOrderId"]?.jsonPrimitive?.content
                    ?: throw Exception("orderId missing")

                println("✅ Order Created: token=${orderToken.take(50)}..., orderId=$phonepeOrderId")

                // ✅ 2. Launch PhonePe SDK
                println("📱 Launching PhonePe SDK...")
                openPhonePeSdk(orderToken) { result ->
                    handlePaymentResult(result)
                }

            } catch (e: Exception) {
                println("❌ Order creation failed: ${e.message}")
                _paymentState.value = PaymentState.Failed(e.message ?: "Order creation failed")
            }
        }
    }

    // ✅ FIXED: Payment result handler
    private fun handlePaymentResult(result: com.developer.ev2z.phonepe.PaymentResult) {
        println("📊 Payment Result Received:")
        println("  - Success: ${result.success}")
        println("  - TxnId: ${result.transactionId}")
        println("  - Message: ${result.message}")

        if (result.success) {
            // ⭐ IMPORTANT: User completed payment flow
            // Backend will verify via webhook
            println("✅ Payment flow completed successfully")
            _paymentState.value = PaymentState.Success(result.transactionId)

            // ✅ Clear cart ONLY after successful payment
            clearCart()

        } else {
            // ❌ Payment failed or cancelled
            println("❌ Payment failed: ${result.message}")
            _paymentState.value = PaymentState.Failed(result.message ?: "Payment failed")
        }
    }

    // ✅ Clear cart after successful payment
    private fun clearCart() {
        scope.launch {
            try {
                _state.value = _state.value.copy(
                    cart = emptyList(),
                    cartCount = 0,
                    totalPrice = "0",
                    finalAmount = null,
                    promoApplied = false,
                    appliedPromoCode = null,
                    discountApplied = null
                )
                println("✅ Cart cleared successfully")
            } catch (e: Exception) {
                println("❌ Cart clear error: ${e.message}")
            }
        }
    }

    // ✅ Reset payment state (for retry)
    fun resetPaymentState() {
        println("🔄 Resetting payment state to Idle")
        _paymentState.value = PaymentState.Idle
    }
}