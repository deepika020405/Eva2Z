package com.developer.ev2z.ui.screens.market_screen


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.developer.ev2z.repository.ProductRepository
import com.developer.ev2z.request.CreateOrderRequest
import com.developer.ev2z.response.CartItemDto
import com.developer.ev2z.response.ProductDto
import com.developer.ev2z.response.WishlistProductWrapper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.jsonPrimitive

data class ShopUiState(
    val loading: Boolean = false,
    val products: List<ProductDto> = emptyList(),
    val wishlist: List<WishlistProductWrapper> = emptyList(),
    val cart: List<CartItemDto> = emptyList(),       // new
    val cartCount: Int = 0,                          // new (sum of quantities)
    val totalPrice: String? = null,
    val addresses: List<BillingAddress> = emptyList(),   // ⭐ NEW
    val selectedAddress: BillingAddress? = null,         // ⭐ NEW
    val error: String? = null,
    val promoApplied: Boolean = false,
    val appliedPromoCode: String? = null,
    val originalAmount: String? = null,
    val finalAmount: String? = null,
    val discountApplied: Double? = null,
    val promoError: String? = null,
    val orderLoading: Boolean = false,
    val orderToken: String? = null,
    val merchantOrderId: String? = null,
    val phonePeOrderId: String? = null,
    val orderError: String? = null
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

data class CartState(
    val loading: Boolean = false,
    val cart: List<CartItemDto> = emptyList(),
    val cartCount: Int = 0,
    val totalPrice: String? = "0",

    val addresses: List<BillingAddress> = emptyList(),
    val selectedAddress: BillingAddress? = null
)


class ShopViewModel(
    private val repo: ProductRepository,
    private val tokenProvider: () -> String
) : ViewModel() {

    private val _state = MutableStateFlow(ShopUiState())
    val state: StateFlow<ShopUiState> = _state

//    fun loadData() {
//        _state.value = _state.value.copy(loading = true)
//        val token = tokenProvider()
//
//        viewModelScope.launch {
//            try {
//                val products = repo.getProducts(token)
//                val wishlist = repo.getWishlist(token)
//
//                _state.value = ShopUiState(
//                    loading = false,
//                    products = products,
//                    wishlist = wishlist
//                )
//
//            } catch (e: Exception) {
//                _state.value = ShopUiState(
//                    loading = false,
//                    error = e.message ?: "Unknown Error"
//                )
//            }
//        }
//    }

    fun loadData() {
        _state.value = _state.value.copy(loading = true)
        val token = tokenProvider()

        viewModelScope.launch {
            try {
                val products = repo.getProducts(token)
                val wishlist = repo.getWishlist(token)
                val cartResp = repo.getCart(token)

                // compute cart count as sum of quantities
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

        viewModelScope.launch {
            val isWishlisted = _state.value.wishlist.any { it.product_id == productId }

            if (isWishlisted) repo.removeWishlist(token, productId)
            else repo.addWishlist(token, productId)

            // Refresh wishlist
            val wishlist = repo.getWishlist(token)

            _state.value = _state.value.copy(wishlist = wishlist)
        }
    }

    fun isFavorite(productId: Int): Boolean {
        return _state.value.wishlist.any { it.product_id == productId }
    }


    fun refreshCart() {
        val token = tokenProvider()
        viewModelScope.launch {
            try {
                val cartResp = repo.getCart(token)
                val count = cartResp.cart.sumOf { it.quantity }
                _state.value = _state.value.copy(
                    cart = cartResp.cart,
                    cartCount = count,
                    totalPrice = cartResp.totalPrice
                )
            } catch (e: Exception) {
                // ignore or set error
            }
        }
    }


    fun addToCart(add: String,productId: Int, quantity: Int = 1,plan_type: String,device_type: String) {
        val token = tokenProvider()

        viewModelScope.launch {
            try {
                val ok = repo.addToCart(token, add,productId, quantity,plan_type,device_type)

                if (ok) {
//                    refreshCart() // update cart count UI
                } else {
                    println("❌ Failed to add to cart")
                }

            } catch (e: Exception) {
                println("❌ addToCart error: ${e.message}")
            }
        }
    }


    fun loadCart() {
        val token = tokenProvider()
        _state.value = _state.value.copy(loading = true)

        viewModelScope.launch {
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



    fun updateQuantity(
        productId: Int,
        newQty: Int,
        plan_type: String?,
        device_type: String?
    ) {
        val token = tokenProvider()

        viewModelScope.launch {

            // ⭐ SAME LOGIC APPLY AS ADD TO CART
            val finalPlan = if (productId == 10 || productId == 11) {
                when (plan_type?.lowercase()) {
                    "with_recharge" -> "with_recharge"
                    "without_recharge" -> "without_recharge"
                    else -> ""
                }
            } else {
                ""   // ⭐ OTHER PRODUCTS MUST NOT SEND PLAN TYPE
            }

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

                if (ok) {
                    refreshCart()
                } else {
                    println("❌ Failed to update quantity (Server Reject)")
                }

            } catch (e: Exception) {
                println("❌ updateQuantity error: ${e.message}")
            }
        }
    }


    fun loadBillingAddresses() {
        val token = tokenProvider()

        viewModelScope.launch {

            try {
                _state.value = _state.value.copy(loading = true)

                println("🔄 Loading Billing Addresses...")

                val list = repo.getBillingAddresses(token)
                println("📦 Raw Billing Count = ${list.size}")

                val mapped = list.sortedByDescending { it.id }.map {
                    println("➡ Address Loaded: ${it.fname} ${it.address}")
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

                println("🎯 Final Address List Mapped: ${mapped.size}")

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

        viewModelScope.launch {

            val ok = repo.addBillingAddress(
                token, fname, phone, email, address, city, state, pincode
            )

            if (ok) {
                println("✔ Address Added Successfully")
                loadBillingAddresses()   // refresh list
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

        // ❌ Invalid format?
        if (!upper.startsWith("WELCOME") && !upper.startsWith("REFER")) {
            _state.value = _state.value.copy(
                promoApplied = false,
                promoError = "Please enter a valid coupon code"
            )
            return
        }

        viewModelScope.launch {
            try {
                val resp = repo.applyPromo(token, amount, upper)

                if (!resp.success) {
                    _state.value = _state.value.copy(
                        promoApplied = false,
                        promoError = "Please enter a valid coupon code"
                    )
                    return@launch
                }

                // ⭐ SUCCESS
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



    fun createOrder(onReady: (redirectUrl: String) -> Unit) {
        val s = _state.value
        val token = tokenProvider()

        val amount = (s.finalAmount ?: s.totalPrice ?: "0").toDouble()
        val billingId = s.selectedAddress?.id ?: return

        viewModelScope.launch {
            try {
                val resp = repo.createPhonePeOrder(
                    token = token,
                    amount = amount,
                    billingId = billingId
                )

                println("📦 SERVER ORDER RESPONSE = $resp")

                val merchantId = resp["merchantId"]?.jsonPrimitive?.content
                    ?: error("merchantId missing")

                val orderId = resp["phonepeOrderId"]?.jsonPrimitive?.content
                    ?: error("phonepeOrderId missing")

                val tokenKey = resp["orderToken"]?.jsonPrimitive?.content
                    ?: error("orderToken missing")

                // ⭐ CREATE REDIRECT URL (PhonePe standard format)
                val redirectUrl =
                    "https://mercury.phonepe.com/transact/pg/$merchantId/$orderId?token=$tokenKey"

                onReady(redirectUrl)

            } catch (e: Exception) {
                println("❌ createOrder ERROR = ${e.message}")
            }
        }
    }



    fun removeFromCart(item: CartItemDto) {
        val token = tokenProvider()

        viewModelScope.launch {

            // ⭐ PLAN TYPE — ONLY FOR RECHARGE PRODUCTS
            val finalPlan = if (item.product_id == 10 || item.product_id == 11) {
                when (item.plan_type?.lowercase()) {
                    "with_recharge" -> "with_recharge"
                    "without_recharge" -> "without_recharge"
                    else -> ""
                }
            } else {
                ""   // 🚫 others MUST NOT send plan_type
            }

            // ⭐ DEVICE TYPE — SAME AS UPDATE QUANTITY
            val finalDevice = when (item.device_type?.lowercase()) {
                "android" -> "android"
                "ios" -> "ios"
                else -> ""
            }

            try {
                val ok = repo.addToCart(
                    token = token,
                    add = "remove",          // 🔥 THIS IS THE KEY
                    productId = item.product_id,
                    quantity = 0,            // ignored by backend for remove
                    plan_type = finalPlan,
                    device_type = finalDevice
                )

                if (ok) {
                    refreshCart()
                } else {
                    println("❌ Failed to remove item (Server Reject)")
                }

            } catch (e: Exception) {
                println("❌ removeFromCart error = ${e.message}")
            }
        }
    }
}