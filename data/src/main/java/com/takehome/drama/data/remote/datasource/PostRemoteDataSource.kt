package com.takehome.drama.data.remote.datasource

import com.takehome.drama.data.remote.dto.PostsResponse

/** SAMPLE — chỉ nói chuyện bằng DTO, không bắt exception (repository lo). */
interface PostRemoteDataSource {
    suspend fun getPosts(skip: Int, limit: Int): PostsResponse
}
