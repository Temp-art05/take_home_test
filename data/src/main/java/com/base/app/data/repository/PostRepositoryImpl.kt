package com.base.app.data.repository

import com.base.app.data.local.datasource.BookmarkLocalDataSource
import com.base.app.data.mapper.toBookmarkEntity
import com.base.app.data.mapper.toDomain
import com.base.app.data.remote.datasource.PostRemoteDataSource
import com.base.app.data.remote.error.toDomainException
import com.base.app.data.util.mapError
import com.base.app.data.util.withCatching
import com.base.app.domain.model.Page
import com.base.app.domain.model.Post
import com.base.app.domain.repository.PostRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * SAMPLE — ghép remote (Ktor) + local (Room). Đây là chỗ DUY NHẤT đổi thread + bắt exception
 * (`withCatching`) và dịch lỗi hạ tầng sang `DomainException` (`toDomainException`).
 */
class PostRepositoryImpl(
    private val postRemoteDataSource: PostRemoteDataSource,
    private val bookmarkLocalDataSource: BookmarkLocalDataSource,
) : PostRepository {

    override suspend fun getPosts(skip: Int, limit: Int): Result<Page<Post>> = withCatching {
        postRemoteDataSource.getPosts(skip = skip, limit = limit).toDomain()
    }.mapError { it.toDomainException() }

    override fun observeBookmarkedIds(): Flow<Set<Int>> =
        bookmarkLocalDataSource.observeIds().map { it.toSet() }

    override suspend fun toggleBookmark(post: Post): Result<Unit> = withCatching {
        if (bookmarkLocalDataSource.exists(post.id)) {
            bookmarkLocalDataSource.delete(post.id)
        } else {
            bookmarkLocalDataSource.insert(post.toBookmarkEntity())
        }
    }
}
