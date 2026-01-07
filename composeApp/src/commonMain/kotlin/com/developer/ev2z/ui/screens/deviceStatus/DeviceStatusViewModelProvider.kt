package com.developer.ev2z.ui.screens.deviceStatus

import com.developer.ev2z.repository.DeviceStatusRepository
import com.developer.ev2z.util.ApiProvider
import com.developer.ev2z.util.AppTokenProvider

object DeviceStatusViewModelProvider {

    val viewModel: DeviceStatusViewModel by lazy {
        DeviceStatusViewModel(
            repo = DeviceStatusRepository(ApiProvider.apiService),
            tokenProvider = {
                AppTokenProvider.getToken()
            }
        )
    }
}
