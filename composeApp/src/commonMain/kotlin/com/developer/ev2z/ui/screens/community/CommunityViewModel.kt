// CommunityViewModel.kt
package com.developer.ev2z.ui.screens.community

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.developer.ev2z.repository.CommunityRepository
import com.developer.ev2z.response.Comment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import com.developer.ev2z.response.Post
import com.developer.ev2z.response.Story
import com.developer.ev2z.response.StoryViewer
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

data class CommunityUiState(
    val loading: Boolean = false,
    val posts: List<Post> = emptyList(),
    val stories: List<Story> = emptyList(),
    val authUserId: Int = 0,
    val error: String? = null,
    val comments: List<Comment> = emptyList(),
    val replyTo: Comment? = null,
    val storyIndex: Int = 0,
    val storyProgress: Float = 0f,
    val storyPaused: Boolean = false,
    val storyViewers: List<StoryViewer> = emptyList()
)


class CommunityViewModel(
    private val repository: CommunityRepository
) {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private var _state by mutableStateOf(CommunityUiState())
    val state: CommunityUiState get() = _state

    // 🔑 Refresh trigger
    private var refreshKey by mutableStateOf(0)
    private val CACHE_VALIDITY = 2 * 60 * 1000 // ⏱️ 2 minutes
    // 🔥 In-memory cache
    private var cachedPosts: List<Post>? = null
    private var cachedStories: List<Story>? = null
    private var cachedAuthUserId: Int = 0
    private var lastLoadedAt: Long = 0L


    fun observeRefreshKey(): Int = refreshKey
    private val viewedStoryIds = mutableSetOf<Int>()
    private val cachedViewedStoryIds = mutableSetOf<Int>()





//    fun loadPosts() {
//        if (_state.loading) return
//
//        _state = _state.copy(loading = true)
//
//        scope.launch {
//            try {
//                val posts = repository.fetchPosts()
//                val storiesRes = repository.fetchStories()
//
//                _state = _state.copy(
//                    loading = false,
//                    posts = posts,
//                    stories = storiesRes.stories,
//                    authUserId = storiesRes.auth_user_id
//                )
//            } catch (e: Exception) {
//                _state = _state.copy(
//                    loading = false,
//                    error = e.message
//                )
//            }
//        }
//    }

    @OptIn(ExperimentalTime::class)
    fun loadPosts(forceRefresh: Boolean = false) {
        val now = Clock.System.now().toEpochMilliseconds()
        // ✅ USE CACHE
        if (
            !forceRefresh &&
            cachedPosts != null &&
            cachedStories != null &&
            now - lastLoadedAt < CACHE_VALIDITY
        ) {
            viewedStoryIds.clear()
            viewedStoryIds.addAll(cachedViewedStoryIds)

            _state = _state.copy(
                loading = false,
                posts = cachedPosts!!,
                stories = cachedStories!!,
                authUserId = cachedAuthUserId,
                error = null
            )
            return
        }

        if (_state.loading) return

        _state = _state.copy(loading = true)

        scope.launch {
            try {
                val posts = repository.fetchPosts()
                val storiesRes = repository.fetchStories()

                // 🔥 SAVE TO CACHE
                cachedPosts = posts
                cachedStories = storiesRes.stories
                cachedAuthUserId = storiesRes.auth_user_id
                lastLoadedAt = Clock.System.now().toEpochMilliseconds()

                _state = _state.copy(
                    loading = false,
                    posts = posts,
                    stories = storiesRes.stories,
                    authUserId = storiesRes.auth_user_id,
                    error = null
                )
            } catch (e: Exception) {
                _state = _state.copy(
                    loading = false,
                    error = e.message
                )
            }
        }
    }


    // 🔥 Call this when ANY change happens
    fun refreshPosts() {
        refreshKey++
        loadPosts(forceRefresh = true)
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



    fun toggleLike(postId: Int) {

        // 🔥 Optimistic UI (instant feel)
        val currentPost = _state.posts.find { it.id == postId } ?: return

        val optimisticLiked = !currentPost.liked
        val optimisticCount =
            if (optimisticLiked) currentPost.likes_count + 1
            else maxOf(0, currentPost.likes_count - 1)

        _state = _state.copy(
            posts = _state.posts.map { post ->
                if (post.id == postId) {
                    post.copy(
                        liked = optimisticLiked,
                        likes_count = optimisticCount
                    )
                } else post
            }
        )

        // 🔁 API call
        scope.launch {
            try {
                val serverLiked = repository.toggleLike(postId)

                // 🔄 Sync with server response
                _state = _state.copy(
                    posts = _state.posts.map { post ->
                        if (post.id == postId) {
                            post.copy(
                                liked = serverLiked,
                                likes_count =
                                    if (serverLiked) optimisticCount
                                    else maxOf(0, optimisticCount - 1)
                            )
                        } else post
                    }
                )

            } catch (e: Exception) {
                // ❌ Revert on error
                _state = _state.copy(
                    posts = _state.posts.map { post ->
                        if (post.id == postId) currentPost else post
                    }
                )
            }
        }
        cachedPosts = _state.posts
    }

    fun load(postId: Int) {
        scope.launch {
            _state = _state.copy(loading = true)

            val list = repository.getComments(postId)

            _state = _state.copy(
                loading = false,
                comments = list
            )
        }
    }

    fun sendComment(postId: Int, text: String) {
        scope.launch {
            repository.addComment(postId, text, _state.replyTo?.id)

            _state = _state.copy(replyTo = null)

            load(postId)
            incrementCommentCount(postId)
//            refreshPosts()
        }
    }

    fun like(commentId: Int, postId: Int) {
        scope.launch {
            repository.likeComment(commentId)
            load(postId)
        }
    }

    fun setReply(comment: Comment) {
        _state = _state.copy(replyTo = comment)
    }

    fun clearReply() {
        _state = _state.copy(replyTo = null)
    }


    fun loadStories() {
        scope.launch {
            try {
                val res = repository.fetchStories()
                _state = _state.copy(
                    stories = res.stories,
                    authUserId = res.auth_user_id
                )
            } catch (e: Exception) {
                // ignore or log
            }
        }
    }

    fun createStory(
        body: String,
        media: List<ByteArray>
    ) {
        scope.launch {
            try {
                repository.addStory(body, media)

                // 🔄 refresh stories after upload
                loadStories()

            } catch (e: Exception) {
                println("❌ Create Story Error: ${e.message}")
            }
        }
    }


//    fun startStory(index: Int) {
//        val story = _state.stories.getOrNull(index) ?: return
//
//        _state = _state.copy(
//            storyIndex = index,
//            storyProgress = 0f,
//            storyPaused = false
//        )
//
//        if (!viewedStoryIds.contains(story.id)) {
//            viewedStoryIds.add(story.id)
//
//            // 🔥 cache ko sync rakho
//            cachedViewedStoryIds.add(story.id)
//
//            scope.launch {
//                repository.viewStory(story.id)
//            }
//        }
//        onStoryOpened(story)
//
//        loadStoryViewers(story.id)
//        runStoryTimer()
//    }

    fun startStory(index: Int) {
        val story = _state.stories.getOrNull(index) ?: return

        _state = _state.copy(
            storyIndex = index,
            storyProgress = 0f,
            storyPaused = false
        )

        onStoryOpened(story)      // ✅ SINGLE SOURCE
        loadStoryViewers(story.id)
        runStoryTimer()
    }
    private fun runStoryTimer() {
        scope.launch {
            while (_state.storyProgress < 1f) {
                if (!_state.storyPaused) {
                    kotlinx.coroutines.delay(150) // ~15 sec
                    _state = _state.copy(
                        storyProgress = _state.storyProgress + 0.01f
                    )
                } else {
                    kotlinx.coroutines.delay(50)
                }
            }

            nextStory()
        }
    }

    fun pauseStory() {
        _state = _state.copy(storyPaused = true)
    }

    fun resumeStory() {
        _state = _state.copy(storyPaused = false)
    }

    fun nextStory() {
        val next = _state.storyIndex + 1
        if (next < _state.stories.size) {
            startStory(next)
        }
    }

    fun prevStory() {
        val prev = _state.storyIndex - 1
        if (prev >= 0) {
            startStory(prev)
        }
    }

    fun loadStoryViewers(storyId: Int) {
        scope.launch {
            val res = repository.getStoryViewers(storyId)
            _state = _state.copy(storyViewers = res.viewers)
        }
    }

    fun clearStoryViewers() {
        _state = _state.copy(storyViewers = emptyList())
    }

    fun isStoryViewed(storyId: Int): Boolean {
        return viewedStoryIds.contains(storyId)
    }

    fun onStoryOpened(story: Story) {
        if (!viewedStoryIds.contains(story.id)) {
            viewedStoryIds.add(story.id)
            cachedViewedStoryIds.add(story.id)

            scope.launch {
                repository.viewStory(story.id)
            }
        }
    }
    fun incrementCommentCount(postId: Int) {
        _state = state.copy(
            posts = state.posts.map { post ->
                if (post.id == postId) {
                    post.copy(
                        comments_count = post.comments_count + 1
                    )
                } else post
            }
        )
        cachedPosts = _state.posts
    }

}