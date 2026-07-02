package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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

      MyApplicationTheme(darkTheme = darkTheme) {
        BookAppContent(viewModel = bookViewModel)
      }
    }
  }
}
