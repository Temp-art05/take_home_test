package com.takehome.drama.data.mapper

import com.takehome.drama.data.local.entity.BookmarkEntity
import com.takehome.drama.data.remote.dto.PostDto
import com.takehome.drama.data.remote.dto.PostsResponse
import com.takehome.drama.domain.model.Page
import com.takehome.drama.domain.model.Post

// SAMPLE — mapper là extension function, không dùng interface mapper.

fun PostDto.toDomain(): Post = Post(id = id, title = title, body = body, tags = tags)

fun List<PostDto>.toDomain(): List<Post> = map { it.toDomain() }

/** `hasMore` suy từ `total` của envelope — BE khác thì đọc đúng field của BE. */
fun PostsResponse.toDomain(): Page<Post> = Page(
    items = posts.toDomain(),
    hasMore = skip + posts.size < total,
)

fun Post.toBookmarkEntity(): BookmarkEntity = BookmarkEntity(id = id, title = title, body = body)
