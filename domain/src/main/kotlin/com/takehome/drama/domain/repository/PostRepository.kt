package com.takehome.drama.domain.repository

import com.takehome.drama.domain.model.Page
import com.takehome.drama.domain.model.Post
import kotlinx.coroutines.flow.Flow

/**
 * SAMPLE — interface ở `:domain`, impl ở `:data` (`PostRepositoryImpl`). Domain không biết dữ liệu
 * đến từ Ktor hay Room. Hàm một lần trả `Result`, stream trả thẳng `Flow`.
 */
interface PostRepository {
    suspend fun getPosts(skip: Int, limit: Int): Result<Page<Post>>

    fun observeBookmarkedIds(): Flow<Set<Int>>

    suspend fun toggleBookmark(post: Post): Result<Unit>
}
