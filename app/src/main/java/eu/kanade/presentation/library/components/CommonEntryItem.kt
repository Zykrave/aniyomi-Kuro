package eu.kanade.presentation.library.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import eu.kanade.presentation.entries.components.ItemCover
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.BadgeGroup
import tachiyomi.presentation.core.components.material.elevation
import tachiyomi.presentation.core.components.material.radius
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.selectedBackground
import tachiyomi.domain.entries.EntryCover as EntryCoverModel

object CommonEntryItemDefaults {
    val GridHorizontalSpacer = 4.dp
    val GridVerticalSpacer = 4.dp

    @Suppress("ConstPropertyName")
    const val BrowseFavoriteCoverAlpha = 0.34f
}

private val ContinueViewingButtonSizeSmall = 28.dp
private val ContinueViewingButtonSizeLarge = 32.dp

private val ContinueViewingButtonIconSizeSmall = 16.dp
private val ContinueViewingButtonIconSizeLarge = 20.dp

private val ContinueViewingButtonGridPadding = 6.dp
private val ContinueViewingButtonListSpacing = 8.dp

private const val GRID_SELECTED_COVER_ALPHA = 0.76f

/**
 * Layout of grid list item with title overlaying the cover.
 * Accepts null [title] for a cover-only view.
 */
@Composable
fun EntryCompactGridItem(
    coverData: EntryCoverModel,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    isSelected: Boolean = false,
    title: String? = null,
    onClickContinueViewing: (() -> Unit)? = null,
    coverAlpha: Float = 1f,
    coverBadgeStart: @Composable (RowScope.() -> Unit)? = null,
    coverBadgeEnd: @Composable (RowScope.() -> Unit)? = null,
) {
    GridItemSelectable(
        isSelected = isSelected,
        onClick = onClick,
        onLongClick = onLongClick,
    ) {
        EntryGridCover(
            cover = {
                ItemCover.Book(
                    modifier = Modifier
                        .fillMaxWidth()
                        .alpha(if (isSelected) GRID_SELECTED_COVER_ALPHA else coverAlpha),
                    data = coverData,
                )
            },
            badgesStart = coverBadgeStart,
            badgesEnd = coverBadgeEnd,
            content = {
                if (title != null) {
                    CoverTextOverlay(
                        title = title,
                        onClickContinueViewing = onClickContinueViewing,
                        compact = true,
                    )
                } else if (onClickContinueViewing != null) {
                    ContinueViewingButton(
                        size = ContinueViewingButtonSizeLarge,
                        iconSize = ContinueViewingButtonIconSizeLarge,
                        onClick = onClickContinueViewing,
                        modifier = Modifier
                            .padding(ContinueViewingButtonGridPadding)
                            .align(Alignment.BottomEnd),
                    )
                }
            },
        )
    }
}

/**
 * Title overlay for [EntryCompactGridItem]
 */
@Composable
private fun BoxScope.CoverTextOverlay(
    title: String,
    subtitle: String? = null,
    onClickContinueViewing: (() -> Unit)? = null,
    compact: Boolean = false,
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(bottomStart = 4.dp, bottomEnd = 4.dp))
            .background(
                Brush.verticalGradient(
                    0f to Color.Transparent,
                    1f to Color.Black.copy(alpha = 0.9f),
                ),
            )
            .fillMaxHeight(if (compact) 0.33f else 0.45f)
            .fillMaxWidth()
            .align(Alignment.BottomCenter),
    )
    Row(
        modifier = Modifier.align(Alignment.BottomStart),
        verticalAlignment = Alignment.Bottom,
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(if (compact) 8.dp else 12.dp),
        ) {
            GridItemTitle(
                title = title,
                style = if (compact) {
                    MaterialTheme.typography.titleSmall.copy(
                        color = Color.White,
                        shadow = Shadow(
                            color = Color.Black,
                            blurRadius = 4f,
                        ),
                    )
                } else {
                    MaterialTheme.typography.titleMedium.copy(
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        shadow = Shadow(
                            color = Color.Black,
                            blurRadius = 4f,
                        ),
                    )
                },
                minLines = 1,
                compact = compact,
            )
            if (subtitle != null && !compact) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color.White.copy(alpha = 0.8f),
                        shadow = Shadow(
                            color = Color.Black,
                            blurRadius = 4f,
                        ),
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (onClickContinueViewing != null) {
            ContinueViewingButton(
                size = if (compact) ContinueViewingButtonSizeSmall else ContinueViewingButtonSizeLarge,
                iconSize = if (compact) ContinueViewingButtonIconSizeSmall else ContinueViewingButtonIconSizeLarge,
                onClick = onClickContinueViewing,
                modifier = Modifier.padding(
                    end = ContinueViewingButtonGridPadding,
                    bottom = ContinueViewingButtonGridPadding,
                ),
            )
        }
    }
}

