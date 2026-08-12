package eu.kanade.tachiyomi.ui.browse

import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.tab.LocalTabNavigator
import cafe.adriel.voyager.navigator.tab.TabOptions
import eu.kanade.presentation.components.AppBarActions
import eu.kanade.presentation.components.AppBarTitle
import eu.kanade.presentation.components.SearchToolbar
import eu.kanade.presentation.components.SegmentedPill
import eu.kanade.presentation.components.TabContent
import eu.kanade.presentation.util.Tab
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.browse.anime.extension.AnimeExtensionsScreenModel
import eu.kanade.tachiyomi.ui.browse.anime.extension.animeExtensionsTab
import eu.kanade.tachiyomi.ui.browse.anime.migration.sources.migrateAnimeSourceTab
import eu.kanade.tachiyomi.ui.browse.anime.source.animeSourcesTab
import eu.kanade.tachiyomi.ui.browse.anime.source.globalsearch.GlobalAnimeSearchScreen
import eu.kanade.tachiyomi.ui.browse.manga.extension.MangaExtensionsScreenModel
import eu.kanade.tachiyomi.ui.browse.manga.extension.mangaExtensionsTab
import eu.kanade.tachiyomi.ui.browse.manga.migration.sources.migrateMangaSourceTab
import eu.kanade.tachiyomi.ui.browse.manga.source.mangaSourcesTab
import eu.kanade.tachiyomi.ui.main.MainActivity
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import tachiyomi.i18n.MR
import tachiyomi.i18n.aniyomi.AYMR
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.i18n.stringResource
import kotlinx.collections.immutable.ImmutableList

data object BrowseTab : Tab {

    override val options: TabOptions
        @Composable
        get() {
            val isSelected = LocalTabNavigator.current.current is BrowseTab
            val image = AnimatedImageVector.animatedVectorResource(R.drawable.anim_browse_enter)
            return TabOptions(
                index = 3u,
                title = stringResource(MR.strings.browse),
                icon = rememberAnimatedVectorPainter(image, isSelected),
            )
        }

    // TODO: Find a way to let it open Global Anime/Manga Search depending on what Tab(e.g. Anime/Manga Source Tab) is open
    override suspend fun onReselect(navigator: Navigator) {
        navigator.push(GlobalAnimeSearchScreen())
    }

    private val switchToTabNumberChannel = Channel<Pair<Int, Int>>(1, BufferOverflow.DROP_OLDEST)

    fun showExtension() {
        switchToTabNumberChannel.trySend(1 to 1) // Manga extensions: tab no. 3
    }

    fun showAnimeExtension() {
        switchToTabNumberChannel.trySend(0 to 1) // Anime extensions: tab no. 2
    }

    @Composable
    override fun Content() {
        val context = LocalContext.current

        // Hoisted for extensions tab's search bar
        val mangaExtensionsScreenModel = rememberScreenModel { MangaExtensionsScreenModel() }
        val mangaExtensionsState by mangaExtensionsScreenModel.state.collectAsState()

        val animeExtensionsScreenModel = rememberScreenModel { AnimeExtensionsScreenModel() }
        val animeExtensionsState by animeExtensionsScreenModel.state.collectAsState()

        val tabs = persistentListOf(
            animeSourcesTab(),
            mangaSourcesTab(),
            animeExtensionsTab(animeExtensionsScreenModel),
            mangaExtensionsTab(mangaExtensionsScreenModel),
            migrateAnimeSourceTab(),
            migrateMangaSourceTab(),
        )

        BrowseGroupedContent(
            tabs = tabs,
            mangaSearchQuery = mangaExtensionsState.searchQuery,
            onChangeMangaSearchQuery = mangaExtensionsScreenModel::search,
            animeSearchQuery = animeExtensionsState.searchQuery,
            onChangeAnimeSearchQuery = animeExtensionsScreenModel::search,
        )

        LaunchedEffect(Unit) {
            (context as? MainActivity)?.ready = true
        }
    }

