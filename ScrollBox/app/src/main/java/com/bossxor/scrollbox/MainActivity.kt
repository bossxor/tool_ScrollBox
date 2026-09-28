package com.bossxor.scrollbox

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.KeyEvent
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.bossxor.scrollbox.data.Prefs
import com.bossxor.scrollbox.ui.ScrollBoxTheme
import com.bossxor.scrollbox.ui.browser.HomeScreen
import com.bossxor.scrollbox.ui.lock.LockScreen
import com.bossxor.scrollbox.ui.settings.SettingsScreen
import com.bossxor.scrollbox.ui.viewer.ViewerScreen
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class MainActivity : FragmentActivity() {
    var volumeCallback: ((Boolean) -> Boolean)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val openPath = intentPath(intent)
        setContent {
            val app = ScrollBoxApp.instance
            val themeMode by app.prefs.themeMode.collectAsState(initial = "system")

            ScrollBoxTheme(themeMode = themeMode) {
                val nav = rememberNavController()
                var unlocked by remember { mutableStateOf(false) }
                var lockChecked by remember { mutableStateOf(false) }
                var lockOn by remember { mutableStateOf(false) }

                LaunchedEffect(Unit) {
                    lockOn = app.prefs.get(Prefs.Keys.LOCK_ENABLED, false)
                    lockChecked = true
                    unlocked = !lockOn
                }

                if (!lockChecked) return@ScrollBoxTheme

                if (lockOn && !unlocked) {
                    LockScreen(onUnlocked = { unlocked = true })
                    return@ScrollBoxTheme
                }

                LaunchedEffect(openPath) {
                    if (openPath != null) {
                        nav.navigate("viewer/${enc(openPath)}") { launchSingleTop = true }
                    }
                }

                NavHost(navController = nav, startDestination = "home") {
                    composable("home") {
                        HomeScreen(
                            onOpenFile = { path -> nav.navigate("viewer/${enc(path)}") },
                            onSettings = { nav.navigate("settings") }
                        )
                    }
                    composable(
                        "viewer/{path}",
                        arguments = listOf(navArgument("path") { type = NavType.StringType })
                    ) { entry ->
                        val path = dec(entry.arguments?.getString("path") ?: "")
                        ViewerScreen(
                            path = path,
                            onBack = { nav.popBackStack() },
                            onOpenPath = { p -> nav.navigate("viewer/${enc(p)}") { popUpTo("home") } },
                            registerVolume = { cb -> volumeCallback = cb }
                        )
                    }
                    composable("settings") {
                        SettingsScreen(onBack = { nav.popBackStack() })
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            val handled = volumeCallback?.invoke(keyCode == KeyEvent.KEYCODE_VOLUME_UP)
            if (handled == true) return true
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun intentPath(intent: Intent?): String? {
        if (intent == null) return null
        val uri: Uri? = intent.data
        if (uri != null) {
            if (uri.scheme == "file") return uri.path
            return "content:${uri}"
        }
        return intent.getStringExtra(Intent.EXTRA_TEXT)
    }

    companion object {
        fun enc(s: String) = URLEncoder.encode(s, StandardCharsets.UTF_8.toString())
        fun dec(s: String) = URLDecoder.decode(s, StandardCharsets.UTF_8.toString())
    }
}
