package com.base.app.feature.main.presentation.feature

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.base.app.core.designsystem.theme.AppTheme
import com.base.app.core.designsystem.theme.AppTypography
import com.base.app.feature.main.R
import org.koin.androidx.compose.koinViewModel

@Composable
fun FeatureScreen(
    viewModel: FeatureViewModel = koinViewModel(),
) {
    val viewState by viewModel.viewState.collectAsStateWithLifecycle()

    FeatureContent(
        viewState = viewState,
        onRefreshClick = { viewModel.onRefreshClick() },
    )
}

@Composable
private fun FeatureContent(
    viewState: FeatureViewState,
    onRefreshClick: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.tab_feature),
            style = AppTypography.headingH4Semibold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Box(modifier = Modifier.height(24.dp), contentAlignment = Alignment.Center) {
            if (viewState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                Text(
                    text = stringResource(R.string.main_feature_loaded, viewState.loadCount),
                    style = AppTypography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        OutlinedButton(
            onClick = onRefreshClick,
            enabled = !viewState.isLoading,
        ) {
            Text(text = stringResource(R.string.main_feature_refresh))
        }
    }
}

@PreviewLightDark
@Composable
private fun FeatureContentPreview() {
    AppTheme {
        FeatureContent(viewState = FeatureViewState(loadCount = 1), onRefreshClick = {})
    }
}
