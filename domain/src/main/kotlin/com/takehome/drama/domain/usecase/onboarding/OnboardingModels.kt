package com.takehome.drama.domain.usecase.onboarding

import com.takehome.drama.domain.model.Tag

/**
 * Bốn mood của SCR-0004, mỗi mood là một cụm tag **có thật** trong taxonomy của catalogue (đề: "map
 * vào `tagIds`"). Fixtures không có suspense/horror/action nên Thrill dùng plot-twist + fantasy;
 * `comedy` không thuộc mood nào, chỉ chọn được ở hàng fine-tune.
 */
enum class Mood(val tagIds: List<String>) {
    Rage(listOf("revenge", "comeback", "underdog-triumph", "urban")),
    Swoon(listOf("sweet-romance", "ceo", "contract-marriage", "marriage-first-love-later", "love")),
    Cry(listOf("angsty-romance", "reunion", "redemption")),
    Thrill(listOf("plot-twist", "fantasy")),
}

/** Một mood hiển thị được: các tag của nó còn trong catalogue, theo thứ tự cụm. */
data class MoodOption(val mood: Mood, val tags: List<Tag>)

/**
 * Lựa chọn ở SCR-0004: mood bật cả cụm chip của nó; chip sửa tay được ([addedTags] / [removedTags]).
 * Bỏ một mood thì tắt chip của cụm đó, chip bật tay vẫn giữ. Gu lưu xuống = [tagIds].
 */
data class TasteSelection(
    val moods: Set<Mood> = emptySet(),
    val addedTags: Set<String> = emptySet(),
    val removedTags: Set<String> = emptySet(),
) {
    fun tagIds(options: List<MoodOption>): Set<String> =
        options.filter { it.mood in moods }.flatMapTo(LinkedHashSet()) { option -> option.tags.map { it.id } } +
            addedTags - removedTags

    /** Chọn lại một mood thì cả cụm bật lại, kể cả chip từng tắt tay. */
    fun toggleMood(option: MoodOption): TasteSelection =
        if (option.mood in moods) {
            copy(moods = moods - option.mood)
        } else {
            copy(moods = moods + option.mood, removedTags = removedTags - option.tags.map { it.id }.toSet())
        }

    fun toggleTag(tagId: String, options: List<MoodOption>): TasteSelection =
        if (tagId in tagIds(options)) {
            copy(addedTags = addedTags - tagId, removedTags = removedTags + tagId)
        } else {
            copy(addedTags = addedTags + tagId, removedTags = removedTags - tagId)
        }

    val isEmpty: Boolean get() = moods.isEmpty() && addedTags.isEmpty()
}

/** Một dòng ở SCR-0002. [label] là endonym — tên ngôn ngữ viết bằng chính nó. */
data class LanguageOption(val code: String, val label: String)

data class LanguageChoice(val options: List<LanguageOption>, val selected: String)

/** Pre-prompt SCR-0005 chỉ có nghĩa từ Android 13 (API 33, `POST_NOTIFICATIONS` runtime) và khi chưa có quyền. */
object NotificationPromptPolicy {
    const val RUNTIME_PERMISSION_SDK = 33

    fun shouldShow(sdkInt: Int, granted: Boolean): Boolean = sdkInt >= RUNTIME_PERMISSION_SDK && !granted
}
