package com.base.app.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.base.app.core.designsystem.theme.AppTheme
import com.base.app.core.designsystem.theme.AppTypography

/*
 * Ba trạng thái toàn màn hình dùng chung. Nhận String đã resolve chứ không nhận @StringRes:
 * designsystem không sở hữu string nào — màn hình gọi stringResource() rồi truyền vào.
 */

@Composable
fun LoadingState(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
fun ErrorState(
    message: String,
    retryLabel: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    MessageState(modifier = modifier, message = message) {
        Button(onClick = onRetry) { Text(text = retryLabel) }
    }
}

@Composable
fun EmptyState(
    message: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) {
    MessageState(modifier = modifier, message = message, action = action)
}

@Composable
private fun MessageState(
    message: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    ) {
        Text(
            text = message,
            style = AppTypography.bodyRegular,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        action?.invoke()
    }
}

@PreviewLightDark
@Composable
private fun ErrorStatePreview() {
    AppTheme {
        ErrorState(message = "Something went wrong.", retryLabel = "Retry", onRetry = {})
    }
}
