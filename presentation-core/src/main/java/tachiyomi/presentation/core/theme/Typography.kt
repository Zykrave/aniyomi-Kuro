package tachiyomi.presentation.core.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import tachiyomi.presentation.core.R

private val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs,
)

val KuroDisplayFontFamily = FontFamily(
    Font(googleFont = GoogleFont("Space Grotesk"), fontProvider = provider, weight = FontWeight.Medium),
    Font(googleFont = GoogleFont("Space Grotesk"), fontProvider = provider, weight = FontWeight.SemiBold),
    Font(googleFont = GoogleFont("Space Grotesk"), fontProvider = provider, weight = FontWeight.Bold),
)

val KuroBodyFontFamily = FontFamily(
    Font(googleFont = GoogleFont("Inter"), fontProvider = provider, weight = FontWeight.Normal),
    Font(googleFont = GoogleFont("Inter"), fontProvider = provider, weight = FontWeight.Medium),
    Font(googleFont = GoogleFont("Inter"), fontProvider = provider, weight = FontWeight.SemiBold),
)

val KuroTypography = Typography().run {
    copy(
        displayLarge = displayLarge.copy(fontFamily = KuroDisplayFontFamily, fontWeight = FontWeight.Bold),
        displayMedium = displayMedium.copy(fontFamily = KuroDisplayFontFamily, fontWeight = FontWeight.Bold),
        displaySmall = displaySmall.copy(fontFamily = KuroDisplayFontFamily, fontWeight = FontWeight.Bold),
        headlineLarge = headlineLarge.copy(fontFamily = KuroDisplayFontFamily, fontWeight = FontWeight.SemiBold),
        headlineMedium = headlineMedium.copy(fontFamily = KuroDisplayFontFamily, fontWeight = FontWeight.SemiBold),
        headlineSmall = headlineSmall.copy(fontFamily = KuroDisplayFontFamily, fontWeight = FontWeight.SemiBold),
        titleLarge = titleLarge.copy(fontFamily = KuroDisplayFontFamily, fontWeight = FontWeight.Medium),
        titleMedium = titleMedium.copy(fontFamily = KuroDisplayFontFamily, fontWeight = FontWeight.Medium),
        titleSmall = titleSmall.copy(fontFamily = KuroDisplayFontFamily, fontWeight = FontWeight.Medium),
        bodyLarge = bodyLarge.copy(fontFamily = KuroBodyFontFamily, fontWeight = FontWeight.Normal),
        bodyMedium = bodyMedium.copy(fontFamily = KuroBodyFontFamily, fontWeight = FontWeight.Normal),
        bodySmall = bodySmall.copy(fontFamily = KuroBodyFontFamily, fontWeight = FontWeight.Normal),
        labelLarge = labelLarge.copy(fontFamily = KuroBodyFontFamily, fontWeight = FontWeight.Medium),
        labelMedium = labelMedium.copy(fontFamily = KuroBodyFontFamily, fontWeight = FontWeight.Medium),
        labelSmall = labelSmall.copy(fontFamily = KuroBodyFontFamily, fontWeight = FontWeight.Medium),
    )
}


val Typography.header: TextStyle
    @Composable
    get() = bodyMedium.copy(
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.SemiBold,
    )
