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
  primary = DarkPrimary,
  onPrimary = DarkOnPrimary,
  primaryContainer = DarkPrimaryContainer,
  onPrimaryContainer = DarkOnPrimaryContainer,
  secondary = DarkSecondary,
  onSecondary = DarkOnSecondary,
  secondaryContainer = DarkSecondaryContainer,
  onSecondaryContainer = DarkOnSecondaryContainer,
  background = DarkBackground,
  onBackground = DarkOnBackground,
  surface = DarkSurface,
  onSurface = DarkOnSurface,
  surfaceVariant = DarkSurfaceVariant,
  onSurfaceVariant = DarkOnSurfaceVariant,
  outline = DarkOutline,
  outlineVariant = DarkOutlineVariant
)

private val LightColorScheme = lightColorScheme(
  primary = LightPrimary,
  onPrimary = LightOnPrimary,
  primaryContainer = LightPrimaryContainer,
  onPrimaryContainer = LightOnPrimaryContainer,
  secondary = LightSecondary,
  onSecondary = LightOnSecondary,
  secondaryContainer = LightSecondaryContainer,
  onSecondaryContainer = LightOnSecondaryContainer,
  background = LightBackground,
  onBackground = LightOnBackground,
  surface = LightSurface,
  onSurface = LightOnSurface,
  surfaceVariant = LightSurfaceVariant,
  onSurfaceVariant = LightOnSurfaceVariant,
  outline = LightOutline,
  outlineVariant = LightOutlineVariant
)

private val DarkBlueColorScheme = darkColorScheme(
  primary = DarkBluePrimary,
  onPrimary = DarkBlueOnPrimary,
  primaryContainer = DarkBluePrimaryContainer,
  onPrimaryContainer = DarkBlueOnPrimaryContainer,
  secondary = DarkBlueSecondary,
  onSecondary = DarkBlueOnSecondary,
  secondaryContainer = DarkBlueSecondaryContainer,
  onSecondaryContainer = DarkBlueOnSecondaryContainer,
  background = DarkBackground,
  onBackground = DarkOnBackground,
  surface = DarkSurface,
  onSurface = DarkOnSurface,
  surfaceVariant = DarkSurfaceVariant,
  onSurfaceVariant = DarkOnSurfaceVariant,
  outline = DarkOutline,
  outlineVariant = DarkOutlineVariant
)

private val LightBlueColorScheme = lightColorScheme(
  primary = LightBluePrimary,
  onPrimary = LightBlueOnPrimary,
  primaryContainer = LightBluePrimaryContainer,
  onPrimaryContainer = LightBlueOnPrimaryContainer,
  secondary = LightBlueSecondary,
  onSecondary = LightBlueOnSecondary,
  secondaryContainer = LightBlueSecondaryContainer,
  onSecondaryContainer = LightBlueOnSecondaryContainer,
  background = LightBackground,
  onBackground = LightOnBackground,
  surface = LightSurface,
  onSurface = LightOnSurface,
  surfaceVariant = LightBlueSecondaryContainer,
  onSurfaceVariant = LightOnSurfaceVariant,
  outline = LightOutline,
  outlineVariant = LightOutlineVariant
)

private val DarkGreenColorScheme = darkColorScheme(
  primary = DarkGreenPrimary,
  onPrimary = DarkGreenOnPrimary,
  primaryContainer = DarkGreenPrimaryContainer,
  onPrimaryContainer = DarkGreenOnPrimaryContainer,
  secondary = DarkGreenSecondary,
  onSecondary = DarkGreenOnSecondary,
  secondaryContainer = DarkGreenSecondaryContainer,
  onSecondaryContainer = DarkGreenOnSecondaryContainer,
  background = DarkBackground,
  onBackground = DarkOnBackground,
  surface = DarkSurface,
  onSurface = DarkOnSurface,
  surfaceVariant = DarkSurfaceVariant,
  onSurfaceVariant = DarkOnSurfaceVariant,
  outline = DarkOutline,
  outlineVariant = DarkOutlineVariant
)

