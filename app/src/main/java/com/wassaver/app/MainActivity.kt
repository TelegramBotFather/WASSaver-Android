package com.wassaver.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wassaver.app.data.model.StatusFile
import com.wassaver.app.ui.screens.*
import com.wassaver.app.ui.theme.WASSaverTheme
import com.wassaver.app.viewmodel.StatusViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WASSaverTheme {
                MainApp()
            }
        }
    }
}

@Composable
fun MainApp() {
    val viewModel: StatusViewModel = viewModel()

    // Navigation state
    var currentScreen by remember { mutableStateOf<MenuDestination?>(null) }

    // Status viewer state
    var viewerStatuses by remember { mutableStateOf<List<StatusFile>?>(null) }
    var viewerInitialIndex by remember { mutableIntStateOf(0) }

    // Back handler for sub-screens
    BackHandler(enabled = viewerStatuses != null || currentScreen != null) {
        when {
            viewerStatuses != null -> viewerStatuses = null
            currentScreen != null -> currentScreen = null
        }
    }

    when {
        // Full-screen status viewer (highest priority)
        viewerStatuses != null -> {
            StatusViewerScreen(
                statusFiles = viewerStatuses!!,
                initialIndex = viewerInitialIndex,
                viewModel = viewModel,
                onBack = { viewerStatuses = null }
            )
        }

        // Sub-screens navigated from menu
        currentScreen != null -> {
            when (currentScreen) {
                MenuDestination.STATUS_VIEWER -> HomeScreen(
                    viewModel = viewModel,
                    onStatusClick = { _, index, statuses ->
                        viewerStatuses = statuses
                        viewerInitialIndex = index
                    },
                    onBack = { currentScreen = null }
                )
                MenuDestination.MEDIA_BROWSER -> ViewOnceScreen(
                    viewModel = viewModel,
                    onStatusClick = { _, index, statuses ->
                        viewerStatuses = statuses
                        viewerInitialIndex = index
                    },
                    onBack = { currentScreen = null }
                )
                MenuDestination.SAVED_STATUSES -> SavedScreen(
                    viewModel = viewModel,
                    onStatusClick = { _, index, statuses ->
                        viewerStatuses = statuses
                        viewerInitialIndex = index
                    },
                    onBack = { currentScreen = null }
                )
                MenuDestination.DELETED_MESSAGES -> DeletedMessagesScreen(
                    onBack = { currentScreen = null }
                )
                MenuDestination.STATUS_SPLITTER -> StatusSplitterScreen(
                    onBack = { currentScreen = null }
                )
                MenuDestination.DIRECT_CHAT -> DirectChatScreen(
                    onBack = { currentScreen = null }
                )
                MenuDestination.UPDATE -> UpdateScreen(
                    onBack = { currentScreen = null }
                )
                MenuDestination.ABOUT -> AboutScreen(
                    onBack = { currentScreen = null }
                )
                else -> { /* exhaustive */ }
            }
        }

        // Home menu dashboard
        else -> {
            MenuScreen(
                onNavigate = { destination ->
                    currentScreen = destination
                }
            )
        }
    }
}
