package com.bossxor.scrollbox.ui.settings

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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

    LaunchedEffect(Unit) {
        lockEnabled = app.prefs.get(Prefs.Keys.LOCK_ENABLED, false)
        pin = app.prefs.get(Prefs.Keys.LOCK_PIN, "")
        pattern = app.prefs.get(Prefs.Keys.LOCK_PATTERN, "")
        bio = app.prefs.get(Prefs.Keys.BIOMETRIC, false)
        tzTop = app.prefs.get(Prefs.Keys.TOUCH_ZONE_TOP, 0.3f)
        tzBottom = app.prefs.get(Prefs.Keys.TOUCH_ZONE_BOTTOM, 0.3f)
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
                }
            )
        }
    ) { pad ->
        Column(
            Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("앱 잠금", style = MaterialTheme.typography.titleMedium)
            Row {
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
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = pattern,
                onValueChange = { pattern = it },
                label = { Text("패턴 (예: 1-2-3-6-9)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Button(onClick = {
                scope.launch {
                    app.prefs.set(Prefs.Keys.LOCK_PIN, pin)
                    app.prefs.set(Prefs.Keys.LOCK_PATTERN, pattern)
                    Toast.makeText(ctx, "잠금 정보 저장", Toast.LENGTH_SHORT).show()
                }
            }) { Text("PIN/패턴 저장") }
            Row {
                Text("생체 인증 (PIN/패턴 후)", Modifier.weight(1f))
                Switch(checked = bio, onCheckedChange = {
                    bio = it
                    scope.launch { app.prefs.set(Prefs.Keys.BIOMETRIC, it) }
                })
            }

            HorizontalDivider()
            Text("뷰어 터치 영역", style = MaterialTheme.typography.titleMedium)
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

            HorizontalDivider()
            Text("테마", style = MaterialTheme.typography.titleMedium)
            Text("다크 모드는 시스템 설정을 따릅니다.")

            HorizontalDivider()
            Text("백업 / 복원", style = MaterialTheme.typography.titleMedium)
            Button(onClick = { exportLauncher.launch("scrollbox-backup.json") }) { Text("JSON 백업") }
            Button(onClick = { importLauncher.launch(arrayOf("application/json", "*/*")) }) { Text("JSON 복원") }

            HorizontalDivider()
            Text("ScrollBox 1.0.0", style = MaterialTheme.typography.bodySmall)
        }
    }
}
