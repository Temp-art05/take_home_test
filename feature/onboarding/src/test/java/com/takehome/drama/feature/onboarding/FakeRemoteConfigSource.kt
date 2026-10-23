package com.takehome.drama.feature.onboarding

import com.takehome.drama.core.analytics.remoteconfig.RemoteConfigKey
import com.takehome.drama.core.analytics.remoteconfig.RemoteConfigSource
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf

/** Fetch treo tới khi test `complete` [gate] — giả lập Remote Config chậm / mất mạng. */
class FakeRemoteConfigSource(val gate: CompletableDeferred<Boolean> = CompletableDeferred()) : RemoteConfigSource {
    override val isReady: StateFlow<Boolean> = MutableStateFlow(false)
    var fetchCalls = 0
        private set

    override suspend fun fetchAndSync(keys: List<RemoteConfigKey<*>>): Boolean {
        fetchCalls++
        return gate.await()
    }

    override fun <T> get(key: RemoteConfigKey<T>): T = key.default
    override fun <T> observe(key: RemoteConfigKey<T>): Flow<T> = flowOf(key.default)
    override suspend fun <T> getAsync(key: RemoteConfigKey<T>): T = key.default
}
