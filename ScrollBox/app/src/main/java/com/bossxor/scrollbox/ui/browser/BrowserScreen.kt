package com.bossxor.scrollbox.ui.browser

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.bossxor.scrollbox.ScrollBoxApp
import com.bossxor.scrollbox.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

private enum class ClipMode { COPY, CUT }

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(onOpenFile: (String) -> Unit, onSettings: () -> Unit) {
    var tab by remember { mutableIntStateOf(0) }
    val cs = MaterialTheme.colorScheme

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "ScrollBox",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "텍스트 · 파일 뷰어",
                            style = MaterialTheme.typography.bodySmall,
                            color = cs.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "설정")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = cs.surface,
                    titleContentColor = cs.onSurface
                )
            )
        },
        bottomBar = {
            NavigationBar(tonalElevation = 3.dp) {
                NavigationBarItem(
                    selected = tab == 0, onClick = { tab = 0 },
                    icon = { Icon(Icons.Default.Folder, null) }, label = { Text("파일") }
                )
                NavigationBarItem(
                    selected = tab == 1, onClick = { tab = 1 },
                    icon = { Icon(Icons.Default.History, null) }, label = { Text("최근") }
                )
                NavigationBarItem(
                    selected = tab == 2, onClick = { tab = 2 },
                    icon = { Icon(Icons.Default.Star, null) }, label = { Text("즐겨찾기") }
                )
            }
        },
        containerColor = cs.background
    ) { pad ->
        Box(Modifier.padding(pad).fillMaxSize()) {
            when (tab) {
                0 -> BrowserPane(onOpenFile)
                1 -> RecentPane(onOpenFile)
                2 -> FavPane(onOpenFile)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun BrowserPane(onOpenFile: (String) -> Unit) {
    val ctx = LocalContext.current
    val app = ScrollBoxApp.instance
    val scope = rememberCoroutineScope()
    val cs = MaterialTheme.colorScheme
    var current by remember { mutableStateOf(FileOps.defaultRoot()) }
    var sort by remember { mutableStateOf(SortMode.NAME_ASC) }
    var query by remember { mutableStateOf("") }
    var items by remember { mutableStateOf(listOf<FileItem>()) }
    var selected by remember { mutableStateOf(setOf<String>()) }
    var selecting by remember { mutableStateOf(false) }
    var clipboard by remember { mutableStateOf<List<File>>(emptyList()) }
    var clipMode by remember { mutableStateOf(ClipMode.COPY) }
    var showSort by remember { mutableStateOf(false) }
    var showNew by remember { mutableStateOf(false) }
    var showRename by remember { mutableStateOf<File?>(null) }
    var showUnzip by remember { mutableStateOf<File?>(null) }
    var unzipProgress by remember { mutableFloatStateOf(-1f) }
    var renameText by remember { mutableStateOf("") }
    var newName by remember { mutableStateOf("") }
    var newIsFolder by remember { mutableStateOf(true) }
    var storageOk by remember { mutableStateOf(hasStorage(ctx)) }
    var unzipCreateFolder by remember { mutableStateOf(true) }
    var unzipPassword by remember { mutableStateOf("") }
    var needPassword by remember { mutableStateOf(false) }

    fun refresh() {
        items = FileOps.list(current, sort, query)
        scope.launch { app.prefs.set(Prefs.Keys.LAST_DIR, current.absolutePath) }
    }

    LaunchedEffect(Unit) {
        val last = app.prefs.get(Prefs.Keys.LAST_DIR, "")
        if (last.isNotEmpty()) {
            val f = File(last)
            if (f.isDirectory) current = f
        }
        val sm = app.prefs.get(Prefs.Keys.SORT_MODE, "name_asc")
        sort = runCatching { SortMode.valueOf(sm.uppercase()) }.getOrDefault(SortMode.NAME_ASC)
        refresh()
    }
    LaunchedEffect(current, sort, query) { refresh() }

    val safLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            ctx.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            FileOps.pathFromTreeUri(ctx, uri)?.let {
                val f = File(it)
                if (f.isDirectory) { current = f; refresh() }
            }
        }
    }

    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        storageOk = hasStorage(ctx)
        refresh()
    }

    BackHandler(enabled = current.parentFile != null) {
        if (selecting) {
            selecting = false; selected = emptySet()
        } else {
            current.parentFile?.let { current = it }
        }
    }

    Column(Modifier.fillMaxSize()) {
        if (!storageOk) {
            AssistChip(
                onClick = {
                    if (Build.VERSION.SDK_INT >= 30) {
                        try {
                            ctx.startActivity(
                                Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                                    data = Uri.parse("package:${ctx.packageName}")
                                }
                            )
                        } catch (_: Exception) {
                            permLauncher.launch(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE))
                        }
                    } else {
                        permLauncher.launch(
                            arrayOf(
                                Manifest.permission.READ_EXTERNAL_STORAGE,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE
                            )
                        )
                    }
                    storageOk = hasStorage(ctx)
                },
                label = { Text("저장소 권한 필요 — 탭하여 허용") },
                modifier = Modifier.padding(8.dp)
            )
        }

        Surface(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
            shape = RoundedCornerShape(12.dp),
            color = cs.surfaceVariant.copy(alpha = 0.55f)
        ) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { current.parentFile?.let { current = it } }) {
                    Icon(Icons.Default.ArrowUpward, "상위")
                }
                Text(
                    current.absolutePath,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).clickable {
                        val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        cm.setPrimaryClip(ClipData.newPlainText("path", current.absolutePath))
                        Toast.makeText(ctx, "경로 복사됨", Toast.LENGTH_SHORT).show()
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = cs.onSurfaceVariant
                )
                IconButton(onClick = { safLauncher.launch(null) }) {
                    Icon(Icons.Default.FolderOpen, "SAF")
                }
                IconButton(onClick = { showSort = true }) {
                    Icon(Icons.Default.Sort, "정렬")
                }
                IconButton(onClick = { showNew = true; newIsFolder = true; newName = "" }) {
                    Icon(Icons.Default.CreateNewFolder, "새 항목")
                }
            }
        }

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
            singleLine = true,
            placeholder = { Text("파일명 검색") },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            trailingIcon = {
                if (query.isNotEmpty()) IconButton(onClick = { query = "" }) {
                    Icon(Icons.Default.Clear, null)
                }
            },
            shape = RoundedCornerShape(12.dp)
        )

        if (selecting || clipboard.isNotEmpty()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (selecting) {
                    FilterChip(selected = false, onClick = {
                        selected = items.map { it.file.absolutePath }.toSet()
                    }, label = { Text("전체") })
                    FilledTonalButton(onClick = {
                        clipboard = selected.map { File(it) }; clipMode = ClipMode.COPY
                        Toast.makeText(ctx, "복사", Toast.LENGTH_SHORT).show()
                    }) { Text("복사") }
                    FilledTonalButton(onClick = {
                        clipboard = selected.map { File(it) }; clipMode = ClipMode.CUT
                        Toast.makeText(ctx, "잘라내기", Toast.LENGTH_SHORT).show()
                    }) { Text("잘라내기") }
                    FilledTonalButton(onClick = {
                        scope.launch(Dispatchers.IO) {
                            selected.forEach { FileOps.delete(File(it)) }
                            withContext(Dispatchers.Main) {
                                selected = emptySet(); selecting = false; refresh()
                            }
                        }
                    }) { Text("삭제") }
                    if (selected.size == 1) {
                        FilterChip(selected = false, onClick = {
                            val f = File(selected.first())
                            showRename = f; renameText = f.name
                        }, label = { Text("이름변경") })
                    }
                    FilterChip(selected = false, onClick = {
                        selecting = false; selected = emptySet()
                    }, label = { Text("취소") })
                }
                if (clipboard.isNotEmpty()) {
                    FilledTonalButton(onClick = {
                        scope.launch(Dispatchers.IO) {
                            clipboard.forEach { src ->
                                if (clipMode == ClipMode.COPY) FileOps.copy(src, current)
                                else FileOps.move(src, current)
                            }
                            withContext(Dispatchers.Main) {
                                if (clipMode == ClipMode.CUT) clipboard = emptyList()
                                refresh()
                            }
                        }
                    }) { Text("붙여넣기") }
                }
            }
        }

        if (unzipProgress in 0f..1f) {
            LinearProgressIndicator(progress = { unzipProgress }, modifier = Modifier.fillMaxWidth().padding(8.dp))
        }

        if (items.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    if (query.isNotEmpty()) "검색 결과 없음" else "이 폴더가 비어 있습니다",
                    style = MaterialTheme.typography.bodyLarge,
                    color = cs.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(Modifier.fillMaxSize()) {
                items(items, key = { it.file.absolutePath }) { item ->
                    val path = item.file.absolutePath
                    val isSel = path in selected
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick = {
                                    if (selecting) {
                                        selected = if (isSel) selected - path else selected + path
                                    } else when {
                                        item.isDir -> current = item.file
                                        item.name.endsWith(".zip", true) -> showUnzip = item.file
                                        item.name.endsWith(".cbz", true) -> onOpenFile(path)
                                        isImage(item.name) -> onOpenFile(path)
                                        item.name.endsWith(".epub", true) -> onOpenFile(path)
                                        else -> onOpenFile(path)
                                    }
                                },
                                onLongClick = {
                                    selecting = true
                                    selected = selected + path
                                }
                            )
                            .padding(horizontal = 12.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(cs.primaryContainer.copy(alpha = 0.55f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                when {
                                    item.isDir -> Icons.Default.Folder
                                    item.name.endsWith(".zip", true) || item.name.endsWith(".cbz", true) -> Icons.Default.Archive
                                    isImage(item.name) -> Icons.Default.Image
                                    else -> Icons.Default.Description
                                },
                                null,
                                tint = cs.onPrimaryContainer
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(item.name, maxLines = 1, overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodyLarge)
                            val sdf = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }
                            Text(
                                if (item.isDir) "폴더 · ${sdf.format(Date(item.modified))}"
                                else "${formatSize(item.size)} · ${sdf.format(Date(item.modified))}",
                                style = MaterialTheme.typography.bodySmall,
                                color = cs.onSurfaceVariant
                            )
                        }
                        if (selecting) {
                            Checkbox(checked = isSel, onCheckedChange = {
                                selected = if (isSel) selected - path else selected + path
                            })
                        }
                    }
                    HorizontalDivider(
                        Modifier.padding(start = 64.dp, end = 12.dp),
                        thickness = 0.5.dp,
                        color = cs.outlineVariant.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }

    if (showSort) {
        AlertDialog(
            onDismissRequest = { showSort = false },
            title = { Text("정렬") },
            text = {
                Column {
                    SortMode.entries.forEach { m ->
                        TextButton(onClick = {
                            sort = m
                            scope.launch { app.prefs.set(Prefs.Keys.SORT_MODE, m.name.lowercase()) }
                            showSort = false
                        }) { Text(sortLabel(m)) }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showSort = false }) { Text("닫기") } }
        )
    }

    if (showNew) {
        AlertDialog(
            onDismissRequest = { showNew = false },
            title = { Text(if (newIsFolder) "새 폴더" else "새 텍스트") },
            text = {
                Column {
                    Row {
                        FilterChip(selected = newIsFolder, onClick = { newIsFolder = true }, label = { Text("폴더") })
                        Spacer(Modifier.width(8.dp))
                        FilterChip(selected = !newIsFolder, onClick = { newIsFolder = false }, label = { Text("txt") })
                    }
                    OutlinedTextField(value = newName, onValueChange = { newName = it }, singleLine = true)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newName.isNotBlank()) {
                        if (newIsFolder) FileOps.createFolder(current, newName)
                        else FileOps.createEmptyTxt(current, newName)
                        refresh(); showNew = false
                    }
                }) { Text("생성") }
            },
            dismissButton = { TextButton(onClick = { showNew = false }) { Text("취소") } }
        )
    }

    showRename?.let { f ->
        AlertDialog(
            onDismissRequest = { showRename = null },
            title = { Text("이름 변경") },
            text = { OutlinedTextField(value = renameText, onValueChange = { renameText = it }, singleLine = true) },
            confirmButton = {
                TextButton(onClick = {
                    FileOps.rename(f, renameText)
                    showRename = null; selecting = false; selected = emptySet(); refresh()
                }) { Text("확인") }
            },
            dismissButton = { TextButton(onClick = { showRename = null }) { Text("취소") } }
        )
    }

    showUnzip?.let { zip ->
        AlertDialog(
            onDismissRequest = { if (unzipProgress < 0) showUnzip = null },
            title = { Text("압축 해제") },
            text = {
                Column {
                    Text(zip.name)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = unzipCreateFolder, onCheckedChange = { unzipCreateFolder = it })
                        Text("ZIP 이름 폴더 생성", Modifier.clickable { unzipCreateFolder = !unzipCreateFolder })
                    }
                    if (needPassword) {
                        OutlinedTextField(
                            value = unzipPassword,
                            onValueChange = { unzipPassword = it },
                            label = { Text("비밀번호") },
                            singleLine = true
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    unzipProgress = 0f
                    scope.launch(Dispatchers.IO) {
                        val pw = unzipPassword.takeIf { it.isNotEmpty() }?.toCharArray()
                        val r = UnzipHelper.unzip(zip, current, unzipCreateFolder, pw) { unzipProgress = it }
                        withContext(Dispatchers.Main) {
                            if (!r.ok && r.message == "password_required") {
                                needPassword = true
                                unzipProgress = -1f
                                Toast.makeText(ctx, "비밀번호 필요", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(
                                    ctx,
                                    if (r.ok) "완료: ${r.message}" else "실패: ${r.message}",
                                    Toast.LENGTH_LONG
                                ).show()
                                showUnzip = null; needPassword = false; unzipPassword = ""
                                unzipProgress = -1f; refresh()
                            }
                        }
                    }
                }) { Text("해제") }
            },
            dismissButton = { TextButton(onClick = { showUnzip = null }) { Text("취소") } }
        )
    }
}