    @Composable
    private fun BrowseMediaTypeToggle(
        selectedIndex: Int,
        onSelect: (Int) -> Unit,
        modifier: Modifier = Modifier,
    ) {
        SegmentedPill(
            selectedIndex = selectedIndex,
            items = persistentListOf(
                stringResource(AYMR.strings.label_anime),
                stringResource(AYMR.strings.label_manga),
            ),
            onSelect = onSelect,
            modifier = modifier,
        )
    }

    @Composable
    private fun BrowseSubTabRow(
        selectedIndex: Int,
        onSelect: (Int) -> Unit,
        modifier: Modifier = Modifier,
    ) {
        val options = persistentListOf(
            stringResource(MR.strings.label_sources),
            stringResource(MR.strings.label_extensions),
            stringResource(MR.strings.label_migration),
        )
        TabRow(
            selectedTabIndex = selectedIndex,
            modifier = modifier,
        ) {
            options.forEachIndexed { index, label ->
                androidx.compose.material3.Tab(
                    selected = index == selectedIndex,
                    onClick = { onSelect(index) },
                    text = {
                        Text(
                            text = label,
                            maxLines = 1,
                        )
                    },
                )
            }
        }
    }

    @Composable
    private fun BrowseGroupedContent(
        tabs: ImmutableList<TabContent>,
        modifier: Modifier = Modifier,
        mangaSearchQuery: String?,
        onChangeMangaSearchQuery: (String?) -> Unit,
        animeSearchQuery: String?,
        onChangeAnimeSearchQuery: (String?) -> Unit,
    ) {
        val pagerState = rememberPagerState(initialPage = 0) { 2 }
        val scope = rememberCoroutineScope()
        var selectedSubTab by rememberSaveable { mutableIntStateOf(0) }

        val snackbarHostState = remember { SnackbarHostState() }

        val index = when (pagerState.currentPage) {
            0 -> when (selectedSubTab) {
                0 -> 0
                1 -> 2
                else -> 4
            }
            else -> when (selectedSubTab) {
                0 -> 1
                1 -> 3
                else -> 5
            }
        }
        val tab = tabs[index]

        Scaffold(
            modifier = modifier,
            containerColor = Color.Transparent,
            topBar = {
                val searchEnabled = tab.searchEnabled
                val actualQuery = if (pagerState.currentPage == 0) animeSearchQuery else mangaSearchQuery
                val actualOnChange = if (pagerState.currentPage == 0) onChangeAnimeSearchQuery else onChangeMangaSearchQuery

                SearchToolbar(
                    titleContent = {
                        AppBarTitle(
                            stringResource(MR.strings.browse),
                            modifier = Modifier,
                            null,
                            tab.numberTitle,
                        )
                    },
                    searchEnabled = searchEnabled,
                    searchQuery = if (searchEnabled) actualQuery else null,
                    onChangeSearchQuery = actualOnChange,
                    actions = { AppBarActions(tab.actions) },
                    navigateUp = tab.navigateUp,
                )
            },
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        ) { contentPadding ->
            Column(
                modifier = Modifier.padding(contentPadding),
            ) {
                BrowseMediaTypeToggle(
                    selectedIndex = pagerState.currentPage,
                    onSelect = { index -> scope.launch { pagerState.animateScrollToPage(index) } },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                )
                BrowseSubTabRow(
                    selectedIndex = selectedSubTab,
                    onSelect = { selectedSubTab = it },
                    modifier = Modifier.fillMaxWidth(),
                )
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.Top,
                ) { page ->
                    val pageIndex = when (page) {
                        0 -> when (selectedSubTab) {
                            0 -> 0
                            1 -> 2
                            else -> 4
                        }
                        else -> when (selectedSubTab) {
                            0 -> 1
                            1 -> 3
                            else -> 5
                        }
                    }
                    tabs[pageIndex].content(PaddingValues(0.dp), snackbarHostState)
                }
            }
        }

        LaunchedEffect(Unit) {
            switchToTabNumberChannel.receiveAsFlow()
                .collectLatest { (mediaType, subTab) ->
                    scope.launch { pagerState.animateScrollToPage(mediaType) }
                    selectedSubTab = subTab
                }
        }
    }
}
