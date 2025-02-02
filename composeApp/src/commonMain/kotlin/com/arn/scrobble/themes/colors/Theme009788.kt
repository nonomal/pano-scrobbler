package com.arn.scrobble.themes.colors

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

object Theme009788 : ThemeVariants {
    override val name = this::class.simpleName!!

    override val light
        get() = lightColorScheme(
            primary = Color(0xFF006B60),
            onPrimary = Color(0xFFFFFFFF),
            primaryContainer = Color(0xFF9EF2E3),
            onPrimaryContainer = Color(0xFF00201C),
            secondary = Color(0xFF4A635E),
            onSecondary = Color(0xFFFFFFFF),
            secondaryContainer = Color(0xFFCCE8E2),
            onSecondaryContainer = Color(0xFF05201C),
            tertiary = Color(0xFF456179),
            onTertiary = Color(0xFFFFFFFF),
            tertiaryContainer = Color(0xFFCCE5FF),
            onTertiaryContainer = Color(0xFF001E31),
            error = Color(0xFFBA1A1A),
            onError = Color(0xFFFFFFFF),
            errorContainer = Color(0xFFFFDAD6),
            onErrorContainer = Color(0xFF410002),
            background = Color(0xFFF4FBF8),
            onBackground = Color(0xFF161D1B),
            surface = Color(0xFFF4FBF8),
            onSurface = Color(0xFF161D1B),
            surfaceVariant = Color(0xFFDAE5E1),
            onSurfaceVariant = Color(0xFF3F4946),
            outline = Color(0xFF6F7976),
            outlineVariant = Color(0xFFBEC9C5),
            scrim = Color(0xFF000000),
            inverseSurface = Color(0xFF2B3230),
            inverseOnSurface = Color(0xFFECF2EF),
            inversePrimary = Color(0xFF82D5C7),
            surfaceDim = Color(0xFFD5DBD9),
            surfaceBright = Color(0xFFF4FBF8),
            surfaceContainerLowest = Color(0xFFFFFFFF),
            surfaceContainerLow = Color(0xFFEFF5F2),
            surfaceContainer = Color(0xFFE9EFEC),
            surfaceContainerHigh = Color(0xFFE3EAE7),
            surfaceContainerHighest = Color(0xFFDDE4E1)
        )

    override val dark
        get() = darkColorScheme(
            primary = Color(0xFF82D5C7),
            onPrimary = Color(0xFF003731),
            primaryContainer = Color(0xFF005048),
            onPrimaryContainer = Color(0xFF9EF2E3),
            secondary = Color(0xFFB1CCC6),
            onSecondary = Color(0xFF1C3531),
            secondaryContainer = Color(0xFF334B47),
            onSecondaryContainer = Color(0xFFCCE8E2),
            tertiary = Color(0xFFADCAE5),
            onTertiary = Color(0xFF143349),
            tertiaryContainer = Color(0xFF2D4960),
            onTertiaryContainer = Color(0xFFCCE5FF),
            error = Color(0xFFFFB4AB),
            onError = Color(0xFF690005),
            errorContainer = Color(0xFF93000A),
            onErrorContainer = Color(0xFFFFDAD6),
            background = Color(0xFF0E1513),
            onBackground = Color(0xFFDDE4E1),
            surface = Color(0xFF0E1513),
            onSurface = Color(0xFFDDE4E1),
            surfaceVariant = Color(0xFF3F4946),
            onSurfaceVariant = Color(0xFFBEC9C5),
            outline = Color(0xFF899390),
            outlineVariant = Color(0xFF3F4946),
            scrim = Color(0xFF000000),
            inverseSurface = Color(0xFFDDE4E1),
            inverseOnSurface = Color(0xFF2B3230),
            inversePrimary = Color(0xFF006B60),
            surfaceDim = Color(0xFF0E1513),
            surfaceBright = Color(0xFF343B39),
            surfaceContainerLowest = Color(0xFF090F0E),
            surfaceContainerLow = Color(0xFF161D1B),
            surfaceContainer = Color(0xFF1A211F),
            surfaceContainerHigh = Color(0xFF252B2A),
            surfaceContainerHighest = Color(0xFF303634)
        )

