package com.bossxor.scrollbox.data

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.exception.ZipException
import java.io.File
import java.io.FileInputStream
import java.nio.charset.Charset
import java.util.zip.ZipInputStream

enum class SortMode { NAME_ASC, NAME_DESC, SIZE_ASC, SIZE_DESC, DATE_ASC, DATE_DESC }

data class FileItem(
    val file: File,
    val name: String = file.name,
    val isDir: Boolean = file.isDirectory,
    val size: Long = if (file.isFile) file.length() else 0L,
    val modified: Long = file.lastModified()
)

object FileOps {
    fun defaultRoot(): File {
        val ext = Environment.getExternalStorageDirectory()
        return if (ext.exists()) ext else File("/")
    }

    fun list(dir: File, sort: SortMode, query: String = ""): List<FileItem> {
        val files = dir.listFiles()?.toList().orEmpty()
        var items = files.map { FileItem(it) }
        if (query.isNotBlank()) {
            val q = query.lowercase()
            items = items.filter { it.name.lowercase().contains(q) }
        }
        val dirsFirst = items.sortedWith(
            compareByDescending<FileItem> { it.isDir }.thenComparing { a, b ->
                when (sort) {
                    SortMode.NAME_ASC -> a.name.compareTo(b.name, true)
                    SortMode.NAME_DESC -> b.name.compareTo(a.name, true)
                    SortMode.SIZE_ASC -> a.size.compareTo(b.size)
                    SortMode.SIZE_DESC -> b.size.compareTo(a.size)
                    SortMode.DATE_ASC -> a.modified.compareTo(b.modified)
                    SortMode.DATE_DESC -> b.modified.compareTo(a.modified)
                }
            }
        )
        return dirsFirst
    }

    fun copy(src: File, destDir: File): Boolean {
        return try {
            val dest = uniqueName(File(destDir, src.name))
            if (src.isDirectory) src.copyRecursively(dest, overwrite = false)
            else src.copyTo(dest, overwrite = false)
            true
        } catch (_: Exception) { false }
    }

    fun move(src: File, destDir: File): Boolean {
        return try {
            val dest = uniqueName(File(destDir, src.name))
            src.renameTo(dest) || (copy(src, destDir).also { if (it) delete(src) })
        } catch (_: Exception) { false }
    }

    fun delete(f: File): Boolean = try {
        if (f.isDirectory) f.deleteRecursively() else f.delete()
    } catch (_: Exception) { false }

    fun rename(f: File, newName: String): File? {
        val dest = File(f.parentFile, newName)
        return if (f.renameTo(dest)) dest else null
    }

    fun createFolder(parent: File, name: String): File? {
        val d = File(parent, name)
        return if (d.mkdirs() || d.exists()) d else null
    }

    fun createEmptyTxt(parent: File, name: String): File? {
        val n = if (name.endsWith(".txt", true)) name else "$name.txt"
        val f = uniqueName(File(parent, n))
        return try { f.writeText(""); f } catch (_: Exception) { null }
    }

    private fun uniqueName(f: File): File {
        if (!f.exists()) return f
        val base = f.nameWithoutExtension
        val ext = if (f.extension.isEmpty()) "" else ".${f.extension}"
        var i = 1
        while (true) {
            val c = File(f.parentFile, "$base ($i)$ext")
            if (!c.exists()) return c
            i++
        }
    }

    fun siblings(file: File, extensions: Set<String> = setOf("txt", "text", "log", "md")): List<File> {
        val parent = file.parentFile ?: return listOf(file)
        return parent.listFiles()
            ?.filter { it.isFile && it.extension.lowercase() in extensions }
            ?.sortedBy { it.name.lowercase() }
            .orEmpty()
    }

    fun pathFromTreeUri(ctx: Context, uri: Uri): String? {
        // ponytail: best-effort SAF → path for common primary storage trees
        return try {
            val docId = DocumentsContract.getTreeDocumentId(uri)
            val split = docId.split(":")
            if (split.size >= 2 && split[0].equals("primary", true)) {
                File(Environment.getExternalStorageDirectory(), split[1]).absolutePath
            } else null
        } catch (_: Exception) { null }
    }
}

object TextLoader {
    fun detectCharset(bytes: ByteArray): Charset {
        if (bytes.size >= 3 && bytes[0] == 0xEF.toByte() && bytes[1] == 0xBB.toByte() && bytes[2] == 0xBF.toByte())
            return Charsets.UTF_8
        val utf8 = Charsets.UTF_8
        val asUtf = String(bytes, utf8)
        val reenc = asUtf.toByteArray(utf8)
        // crude: if many replacement chars, try EUC-KR
        val bad = asUtf.count { it == '\uFFFD' }
        return if (bad > bytes.size / 50) {
            try { Charset.forName("EUC-KR") } catch (_: Exception) { utf8 }
        } else utf8
    }

    fun load(file: File, encoding: String = "auto"): String {
        val bytes = file.readBytes()
        val cs = when (encoding.lowercase()) {
            "utf-8", "utf8" -> Charsets.UTF_8
            "euc-kr", "euckr" -> Charset.forName("EUC-KR")
            else -> detectCharset(bytes)
        }
        return String(bytes, cs)
    }

    fun loadUri(ctx: Context, uri: Uri, encoding: String = "auto"): String {
        val bytes = ctx.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return ""
        val cs = when (encoding.lowercase()) {
            "utf-8", "utf8" -> Charsets.UTF_8
            "euc-kr", "euckr" -> Charset.forName("EUC-KR")
            else -> detectCharset(bytes)
        }
        return String(bytes, cs)
    }

