package com.takehome.drama.domain.usecase.ads

import com.takehome.drama.domain.config.AppConfig
import com.takehome.drama.domain.model.ads.AdPlacement
import com.takehome.drama.domain.repository.AdsConfigRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class ObserveAdPlacementUseCase(
    private val adsConfigRepository: AdsConfigRepository,
    private val appConfig: AppConfig,
) {
    operator fun invoke(placement: AdPlacement): Flow<Boolean> = adsConfigRepository.config
        .map { AdsPolicy.isEnabled(it, appConfig.versionName, placement) }
        .distinctUntilChanged()
}
