package com.takehome.drama.domain.usecase.ads

import com.takehome.drama.domain.config.AppConfig
import com.takehome.drama.domain.model.ads.AdPlacement
import com.takehome.drama.domain.repository.AdsConfigRepository

/** Kiểm một lần, ngay trước khi load/show — cấu hình có thể đã đổi từ lúc màn mở. */
class IsAdPlacementEnabledUseCase(
    private val adsConfigRepository: AdsConfigRepository,
    private val appConfig: AppConfig,
) {
    operator fun invoke(placement: AdPlacement): Boolean =
        AdsPolicy.isEnabled(adsConfigRepository.current(), appConfig.versionName, placement)
}
