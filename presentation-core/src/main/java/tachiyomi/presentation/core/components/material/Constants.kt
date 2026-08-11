package tachiyomi.presentation.core.components.material

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.unit.dp

val topSmallPaddingValues = PaddingValues(top = MaterialTheme.padding.small)

const val DISABLED_ALPHA = .38f
const val SECONDARY_ALPHA = .78f

class Padding {

    val extraLarge = 32.dp

    val large = 24.dp

    val medium = 16.dp

    val mediumSmall = 12.dp

    val small = 8.dp

    val extraSmall = 4.dp
}

val MaterialTheme.padding: Padding
    get() = Padding()

class Radius {
    val extraLarge = 24.dp
    val large = 16.dp
    val medium = 14.dp
    val small = 8.dp
    val extraSmall = 6.dp
}

val MaterialTheme.radius: Radius
    get() = Radius()

class Elevation {
    val level0 = 0.dp
    val level1 = 1.dp
    val level2 = 2.dp
    val level3 = 4.dp
    val level4 = 8.dp
}

val MaterialTheme.elevation: Elevation
    get() = Elevation()
