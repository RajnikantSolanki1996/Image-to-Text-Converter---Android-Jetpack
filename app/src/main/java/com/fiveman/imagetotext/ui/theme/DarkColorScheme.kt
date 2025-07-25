package com.fiveman.imagetotext.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80,
    background = BackgroundDark,
    surface = SurfaceDark,
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onTertiary = Color.Black,
    onBackground = OnSurfaceDark,
    onSurface = OnSurfaceDark,
    error = ErrorDark,
    onError = OnErrorDark
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE3F2FD),
    onPrimaryContainer = Color(0xFF0D47A1),
    
    secondary = SecondaryGreen,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE8F5E8),
    onSecondaryContainer = Color(0xFF1B5E20),
    
    tertiary = TertiaryOrange,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFF3E0),
    onTertiaryContainer = Color(0xFFE65100),
    
    background = BackgroundLight,
    onBackground = OnSurfaceLight,
    
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = Color(0xFFF5F5F5),
    onSurfaceVariant = Color(0xFF49454F),
    
    outline = Color(0xFF79747E),
    outlineVariant = Color(0xFFCAC4D0),
    
    error = ErrorLight,
    onError = OnErrorLight,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    
    inverseSurface = Color(0xFF313033),
    inverseOnSurface = Color(0xFFF4EFF4),
    inversePrimary = Color(0xFFBB86FC),
    
    scrim = Color(0xFF000000),
    surfaceTint = PrimaryBlue
)

@Composable
fun ImageToTextTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

