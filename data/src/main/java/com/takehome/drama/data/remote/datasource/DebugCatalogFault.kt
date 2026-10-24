package com.takehome.drama.data.remote.datasource

import com.takehome.drama.data.remote.dto.CatalogDto
import com.takehome.drama.data.remote.dto.ShowEpisodesDto
import java.io.IOException
import kotlinx.coroutines.delay

/**
 * Chỉ flavor dev: ép catalogue tải chậm (chụp SCR-0601) hoặc lỗi mạng (SCR-0603) — flavor dev đọc fixtures
 * nên bật máy bay cũng không ra lỗi. `DebugScreenLauncher` đặt từ extra; bản prod không bao giờ gán.
 */
class DebugCatalogFault {
    @Volatile
    var delayMs: Long = 0

    /** Số lần `getCatalog` kế tiếp ném lỗi mạng; hết thì chạy thật (để thử "Try again"). */
    @Volatile
    var offlineCalls: Int = 0
}

/** Bọc datasource thật bằng [DebugCatalogFault]. */
class FaultInjectingCatalogDataSource(
    private val delegate: CatalogDataSource,
    private val fault: DebugCatalogFault,
) : CatalogDataSource {
    override suspend fun getCatalog(): CatalogDto {
        if (fault.delayMs > 0) delay(fault.delayMs)
        if (fault.offlineCalls > 0) {
            fault.offlineCalls--
            throw IOException("debug offline")
        }
        return delegate.getCatalog()
    }

    override suspend fun getShowEpisodes(showId: String): ShowEpisodesDto = delegate.getShowEpisodes(showId)
}
