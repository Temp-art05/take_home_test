package com.takehome.drama.feature.main.presentation.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.takehome.drama.domain.model.ShowStatus
import com.takehome.drama.feature.main.R

/**
 * "complete" / "ongoing"; status lạ (hoặc thiếu) → `null`, caller bỏ hẳn phần đó: không in chữ thô của server
 * (sai ngôn ngữ, có thể rỗng → "44 eps · ").
 */
@Composable
fun ShowStatus.label(): String? = when (this) {
    ShowStatus.Completed -> stringResource(R.string.status_complete)
    ShowStatus.Ongoing -> stringResource(R.string.status_ongoing)
    is ShowStatus.Unknown -> null
}