    /** Basic EPUB: concatenate xhtml/html text from zip, strip tags. */
    fun loadEpub(file: File): String {
        val sb = StringBuilder()
        ZipInputStream(FileInputStream(file)).use { zis ->
            var e = zis.nextEntry
            while (e != null) {
                val n = e.name.lowercase()
                if (!e.isDirectory && (n.endsWith(".xhtml") || n.endsWith(".html") || n.endsWith(".htm"))) {
                    val raw = zis.readBytes().toString(Charsets.UTF_8)
                    sb.append(raw.replace(Regex("<[^>]+>"), " ").replace(Regex("\\s+"), " ").trim())
                    sb.append("\n\n")
                }
                zis.closeEntry()
                e = zis.nextEntry
            }
        }
        return sb.toString()
    }
}

object UnzipHelper {
    data class Result(val ok: Boolean, val message: String)

    fun unzip(
        zip: File,
        destDir: File,
        createNamedFolder: Boolean,
        password: CharArray? = null,
        onProgress: (Float) -> Unit = {}
    ): Result {
        return try {
            val target = if (createNamedFolder) {
                File(destDir, zip.nameWithoutExtension).also { it.mkdirs() }
            } else destDir
            val zf = ZipFile(zip)
            if (password != null) zf.setPassword(password)
            if (zf.isEncrypted && password == null) {
                return Result(false, "password_required")
            }
            zf.isRunInThread = false
            zf.extractAll(target.absolutePath)
            onProgress(1f)
            Result(true, target.absolutePath)
        } catch (e: ZipException) {
            Result(false, e.message ?: "zip error")
        } catch (e: Exception) {
            Result(false, e.message ?: "error")
        }
    }
}

object Backup {
    fun exportJson(db: AppDatabase, prefs: Prefs): String {
        // sync wrapper — call from coroutine
        throw UnsupportedOperationException("use exportSuspend")
    }

    suspend fun exportSuspend(db: AppDatabase, prefs: Prefs): String {
        val settings = prefs.snapshot()
        val progress = db.progress().all()
        val bookmarks = db.bookmarks().all()
        val favorites = db.favorites().all()
        val highlights = db.highlights().all()
        // minimal JSON without gson
        fun esc(s: String) = s.replace("\\", "\\\\").replace("\"", "\\\"")
        val sb = StringBuilder()
        sb.append("{\"settings\":{")
        sb.append(settings.entries.joinToString(",") { (k, v) ->
            when (v) {
                is String -> "\"$k\":\"${esc(v)}\""
                is Boolean -> "\"$k\":$v"
                is Number -> "\"$k\":$v"
                else -> "\"$k\":null"
            }
        })
        sb.append("},\"progress\":[")
        sb.append(progress.joinToString(",") {
            "{\"path\":\"${esc(it.path)}\",\"offset\":${it.offset},\"pageMode\":\"${esc(it.pageMode)}\"," +
                "\"encoding\":\"${esc(it.encoding)}\",\"lastOpened\":${it.lastOpened},\"percent\":${it.percent}}"
        })
        sb.append("],\"bookmarks\":[")
        sb.append(bookmarks.joinToString(",") {
            "{\"path\":\"${esc(it.path)}\",\"offset\":${it.offset},\"label\":\"${esc(it.label)}\",\"createdAt\":${it.createdAt}}"
        })
        sb.append("],\"favorites\":[")
        sb.append(favorites.joinToString(",") {
            "{\"path\":\"${esc(it.path)}\",\"name\":\"${esc(it.name)}\",\"addedAt\":${it.addedAt}}"
        })
        sb.append("],\"highlights\":[")
        sb.append(highlights.joinToString(",") {
            "{\"path\":\"${esc(it.path)}\",\"start\":${it.start},\"end\":${it.end},\"color\":${it.color}," +
                "\"memo\":\"${esc(it.memo)}\",\"createdAt\":${it.createdAt}}"
        })
        sb.append("]}")
        return sb.toString()
    }

    suspend fun importSuspend(db: AppDatabase, prefs: Prefs, json: String) {
        // ponytail: naive parse via org.json
        val root = org.json.JSONObject(json)
        if (root.has("settings")) {
            val s = root.getJSONObject("settings")
            val map = mutableMapOf<String, Any?>()
            s.keys().forEach { k -> map[k] = s.get(k) }
            prefs.restore(map)
        }
        if (root.has("progress")) {
            val arr = root.getJSONArray("progress")
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                db.progress().upsert(
                    ReadingProgress(
                        path = o.getString("path"),
                        offset = o.optInt("offset"),
                        pageMode = o.optString("pageMode", "scroll"),
                        encoding = o.optString("encoding", "auto"),
                        lastOpened = o.optLong("lastOpened", System.currentTimeMillis()),
                        percent = o.optDouble("percent", 0.0).toFloat()
                    )
                )
            }
        }
        if (root.has("bookmarks")) {
            val arr = root.getJSONArray("bookmarks")
            val list = (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                Bookmark(
                    path = o.getString("path"),
                    offset = o.optInt("offset"),
                    label = o.optString("label"),
                    createdAt = o.optLong("createdAt", System.currentTimeMillis())
                )
            }
            db.bookmarks().insertAll(list)
        }
        if (root.has("favorites")) {
            val arr = root.getJSONArray("favorites")
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                db.favorites().upsert(
                    Favorite(o.getString("path"), o.optString("name"), o.optLong("addedAt", System.currentTimeMillis()))
                )
            }
        }
        if (root.has("highlights")) {
            val arr = root.getJSONArray("highlights")
            val list = (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                Highlight(
                    path = o.getString("path"),
                    start = o.optInt("start"),
                    end = o.optInt("end"),
                    color = o.optLong("color", 0xFFFFFF00),
                    memo = o.optString("memo"),
                    createdAt = o.optLong("createdAt", System.currentTimeMillis())
                )
            }
            db.highlights().insertAll(list)
        }
    }
}
