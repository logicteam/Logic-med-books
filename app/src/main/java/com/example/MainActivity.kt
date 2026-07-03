package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import java.util.Locale
import com.example.ui.BookViewModel
import com.example.ui.screens.BookAppContent
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

  private val bookViewModel: BookViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      val isDarkThemeOverride by bookViewModel.isDarkThemeOverride.collectAsState()
      val darkTheme = isDarkThemeOverride ?: isSystemInDarkTheme()

      val languageCode by bookViewModel.languageCode.collectAsState()
      val selectedThemeStyle by bookViewModel.selectedThemeStyle.collectAsState()

      // Dynamically apply selected app locale
      val locale = Locale(languageCode)
      Locale.setDefault(locale)
      val resources = this.resources
      val config = resources.configuration
      config.setLocale(locale)
      // Standard Android configuration update for local context
      resources.updateConfiguration(config, resources.displayMetrics)

      val layoutDirection = if (languageCode == "ku") LayoutDirection.Rtl else LayoutDirection.Ltr

      CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        MyApplicationTheme(darkTheme = darkTheme, themeStyle = selectedThemeStyle) {
          BookAppContent(viewModel = bookViewModel)
        }
      }
    }
  }
}
