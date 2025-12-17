// CommunityViewModel.kt
package com.developer.ev2z.ui.screens.community

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.developer.ev2z.repository.CommunityRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import com.developer.ev2z.response.Post

data class CommunityUiState(
    val loading: Boolean = false,
    val posts: List<Post> = emptyList(),
    val error: String? = null
)

class CommunityViewModel(
    private val repository: CommunityRepository
) {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private var _state by mutableStateOf(CommunityUiState())
    val state: CommunityUiState get() = _state

    // 🔑 Refresh trigger
    private var refreshKey by mutableStateOf(0)

    fun observeRefreshKey(): Int = refreshKey

    // ❌ init me loadPosts mat karo
    // init { loadPosts() }

    fun loadPosts() {
        if (_state.loading) return   // 👈 safety: double call avoid

        _state = _state.copy(loading = true, error = null)

        scope.launch {
            try {
                val posts = repository.fetchPosts()
                _state = CommunityUiState(
                    loading = false,
                    posts = posts
                )
            } catch (e: Exception) {
                _state = CommunityUiState(
                    loading = false,
                    error = e.message ?: "Something went wrong"
                )
            }
        }
    }

    // 🔥 Call this when ANY change happens
    fun refreshPosts() {
        refreshKey++
    }

    fun createPost(
        title: String,
        body: String,
        images: List<ByteArray>
    ) {
        scope.launch {
            try {
                repository.createPost(title, body, images)
                refreshPosts()
            } catch (e: Exception) {
                // handle error
            }
        }
    }
}