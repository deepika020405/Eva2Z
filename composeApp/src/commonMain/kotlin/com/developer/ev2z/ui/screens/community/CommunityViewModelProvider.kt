package com.developer.ev2z.ui.screens.community

// CommunityViewModelProvider.kt

import com.developer.ev2z.ui.screens.community.CommunityViewModel

object CommunityViewModelProvider {
    val viewModel by lazy {
        CommunityViewModel(CommunityProvider.repository)
    }
}