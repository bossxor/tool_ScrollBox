package com.bossxor.scrollbox.ui.settings

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.bossxor.scrollbox.ScrollBoxApp
import com.bossxor.scrollbox.data.Backup
import com.bossxor.scrollbox.data.Prefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val app = ScrollBoxApp.instance
    val scope = rememberCoroutineScope()

    var lockEnabled by remember { mutableStateOf(false) }
    var pin by remember { mutableStateOf("") }
    var pattern by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf(false) }
    var tzTop by remember { mutableFloatStateOf(0.3f) }
    var tzBottom by remember { mutableFloatStateOf(0.3f) }
    var themeMode by remember { mutableStateOf("system") }

    LaunchedEffect(Unit) {
        lockEnabled = app.prefs.get(Prefs.Keys.LOCK_ENABLED, false)
        pin = app.prefs.get(Prefs.Keys.LOCK_PIN, "")
        pattern = app.prefs.get(Prefs.Keys.LOCK_PATTERN, "")
        bio = app.prefs.get(Prefs.Keys.BIOMETRIC, false)
        tzTop = app.prefs.get(Prefs.Keys.TOUCH_ZONE_TOP, 0.3f)
        tzBottom = app.prefs.get(Prefs.Keys.TOUCH_ZONE_BOTTOM, 0.3f)
        themeMode = app.prefs.get(Prefs.Keys.THEME_MODE, "system")
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val json = Backup.exportSuspend(app.db, app.prefs)
            withContext(Dispatchers.IO) {
                ctx.contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray()) }
            }
            Toast.makeText(ctx, "백업 완료", Toast.LENGTH_SHORT).show()
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val json = withContext(Dispatchers.IO) {
                ctx.contentResolver.openInputStream(uri)?.use { ins ->
                    BufferedReader(InputStreamReader(ins)).readText()
                } ?: ""
            }
            if (json.isNotBlank()) {
                Backup.importSuspend(app.db, app.prefs, json)
                Toast.makeText(ctx, "복원 완료", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("설정") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { pad ->
        Column(
            Modifier
                .padding(pad)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SettingsSection(title = "화면") {
                Column(Modifier.selectableGroup()) {
                    listOf(
                        "system" to "시스템",
                        "light" to "라이트",
                        "dark" to "다크"
                    ).forEach { (value, label) ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = themeMode == value,
                                    onClick = {
                                        themeMode = value
                                        scope.launch { app.prefs.set(Prefs.Keys.THEME_MODE, value) }
                                    },
                                    role = Role.RadioButton
                                )
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = themeMode == value,
                                onClick = null
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(label, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }

            SettingsSection(title = "앱 잠금") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("잠금 사용", Modifier.weight(1f))
                    Switch(checked = lockEnabled, onCheckedChange = {
                        lockEnabled = it
                        scope.launch { app.prefs.set(Prefs.Keys.LOCK_ENABLED, it) }
                    })
                }
                OutlinedTextField(
                    value = pin,
                    onValueChange = { pin = it.filter { c -> c.isDigit() }.take(8) },
                    label = { Text("PIN (숫자)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium
                )
                OutlinedTextField(
                    value = pattern,
                    onValueChange = { pattern = it },
                    label = { Text("패턴 (예: 1-2-3-6-9)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium
                )
                Button(onClick = {
                    scope.launch {
                        app.prefs.set(Prefs.Keys.LOCK_PIN, pin)
                        app.prefs.set(Prefs.Keys.LOCK_PATTERN, pattern)
                        Toast.makeText(ctx, "잠금 정보 저장", Toast.LENGTH_SHORT).show()
                    }
                }) { Text("PIN/패턴 저장") }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("생체 인증 (PIN/패턴 후)", Modifier.weight(1f))
                    Switch(checked = bio, onCheckedChange = {
                        bio = it
                        scope.launch { app.prefs.set(Prefs.Keys.BIOMETRIC, it) }
                    })
                }
            }

            SettingsSection(title = "뷰어 터치 영역") {
                Text("상단(이전): ${(tzTop * 100).toInt()}%")
                Slider(value = tzTop, onValueChange = {
                    tzTop = it
                    scope.launch { app.prefs.set(Prefs.Keys.TOUCH_ZONE_TOP, it) }
                }, valueRange = 0.1f..0.45f)
                Text("하단(다음): ${(tzBottom * 100).toInt()}%")
                Slider(value = tzBottom, onValueChange = {
                    tzBottom = it
                    scope.launch { app.prefs.set(Prefs.Keys.TOUCH_ZONE_BOTTOM, it) }
                }, valueRange = 0.1f..0.45f)
            }

            SettingsSection(title = "백업 / 복원") {
                Button(onClick = { exportLauncher.launch("scrollbox-backup.json") }) { Text("JSON 백업") }
                Button(onClick = { importLauncher.launch(arrayOf("application/json", "*/*")) }) { Text("JSON 복원") }
            }

            Text(
                "ScrollBox 1.0.1",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.large,
        tonalElevation = 1.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}
