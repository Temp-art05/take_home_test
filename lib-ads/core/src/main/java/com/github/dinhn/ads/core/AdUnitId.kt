package com.github.dinhn.ads.core

/**
 * Bộ ad unit id cho MỘT vị trí, hỗ trợ waterfall high-floor -> all-price.
 * Helper tuỳ chọn cho tầng app khi muốn gom 2 id lại; app vẫn có thể dùng String rời.
 */
data class AdUnitId(
    val highFloor: String = "",
    val allPrice: String = "",
) {
    fun waterfall(): List<String> =
        listOf(highFloor, allPrice).filter { it.isNotBlank() }

    val isBlank: Boolean get() = waterfall().isEmpty()

    companion object {
        fun of(id: String): AdUnitId = AdUnitId(allPrice = id)
    }
}
