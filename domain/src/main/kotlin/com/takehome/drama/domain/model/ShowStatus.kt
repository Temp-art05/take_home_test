package com.takehome.drama.domain.model

/**
 * `status` là string tự do của server — fixtures chỉ có `"completed"` nhưng đề báo sẽ có giá trị
 * khác. Không dùng enum đóng: giá trị lạ rơi vào [Unknown] thay vì làm vỡ parse.
 */
sealed interface ShowStatus {
    data object Completed : ShowStatus
    data object Ongoing : ShowStatus

    /** Giá trị server mà app chưa biết. Giữ [raw] để log, không hiện cho user (không theo ngôn ngữ app). */
    data class Unknown(val raw: String) : ShowStatus

    companion object {
        private val COMPLETED = setOf("completed", "complete", "finished", "ended")
        private val ONGOING = setOf("ongoing", "airing", "updating", "serializing")

        fun parse(raw: String?): ShowStatus {
            val value = raw?.trim()?.lowercase().orEmpty()
            return when (value) {
                in COMPLETED -> Completed
                in ONGOING -> Ongoing
                else -> Unknown(raw.orEmpty())
            }
        }
    }
}
