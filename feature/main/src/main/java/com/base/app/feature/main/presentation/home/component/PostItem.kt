package com.base.app.feature.main.presentation.home.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.base.app.core.designsystem.theme.AppTheme
import com.base.app.core.designsystem.theme.AppTypography
import com.base.app.domain.model.Post
import com.base.app.feature.main.R

@Composable
fun PostItem(
    post: Post,
    bookmarked: Boolean,
    onBookmarkClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.medium)
            .padding(start = 16.dp, top = 8.dp, bottom = 16.dp, end = 4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = post.title,
                style = AppTypography.bodyBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onBookmarkClick) {
                Icon(
                    imageVector = if (bookmarked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = stringResource(
                        if (bookmarked) R.string.home_remove_bookmark else R.string.home_add_bookmark
                    ),
                    tint = if (bookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = post.body,
            style = AppTypography.captionRegular,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(end = 12.dp),
        )
    }
}

@PreviewLightDark
@Composable
private fun PostItemPreview() {
    AppTheme {
        PostItem(
            post = Post(id = 1, title = "Sample title", body = "Sample body that is long enough to wrap onto two lines."),
            bookmarked = true,
            onBookmarkClick = {},
        )
    }
}
