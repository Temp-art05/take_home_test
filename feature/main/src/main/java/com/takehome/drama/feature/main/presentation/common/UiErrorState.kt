package com.takehome.drama.feature.main.presentation.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.takehome.drama.core.designsystem.component.ErrorBlankState
import com.takehome.drama.core.ui.foundation.error.ErrorKind
import com.takehome.drama.core.ui.foundation.error.UiError
import com.takehome.drama.core.ui.foundation.error.body
import com.takehome.drama.core.ui.foundation.error.retryLabel
import com.takehome.drama.core.ui.foundation.error.title

/** SCR-0603 cho mọi màn dữ liệu của feature — Offline có câu riêng, lỗi khác cùng layout. */
@Composable
internal fun UiErrorState(error: UiError, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    ErrorBlankState(
        title = error.title(),
        body = error.body(),
        retryLabel = retryLabel(),
        onRetry = onRetry,
        offline = error.kind == ErrorKind.Offline,
        modifier = modifier,
    )
}
