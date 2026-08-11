package eu.kanade.presentation.browse.manga

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import eu.kanade.presentation.browse.manga.components.GlobalMangaSearchToolbar
import eu.kanade.tachiyomi.source.CatalogueSource
import eu.kanade.tachiyomi.ui.browse.manga.source.globalsearch.MangaSearchScreenModel
import eu.kanade.tachiyomi.ui.browse.manga.source.globalsearch.MangaSourceFilter
import tachiyomi.domain.entries.manga.model.Manga
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.screens.EmptyScreen

@Composable
fun MigrateMangaSearchScreen(
    state: MangaSearchScreenModel.State,
    fromSourceId: Long?,
    navigateUp: () -> Unit,
    onChangeSearchQuery: (String?) -> Unit,
    onSearch: (String) -> Unit,
    onChangeSearchFilter: (MangaSourceFilter) -> Unit,
    onToggleResults: () -> Unit,
    getManga: @Composable (Manga) -> State<Manga>,
    onClickSource: (CatalogueSource) -> Unit,
    onClickItem: (Manga) -> Unit,
    onLongClickItem: (Manga) -> Unit,
) {
    Scaffold(
        topBar = { scrollBehavior ->
            GlobalMangaSearchToolbar(
                searchQuery = state.searchQuery,
                progress = state.progress,
                total = state.total,
                navigateUp = navigateUp,
                onChangeSearchQuery = onChangeSearchQuery,
                onSearch = onSearch,
                sourceFilter = state.sourceFilter,
                onChangeSearchFilter = onChangeSearchFilter,
                onlyShowHasResults = state.onlyShowHasResults,
                onToggleResults = onToggleResults,
                scrollBehavior = scrollBehavior,
            )
        },
    ) { paddingValues ->
        if (state.searchQuery.isNullOrBlank()) {
            EmptyScreen(
                stringRes = MR.strings.action_search_hint,
                modifier = Modifier.padding(paddingValues),
            )
            return@Scaffold
        }

        if (state.filteredItems.isEmpty() && state.progress == state.total) {
            EmptyScreen(
                stringRes = MR.strings.no_results_found,
                modifier = Modifier.padding(paddingValues),
            )
            return@Scaffold
        }

        GlobalSearchContent(
            fromSourceId = fromSourceId,
            items = state.filteredItems,
            contentPadding = paddingValues,
            getManga = getManga,
            onClickSource = onClickSource,
            onClickItem = onClickItem,
            onLongClickItem = onLongClickItem,
        )
    }
}
