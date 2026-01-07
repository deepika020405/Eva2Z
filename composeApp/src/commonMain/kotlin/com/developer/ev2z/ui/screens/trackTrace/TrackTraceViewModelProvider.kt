package com.developer.ev2z.ui.screens.trackTrace


import com.developer.ev2z.repository.TrackTraceRepository
import com.developer.ev2z.util.ApiProvider
import com.developer.ev2z.util.AppTokenProvider



object TrackTraceViewModelProvider {

    val viewModel: TrackTraceViewModel by lazy {
        TrackTraceViewModel(
            repository = TrackTraceRepository(ApiProvider.apiService),
            tokenProvider = AppTokenProvider.getToken
        )
    }
}