    override val lightHighContrast
        get() = lightColorScheme(
            primary = Color(0xFF002823),
            onPrimary = Color(0xFFFFFFFF),
            primaryContainer = Color(0xFF004C44),
            onPrimaryContainer = Color(0xFFFFFFFF),
            secondary = Color(0xFF0D2623),
            onSecondary = Color(0xFFFFFFFF),
            secondaryContainer = Color(0xFF2F4743),
            onSecondaryContainer = Color(0xFFFFFFFF),
            tertiary = Color(0xFF02243A),
            onTertiary = Color(0xFFFFFFFF),
            tertiaryContainer = Color(0xFF29465C),
            onTertiaryContainer = Color(0xFFFFFFFF),
            error = Color(0xFF4E0002),
            onError = Color(0xFFFFFFFF),
            errorContainer = Color(0xFF8C0009),
            onErrorContainer = Color(0xFFFFFFFF),
            background = Color(0xFFF4FBF8),
            onBackground = Color(0xFF161D1B),
            surface = Color(0xFFF4FBF8),
            onSurface = Color(0xFF000000),
            surfaceVariant = Color(0xFFDAE5E1),
            onSurfaceVariant = Color(0xFF1C2624),
            outline = Color(0xFF3B4543),
            outlineVariant = Color(0xFF3B4543),
            scrim = Color(0xFF000000),
            inverseSurface = Color(0xFF2B3230),
            inverseOnSurface = Color(0xFFFFFFFF),
            inversePrimary = Color(0xFFA8FCED),
            surfaceDim = Color(0xFFD5DBD9),
            surfaceBright = Color(0xFFF4FBF8),
            surfaceContainerLowest = Color(0xFFFFFFFF),
            surfaceContainerLow = Color(0xFFEFF5F2),
            surfaceContainer = Color(0xFFE9EFEC),
            surfaceContainerHigh = Color(0xFFE3EAE7),
            surfaceContainerHighest = Color(0xFFDDE4E1)
        )

    override val darkHighContrast
        get() = darkColorScheme(
            primary = Color(0xFFEBFFFA),
            onPrimary = Color(0xFF000000),
            primaryContainer = Color(0xFF86DACC),
            onPrimaryContainer = Color(0xFF000000),
            secondary = Color(0xFFEBFFFA),
            onSecondary = Color(0xFF000000),
            secondaryContainer = Color(0xFFB5D0CA),
            onSecondaryContainer = Color(0xFF000000),
            tertiary = Color(0xFFF9FBFF),
            onTertiary = Color(0xFF000000),
            tertiaryContainer = Color(0xFFB1CEEA),
            onTertiaryContainer = Color(0xFF000000),
            error = Color(0xFFFFF9F9),
            onError = Color(0xFF000000),
            errorContainer = Color(0xFFFFBAB1),
            onErrorContainer = Color(0xFF000000),
            background = Color(0xFF0E1513),
            onBackground = Color(0xFFDDE4E1),
            surface = Color(0xFF0E1513),
            onSurface = Color(0xFFFFFFFF),
            surfaceVariant = Color(0xFF3F4946),
            onSurfaceVariant = Color(0xFFF3FDF9),
            outline = Color(0xFFC3CDCA),
            outlineVariant = Color(0xFFC3CDCA),
            scrim = Color(0xFF000000),
            inverseSurface = Color(0xFFDDE4E1),
            inverseOnSurface = Color(0xFF000000),
            inversePrimary = Color(0xFF00302B),
            surfaceDim = Color(0xFF0E1513),
            surfaceBright = Color(0xFF343B39),
            surfaceContainerLowest = Color(0xFF090F0E),
            surfaceContainerLow = Color(0xFF161D1B),
            surfaceContainer = Color(0xFF1A211F),
            surfaceContainerHigh = Color(0xFF252B2A),
            surfaceContainerHighest = Color(0xFF303634)
        )

