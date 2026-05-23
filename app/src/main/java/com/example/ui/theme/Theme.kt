package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = EyeSafePrimaryDark,
    onPrimary = EyeSafeOnPrimaryDark,
    primaryContainer = EyeSafePrimaryContainerDark,
    onPrimaryContainer = EyeSafeOnPrimaryContainerDark,
    background = EyeSafeBackgroundDark,
    onBackground = EyeSafeOnBackgroundDark,
    surface = EyeSafeSurfaceDark,
    onSurface = EyeSafeOnSurfaceDark,
    surfaceVariant = EyeSafeSurfaceVariantDark,
    onSurfaceVariant = EyeSafeOnSurfaceVariantDark
)

private val LightColorScheme = lightColorScheme(
    primary = EyeSafePrimaryLight,
    onPrimary = EyeSafeOnPrimaryLight,
    primaryContainer = EyeSafePrimaryContainerLight,
    onPrimaryContainer = EyeSafeOnPrimaryContainerLight,
    background = EyeSafeBackgroundLight,
    onBackground = EyeSafeOnBackgroundLight,
    surface = EyeSafeSurfaceLight,
    onSurface = EyeSafeOnSurfaceLight,
    surfaceVariant = EyeSafeSurfaceVariantLight,
    onSurfaceVariant = EyeSafeOnSurfaceVariantLight
)

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Dynamic color is available on Android 12+
  dynamicColor: Boolean = true,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
