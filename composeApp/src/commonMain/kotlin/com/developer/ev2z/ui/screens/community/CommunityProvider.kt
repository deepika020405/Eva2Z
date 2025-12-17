package com.developer.ev2z.ui.screens.community

// CommunityProvider.kt

import com.developer.ev2z.repository.CommunityRepository
import com.developer.ev2z.util.ApiProvider

object CommunityProvider {
    val repository by lazy {
        CommunityRepository(ApiProvider.apiService)
    }
}