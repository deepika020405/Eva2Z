package com.developer.ev2z.ui.screens.community

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.developer.ev2z.repository.CommunityRepository
import com.developer.ev2z.response.*
import com.developer.ev2z.util.getCityFromLatLng
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class CommunityUiState(
    val loading: Boolean = false,
    val posts: List<Post> = emptyList(),
    val stories: List<Story> = emptyList(),
    val authUserId: Int = 0,               // 🔥 KEEP (UI SAFE)
    val error: String? = null,

    val postUpdating: Boolean = false,
    val storyUpdating: Boolean = false,

    val comments: List<Comment> = emptyList(),
    val replyTo: Comment? = null,

    val storyViewers: List<StoryViewer> = emptyList()
)

class CommunityViewModel(
    private val repository: CommunityRepository
) {

    private val supervisorJob = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.Main + supervisorJob)
    private val loadMutex = Mutex()
    private var loadPostsJob: Job? = null

    private var _state by mutableStateOf(CommunityUiState())
    val state: CommunityUiState get() = _state

    /** ---------------- STORIES CORE ---------------- */

    var storyGroups by mutableStateOf<List<StoryGroup>>(emptyList())
        private set

    private val viewedStoryIds = mutableSetOf<Int>()

    var currentGroup: StoryGroup? = null
    private var currentGroupIndex by mutableStateOf(0)

    var storyIndex by mutableStateOf(0)
    var storyProgress by mutableStateOf(0f)
    var storyPaused by mutableStateOf(false)

    private var storyJob: Job? = null

    /** ---------------- LOAD POSTS + STORIES ---------------- */


    private fun mapStoryGroups(
        response: List<StoryGroupResponse>
    ): List<StoryGroup> {
        return response.map { group ->
            StoryGroup(
                user = group.user,
                stories = group.stories,
                isOwnStory = group.is_own_story
            )
        }
    }

    fun createPost(
        title: String,
        body: String,
        images: List<ByteArray>,
        lat: Double? = null,
        long: Double? = null,
        city: String? = null,
        category: String? = null
    ) {
        scope.launch {
            _state = _state.copy(postUpdating = true)

            try {
                val response = repository.createPost(
                    title = title,
                    body = body,
                    images = images,
                    lat = lat,
                    long = long,
                    city = city,
                    category = category
                )

                // New API returns the created post directly with an "id";
                // older shape wrapped it in "post".
                val created = response["id"] != null || response["post"] != null

                if (created) {
                    val posts = repository.fetchPosts()

                    _state = _state.copy(
                        posts = posts,
                        postUpdating = false,
                        error = null
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

    /** Reverse-geocode coordinates to a city name; result delivered on the main scope. */
    fun resolveCity(lat: Double, lng: Double, onResult: (String) -> Unit) {
        scope.launch {
            val city = try {
                getCityFromLatLng(lat, lng)
            } catch (e: Exception) {
                ""
            }
            onResult(city)
        }
    }

    fun loadPosts() {
        loadPostsJob?.cancel()
        loadPostsJob = scope.launch {
            loadMutex.withLock {
                if (_state.loading) return@withLock

                _state = _state.copy(loading = true, error = null)

                try {
                    val posts = repository.fetchPosts()
                    val storiesRes = repository.fetchStories()

                    storyGroups = mapStoryGroups(storiesRes.data)

                    viewedStoryIds.clear()

                    _state = _state.copy(
                        loading = false,
                        posts = posts,
                        stories = storyGroups.flatMap { it.stories },
                        authUserId = _state.authUserId
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

    fun loadStories() {
        scope.launch {
            try {
                val res = repository.fetchStories()

                storyGroups = mapStoryGroups(res.data)

                _state = _state.copy(
                    stories = storyGroups.flatMap { it.stories }
                )

                viewedStoryIds.clear()

            } catch (e: Exception) {
                println("❌ loadStories error: ${e.message}")
            }
        }
    }

    /** ---------------- STORY PLAYBACK ---------------- */

    fun startStoryGroup(group: StoryGroup) {

        currentGroupIndex =
            storyGroups.indexOfFirst { it.user.id == group.user.id }
                .coerceAtLeast(0)

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
                    delay(150)
                    storyProgress += 0.01f
                } else {
                    delay(50)
                }
            }
            nextStoryInGroup()
        }
    }

    fun pauseStory() {
        storyPaused = true
    }

    fun resumeStory() {
        storyPaused = false
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
            currentStory()?.let { story ->
                if (viewedStoryIds.add(story.id)) {
                    scope.launch { repository.viewStory(story.id) }
                }
            }

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
        currentStory()?.let {
            onStoryOpened(it)
            startProgress()
        }
    }

    fun currentStory(): Story? =
        currentGroup?.stories?.getOrNull(storyIndex)

    fun onStoryOpened(story: Story) {
        if (viewedStoryIds.add(story.id)) {
            scope.launch { repository.viewStory(story.id) }
        }
    }

    fun isGroupViewed(group: StoryGroup): Boolean {
        return group.stories.all { viewedStoryIds.contains(it.id) }
    }

    /** ---------------- STORY CREATE ---------------- */

    fun createStory(
        body: String,
        media: List<ByteArray>
    ) {
        scope.launch {
            try {
                val response = repository.addStory(body, media)

                val success =
                    response["story"] != null ||
                            response["success"]?.toString() == "true"

                if (success) {
                    delay(300)
                    loadStories()
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
            val success = response["success"]?.toString() == "true"
            if (success) loadStories()
            success
        } catch (e: Exception) {
            false
        }
    }

    /** ---------------- STORY VIEWERS ---------------- */

    fun loadStoryViewers(storyId: Int) {
        scope.launch {
            val res = repository.getStoryViewers(storyId)
            _state = _state.copy(storyViewers = res.viewers)
        }
    }

    fun clearStoryViewers() {
        _state = _state.copy(storyViewers = emptyList())
    }

    /** ---------------- POSTS / COMMENTS (UNCHANGED) ---------------- */

    fun toggleLike(postId: Int) {
        val currentPost = _state.posts.find { it.id == postId } ?: return

        val optimisticLiked = !currentPost.liked
        val optimisticCount =
            if (optimisticLiked) currentPost.likes_count + 1
            else maxOf(0, currentPost.likes_count - 1)

        // 1️⃣ Optimistic UI update
        _state = _state.copy(
            posts = _state.posts.map {
                if (it.id == postId)
                    it.copy(
                        liked = optimisticLiked,
                        likes_count = optimisticCount
                    )
                else it
            }
        )

        // 2️⃣ API call
        scope.launch {
            try {
                val serverLiked = repository.toggleLike(postId)

                // 3️⃣ Sync with server response
                _state = _state.copy(
                    posts = _state.posts.map {
                        if (it.id == postId)
                            it.copy(liked = serverLiked)
                        else it
                    }
                )

            } catch (e: Exception) {
                // ❌ Rollback on failure
                _state = _state.copy(
                    posts = _state.posts.map {
                        if (it.id == postId) currentPost else it
                    }
                )
            }
        }
    }

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

    fun incrementCommentCount(postId: Int) {
        _state = _state.copy(
            posts = _state.posts.map {
                if (it.id == postId)
                    it.copy(comments_count = it.comments_count + 1)
                else it
            }
        )
    }

    fun resetStoryViewer() {
        storyJob?.cancel()
        storyJob = null

        currentGroup = null
        currentGroupIndex = 0

        storyIndex = 0
        storyProgress = 0f
        storyPaused = false
    }

    fun cancelActiveJobs() {
        loadPostsJob?.cancel()
        storyJob?.cancel()
    }




}