/**
 * Layout of grid list item with title below the cover.
 */
@Composable
fun EntryComfortableGridItem(
    isSelected: Boolean = false,
    title: String,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    titleMaxLines: Int = 2,
    coverData: EntryCoverModel,
    coverAlpha: Float = 1f,
    coverBadgeStart: (@Composable RowScope.() -> Unit)? = null,
    coverBadgeEnd: (@Composable RowScope.() -> Unit)? = null,
    onClickContinueViewing: (() -> Unit)? = null,
) {
    GridItemSelectable(
        isSelected = isSelected,
        onClick = onClick,
        onLongClick = onLongClick,
    ) {
        Column {
            EntryGridCover(
                cover = {
                    ItemCover.Book(
                        modifier = Modifier
                            .fillMaxWidth()
                            .alpha(if (isSelected) GRID_SELECTED_COVER_ALPHA else coverAlpha),
                        data = coverData,
                    )
                },
                badgesStart = coverBadgeStart,
                badgesEnd = coverBadgeEnd,
                content = {
                    if (onClickContinueViewing != null) {
                        ContinueViewingButton(
                            size = ContinueViewingButtonSizeLarge,
                            iconSize = ContinueViewingButtonIconSizeLarge,
                            onClick = onClickContinueViewing,
                            modifier = Modifier
                                .padding(ContinueViewingButtonGridPadding)
                                .align(Alignment.BottomEnd),
                        )
                    }
                },
            )
            GridItemTitle(
                modifier = Modifier.padding(4.dp),
                title = title,
                style = MaterialTheme.typography.titleSmall,
                minLines = 2,
                maxLines = titleMaxLines,
            )
        }
    }
}

/**
 * Common cover layout to add contents to be drawn on top of the cover.
 */
@Composable
private fun EntryGridCover(
    modifier: Modifier = Modifier,
    cover: @Composable BoxScope.() -> Unit = {},
    badgesStart: (@Composable RowScope.() -> Unit)? = null,
    badgesEnd: (@Composable RowScope.() -> Unit)? = null,
    content: @Composable (BoxScope.() -> Unit)? = null,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(ItemCover.Book.ratio),
    ) {
        cover()
        content?.invoke(this)
        if (badgesStart != null) {
            BadgeGroup(
                modifier = Modifier
                    .padding(4.dp)
                    .align(Alignment.TopStart),
                content = badgesStart,
            )
        }

        if (badgesEnd != null) {
            BadgeGroup(
                modifier = Modifier
                    .padding(4.dp)
                    .align(Alignment.TopEnd),
                content = badgesEnd,
            )
        }
    }
}

@Composable
private fun GridItemTitle(
    title: String,
    style: TextStyle,
    minLines: Int,
    modifier: Modifier = Modifier,
    maxLines: Int = 2,
    compact: Boolean = false,
) {
    Text(
        modifier = modifier,
        text = title,
        fontSize = if (compact) 10.sp else 12.sp,
        lineHeight = if (compact) 14.sp else 18.sp,
        minLines = minLines,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        style = style,
    )
}

