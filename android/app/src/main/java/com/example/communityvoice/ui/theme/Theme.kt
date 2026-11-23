package com.example.communityvoice.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.communityvoice.data.IssueCategory
import com.example.communityvoice.data.IssueStatus

// ---------------------------------------------------------------- base brand

private val Green = Color(0xFF1B6B4A)
private val GreenLight = Color(0xFF7FD1AB)

private val LightColors = lightColorScheme(
    primary = Green,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFCDEFDE),
    onPrimaryContainer = Color(0xFF05291A),
    secondary = Color(0xFF4F6354),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD2E8D8),
    onSecondaryContainer = Color(0xFF0F1F16),
    tertiary = Color(0xFF1E5F8C),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFCFE5F6),
    onTertiaryContainer = Color(0xFF07243A),
    background = Color(0xFFF6FAF7),
    onBackground = Color(0xFF171C19),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF171C19),
    surfaceVariant = Color(0xFFE7EFE9),
    onSurfaceVariant = Color(0xFF424B45),
    outline = Color(0xFF9AA79F),
    outlineVariant = Color(0xFFD3DED7),
    error = Color(0xFFB3261E),
    onError = Color.White,
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B)
)

private val DarkColors = darkColorScheme(
    primary = GreenLight,
    onPrimary = Color(0xFF003822),
    primaryContainer = Color(0xFF115137),
    onPrimaryContainer = Color(0xFF9CF6C7),
    secondary = Color(0xFFB6CCBA),
    onSecondary = Color(0xFF213528),
    secondaryContainer = Color(0xFF374B3D),
    onSecondaryContainer = Color(0xFFD2E8D8),
    tertiary = Color(0xFF9CCBF0),
    onTertiary = Color(0xFF00334F),
    tertiaryContainer = Color(0xFF104A70),
    onTertiaryContainer = Color(0xFFCFE5F6),
    background = Color(0xFF0E1311),
    onBackground = Color(0xFFDFE4E0),
    surface = Color(0xFF171D1A),
    onSurface = Color(0xFFDFE4E0),
    surfaceVariant = Color(0xFF2A322D),
    onSurfaceVariant = Color(0xFFBFC9C2),
    outline = Color(0xFF89938C),
    outlineVariant = Color(0xFF3F4943),
    error = Color(0xFFF2B8B5),
    onError = Color(0xFF601410),
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = Color(0xFFF9DEDC)
)

// -------------------------------------------------- semantic accent colours
// These live outside the Material scheme because each one means exactly one
// thing - "support", "resolved" - rather than filling a general-purpose role.
// Every value has a light and a dark variant, so nothing glows or vanishes
// when the theme flips.

/** A tint for icons and text, plus the container colour that sits behind it. */
data class AccentPair(
    val tint: Color,
    val container: Color
)

data class AccentColors(
    val support: AccentPair,
    val decline: AccentPair,
    val statusOpen: Color,
    val statusReview: Color,
    val statusProgress: Color,
    val statusResolved: Color,
    val statusDeclined: Color,
    val categories: Map<IssueCategory, AccentPair>
)

private val LightAccents = AccentColors(
    support = AccentPair(Color(0xFF1B6B4A), Color(0xFFD6F0E2)),
    decline = AccentPair(Color(0xFFA33A2B), Color(0xFFFBE0DB)),
    statusOpen = Color(0xFF5A6B61),
    statusReview = Color(0xFF8A6A12),
    statusProgress = Color(0xFF1E5F8C),
    statusResolved = Color(0xFF1B6B4A),
    statusDeclined = Color(0xFF7A3E3E),
    categories = mapOf(
        IssueCategory.ROADS to AccentPair(Color(0xFF8A5A12), Color(0xFFF6E7CB)),
        IssueCategory.WATER to AccentPair(Color(0xFF13607A), Color(0xFFCFE9F2)),
        IssueCategory.ELECTRICITY to AccentPair(Color(0xFF7A5C00), Color(0xFFF7EBC2)),
        IssueCategory.SANITATION to AccentPair(Color(0xFF4A6B1B), Color(0xFFE2F0CD)),
        IssueCategory.HEALTH to AccentPair(Color(0xFFA32B50), Color(0xFFFADCE4)),
        IssueCategory.SCHOOLS to AccentPair(Color(0xFF4A3C8C), Color(0xFFE0DCF5)),
        IssueCategory.SAFETY to AccentPair(Color(0xFF8C3A1E), Color(0xFFFADFD3)),
        IssueCategory.OTHER to AccentPair(Color(0xFF4F6354), Color(0xFFDDE7E0))
    )
)