private val LightGreenColorScheme = lightColorScheme(
  primary = LightGreenPrimary,
  onPrimary = LightGreenOnPrimary,
  primaryContainer = LightGreenPrimaryContainer,
  onPrimaryContainer = LightGreenOnPrimaryContainer,
  secondary = LightGreenSecondary,
  onSecondary = LightGreenOnSecondary,
  secondaryContainer = LightGreenSecondaryContainer,
  onSecondaryContainer = LightGreenOnSecondaryContainer,
  background = LightBackground,
  onBackground = LightOnBackground,
  surface = LightSurface,
  onSurface = LightOnSurface,
  surfaceVariant = LightGreenSecondaryContainer,
  onSurfaceVariant = LightOnSurfaceVariant,
  outline = LightOutline,
  outlineVariant = LightOutlineVariant
)

private val DarkOrangeColorScheme = darkColorScheme(
  primary = DarkOrangePrimary,
  onPrimary = DarkOrangeOnPrimary,
  primaryContainer = DarkOrangePrimaryContainer,
  onPrimaryContainer = DarkOrangeOnPrimaryContainer,
  secondary = DarkOrangeSecondary,
  onSecondary = DarkOrangeOnSecondary,
  secondaryContainer = DarkOrangeSecondaryContainer,
  onSecondaryContainer = DarkOrangeOnSecondaryContainer,
  background = DarkBackground,
  onBackground = DarkOnBackground,
  surface = DarkSurface,
  onSurface = DarkOnSurface,
  surfaceVariant = DarkSurfaceVariant,
  onSurfaceVariant = DarkOnSurfaceVariant,
  outline = DarkOutline,
  outlineVariant = DarkOutlineVariant
)

private val LightOrangeColorScheme = lightColorScheme(
  primary = LightOrangePrimary,
  onPrimary = LightOrangeOnPrimary,
  primaryContainer = LightOrangePrimaryContainer,
  onPrimaryContainer = LightOrangeOnPrimaryContainer,
  secondary = LightOrangeSecondary,
  onSecondary = LightOrangeOnSecondary,
  secondaryContainer = LightOrangeSecondaryContainer,
  onSecondaryContainer = LightOrangeOnSecondaryContainer,
  background = LightBackground,
  onBackground = LightOnBackground,
  surface = LightSurface,
  onSurface = LightOnSurface,
  surfaceVariant = LightOrangeSecondaryContainer,
  onSurfaceVariant = LightOnSurfaceVariant,
  outline = LightOutline,
  outlineVariant = LightOutlineVariant
)

private val DarkRedColorScheme = darkColorScheme(
  primary = DarkRedPrimary,
  onPrimary = DarkRedOnPrimary,
  primaryContainer = DarkRedPrimaryContainer,
  onPrimaryContainer = DarkRedOnPrimaryContainer,
  secondary = DarkRedSecondary,
  onSecondary = DarkRedOnSecondary,
  secondaryContainer = DarkRedSecondaryContainer,
  onSecondaryContainer = DarkRedOnSecondaryContainer,
  background = DarkBackground,
  onBackground = DarkOnBackground,
  surface = DarkSurface,
  onSurface = DarkOnSurface,
  surfaceVariant = DarkSurfaceVariant,
  onSurfaceVariant = DarkOnSurfaceVariant,
  outline = DarkOutline,
  outlineVariant = DarkOutlineVariant
)

private val LightRedColorScheme = lightColorScheme(
  primary = LightRedPrimary,
  onPrimary = LightRedOnPrimary,
  primaryContainer = LightRedPrimaryContainer,
  onPrimaryContainer = LightRedOnPrimaryContainer,
  secondary = LightRedSecondary,
  onSecondary = LightRedOnSecondary,
  secondaryContainer = LightRedSecondaryContainer,
  onSecondaryContainer = LightRedOnSecondaryContainer,
  background = LightBackground,
  onBackground = LightOnBackground,
  surface = LightSurface,
  onSurface = LightOnSurface,
  surfaceVariant = LightRedSecondaryContainer,
  onSurfaceVariant = LightOnSurfaceVariant,
  outline = LightOutline,
  outlineVariant = LightOutlineVariant
)

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  themeStyle: String = "purple",
  dynamicColor: Boolean = false, // Set to false to prioritize our Geometric Balance styling
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> {
        when (themeStyle) {
          "blue" -> DarkBlueColorScheme
          "green" -> DarkGreenColorScheme
          "orange" -> DarkOrangeColorScheme
          "red" -> DarkRedColorScheme
          else -> DarkColorScheme
        }
      }
      else -> {
        when (themeStyle) {
          "blue" -> LightBlueColorScheme
          "green" -> LightGreenColorScheme
          "orange" -> LightOrangeColorScheme
          "red" -> LightRedColorScheme
          else -> LightColorScheme
        }
      }
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
