package com.defnf.syndicate.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.defnf.syndicate.data.models.ThemeMode
import com.defnf.syndicate.navigation.RssNavigation
import com.defnf.syndicate.ui.theme.SyndicateTheme
import com.defnf.syndicate.ui.viewmodel.ThemeViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
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
            
            // Handle notification intents
            var notificationData by remember { mutableStateOf<NotificationData?>(null) }
            
            LaunchedEffect(intent) {
                notificationData = extractNotificationData(intent)
            }
            
            SyndicateTheme(darkTheme = isDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    RssNavigation(
                        themeViewModel = themeViewModel,
                        notificationData = notificationData,
                        onNotificationHandled = { notificationData = null }
                    )
                }
            }
        }
    }
    
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }
    
    private fun extractNotificationData(intent: Intent): NotificationData? {
        return when {
            intent.hasExtra("articleId") && intent.hasExtra("feedId") -> {
                NotificationData.Article(
                    articleId = intent.getStringExtra("articleId")!!,
                    feedId = intent.getLongExtra("feedId", -1)
                )
            }
            intent.hasExtra("groupId") -> {
                NotificationData.Group(
                    groupId = intent.getLongExtra("groupId", -1)
                )
            }
            else -> null
        }
    }
}

sealed class NotificationData {
    data class Article(val articleId: String, val feedId: Long) : NotificationData()
    data class Group(val groupId: Long) : NotificationData()
}