package com.takehome.drama.domain.usecase.onboarding

import com.takehome.drama.domain.model.Catalog
import com.takehome.drama.domain.model.Tag

class GetMoodOptionsUseCase {

    /** Mood mà catalogue còn ít nhất một tag; tag không có trong catalogue bị lọc, không bịa. */
    operator fun invoke(catalog: Catalog): List<MoodOption> {
        val tagsById = catalog.tags.associateBy { it.id }
        return Mood.entries
            .map { mood -> MoodOption(mood, mood.tagIds.mapNotNull { tagsById[it] }) }
            .filter { it.tags.isNotEmpty() }
    }

    /** Chip fine-tune: mọi genre (`isCategory`) theo `orderPriority`. */
    fun fineTuneTags(catalog: Catalog): List<Tag> =
        catalog.tags.filter { it.isCategory }.sortedBy { it.orderPriority }
}
