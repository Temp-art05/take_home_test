package com.base.app.domain.model

/**
 * SAMPLE — model domain của luồng "post feed". Không mang annotation của Ktor/Room: domain độc lập
 * với cách dữ liệu được truyền hay lưu. Xoá cùng luồng sample khi làm project thật.
 */
data class Post(
    val id: Int,
    val title: String,
    val body: String,
    val tags: List<String> = emptyList(),
)
