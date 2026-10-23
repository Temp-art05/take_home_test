package com.takehome.drama.core.ads.config

import com.takehome.drama.core.analytics.remoteconfig.RemoteConfigSource
import com.takehome.drama.core.analytics.remoteconfig.RemoteKeys
import com.takehome.drama.domain.model.ads.AdsConfig
import com.takehome.drama.domain.repository.AdsConfigRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import timber.log.Timber

class AdsConfigRepositoryImpl(
    private val remoteConfig: RemoteConfigSource,
    private val debugOverride: AdsDebugOverride,
) : AdsConfigRepository {

    override val config: Flow<AdsConfig> = combine(
        remoteConfig.observe(RemoteKeys.ADS_CONFIG)
            .map(AdsConfigParser::parse)
            .catch { e ->
                Timber.tag(AdsConfigParser.TAG).w(e, "ads_config unreadable, using embedded default")
                emit(AdsConfig.DEFAULT)
            },
        debugOverride.config,
    ) { remote, override -> override ?: remote }
        .distinctUntilChanged()

    override fun current(): AdsConfig =
        debugOverride.config.value ?: runCatching { AdsConfigParser.parse(remoteConfig.get(RemoteKeys.ADS_CONFIG)) }
            .getOrDefault(AdsConfig.DEFAULT)
}
