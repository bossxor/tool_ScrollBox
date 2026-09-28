package com.bossxor.scrollbox.ui.lock

import android.widget.Toast
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.bossxor.scrollbox.ScrollBoxApp
import com.bossxor.scrollbox.data.Prefs
import kotlinx.coroutines.launch

@Composable
fun LockScreen(onUnlocked: () -> Unit) {
    val ctx = LocalContext.current
    val app = ScrollBoxApp.instance
    val scope = rememberCoroutineScope()
    var pin by remember { mutableStateOf("") }
    var pattern by remember { mutableStateOf(listOf<Int>()) }
    var mode by remember { mutableStateOf("pin") } // pin | pattern
    var storedPin by remember { mutableStateOf("") }
    var storedPattern by remember { mutableStateOf("") }
    var bioEnabled by remember { mutableStateOf(false) }
    var credentialOk by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        storedPin = app.prefs.get(Prefs.Keys.LOCK_PIN, "")
        storedPattern = app.prefs.get(Prefs.Keys.LOCK_PATTERN, "")
        bioEnabled = app.prefs.get(Prefs.Keys.BIOMETRIC, false)
        if (storedPattern.isNotEmpty() && storedPin.isEmpty()) mode = "pattern"
    }

    fun tryUnlockPin() {
        if (storedPin.isEmpty() || pin == storedPin) {
            credentialOk = true
            if (bioEnabled) tryBio(ctx, onUnlocked) { onUnlocked() }
            else onUnlocked()
        } else {
            Toast.makeText(ctx, "PIN 불일치", Toast.LENGTH_SHORT).show()
            pin = ""
        }
    }

    fun tryUnlockPattern() {
        val joined = pattern.joinToString("-")
        if (storedPattern.isEmpty() || joined == storedPattern) {
            credentialOk = true
            if (bioEnabled) tryBio(ctx, onUnlocked) { onUnlocked() }
            else onUnlocked()
        } else {
            Toast.makeText(ctx, "패턴 불일치", Toast.LENGTH_SHORT).show()
            pattern = emptyList()
        }
    }

    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("ScrollBox 잠금", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text("PIN/패턴 인증 후 생체 사용 가능", style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(16.dp))
        Row {
            FilterChip(selected = mode == "pin", onClick = { mode = "pin" }, label = { Text("PIN") })
            Spacer(Modifier.width(8.dp))
            FilterChip(selected = mode == "pattern", onClick = { mode = "pattern" }, label = { Text("패턴") })
        }
        Spacer(Modifier.height(24.dp))

        if (mode == "pin") {
            Text("•".repeat(pin.length).ifEmpty { "PIN 입력" }, fontSize = 28.sp)
            Spacer(Modifier.height(16.dp))
            Column {
                listOf(listOf(1, 2, 3), listOf(4, 5, 6), listOf(7, 8, 9), listOf(-1, 0, -2)).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        row.forEach { n ->
                            when (n) {
                                -1 -> Box(Modifier.size(64.dp))
                                -2 -> TextButton(onClick = { if (pin.isNotEmpty()) pin = pin.dropLast(1) }) { Text("⌫") }
                                else -> Box(
                                    Modifier.size(64.dp).clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer)
                                        .clickable {
                                            pin += n.toString()
                                            if (pin.length >= (storedPin.length.takeIf { it > 0 } ?: 4) &&
                                                (storedPin.isEmpty() || pin.length >= storedPin.length)
                                            ) {
                                                if (storedPin.isEmpty() && pin.length == 4) {
                                                    // first-time unset: open
                                                    credentialOk = true; onUnlocked()
                                                } else if (pin.length == storedPin.length) tryUnlockPin()
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) { Text("$n", fontSize = 22.sp) }
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }
            }
            Button(onClick = { tryUnlockPin() }) { Text("확인") }
        } else {
            Text("패턴: ${pattern.joinToString("-").ifEmpty { "점을 순서대로 탭" }}")
            Spacer(Modifier.height(12.dp))
            Column {
                for (r in 0..2) {
                    Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                        for (c in 0..2) {
                            val id = r * 3 + c + 1
                            val on = id in pattern
                            Box(
                                Modifier.size(56.dp).clip(CircleShape)
                                    .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                    .background(if (on) MaterialTheme.colorScheme.primary else Color.Transparent)
                                    .clickable {
                                        if (id !in pattern) pattern = pattern + id
                                    },
                                contentAlignment = Alignment.Center
                            ) {}
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                }
            }
            Row {
                TextButton(onClick = { pattern = emptyList() }) { Text("지우기") }
                Button(onClick = { tryUnlockPattern() }) { Text("확인") }
            }
        }
    }
}

private fun tryBio(ctx: android.content.Context, onOk: () -> Unit, fallback: () -> Unit) {
    val act = ctx as? FragmentActivity
    if (act == null) { fallback(); return }
    val bm = BiometricManager.from(ctx)
    if (bm.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) != BiometricManager.BIOMETRIC_SUCCESS) {
        fallback(); return
    }
    val prompt = BiometricPrompt(
        act,
        ContextCompat.getMainExecutor(ctx),
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) = onOk()
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) = fallback()
            override fun onAuthenticationFailed() {}
        }
    )
    prompt.authenticate(
        BiometricPrompt.PromptInfo.Builder()
            .setTitle("ScrollBox")
            .setSubtitle("생체 인증")
            .setNegativeButtonText("취소")
            .build()
    )
}