@Composable
private fun RecentPane(onOpenFile: (String) -> Unit) {
    val app = ScrollBoxApp.instance
    val cs = MaterialTheme.colorScheme
    var list by remember { mutableStateOf(listOf<ReadingProgress>()) }
    LaunchedEffect(Unit) { list = app.db.progress().recent() }
    if (list.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("최근 기록이 없습니다", color = cs.onSurfaceVariant)
        }
    } else {
        LazyColumn(Modifier.fillMaxSize()) {
            items(list) { p ->
                ListItem(
                    headlineContent = { Text(File(p.path).name) },
                    supportingContent = {
                        Text(
                            "${(p.percent * 100).toInt()}% · ${p.path}",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    modifier = Modifier.clickable { onOpenFile(p.path) }
                )
                HorizontalDivider(
                    thickness = 0.5.dp,
                    color = cs.outlineVariant.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@Composable
private fun FavPane(onOpenFile: (String) -> Unit) {
    val app = ScrollBoxApp.instance
    val cs = MaterialTheme.colorScheme
    var list by remember { mutableStateOf(listOf<Favorite>()) }
    LaunchedEffect(Unit) { list = app.db.favorites().all() }
    if (list.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("즐겨찾기가 없습니다", color = cs.onSurfaceVariant)
        }
    } else {
        LazyColumn(Modifier.fillMaxSize()) {
            items(list) { f ->
                ListItem(
                    headlineContent = { Text(f.name) },
                    supportingContent = { Text(f.path, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    trailingContent = {
                        IconButton(onClick = {
                            kotlinx.coroutines.MainScope().launch {
                                app.db.favorites().delete(f.path)
                                list = app.db.favorites().all()
                            }
                        }) { Icon(Icons.Default.Delete, null) }
                    },
                    modifier = Modifier.clickable { onOpenFile(f.path) }
                )
                HorizontalDivider(
                    thickness = 0.5.dp,
                    color = cs.outlineVariant.copy(alpha = 0.5f)
                )
            }
        }
    }
}

private fun hasStorage(ctx: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= 30) Environment.isExternalStorageManager()
    else ContextCompat.checkSelfPermission(ctx, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
}

private fun formatSize(b: Long): String = when {
    b < 1024 -> "$b B"
    b < 1024 * 1024 -> "${b / 1024} KB"
    else -> String.format("%.1f MB", b / (1024.0 * 1024))
}

private fun sortLabel(m: SortMode) = when (m) {
    SortMode.NAME_ASC -> "이름 ↑"
    SortMode.NAME_DESC -> "이름 ↓"
    SortMode.SIZE_ASC -> "크기 ↑"
    SortMode.SIZE_DESC -> "크기 ↓"
    SortMode.DATE_ASC -> "날짜 ↑"
    SortMode.DATE_DESC -> "날짜 ↓"
}

private fun isImage(name: String): Boolean {
    val e = name.substringAfterLast('.', "").lowercase()
    return e in setOf("jpg", "jpeg", "png", "gif", "webp")
}
