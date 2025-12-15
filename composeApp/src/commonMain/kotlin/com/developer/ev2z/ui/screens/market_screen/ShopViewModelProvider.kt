package com.developer.ev2z.ui.screens.market_screen

import com.developer.ev2z.repository.ProductRepository
import com.developer.ev2z.util.AppTokenProvider
import com.developer.ev2z.util.ApiProvider

object ShopViewModelProvider {

    val viewModel: ShopViewModel by lazy {
        ShopViewModel(
            repo = ProductRepository(ApiProvider.apiService),
            tokenProvider = {
                AppTokenProvider.getToken()
            }
        )
    }
}