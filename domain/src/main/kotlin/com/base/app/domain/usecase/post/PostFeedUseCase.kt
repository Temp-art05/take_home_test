package com.base.app.domain.usecase.post

import com.base.app.domain.model.Page
import com.base.app.domain.model.Post
import com.base.app.domain.repository.PostRepository
import kotlinx.coroutines.flow.Flow

/**
 * SAMPLE — một use case = một năng lực nghiệp vụ ("feed bài viết"), không phải một class cho mỗi
 * hàm repository. Đọc/observe thuần là một hàm ở đây.
 *
 * Rule nằm ở đây, không ở ViewModel: kích thước trang, cách tính trang kế, khử trùng lặp khi nối trang.
 */
class PostFeedUseCase(
    private val postRepository: PostRepository,
) {
    suspend fun firstPage(): Result<Page<Post>> =
        postRepository.getPosts(skip = 0, limit = PAGE_SIZE)

    /** Trang kế tiếp sau [loaded]; bỏ post đã có (BE chèn bài mới có thể làm lệch offset). */
    suspend fun nextPage(loaded: List<Post>): Result<Page<Post>> {
        val loadedIds = loaded.mapTo(HashSet()) { it.id }
        return postRepository.getPosts(skip = loaded.size, limit = PAGE_SIZE)
            .map { page -> page.copy(items = page.items.filterNot { it.id in loadedIds }) }
    }

    fun observeBookmarkedIds(): Flow<Set<Int>> = postRepository.observeBookmarkedIds()

    suspend fun toggleBookmark(post: Post): Result<Unit> = postRepository.toggleBookmark(post)

    companion object {
        const val PAGE_SIZE = 20
    }
}