private val DarkAccents = AccentColors(
    support = AccentPair(Color(0xFF7FD1AB), Color(0xFF17402D)),
    decline = AccentPair(Color(0xFFF0A79A), Color(0xFF4A1C15)),
    statusOpen = Color(0xFFAEBBB3),
    statusReview = Color(0xFFE2BE6A),
    statusProgress = Color(0xFF9CCBF0),
    statusResolved = Color(0xFF7FD1AB),
    statusDeclined = Color(0xFFE3A6A6),
    categories = mapOf(
        IssueCategory.ROADS to AccentPair(Color(0xFFE2BE6A), Color(0xFF3D2C0A)),
        IssueCategory.WATER to AccentPair(Color(0xFF8CD2E8), Color(0xFF0A2E3A)),
        IssueCategory.ELECTRICITY to AccentPair(Color(0xFFEBD27A), Color(0xFF3A2C00)),
        IssueCategory.SANITATION to AccentPair(Color(0xFFBEDD92), Color(0xFF263A0C)),
        IssueCategory.HEALTH to AccentPair(Color(0xFFF0A0B8), Color(0xFF4D0F22)),
        IssueCategory.SCHOOLS to AccentPair(Color(0xFFB7AEEB), Color(0xFF231B4D)),
        IssueCategory.SAFETY to AccentPair(Color(0xFFF0AE91), Color(0xFF451806)),
        IssueCategory.OTHER to AccentPair(Color(0xFFB6CCBA), Color(0xFF26332B))
    )
)

private val LocalAccents = staticCompositionLocalOf { LightAccents }

/**
 * Makes the accents reachable from any composable as
 * `CommunityVoiceTheme.accents`, which is the same shape as
 * `MaterialTheme.colorScheme`. An object and a composable function are allowed
 * to share a name in Kotlin - this is exactly how MaterialTheme itself is built.
 */
object CommunityVoiceTheme {
    val accents: AccentColors
        @Composable get() = LocalAccents.current
}

/** The colours for a category pill, so the feed can be scanned by colour. */
@Composable
fun categoryColors(category: IssueCategory): AccentPair =
    CommunityVoiceTheme.accents.categories[category]
        ?: CommunityVoiceTheme.accents.categories.getValue(IssueCategory.OTHER)

/** The colour that stands for a given lifecycle stage. */
@Composable
fun statusColor(status: IssueStatus): Color {
    val accents = CommunityVoiceTheme.accents
    return when (status) {
        IssueStatus.OPEN -> accents.statusOpen
        IssueStatus.UNDER_REVIEW -> accents.statusReview
        IssueStatus.IN_PROGRESS -> accents.statusProgress
        IssueStatus.RESOLVED -> accents.statusResolved
        IssueStatus.DECLINED -> accents.statusDeclined
    }
}

// -------------------------------------------------------------------- type

private val AppTypography = Typography().let { base ->
    base.copy(
        // Slightly tighter and heavier than the Material defaults, which suits
        // short screen titles better than long-form reading.
        headlineMedium = base.headlineMedium.copy(
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-0.4).sp
        ),
        headlineSmall = base.headlineSmall.copy(
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-0.3).sp
        ),
        titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        titleSmall = base.titleSmall.copy(fontWeight = FontWeight.SemiBold),
        labelLarge = base.labelLarge.copy(fontWeight = FontWeight.SemiBold),
        // The big numbers on the stat tiles.
        displaySmall = TextStyle(
            fontSize = 30.sp,
            lineHeight = 34.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.6).sp
        )
    )
}

@Composable
fun CommunityVoiceTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalAccents provides if (darkTheme) DarkAccents else LightAccents
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            typography = AppTypography,
            content = content
        )
    }
}
