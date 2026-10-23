package com.takehome.drama.domain.usecase.library

import com.takehome.drama.domain.repository.SavedShowRepository

class SetShowSavedUseCase(
    private val savedShowRepository: SavedShowRepository,
) {
    /**
     * VM truyền trạng thái **mong muốn** (đảo từ trạng thái đang hiển thị) thay vì toggle
     * đọc-rồi-ghi: hai lần bấm nhanh cùng đọc `false` sẽ cho kết quả ngược ý người dùng.
     */
    suspend operator fun invoke(showId: String, saved: Boolean): Result<Unit> =
        if (saved) savedShowRepository.save(showId) else savedShowRepository.remove(showId)
}
