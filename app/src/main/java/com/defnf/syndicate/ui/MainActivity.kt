package com.defnf.syndicate.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.defnf.syndicate.data.models.ThemeMode
import com.defnf.syndicate.navigation.RssNavigation
import com.defnf.syndicate.notifications.NotificationIntents
import com.defnf.syndicate.ui.theme.SyndicateTheme
import com.defnf.syndicate.ui.viewmodel.ThemeViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    // Notification deep link waiting to be handled by the UI
    private var pendingNotification by mutableStateOf<NotificationData?>(null)
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Only read the launch intent on a fresh start; after a configuration change or process
        // restore the navigation state is restored instead of re-opening the notification target.
        // Relaunching from Recents redelivers the original intent, which must not reopen it either.
        val launchedFromHistory = (intent.flags and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY) != 0
        if (savedInstanceState == null && !launchedFromHistory) {
            pendingNotification = NotificationIntents.parse(intent)
        }
        
        enableEdgeToEdge()
        setContent {
            val themeViewModel: ThemeViewModel = hiltViewModel()
            val themeMode by themeViewModel.themeMode.collectAsState()
            val isSystemInDarkTheme = isSystemInDarkTheme()
            
            val isDarkTheme = when (themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> isSystemInDarkTheme
            }
            
            SyndicateTheme(darkTheme = isDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    RssNavigation(
                        themeViewModel = themeViewModel,
                        notificationData = pendingNotification,
                        onNotificationHandled = { pendingNotification = null }
                    )
                }
            }
        }
    }
    
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        NotificationIntents.parse(intent)?.let { pendingNotification = it }
    }
}

sealed class NotificationData {
    data class Article(val articleId: String, val feedId: Long) : NotificationData()
    data class Feed(val feedId: Long) : NotificationData()
    data class Group(val groupId: Long) : NotificationData()
}