    override val lightMediumContrast
        get() = lightColorScheme(
            primary = Color(0xFF004C44),
            onPrimary = Color(0xFFFFFFFF),
            primaryContainer = Color(0xFF298176),
            onPrimaryContainer = Color(0xFFFFFFFF),
            secondary = Color(0xFF2F4743),
            onSecondary = Color(0xFFFFFFFF),
            secondaryContainer = Color(0xFF607A74),
            onSecondaryContainer = Color(0xFFFFFFFF),
            tertiary = Color(0xFF29465C),
            onTertiary = Color(0xFFFFFFFF),
            tertiaryContainer = Color(0xFF5B7890),
            onTertiaryContainer = Color(0xFFFFFFFF),
            error = Color(0xFF8C0009),
            onError = Color(0xFFFFFFFF),
            errorContainer = Color(0xFFDA342E),
            onErrorContainer = Color(0xFFFFFFFF),
            background = Color(0xFFF4FBF8),
            onBackground = Color(0xFF161D1B),
            surface = Color(0xFFF4FBF8),
            onSurface = Color(0xFF161D1B),
            surfaceVariant = Color(0xFFDAE5E1),
            onSurfaceVariant = Color(0xFF3B4543),
            outline = Color(0xFF57615F),
            outlineVariant = Color(0xFF737D7A),
            scrim = Color(0xFF000000),
            inverseSurface = Color(0xFF2B3230),
            inverseOnSurface = Color(0xFFECF2EF),
            inversePrimary = Color(0xFF82D5C7),
            surfaceDim = Color(0xFFD5DBD9),
            surfaceBright = Color(0xFFF4FBF8),
            surfaceContainerLowest = Color(0xFFFFFFFF),
            surfaceContainerLow = Color(0xFFEFF5F2),
            surfaceContainer = Color(0xFFE9EFEC),
            surfaceContainerHigh = Color(0xFFE3EAE7),
            surfaceContainerHighest = Color(0xFFDDE4E1)
        )

    override val darkMediumContrast
        get() = darkColorScheme(
            primary = Color(0xFF86DACC),
            onPrimary = Color(0xFF001A17),
            primaryContainer = Color(0xFF4A9E92),
            onPrimaryContainer = Color(0xFF000000),
            secondary = Color(0xFFB5D0CA),
            onSecondary = Color(0xFF011A17),
            secondaryContainer = Color(0xFF7C9690),
            onSecondaryContainer = Color(0xFF000000),
            tertiary = Color(0xFFB1CEEA),
            onTertiary = Color(0xFF001829),
            tertiaryContainer = Color(0xFF7794AE),
            onTertiaryContainer = Color(0xFF000000),
            error = Color(0xFFFFBAB1),
            onError = Color(0xFF370001),
            errorContainer = Color(0xFFFF5449),
            onErrorContainer = Color(0xFF000000),
            background = Color(0xFF0E1513),
            onBackground = Color(0xFFDDE4E1),
            surface = Color(0xFF0E1513),
            onSurface = Color(0xFFF6FCF9),
            surfaceVariant = Color(0xFF3F4946),
            onSurfaceVariant = Color(0xFFC3CDCA),
            outline = Color(0xFF9BA5A2),
            outlineVariant = Color(0xFF7B8582),
            scrim = Color(0xFF000000),
            inverseSurface = Color(0xFFDDE4E1),
            inverseOnSurface = Color(0xFF252B2A),
            inversePrimary = Color(0xFF005249),
            surfaceDim = Color(0xFF0E1513),
            surfaceBright = Color(0xFF343B39),
            surfaceContainerLowest = Color(0xFF090F0E),
            surfaceContainerLow = Color(0xFF161D1B),
            surfaceContainer = Color(0xFF1A211F),
            surfaceContainerHigh = Color(0xFF252B2A),
            surfaceContainerHighest = Color(0xFF303634)
        )
}