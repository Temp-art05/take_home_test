package com.takehome.drama.domain.usecase.library

import com.takehome.drama.domain.model.Catalog
import com.takehome.drama.domain.model.Show
import com.takehome.drama.domain.model.ShowStatus
import com.takehome.drama.domain.repository.SavedShowRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Chip của Saved, mirror status của Browse. */
enum class SavedFilter { All, Airing, Complete }

class ObserveSavedShowsUseCase(
    private val savedShowRepository: SavedShowRepository,
) {
    /**
     * Show đã lưu, mới lưu trước. Show không còn trong catalogue bị ẩn. Status lạ
     * ([ShowStatus.Unknown]) không đoán là Airing hay Complete — chỉ hiện ở [SavedFilter.All].
     */
    operator fun invoke(catalog: Catalog, filter: SavedFilter): Flow<List<Show>> =
        savedShowRepository.observeAll().map { saved ->
            val showsById = catalog.shows.associateBy { it.id }
            saved.mapNotNull { showsById[it.showId] }.filter { it.matches(filter) }
        }

    private fun Show.matches(filter: SavedFilter): Boolean = when (filter) {
        SavedFilter.All -> true
        SavedFilter.Airing -> status == ShowStatus.Ongoing
        SavedFilter.Complete -> status == ShowStatus.Completed
    }
}
