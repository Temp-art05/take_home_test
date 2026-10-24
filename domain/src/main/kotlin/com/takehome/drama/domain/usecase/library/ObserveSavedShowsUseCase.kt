package com.takehome.drama.domain.usecase.library

import com.takehome.drama.domain.model.Catalog
import com.takehome.drama.domain.model.Show
import com.takehome.drama.domain.model.ShowStatus
import com.takehome.drama.domain.repository.SavedShowRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Chip của Saved, mirror status của Browse. */
enum class SavedFilter { All, Airing, Complete }

/**
 * Show đã lưu (mới lưu trước), lọc sẵn theo chip. Một stream cho cả màn: đổi chip chỉ đọc lại
 * [shows], không subscribe Room lại; [isEmpty] (chưa lưu gì → SCR-0602) khác với chip không khớp.
 */
data class SavedShelf(private val all: List<Show>) {
    val isEmpty: Boolean get() = all.isEmpty()

    fun shows(filter: SavedFilter): List<Show> = all.filter { it.matches(filter) }

    fun count(filter: SavedFilter): Int = all.count { it.matches(filter) }

    /** Status lạ ([ShowStatus.Unknown]) không đoán là Airing hay Complete — chỉ hiện ở [SavedFilter.All]. */
    private fun Show.matches(filter: SavedFilter): Boolean = when (filter) {
        SavedFilter.All -> true
        SavedFilter.Airing -> status == ShowStatus.Ongoing
        SavedFilter.Complete -> status == ShowStatus.Completed
    }
}

class ObserveSavedShowsUseCase(
    private val savedShowRepository: SavedShowRepository,
) {
    /** Show không còn trong catalogue bị ẩn. */
    operator fun invoke(catalog: Catalog): Flow<SavedShelf> =
        savedShowRepository.observeAll().map { saved ->
            val showsById = catalog.shows.associateBy { it.id }
            SavedShelf(saved.mapNotNull { showsById[it.showId] })
        }
}