/**
 * Library-specific grid item with title overlaying the cover.
 */
@Composable
fun LibraryCompactGridItem(
    coverData: EntryCoverModel,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    title: String? = null,
    progress: Float = 0f,
    onClickContinueViewing: (() -> Unit)? = null,
    coverBadgeStart: @Composable (RowScope.() -> Unit)? = null,
    coverBadgeEnd: @Composable (RowScope.() -> Unit)? = null,
) {
    LibraryGridItemSelectable(
        isSelected = isSelected,
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = modifier,
    ) {
        LibraryGridCover(
            cover = {
                ItemCover.Book(
                    modifier = Modifier.fillMaxWidth(),
                    data = coverData,
                    shape = RoundedCornerShape(16.dp),
                )
            },
            progress = progress,
            badgesStart = coverBadgeStart,
            badgesEnd = coverBadgeEnd,
            content = {
                if (title != null) {
                    CoverTextOverlay(
                        title = title,
                        onClickContinueViewing = onClickContinueViewing,
                        compact = true,
                    )
                } else if (onClickContinueViewing != null) {
                    ContinueViewingButton(
                        size = ContinueViewingButtonSizeLarge,
                        iconSize = ContinueViewingButtonIconSizeLarge,
                        onClick = onClickContinueViewing,
                        modifier = Modifier
                            .padding(ContinueViewingButtonGridPadding)
                            .align(Alignment.BottomEnd),
                    )
                }
            },
        )
    }
}

/**
 * Library-specific grid item with title below the cover.
 */
@Composable
fun LibraryComfortableGridItem(
    isSelected: Boolean = false,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    coverData: EntryCoverModel,
    modifier: Modifier = Modifier,
    progress: Float = 0f,
    coverBadgeStart: (@Composable RowScope.() -> Unit)? = null,
    coverBadgeEnd: (@Composable RowScope.() -> Unit)? = null,
    onClickContinueViewing: (() -> Unit)? = null,
    compact: Boolean = false,
) {
    LibraryGridItemSelectable(
        isSelected = isSelected,
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = modifier,
        compact = compact,
    ) {
        LibraryGridCover(
            cover = {
                ItemCover.Book(
                    modifier = Modifier.fillMaxWidth(),
                    data = coverData,
                    shape = androidx.compose.ui.graphics.RectangleShape,
                )
            },
            progress = progress,
            badgesStart = coverBadgeStart,
            badgesEnd = coverBadgeEnd,
            compact = compact,
            content = {
                CoverTextOverlay(
                    title = title,
                    subtitle = subtitle,
                    onClickContinueViewing = onClickContinueViewing,
                    compact = compact,
                )
            },
        )
    }
}

@Composable
private fun LibraryGridItemSelectable(
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    content: @Composable () -> Unit,
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "cardPressScale",
    )
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(300)) +
            slideInVertically(animationSpec = tween(300), initialOffsetY = { it / 4 }),
        exit = ExitTransition.None,
    ) {
        Box(
            modifier = modifier
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .then(if (compact) Modifier.width(90.dp) else Modifier)
                .shadow(
                    elevation = if (compact) 1.dp else MaterialTheme.elevation.level3,
                    shape = RoundedCornerShape(if (compact) 10.dp else MaterialTheme.radius.large),
                )
                .clip(RoundedCornerShape(if (compact) 10.dp else MaterialTheme.radius.large))
                .background(MaterialTheme.colorScheme.surface)
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(if (compact) 10.dp else MaterialTheme.radius.large),
                )
                .combinedClickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick,
                    onLongClick = onLongClick,
                )
                .selectedOutline(isSelected = isSelected, color = MaterialTheme.colorScheme.secondary),
        ) {
            val contentColor = if (isSelected) {
                MaterialTheme.colorScheme.onSecondary
            } else {
                LocalContentColor.current
            }
            CompositionLocalProvider(LocalContentColor provides contentColor) {
                content()
            }
        }
    }
}

