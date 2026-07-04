package com.example

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
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

    // Create Notification Channel on startup
    createNotificationChannel()

    // Request runtime notification permission for Android 13+
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
        requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101)
      }
    }

    // Process notification extras if the app was launched by tapping a notification
    handleNotificationIntent(intent)

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

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    handleNotificationIntent(intent)
  }

  private fun handleNotificationIntent(intent: Intent?) {
    intent?.extras?.let { extras ->
      // If started by a Firebase notification click
      if (extras.containsKey("google.message_id") || extras.containsKey("gcm.notification.title") || extras.containsKey("title")) {
        val title = extras.getString("gcm.notification.title") 
          ?: extras.getString("title") 
          ?: "New Book Notification"
        val body = extras.getString("gcm.notification.body") 
          ?: extras.getString("body") 
          ?: extras.getString("message") 
          ?: "Check out our latest medical library announcements!"
        val category = extras.getString("category") ?: "Updates"

        Log.d("MainActivity", "Processing tapped notification: Title='$title', Category='$category'")
        bookViewModel.addNotification(title, body, category)

        // Clear keys to avoid duplicate insertions on screen orientation changes
        intent.removeExtra("google.message_id")
        intent.removeExtra("gcm.notification.title")
        intent.removeExtra("title")
        intent.removeExtra("gcm.notification.body")
        intent.removeExtra("body")
        intent.removeExtra("message")
        intent.removeExtra("category")
      }
    }
  }

  private fun createNotificationChannel() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val channelId = "logicmed_notifications_channel"
      val name = "Logic Med Library Notifications"
      val descriptionText = "Channel for Logic Med Library book updates and announcements"
      val importance = android.app.NotificationManager.IMPORTANCE_DEFAULT
      val channel = android.app.NotificationChannel(channelId, name, importance).apply {
        description = descriptionText
      }
      val notificationManager = getSystemService(android.content.Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
      notificationManager.createNotificationChannel(channel)
    }
  }
}
