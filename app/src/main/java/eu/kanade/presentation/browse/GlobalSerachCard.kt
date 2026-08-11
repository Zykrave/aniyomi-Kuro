package eu.kanade.presentation.browse

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.library.components.LibraryComfortableGridItem
import tachiyomi.domain.entries.EntryCover

@Composable
fun GlobalSearchCard(
    title: String,
    cover: EntryCover,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    Box(modifier = Modifier.width(112.dp)) {
        LibraryComfortableGridItem(
            title = title,
            coverData = cover,
            coverBadgeStart = {
                InLibraryBadge(enabled = isFavorite)
            },
            onClick = onClick,
            onLongClick = onLongClick,
        )
    }
}