@Composable
private fun LibraryGridCover(
    modifier: Modifier = Modifier,
    cover: @Composable BoxScope.() -> Unit = {},
    progress: Float = 0f,
    badgesStart: (@Composable RowScope.() -> Unit)? = null,
    badgesEnd: (@Composable RowScope.() -> Unit)? = null,
    compact: Boolean = false,
    content: @Composable (BoxScope.() -> Unit)? = null,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(ItemCover.Book.ratio),
    ) {
        cover()
        if (progress > 0f) {
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .align(Alignment.BottomCenter)
                    .clip(
                        RoundedCornerShape(
                            bottomStart = if (compact) 10.dp else MaterialTheme.radius.large,
                            bottomEnd = if (compact) 10.dp else MaterialTheme.radius.large,
                        ),
                    ),
                color = MaterialTheme.colorScheme.secondary,
                trackColor = Color.Transparent,
            )
        }
        content?.invoke(this)
        if (badgesStart != null) {
            BadgeGroup(
                modifier = Modifier
                    .padding(if (compact) 4.dp else 8.dp)
                    .align(Alignment.TopStart),
                content = badgesStart,
            )
        }

        if (badgesEnd != null) {
            BadgeGroup(
                modifier = Modifier
                    .padding(if (compact) 4.dp else 8.dp)
                    .align(Alignment.TopEnd),
                content = badgesEnd,
            )
        }
    }
}

/**
 * Wrapper for grid items to handle selection state, click and long click.
 */
@Composable
private fun GridItemSelectable(
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .clip(MaterialTheme.shapes.small)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            )
            .selectedOutline(isSelected = isSelected, color = MaterialTheme.colorScheme.secondary)
            .padding(4.dp),
    ) {
        val contentColor = if (isSelected) {
            MaterialTheme.colorScheme.onSecondary
        } else {
            LocalContentColor.current
        }
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            content()
        }
    }
}

/**
 * @see GridItemSelectable
 */
private fun Modifier.selectedOutline(
    isSelected: Boolean,
    color: Color,
) = drawBehind { if (isSelected) drawRect(color = color) }

/**
 * Layout of list item.
 */
@Composable
fun EntryListItem(
    isSelected: Boolean = false,
    title: String,
    coverData: EntryCoverModel,
    coverAlpha: Float = 1f,
    onLongClick: () -> Unit,
    onClick: () -> Unit,
    badge: @Composable (RowScope.() -> Unit),
    onClickContinueViewing: (() -> Unit)? = null,
    entries: Int = 0,
    containerHeight: Int = 0,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .selectedBackground(isSelected)
            .height(
                when (entries) {
                    0 -> 76.dp
                    else -> {
                        val density = LocalDensity.current
                        with(density) { (containerHeight / entries).toDp() } - (3 / entries).dp
                    }
                },
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            )
            .padding(horizontal = 16.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ItemCover.Book(
            modifier = Modifier
                .fillMaxHeight()
                .alpha(coverAlpha),
            data = coverData,
        )
        Text(
            text = title,
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .weight(1f),
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodyMedium,
        )
        BadgeGroup(content = badge)
        if (onClickContinueViewing != null) {
            ContinueViewingButton(
                size = ContinueViewingButtonSizeSmall,
                iconSize = ContinueViewingButtonIconSizeSmall,
                onClick = onClickContinueViewing,
                modifier = Modifier.padding(start = ContinueViewingButtonListSpacing),
            )
        }
    }
}

@Composable
private fun ContinueViewingButton(
    size: Dp,
    iconSize: Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        FilledIconButton(
            onClick = onClick,
            shape = MaterialTheme.shapes.small,
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f),
                contentColor = contentColorFor(MaterialTheme.colorScheme.primaryContainer),
            ),
            modifier = Modifier.size(size),
        ) {
            Icon(
                imageVector = Icons.Filled.PlayArrow,
                contentDescription = stringResource(MR.strings.action_resume),
                modifier = Modifier.size(iconSize),
            )
        }
    }
}
