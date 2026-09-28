package com.bossxor.scrollbox.ui.viewer

import android.content.Intent
import android.graphics.BitmapFactory
import android.speech.tts.TextToSpeech
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.bossxor.scrollbox.MainActivity
import com.bossxor.scrollbox.ScrollBoxApp
import com.bossxor.scrollbox.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.lingala.zip4j.ZipFile
import java.io.File
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ViewerScreen(
    path: String,
    onBack: () -> Unit,
    onOpenPath: (String) -> Unit,
    registerVolume: ((Boolean) -> Boolean) -> Unit
) {
    val ctx = LocalContext.current
    val app = ScrollBoxApp.instance
    val scope = rememberCoroutineScope()
    val activity = ctx as? MainActivity

    val isContent = path.startsWith("content:")
    val file = if (isContent) null else File(path)
    val ext = (file?.extension ?: "").lowercase()
    val isImage = ext in setOf("jpg", "jpeg", "png", "gif", "webp")
    val isCbz = ext == "cbz" || (ext == "zip" && false) // zip opens as unzip in browser; cbz as comic
    val isEpub = ext == "epub"

    if (isImage && file != null) {
        ImageViewer(file, onBack)
        return
    }
    if (isCbz && file != null) {
        ComicViewer(file, onBack)
        return
    }

    var text by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(true) }
    var encoding by remember { mutableStateOf("auto") }
    var pageMode by remember { mutableStateOf("scroll") }
    var fontSize by remember { mutableFloatStateOf(18f) }
    var lineSpacing by remember { mutableFloatStateOf(1.4f) }
    var margin by remember { mutableFloatStateOf(16f) }
    var bg by remember { mutableLongStateOf(0xFFFFF8E7) }
    var fg by remember { mutableLongStateOf(0xFF222222) }
    var brightness by remember { mutableFloatStateOf(0f) }
    var showLineNum by remember { mutableStateOf(false) }
    var gestureLock by remember { mutableStateOf(false) }
    var keepOn by remember { mutableStateOf(false) }
    var volumeKeys by remember { mutableStateOf(true) }
    var showUi by remember { mutableStateOf(true) }
    var showSearch by remember { mutableStateOf(false) }
    var searchQ by remember { mutableStateOf("") }
    var searchIdx by remember { mutableIntStateOf(0) }
    var searchHits by remember { mutableStateOf(listOf<Int>()) }
    var showMenu by remember { mutableStateOf(false) }
    var showBookmarks by remember { mutableStateOf(false) }
    var showJump by remember { mutableStateOf(false) }
    var showInfo by remember { mutableStateOf(false) }
    var showHighlight by remember { mutableStateOf(false) }
    var bookmarks by remember { mutableStateOf(listOf<Bookmark>()) }
    val highlights by remember { mutableStateOf(mutableStateListOf<Highlight>()) }
    val scrollState = rememberScrollState()
    var resumeOffset by remember { mutableIntStateOf(0) }
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    var ttsReady by remember { mutableStateOf(false) }
    var jumpPage by remember { mutableStateOf("") }
    var charsPerPage by remember { mutableIntStateOf(1200) }
    var tzTop by remember { mutableFloatStateOf(0.3f) }
    var tzBottom by remember { mutableFloatStateOf(0.3f) }

    DisposableEffect(Unit) {
        val engine = TextToSpeech(ctx) { status ->
            ttsReady = status == TextToSpeech.SUCCESS
        }
        tts = engine
        onDispose {
            engine.stop(); engine.shutdown()
            activity?.volumeCallback = null
        }
    }

    LaunchedEffect(path) {
        fontSize = app.prefs.get(Prefs.Keys.FONT_SIZE, 18f)
        lineSpacing = app.prefs.get(Prefs.Keys.LINE_SPACING, 1.4f)
        margin = app.prefs.get(Prefs.Keys.MARGIN, 16f)
        bg = app.prefs.get(Prefs.Keys.THEME_BG, 0xFFFFF8E7)
        fg = app.prefs.get(Prefs.Keys.THEME_FG, 0xFF222222)
        brightness = app.prefs.get(Prefs.Keys.BRIGHTNESS, 0f)
        showLineNum = app.prefs.get(Prefs.Keys.SHOW_LINE_NUM, false)
        gestureLock = app.prefs.get(Prefs.Keys.GESTURE_LOCK, false)
        keepOn = app.prefs.get(Prefs.Keys.KEEP_SCREEN_ON, false)
        volumeKeys = app.prefs.get(Prefs.Keys.VOLUME_KEYS, true)
        pageMode = app.prefs.get(Prefs.Keys.PAGE_MODE, "scroll")
        tzTop = app.prefs.get(Prefs.Keys.TOUCH_ZONE_TOP, 0.3f)
        tzBottom = app.prefs.get(Prefs.Keys.TOUCH_ZONE_BOTTOM, 0.3f)

        val prog = if (!isContent) app.db.progress().get(path) else null
        if (prog != null) {
            resumeOffset = prog.offset
            encoding = prog.encoding
            pageMode = prog.pageMode.ifBlank { pageMode }
        }
        text = withContext(Dispatchers.IO) {
            when {
                isContent -> TextLoader.loadUri(ctx, android.net.Uri.parse(path.removePrefix("content:")), encoding)
                isEpub && file != null -> TextLoader.loadEpub(file)
                file != null && file.exists() -> TextLoader.load(file, encoding)
                else -> "파일을 열 수 없습니다: $path"
            }
        }
        if (!isContent) {
            bookmarks = app.db.bookmarks().forPath(path)
            highlights.clear()
            highlights.addAll(app.db.highlights().forPath(path))
        }
        loading = false
    }

    LaunchedEffect(keepOn) {
        activity?.window?.let { w ->
            if (keepOn) w.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            else w.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    LaunchedEffect(text, resumeOffset, loading) {
        if (!loading && resumeOffset > 0 && pageMode == "scroll") {
            scrollState.scrollTo(resumeOffset.coerceIn(0, scrollState.maxValue.coerceAtLeast(0)))
        }
    }

    fun saveProgress() {
        if (isContent || file == null) return
        scope.launch {
            val off = when (pageMode) {
                "scroll" -> scrollState.value
                else -> resumeOffset
            }
            val pct = if (text.isEmpty()) 0f else (off.toFloat() / text.length.coerceAtLeast(1)).coerceIn(0f, 1f)
            app.db.progress().upsert(
                ReadingProgress(path, off, pageMode, encoding, System.currentTimeMillis(), pct)
            )
        }
    }

    BackHandler {
        saveProgress()
        onBack()
    }

    fun pageCount() = ((text.length + charsPerPage - 1) / charsPerPage).coerceAtLeast(1)
    fun pageText(i: Int): String {
        val start = i * charsPerPage
        return text.substring(start, minOf(start + charsPerPage, text.length))
    }

    fun goPage(delta: Int) {
        when (pageMode) {
            "scroll" -> scope.launch {
                val step = (scrollState.maxValue * 0.9f).toInt().coerceAtLeast(200)
                scrollState.animateScrollTo((scrollState.value + delta * step).coerceIn(0, scrollState.maxValue))
            }
            else -> {
                val cur = (resumeOffset / charsPerPage).coerceIn(0, pageCount() - 1)
                val next = (cur + delta).coerceIn(0, pageCount() - 1)
                resumeOffset = next * charsPerPage
            }
        }
    }

    registerVolume { up ->
        if (!volumeKeys) return@registerVolume false
        goPage(if (up) -1 else 1)
        true
    }

    val displayText = remember(text, searchQ, searchIdx, showLineNum, highlights.toList()) {
        buildAnnotatedString {
            val lines = if (showLineNum) {
                text.lines().mapIndexed { i, l -> "${(i + 1).toString().padStart(4)} | $l" }.joinToString("\n")
            } else text
            withStyle(SpanStyle(color = Color(fg), fontSize = fontSize.sp)) {
                append(lines)
            }
            // highlight overlays approximate by range on raw text (skip if line nums)
            if (!showLineNum) {
                highlights.forEach { h ->
                    if (h.start in 0 until lines.length && h.end in 1..lines.length && h.start < h.end) {
                        addStyle(SpanStyle(background = Color(h.color).copy(alpha = 0.4f)), h.start, h.end)
                    }
                }
            }
            if (searchQ.isNotEmpty() && searchHits.isNotEmpty()) {
                val hit = searchHits.getOrNull(searchIdx)
                if (hit != null && hit + searchQ.length <= lines.length) {
                    addStyle(SpanStyle(background = Color.Yellow), hit, hit + searchQ.length)
                }
            }
        }
    }

    Box(Modifier.fillMaxSize().background(Color(bg))) {
        if (loading) {
            CircularProgressIndicator(Modifier.align(Alignment.Center))
        } else {
            val contentMod = Modifier
                .fillMaxSize()
                .padding(margin.dp)
                .then(
                    if (gestureLock) Modifier
                    else Modifier.pointerInput(pageMode, tzTop, tzBottom) {
                        detectTapGestures { offset ->
                            val h = size.height.toFloat()
                            val y = offset.y / h
                            when {
                                y < tzTop -> goPage(-1)
                                y > 1f - tzBottom -> goPage(1)
                                else -> showUi = !showUi
                            }
                        }
                    }
                )

            when (pageMode) {
                "horizontal" -> {
                    val pager = rememberPagerState(initialPage = (resumeOffset / charsPerPage).coerceAtLeast(0)) { pageCount() }
                    LaunchedEffect(pager.currentPage) { resumeOffset = pager.currentPage * charsPerPage }
                    HorizontalPager(state = pager, modifier = contentMod) { page ->
                        SelectionContainer {
                            Text(
                                text = pageText(page),
                                color = Color(fg),
                                fontSize = fontSize.sp,
                                lineHeight = (fontSize * lineSpacing).sp,
                                fontFamily = FontFamily.Serif,
                                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                            )
                        }
                    }
                }
                "vertical_touch" -> {
                    // page-by-page vertical (not continuous scroll)
                    val page = (resumeOffset / charsPerPage).coerceIn(0, pageCount() - 1)
                    SelectionContainer {
                        Text(
                            text = pageText(page),
                            color = Color(fg),
                            fontSize = fontSize.sp,
                            lineHeight = (fontSize * lineSpacing).sp,
                            fontFamily = FontFamily.Serif,
                            modifier = contentMod.verticalScroll(rememberScrollState())
                        )
                    }
                }
                else -> {
                    SelectionContainer {
                        Text(
                            text = displayText,
                            lineHeight = (fontSize * lineSpacing).sp,
                            fontFamily = FontFamily.Serif,
                            modifier = contentMod.verticalScroll(scrollState)
                        )
                    }
                }
            }
        }

        // brightness overlay
        if (brightness > 0f) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = brightness.coerceIn(0f, 0.85f))))
        }

        val pct = when (pageMode) {
            "scroll" -> if (scrollState.maxValue == 0) 0 else (scrollState.value * 100 / scrollState.maxValue)
            else -> {
                val p = (resumeOffset / charsPerPage).coerceAtLeast(0)
                ((p + 1) * 100 / pageCount())
            }
        }

        if (showUi) {
            TopAppBar(
                title = {
                    Text(
                        file?.name ?: "문서",
                        maxLines = 1,
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { saveProgress(); onBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                },
                actions = {
                    Text("$pct%", style = MaterialTheme.typography.labelMedium)
                    IconButton(onClick = { showSearch = !showSearch }) { Icon(Icons.Default.Search, null) }
                    IconButton(onClick = { showMenu = true }) { Icon(Icons.Default.MoreVert, null) }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }

        if (showSearch) {
            Column(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface).padding(8.dp)
            ) {
                OutlinedTextField(
                    value = searchQ,
                    onValueChange = {
                        searchQ = it
                        searchHits = if (it.isEmpty()) emptyList()
                        else {
                            val hits = mutableListOf<Int>()
                            var idx = text.indexOf(it, ignoreCase = true)
                            while (idx >= 0) {
                                hits += idx
                                idx = text.indexOf(it, idx + 1, ignoreCase = true)
                            }
                            hits
                        }
                        searchIdx = 0
                        if (searchHits.isNotEmpty() && pageMode == "scroll") {
                            scope.launch { scrollState.scrollTo(0) } // approx; real offset via char ratio
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("파일 내 검색") },
                    trailingIcon = {
                        Row {
                            Text("${if (searchHits.isEmpty()) 0 else searchIdx + 1}/${searchHits.size}")
                            IconButton(onClick = {
                                if (searchHits.isNotEmpty()) {
                                    searchIdx = (searchIdx - 1 + searchHits.size) % searchHits.size
                                    if (pageMode == "scroll") {
                                        val ratio = searchHits[searchIdx].toFloat() / text.length.coerceAtLeast(1)
                                        scope.launch { scrollState.scrollTo((scrollState.maxValue * ratio).toInt()) }
                                    } else resumeOffset = searchHits[searchIdx]
                                }
                            }) { Icon(Icons.Default.KeyboardArrowUp, null) }
                            IconButton(onClick = {
                                if (searchHits.isNotEmpty()) {
                                    searchIdx = (searchIdx + 1) % searchHits.size
                                    if (pageMode == "scroll") {
                                        val ratio = searchHits[searchIdx].toFloat() / text.length.coerceAtLeast(1)
                                        scope.launch { scrollState.scrollTo((scrollState.maxValue * ratio).toInt()) }
                                    } else resumeOffset = searchHits[searchIdx]
                                }
                            }) { Icon(Icons.Default.KeyboardArrowDown, null) }
                        }
                    }
                )
            }
        }

        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
            DropdownMenuItem(text = { Text("북마크 추가") }, onClick = {
                showMenu = false
                if (!isContent) scope.launch {
                    val off = if (pageMode == "scroll") scrollState.value else resumeOffset
                    app.db.bookmarks().insert(Bookmark(path = path, offset = off, label = "p$pct"))
                    bookmarks = app.db.bookmarks().forPath(path)
                    Toast.makeText(ctx, "북마크 저장", Toast.LENGTH_SHORT).show()
                }
            })
            DropdownMenuItem(text = { Text("북마크 목록") }, onClick = { showMenu = false; showBookmarks = true })
            DropdownMenuItem(text = { Text("페이지 이동") }, onClick = { showMenu = false; showJump = true })
            DropdownMenuItem(text = { Text("파일 정보") }, onClick = { showMenu = false; showInfo = true })
            DropdownMenuItem(text = { Text("하이라이트+메모") }, onClick = { showMenu = false; showHighlight = true })
            DropdownMenuItem(text = { Text("즐겨찾기") }, onClick = {
                showMenu = false
                if (file != null) scope.launch {
                    app.db.favorites().upsert(Favorite(path, file.name))
                    Toast.makeText(ctx, "즐겨찾기 추가", Toast.LENGTH_SHORT).show()
                }
            })
            DropdownMenuItem(text = { Text("공유") }, onClick = {
                showMenu = false
                if (file != null) {
                    val uri = try {
                        FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", file)
                    } catch (_: Exception) {
                        android.net.Uri.fromFile(file)
                    }
                    ctx.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }, "공유"))
                }
            })
            DropdownMenuItem(text = { Text("TTS 읽기") }, onClick = {
                showMenu = false
                if (ttsReady) {
                    tts?.language = Locale.KOREAN
                    val chunk = text.drop(if (pageMode == "scroll") 0 else resumeOffset).take(3000)
                    tts?.speak(chunk, TextToSpeech.QUEUE_FLUSH, null, "sb")
                } else Toast.makeText(ctx, "TTS 준비 중", Toast.LENGTH_SHORT).show()
            })
            DropdownMenuItem(text = { Text("TTS 중지") }, onClick = { showMenu = false; tts?.stop() })
            HorizontalDivider()
            Text("페이지 모드", Modifier.padding(horizontal = 12.dp))
            listOf("scroll" to "스크롤", "vertical_touch" to "세로 터치", "horizontal" to "가로").forEach { (k, l) ->
                DropdownMenuItem(text = { Text(l + if (pageMode == k) " ✓" else "") }, onClick = {
                    pageMode = k
                    scope.launch { app.prefs.set(Prefs.Keys.PAGE_MODE, k) }
                    showMenu = false
                })
            }
            HorizontalDivider()
            Text("인코딩", Modifier.padding(horizontal = 12.dp))
            listOf("auto", "utf-8", "euc-kr").forEach { enc ->
                DropdownMenuItem(text = { Text(enc + if (encoding == enc) " ✓" else "") }, onClick = {
                    encoding = enc
                    showMenu = false
                    scope.launch {
                        loading = true
                        text = withContext(Dispatchers.IO) {
                            when {
                                isContent -> TextLoader.loadUri(ctx, android.net.Uri.parse(path.removePrefix("content:")), enc)
                                isEpub && file != null -> TextLoader.loadEpub(file)
                                file != null -> TextLoader.load(file, enc)
                                else -> text
                            }
                        }
                        loading = false
                    }
                })
            }
            HorizontalDivider()
            DropdownMenuItem(text = { Text("글자 크기 +") }, onClick = {
                fontSize = (fontSize + 2).coerceAtMost(40f)
                scope.launch { app.prefs.set(Prefs.Keys.FONT_SIZE, fontSize) }
            })
            DropdownMenuItem(text = { Text("글자 크기 -") }, onClick = {
                fontSize = (fontSize - 2).coerceAtLeast(10f)
                scope.launch { app.prefs.set(Prefs.Keys.FONT_SIZE, fontSize) }
            })
            DropdownMenuItem(text = { Text("줄간격 +") }, onClick = {
                lineSpacing = (lineSpacing + 0.1f).coerceAtMost(2.5f)
                scope.launch { app.prefs.set(Prefs.Keys.LINE_SPACING, lineSpacing) }
            })
            DropdownMenuItem(text = { Text("줄간격 -") }, onClick = {
                lineSpacing = (lineSpacing - 0.1f).coerceAtLeast(1f)
                scope.launch { app.prefs.set(Prefs.Keys.LINE_SPACING, lineSpacing) }
            })
            DropdownMenuItem(text = { Text("여백 +") }, onClick = {
                margin = (margin + 4f).coerceAtMost(48f)
                scope.launch { app.prefs.set(Prefs.Keys.MARGIN, margin) }
            })
            DropdownMenuItem(text = { Text("여백 -") }, onClick = {
                margin = (margin - 4f).coerceAtLeast(0f)
                scope.launch { app.prefs.set(Prefs.Keys.MARGIN, margin) }
            })
            HorizontalDivider()
            DropdownMenuItem(text = { Text("테마: 베이지") }, onClick = {
                bg = 0xFFFFF8E7; fg = 0xFF222222
                scope.launch { app.prefs.set(Prefs.Keys.THEME_BG, bg); app.prefs.set(Prefs.Keys.THEME_FG, fg) }
            })
            DropdownMenuItem(text = { Text("테마: 흰색") }, onClick = {
                bg = 0xFFFFFFFF; fg = 0xFF000000
                scope.launch { app.prefs.set(Prefs.Keys.THEME_BG, bg); app.prefs.set(Prefs.Keys.THEME_FG, fg) }
            })
            DropdownMenuItem(text = { Text("테마: 다크") }, onClick = {
                bg = 0xFF1A1A1A; fg = 0xFFE0E0E0
                scope.launch { app.prefs.set(Prefs.Keys.THEME_BG, bg); app.prefs.set(Prefs.Keys.THEME_FG, fg) }
            })
            DropdownMenuItem(text = { Text("테마: 연두") }, onClick = {
                bg = 0xFFC7EDCC; fg = 0xFF222222
                scope.launch { app.prefs.set(Prefs.Keys.THEME_BG, bg); app.prefs.set(Prefs.Keys.THEME_FG, fg) }
            })
            DropdownMenuItem(text = { Text("밝기 오버레이 ${(brightness * 100).toInt()}%") }, onClick = {
                brightness = ((brightness + 0.1f) % 0.7f)
                scope.launch { app.prefs.set(Prefs.Keys.BRIGHTNESS, brightness) }
            })
            DropdownMenuItem(
                text = { Text(if (keepOn) "화면 켜짐 유지 ✓" else "화면 켜짐 유지") },
                onClick = {
                    keepOn = !keepOn
                    scope.launch { app.prefs.set(Prefs.Keys.KEEP_SCREEN_ON, keepOn) }
                })
            DropdownMenuItem(
                text = { Text(if (volumeKeys) "볼륨키 넘김 ✓" else "볼륨키 넘김") },
                onClick = {
                    volumeKeys = !volumeKeys
                    scope.launch { app.prefs.set(Prefs.Keys.VOLUME_KEYS, volumeKeys) }
                })
            DropdownMenuItem(
                text = { Text(if (showLineNum) "줄번호 ✓" else "줄번호") },
                onClick = {
                    showLineNum = !showLineNum
                    scope.launch { app.prefs.set(Prefs.Keys.SHOW_LINE_NUM, showLineNum) }
                })
            DropdownMenuItem(
                text = { Text(if (gestureLock) "제스처 잠금 ✓" else "제스처 잠금") },
                onClick = {
                    gestureLock = !gestureLock
                    scope.launch { app.prefs.set(Prefs.Keys.GESTURE_LOCK, gestureLock) }
                })
            if (file != null) {
                val sibs = remember(path) { FileOps.siblings(file) }
                val idx = sibs.indexOfFirst { it.absolutePath == path }
                DropdownMenuItem(text = { Text("이전 파일") }, onClick = {
                    showMenu = false
                    if (idx > 0) { saveProgress(); onOpenPath(sibs[idx - 1].absolutePath) }
                })
                DropdownMenuItem(text = { Text("다음 파일") }, onClick = {
                    showMenu = false
                    if (idx in 0 until sibs.lastIndex) { saveProgress(); onOpenPath(sibs[idx + 1].absolutePath) }
                })
            }
        }

        if (showBookmarks) {
            AlertDialog(
                onDismissRequest = { showBookmarks = false },
                title = { Text("북마크") },
                text = {
                    Column {
                        bookmarks.forEach { b ->
                            TextButton(onClick = {
                                if (pageMode == "scroll") scope.launch { scrollState.scrollTo(b.offset.coerceAtMost(scrollState.maxValue)) }
                                else resumeOffset = b.offset
                                showBookmarks = false
                            }) { Text("${b.label} @${b.offset}") }
                        }
                        if (bookmarks.isEmpty()) Text("없음")
                    }
                },
                confirmButton = { TextButton(onClick = { showBookmarks = false }) { Text("닫기") } }
            )
        }

        if (showJump) {
            AlertDialog(
                onDismissRequest = { showJump = false },
                title = { Text("페이지 이동 (1-${pageCount()})") },
                text = {
                    OutlinedTextField(value = jumpPage, onValueChange = { jumpPage = it }, singleLine = true)
                },
                confirmButton = {
                    TextButton(onClick = {
                        val p = jumpPage.toIntOrNull()?.minus(1)?.coerceIn(0, pageCount() - 1)
                        if (p != null) {
                            resumeOffset = p * charsPerPage
                            if (pageMode == "scroll") {
                                val ratio = p.toFloat() / pageCount()
                                scope.launch { scrollState.scrollTo((scrollState.maxValue * ratio).toInt()) }
                            }
                        }
                        showJump = false
                    }) { Text("이동") }
                },
                dismissButton = { TextButton(onClick = { showJump = false }) { Text("취소") } }
            )
        }

        if (showInfo) {
            AlertDialog(
                onDismissRequest = { showInfo = false },
                title = { Text("파일 정보") },
                text = {
                    Column {
                        Text("경로: $path")
                        if (file != null) {
                            Text("크기: ${file.length()} bytes")
                            Text("수정: ${java.util.Date(file.lastModified())}")
                        }
                        Text("글자 수: ${text.length}")
                        Text("인코딩: $encoding")
                    }
                },
                confirmButton = { TextButton(onClick = { showInfo = false }) { Text("닫기") } }
            )
        }

        if (showHighlight) {
            var hs by remember { mutableStateOf("0") }
            var he by remember { mutableStateOf("20") }
            var memo by remember { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = { showHighlight = false },
                title = { Text("하이라이트 + 메모") },
                text = {
                    Column {
                        OutlinedTextField(hs, { hs = it }, label = { Text("시작 오프셋") }, singleLine = true)
                        OutlinedTextField(he, { he = it }, label = { Text("끝 오프셋") }, singleLine = true)
                        OutlinedTextField(memo, { memo = it }, label = { Text("메모") })
                        highlights.forEach { h ->
                            TextButton(onClick = {
                                scope.launch {
                                    app.db.highlights().delete(h)
                                    highlights.remove(h)
                                }
                            }) { Text("삭제 ${h.start}-${h.end}: ${h.memo}") }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        val s = hs.toIntOrNull() ?: return@TextButton
                        val e = he.toIntOrNull() ?: return@TextButton
                        scope.launch {
                            val id = app.db.highlights().insert(Highlight(path = path, start = s, end = e, memo = memo))
                            highlights.add(Highlight(id, path, s, e, memo = memo))
                            showHighlight = false
                        }
                    }) { Text("추가") }
                },
                dismissButton = { TextButton(onClick = { showHighlight = false }) { Text("닫기") } }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ImageViewer(file: File, onBack: () -> Unit) {
    val bmp = remember(file) { BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap() }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(file.name) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { pad ->
        Box(Modifier.padding(pad).fillMaxSize(), contentAlignment = Alignment.Center) {
            if (bmp != null) Image(bmp, file.name, Modifier.fillMaxWidth().verticalScroll(rememberScrollState()))
            else Text("이미지를 열 수 없습니다")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun ComicViewer(file: File, onBack: () -> Unit) {
    val images = remember(file) {
        try {
            val zf = ZipFile(file)
            zf.fileHeaders
                .filter { !it.isDirectory && it.fileName.substringAfterLast('.').lowercase() in setOf("jpg", "jpeg", "png", "gif", "webp") }
                .sortedBy { it.fileName.lowercase() }
                .map { hdr ->
                    zf.getInputStream(hdr).use { ins -> BitmapFactory.decodeStream(ins)?.asImageBitmap() }
                }
                .filterNotNull()
        } catch (_: Exception) { emptyList() }
    }
    var idx by remember { mutableIntStateOf(0) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("${file.name} (${idx + 1}/${images.size})") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { pad ->
        Box(
            Modifier.padding(pad).fillMaxSize().pointerInput(images.size) {
                detectTapGestures { off ->
                    if (off.x < size.width / 2) idx = (idx - 1).coerceAtLeast(0)
                    else idx = (idx + 1).coerceAtMost(images.lastIndex.coerceAtLeast(0))
                }
            },
            contentAlignment = Alignment.Center
        ) {
            val img = images.getOrNull(idx)
            if (img != null) Image(img, null, Modifier.fillMaxWidth().verticalScroll(rememberScrollState()))
            else Text("이미지 없음")
        }
    }
}
