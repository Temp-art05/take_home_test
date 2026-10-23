package com.takehome.drama.domain.usecase.watch

import com.takehome.drama.domain.model.Episode

/** Xem được khi free hoặc đã mở bằng rewarded — một rule cho Detail, sheet chọn tập và Player. */
fun Episode.isWatchable(unlocked: Set<Int>): Boolean = isFree || index in unlocked
