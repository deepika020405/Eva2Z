package com.developer.ev2z.ui.screens.market_screen


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.developer.ev2z.repository.ProductRepository
import com.developer.ev2z.response.CartItemDto
import com.developer.ev2z.response.ProductDto
import com.developer.ev2z.response.WishlistProductWrapper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class ShopUiState(
    val loading: Boolean = false,
    val products: List<ProductDto> = emptyList(),
    val wishlist: List<WishlistProductWrapper> = emptyList(),
    val cart: List<CartItemDto> = emptyList(),       // new
    val cartCount: Int = 0,                          // new (sum of quantities)
    val totalPrice: String? = null,                  // new
    val error: String? = null
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
}