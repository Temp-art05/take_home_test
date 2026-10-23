package com.takehome.drama.domain.usecase.catalog

import com.takehome.drama.domain.model.Catalog
import com.takehome.drama.domain.repository.CatalogRepository

/** Catalogue (cache in-memory ở repository); [forceRefresh] bỏ qua bản đó. */
class GetCatalogUseCase(
    private val catalogRepository: CatalogRepository,
) {
    suspend operator fun invoke(forceRefresh: Boolean = false): Result<Catalog> =
        catalogRepository.getCatalog(forceRefresh)
}
