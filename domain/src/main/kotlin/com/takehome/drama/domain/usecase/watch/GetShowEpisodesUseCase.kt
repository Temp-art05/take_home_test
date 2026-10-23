package com.takehome.drama.domain.usecase.watch

import com.takehome.drama.domain.model.ShowEpisodes
import com.takehome.drama.domain.repository.CatalogRepository

/** File tập của một show — thứ tự phát là thứ tự mảng của server. */
class GetShowEpisodesUseCase(
    private val catalogRepository: CatalogRepository,
) {
    suspend operator fun invoke(showId: String, forceRefresh: Boolean = false): Result<ShowEpisodes> =
        catalogRepository.getEpisodes(showId, forceRefresh)
}
