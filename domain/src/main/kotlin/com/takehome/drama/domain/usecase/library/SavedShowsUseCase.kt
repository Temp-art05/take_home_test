package com.takehome.drama.domain.usecase.library

import com.takehome.drama.domain.model.Catalog
import com.takehome.drama.domain.model.Show
import com.takehome.drama.domain.model.ShowStatus
import com.takehome.drama.domain.repository.SavedShowRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Chip của Saved, mirror status của Browse. */
enum class SavedFilter { All, Airing, Complete }

class SavedShowsUseCase(
    private val savedShowRepository: SavedShowRepository,
) {
    /**
     * VM truyền trạng thái **mong muốn** (đảo từ [observeIsSaved] đang hiển thị) thay vì toggle
     * đọc-rồi-ghi: hai lần bấm nhanh cùng đọc `false` sẽ cho kết quả ngược ý người dùng.
     */
    suspend fun setSaved(showId: String, saved: Boolean): Result<Unit> =
        if (saved) savedShowRepository.save(showId) else savedShowRepository.remove(showId)

    fun observeIsSaved(showId: String): Flow<Boolean> = savedShowRepository.observeIsSaved(showId)

    /**
     * Show đã lưu, mới lưu trước. Show không còn trong catalogue bị ẩn. Status lạ
     * ([ShowStatus.Unknown]) không đoán là Airing hay Complete — chỉ hiện ở [SavedFilter.All].
     */
    fun observeSaved(catalog: Catalog, filter: SavedFilter): Flow<List<Show>> =
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
