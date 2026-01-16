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
import com.developer.ev2z.response.StoryGroup
import com.developer.ev2z.response.StoryViewer
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

data class CommunityUiState(
    val loading: Boolean = false,        // only for initial load
    val posts: List<Post> = emptyList(),
    val stories: List<Story> = emptyList(),
    val authUserId: Int = 0,

    val error: String? = null,

    val postUpdating: Boolean = false,   // 🔥 NEW
    val storyUpdating: Boolean = false,  // 🔥 NEW

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

    // 🔁 Refresh trigger
    private var refreshKey by mutableStateOf(0)
    fun observeRefreshKey(): Int = refreshKey

    private val viewedStoryIds = mutableSetOf<Int>()

//    private var storyGroups: List<StoryGroup> = emptyList()
var storyGroups by mutableStateOf<List<StoryGroup>>(emptyList())
    private set

    private var currentGroupIndex by mutableStateOf(0)
    private var storyJob: kotlinx.coroutines.Job? = null

    var currentGroup: StoryGroup? = null
    var storyIndex by mutableStateOf(0)
    var storyProgress by mutableStateOf(0f)
    var storyPaused by mutableStateOf(false)
    private val loadMutex = Mutex()
    /* ---------------- POSTS ---------------- */


//    fun loadPosts(forceRefresh: Boolean = false) {
//        scope.launch {
//            loadMutex.withLock {
//
//                // ⛔ Ab ek time pe sirf ONE coroutine yahan enter karegi
//                if (_state.loading) return@withLock
//
//                _state = _state.copy(loading = true)
//
//                try {
//                    val posts = repository.fetchPosts()
//                    val storiesRes = repository.fetchStories()
//
//                    viewedStoryIds.clear()
//
//                    _state = _state.copy(
//                        loading = false,
//                        posts = posts,
//                        stories = storiesRes.stories,
//                        authUserId = storiesRes.auth_user_id,
//                        error = null
//                    )
//
//                } catch (e: Exception) {
//                    _state = _state.copy(
//                        loading = false,
//                        error = e.message
//                    )
//                }
//            }
//        }
//    }

    fun loadPosts() {
        scope.launch {
            loadMutex.withLock {
                if (_state.loading) return@withLock

                _state = _state.copy(
                    loading = true,
                    error = null
                )

                try {
                    val posts = repository.fetchPosts()
//                    val storiesRes = repository.fetchStories()
//
//                    viewedStoryIds.clear()
//
//                    _state = _state.copy(
//                        loading = false,
//                        posts = posts,
//                        stories = storiesRes.stories,
//                        authUserId = storiesRes.auth_user_id
//                    )
                    val storiesRes = repository.fetchStories()

                    val freshStories = storiesRes.stories.toList()
                    viewedStoryIds.clear()          // ✅ IMPORTANT

                    rebuildStoryGroups(freshStories) // 🔥 REQUIRED

                    _state = _state.copy(
                        loading = false,
                        posts = posts,
                        stories = freshStories,
                        authUserId = storiesRes.auth_user_id
                    )

                } catch (e: Exception) {
                    _state = _state.copy(
                        loading = false,
                        error = e.message ?: "Server Error"
                    )
                }
            }
        }
    }

//    fun refreshPosts() {
//        refreshKey++
//        loadPosts(forceRefresh = true)
//    }


//    fun createPost(
//        title: String,
//        body: String,
//        images: List<ByteArray>
//    ) {
//        scope.launch {
//            try {
//                val response = repository.createPost(title, body, images)
//
//                // ✅ JsonObject safe check
//                if (response["post"] != null) {
//                    loadPosts(forceRefresh = true)
//                } else {
//                    println("❌ Post create failed: $response")
//                }
//
//            } catch (e: Exception) {
//                _state = _state.copy(error = e.message)
//            }
//        }
//    }

    fun createPost(
        title: String,
        body: String,
        images: List<ByteArray>
    ) {
        scope.launch {
            _state = _state.copy(postUpdating = true)

            try {
                val response = repository.createPost(title, body, images)

                if (response["post"] != null) {
                    val posts = repository.fetchPosts()
                    _state = _state.copy(
                        posts = posts,
                        postUpdating = false,
                        error = null      // 🔥 CLEAR ERROR
                    )
                } else {
                    _state = _state.copy(
                        postUpdating = false,
                        error = "Post create failed"
                    )
                }

            } catch (e: Exception) {
                _state = _state.copy(
                    postUpdating = false,
                    error = e.message
                )
            }
        }
    }

    fun pauseStory() {
        _state = _state.copy(storyPaused = true)
    }

    fun resumeStory() {
        _state = _state.copy(storyPaused = false)
    }
    /* ---------------- POST LIKE ---------------- */

    fun toggleLike(postId: Int) {
        val currentPost = _state.posts.find { it.id == postId } ?: return

        val optimisticLiked = !currentPost.liked
        val optimisticCount =
            if (optimisticLiked) currentPost.likes_count + 1
            else maxOf(0, currentPost.likes_count - 1)

        _state = _state.copy(
            posts = _state.posts.map {
                if (it.id == postId)
                    it.copy(liked = optimisticLiked, likes_count = optimisticCount)
                else it
            }
        )

        scope.launch {
            try {
                val serverLiked = repository.toggleLike(postId)

                _state = _state.copy(
                    posts = _state.posts.map {
                        if (it.id == postId)
                            it.copy(liked = serverLiked)
                        else it
                    }
                )
            } catch (_: Exception) {
                _state = _state.copy(
                    posts = _state.posts.map {
                        if (it.id == postId) currentPost else it
                    }
                )
            }
        }
    }

    /* ---------------- COMMENTS ---------------- */

    fun load(postId: Int) {
        scope.launch {
            _state = _state.copy(loading = true)
            val list = repository.getComments(postId)
            _state = _state.copy(loading = false, comments = list)
        }
    }

    fun sendComment(postId: Int, text: String) {
        scope.launch {
            repository.addComment(postId, text, _state.replyTo?.id)
            _state = _state.copy(replyTo = null)
            load(postId)
            incrementCommentCount(postId)
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

    /* ---------------- STORIES ---------------- */

//    fun loadStories() {
//        scope.launch {
//            try {
//                val res = repository.fetchStories()
//                _state = _state.copy(
//                    stories = res.stories,
//                    authUserId = res.auth_user_id
//                )
//            } catch (_: Exception) {}
//        }
//    }
fun loadStories() {
    scope.launch {
        try {
            val res = repository.fetchStories()
            val freshStories = res.stories.toList()

            println("🧪 loadStories count = ${freshStories.size}")

            rebuildStoryGroups(freshStories)

            _state = _state.copy(
                stories = freshStories,
                authUserId = res.auth_user_id
            )

        } catch (e: Exception) {
            println("❌ loadStories error: ${e.message}")
            // ❗ keep old UI
        }
    }
}

    private fun rebuildStoryGroups(stories: List<Story>) {
        storyGroups = stories
            .groupBy { it.user.id }
            .map { (_, userStories) ->
                StoryGroup(
                    user = userStories.first().user,
                    stories = userStories.sortedBy { it.created_at }
                )
            }

        println("🔥 rebuildStoryGroups -> groups=${storyGroups.size}, stories=${stories.size}")
    }

    fun groupedStories(): List<StoryGroup> {
        val groups = state.stories
            .groupBy { it.user.id }
            .map { (_, stories) ->
                StoryGroup(
                    user = stories.first().user,
                    stories = stories.sortedBy { it.created_at }
                )
            }

        storyGroups = groups
        return groups
    }

    fun startStoryGroup(group: StoryGroup) {
        storyGroups = groupedStories()
        currentGroupIndex =
            storyGroups.indexOfFirst { it.user.id == group.user.id }.coerceAtLeast(0)

        currentGroup = group
        storyIndex = 0
        storyProgress = 0f
        storyPaused = false

        onStoryOpened(group.stories.first())
        startProgress()
    }

    private fun startProgress() {
        storyJob?.cancel()

        storyJob = scope.launch {
            storyProgress = 0f
            while (storyProgress < 1f) {
                if (!storyPaused) {
                    kotlinx.coroutines.delay(150)
                    storyProgress += 0.01f
                } else {
                    kotlinx.coroutines.delay(50)
                }
            }
            nextStoryInGroup()
        }
    }



    private val storyMutex = Mutex()

//    fun createStory(
//        body: String,
//        media: List<ByteArray>
//    ) {
//        scope.launch {
//            try {
//                val response = repository.addStory(body, media)
//
//                val success = response["success"]?.toString() == "true"
//
//                if (success) {
//                    delay(300)
//                    loadStories()   // ✅ only on success
//                } else {
//                    println("❌ Story upload failed: $response")
//                    // ❗ DO NOTHING to UI
//                }
//
//            } catch (e: Exception) {
//                println("❌ Create Story Exception: ${e.message}")
//                // ❗ DO NOTHING to UI
//            }
//        }
//    }

    fun createStory(
        body: String,
        media: List<ByteArray>
    ) {
        scope.launch {
            try {
                val response = repository.addStory(body, media)

                // ✅ CORRECT success condition
                val isSuccess =
                    response["story"] != null ||
                            response["success"]?.toString() == "true"

                if (isSuccess) {
                    println("✅ Story upload success")
                    delay(300) // backend sync
                    loadStories()
                } else {
                    println("❌ Story upload failed: $response")
                }

            } catch (e: Exception) {
                println("❌ Create Story Exception: ${e.message}")
            }
        }
    }
    suspend fun createStoryAndReturnResult(
        body: String,
        media: List<ByteArray>
    ): Boolean {
        return try {
            val response = repository.addStory(body, media)

            if (response["success"]?.toString() == "true") {
                loadStories() // ✅ only stories
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }
    fun nextStoryInGroup() {
        val group = currentGroup ?: return

        if (storyIndex < group.stories.lastIndex) {
            storyIndex++
            storyProgress = 0f
            onStoryOpened(group.stories[storyIndex])
            startProgress()
        } else if (currentGroupIndex < storyGroups.lastIndex) {
            currentGroupIndex++
            val nextGroup = storyGroups[currentGroupIndex]
            currentGroup = nextGroup
            storyIndex = 0
            storyProgress = 0f
            onStoryOpened(nextGroup.stories.first())
            startProgress()
        } else {
            currentGroup = null
        }
    }

    fun prevStoryInGroup() {
        val group = currentGroup ?: return

        if (storyIndex > 0) {
            storyIndex--
        } else if (currentGroupIndex > 0) {
            currentGroupIndex--
            val prevGroup = storyGroups[currentGroupIndex]
            currentGroup = prevGroup
            storyIndex = prevGroup.stories.lastIndex
        }

        storyProgress = 0f
        onStoryOpened(currentStory()!!)
        startProgress()
    }

    fun currentStory(): Story? =
        currentGroup?.stories?.getOrNull(storyIndex)

    fun onStoryOpened(story: Story) {
        if (viewedStoryIds.add(story.id)) {
            scope.launch { repository.viewStory(story.id) }
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

    fun isGroupViewed(group: StoryGroup): Boolean {
        return group.stories.all { viewedStoryIds.contains(it.id) }
    }
    fun incrementCommentCount(postId: Int) {
        _state = state.copy(
            posts = state.posts.map {
                if (it.id == postId)
                    it.copy(comments_count = it.comments_count + 1)
                else it
            }
        )
    }
}