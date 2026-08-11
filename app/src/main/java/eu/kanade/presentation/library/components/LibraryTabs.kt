package eu.kanade.presentation.library.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import eu.kanade.presentation.category.visualName
import tachiyomi.domain.category.model.Category
import tachiyomi.presentation.core.components.material.TabText

@Composable
internal fun LibraryTabs(
    categories: List<Category>,
    pagerState: PagerState,
    getNumberOfItemsForCategory: (Category) -> Int?,
    onTabItemClick: (Int) -> Unit,
) {
    val currentPageIndex = pagerState.currentPage.coerceAtMost(categories.lastIndex)
    Column(
        modifier = Modifier.zIndex(1f),
    ) {
        PrimaryScrollableTabRow(
            selectedTabIndex = currentPageIndex,
            edgePadding = 0.dp,
            divider = {},
            indicator = {
                Box(
                    Modifier
                        .tabIndicatorOffset(currentPageIndex, matchContentSize = false)
                        .fillMaxHeight()
                        .padding(horizontal = 8.dp, vertical = 8.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primary,
                            shape = CircleShape,
                        ),
                )
            },
        ) {
            categories.forEachIndexed { index, category ->
                val selected = currentPageIndex == index
                Tab(
                    selected = selected,
                    onClick = { onTabItemClick(index) },
                    text = {
                        TabText(
                            text = category.visualName,
                            badgeCount = getNumberOfItemsForCategory(category),
                        )
                    },
                    selectedContentColor = MaterialTheme.colorScheme.onPrimary,
                    unselectedContentColor = MaterialTheme.colorScheme.onSurface,
                )
            }
        }

        HorizontalDivider()
    }
}
