package eu.kanade.presentation.updates.anime

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastAll
import androidx.compose.ui.util.fastAny
import eu.kanade.presentation.entries.anime.components.EpisodeDownloadAction
import eu.kanade.presentation.entries.components.EntryBottomActionMenu
import eu.kanade.presentation.library.components.LibraryComfortableGridItem
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.domain.ui.model.HomeCardStyle
import eu.kanade.tachiyomi.data.download.anime.model.AnimeDownload
import eu.kanade.tachiyomi.ui.player.settings.PlayerPreferences
import eu.kanade.tachiyomi.ui.updates.anime.AnimeUpdatesItem
import eu.kanade.tachiyomi.ui.updates.anime.AnimeUpdatesScreenModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import tachiyomi.domain.entries.anime.model.asAnimeCover
import tachiyomi.i18n.MR
import tachiyomi.i18n.aniyomi.AYMR
import tachiyomi.presentation.core.components.FastScrollLazyColumn
import tachiyomi.presentation.core.components.material.PullRefresh
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState
import tachiyomi.presentation.core.screens.EmptyScreen
import tachiyomi.presentation.core.screens.LoadingScreen
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.time.LocalDate
import kotlin.time.Duration.Companion.seconds

@Composable
fun AnimeUpdateScreen(
    state: AnimeUpdatesScreenModel.State,
    snackbarHostState: SnackbarHostState,
    lastUpdated: Long,
    onClickCover: (Long) -> Unit,
    onSelectAll: (Boolean) -> Unit,
    onInvertSelection: () -> Unit,
    onUpdateLibrary: () -> Boolean,
    onDownloadEpisode: (List<AnimeUpdatesItem>, EpisodeDownloadAction) -> Unit,
    onMultiBookmarkClicked: (List<AnimeUpdatesItem>, bookmark: Boolean) -> Unit,
    onMultiFillermarkClicked: (List<AnimeUpdatesItem>, fillermark: Boolean) -> Unit,
    onMultiMarkAsSeenClicked: (List<AnimeUpdatesItem>, seen: Boolean) -> Unit,
    onMultiDeleteClicked: (List<AnimeUpdatesItem>) -> Unit,
    onUpdateSelected: (AnimeUpdatesItem, Boolean, Boolean, Boolean) -> Unit,
    onOpenEpisode: (AnimeUpdatesItem, altPlayer: Boolean) -> Unit,
) {
    val uiPreferences = remember { Injekt.get<UiPreferences>() }
    val homeCardStyle by uiPreferences.homeCardStyle().collectAsState()

    BackHandler(enabled = state.selectionMode, onBack = { onSelectAll(false) })

    Scaffold(
        bottomBar = {
            AnimeUpdatesBottomBar(
                selected = state.selected,
                onDownloadEpisode = onDownloadEpisode,
                onMultiBookmarkClicked = onMultiBookmarkClicked,
                onMultiFillermarkClicked = onMultiFillermarkClicked,
                onMultiMarkAsSeenClicked = onMultiMarkAsSeenClicked,
                onMultiDeleteClicked = onMultiDeleteClicked,
                onOpenEpisode = onOpenEpisode,
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { contentPadding ->
        when {
            state.isLoading -> LoadingScreen(Modifier.padding(contentPadding))
            state.items.isEmpty() -> EmptyScreen(
                stringRes = MR.strings.information_no_recent,
                modifier = Modifier.padding(contentPadding),
            )
            else -> {
                val scope = rememberCoroutineScope()
                var isRefreshing by remember { mutableStateOf(false) }

                PullRefresh(
                    refreshing = isRefreshing,
                    onRefresh = {
                        val started = onUpdateLibrary()
                        if (!started) return@PullRefresh
                        scope.launch {
                            // Fake refresh status but hide it after a second as it's a long running task
                            isRefreshing = true
                            delay(1.seconds)
                            isRefreshing = false
                        }
                    },
                    enabled = !state.selectionMode,
                    indicatorPadding = contentPadding,
                ) {
                    FastScrollLazyColumn(
                        contentPadding = contentPadding,
                    ) {
                        animeUpdatesLastUpdatedItem(lastUpdated)

                        item(key = "home-continue-watching") {
                            HomeShelf(title = stringResource(AYMR.strings.currently_watching)) {
                                if (state.continueWatching.isNotEmpty()) {
                                    items(
                                        items = state.continueWatching,
                                        key = { "continue-watching-${it.id}" },
                                    ) { item ->
                                        LibraryComfortableGridItem(
                                            modifier = Modifier.width(if (homeCardStyle == HomeCardStyle.COMPACT) 90.dp else 128.dp),
                                            title = item.anime.title,
                                            coverData = item.anime.asAnimeCover(),
                                            progress = if (item.totalCount > 0) {
                                                item.seenCount.toFloat() / item.totalCount
                                            } else {
                                                0f
                                            },
                                            onClick = { onClickCover(item.anime.id) },
                                            onLongClick = { /* Handle long click if needed */ },
                                            compact = homeCardStyle == HomeCardStyle.COMPACT,
                                        )
                                    }
                                } else {
                                    item {
                                        Text(
                                            text = stringResource(MR.strings.information_no_recent),
                                            modifier = Modifier.padding(MaterialTheme.padding.small),
                                            style = MaterialTheme.typography.bodySmall,
                                        )
                                    }
                                }
                            }
                        }

                        item(key = "home-recently-updated") {
                            val recentlyUpdated = state.items
                                .distinctBy { it.update.animeId }
                                .take(20)
                            HomeShelf(title = stringResource(AYMR.strings.label_anime_updates)) {
                                if (recentlyUpdated.isNotEmpty()) {
                                    items(
                                        items = recentlyUpdated,
                                        key = { "recently-updated-${it.update.animeId}" },
                                    ) { item ->
                                        LibraryComfortableGridItem(
                                            modifier = Modifier.width(128.dp),
                                            title = item.update.animeTitle,
                                            coverData = item.update.coverData,
                                            onClick = { onClickCover(item.update.animeId) },
                                            onLongClick = { /* Handle long click if needed */ },
                                        )
                                    }
                                } else {
                                    item {
                                        Text(
                                            text = stringResource(MR.strings.information_no_recent),
                                            modifier = Modifier.padding(MaterialTheme.padding.small),
                                            style = MaterialTheme.typography.bodySmall,
                                        )
                                    }
                                }
                            }
                        }

                        item(key = "home-new-episodes") {
                            val newEpisodes = state.items.take(20)
                            HomeShelf(title = stringResource(AYMR.strings.episodes)) {
                                if (newEpisodes.isNotEmpty()) {
                                    items(
                                        items = newEpisodes,
                                        key = { "new-episodes-${it.update.episodeId}" },
                                    ) { item ->
                                        LibraryComfortableGridItem(
                                            modifier = Modifier.width(128.dp),
                                            title = "${item.update.animeTitle} - ${item.update.episodeName}",
                                            coverData = item.update.coverData,
                                            onClick = { onOpenEpisode(item, false) },
                                            onLongClick = { onUpdateSelected(item, !item.selected, true, true) },
                                        )
                                    }
                                } else {
                                    item {
                                        Text(
                                            text = stringResource(MR.strings.information_no_recent),
                                            modifier = Modifier.padding(MaterialTheme.padding.small),
                                            style = MaterialTheme.typography.bodySmall,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeShelf(
    title: String,
    modifier: Modifier = Modifier,
    content: LazyListScope.() -> Unit,
) {
    Column(modifier = modifier.padding(vertical = MaterialTheme.padding.medium)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = MaterialTheme.padding.medium, vertical = MaterialTheme.padding.medium),
            fontWeight = FontWeight.Bold,
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = MaterialTheme.padding.medium),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            content()
        }
    }
}

@Composable
private fun AnimeUpdatesBottomBar(
    selected: List<AnimeUpdatesItem>,
    onDownloadEpisode: (List<AnimeUpdatesItem>, EpisodeDownloadAction) -> Unit,
    onMultiBookmarkClicked: (List<AnimeUpdatesItem>, bookmark: Boolean) -> Unit,
    onMultiFillermarkClicked: (List<AnimeUpdatesItem>, fillermark: Boolean) -> Unit,
    onMultiMarkAsSeenClicked: (List<AnimeUpdatesItem>, seen: Boolean) -> Unit,
    onMultiDeleteClicked: (List<AnimeUpdatesItem>) -> Unit,
    onOpenEpisode: (AnimeUpdatesItem, altPlayer: Boolean) -> Unit,
) {
    val playerPreferences: PlayerPreferences = Injekt.get()
    EntryBottomActionMenu(
        visible = selected.isNotEmpty(),
        modifier = Modifier.fillMaxWidth(),
        onBookmarkClicked = {
            onMultiBookmarkClicked.invoke(selected, true)
        }.takeIf { selected.fastAny { !it.update.bookmark } },
        onRemoveBookmarkClicked = {
            onMultiBookmarkClicked.invoke(selected, false)
        }.takeIf { selected.fastAll { it.update.bookmark } },
        onFillermarkClicked = {
            onMultiFillermarkClicked.invoke(selected, true)
        }.takeIf { selected.fastAny { !it.update.fillermark } },
        onRemoveFillermarkClicked = {
            onMultiFillermarkClicked.invoke(selected, false)
        }.takeIf { selected.fastAll { it.update.fillermark } },
        onMarkAsViewedClicked = {
            onMultiMarkAsSeenClicked(selected, true)
        }.takeIf { selected.fastAny { !it.update.seen } },
        onMarkAsUnviewedClicked = {
            onMultiMarkAsSeenClicked(selected, false)
        }.takeIf { selected.fastAny { it.update.seen || it.update.lastSecondSeen > 0L } },
        onDownloadClicked = {
            onDownloadEpisode(selected, EpisodeDownloadAction.START)
        }.takeIf {
            selected.fastAny { it.downloadStateProvider() != AnimeDownload.State.DOWNLOADED }
        },
        onDeleteClicked = {
            onMultiDeleteClicked(selected)
        }.takeIf { selected.fastAny { it.downloadStateProvider() == AnimeDownload.State.DOWNLOADED } },
        onExternalClicked = {
            onOpenEpisode(selected[0], true)
        }.takeIf { !playerPreferences.alwaysUseExternalPlayer().get() && selected.size == 1 },
        onInternalClicked = {
            onOpenEpisode(selected[0], true)
        }.takeIf { playerPreferences.alwaysUseExternalPlayer().get() && selected.size == 1 },
        isManga = false,
    )
}

sealed interface AnimeUpdatesUiModel {
    data class Header(val date: LocalDate) : AnimeUpdatesUiModel
    data class Item(val item: AnimeUpdatesItem) : AnimeUpdatesUiModel
}
