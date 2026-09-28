package com.bossxor.scrollbox.data

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("settings")

class Prefs(private val ctx: Context) {
    private val ds get() = ctx.dataStore

    object Keys {
        val FONT_SIZE = floatPreferencesKey("font_size")
        val LINE_SPACING = floatPreferencesKey("line_spacing")
        val MARGIN = floatPreferencesKey("margin")
        val THEME_BG = longPreferencesKey("theme_bg")
        val THEME_FG = longPreferencesKey("theme_fg")
        val BRIGHTNESS = floatPreferencesKey("brightness")
        val KEEP_SCREEN_ON = booleanPreferencesKey("keep_screen_on")
        val VOLUME_KEYS = booleanPreferencesKey("volume_keys")
        val PAGE_MODE = stringPreferencesKey("page_mode")
        val SHOW_LINE_NUM = booleanPreferencesKey("show_line_num")
        val GESTURE_LOCK = booleanPreferencesKey("gesture_lock")
        val TOUCH_ZONE_TOP = floatPreferencesKey("tz_top")
        val TOUCH_ZONE_BOTTOM = floatPreferencesKey("tz_bottom")
        val LOCK_ENABLED = booleanPreferencesKey("lock_enabled")
        val LOCK_PIN = stringPreferencesKey("lock_pin")
        val LOCK_PATTERN = stringPreferencesKey("lock_pattern")
        val BIOMETRIC = booleanPreferencesKey("biometric")
        val LAST_DIR = stringPreferencesKey("last_dir")
        val SORT_MODE = stringPreferencesKey("sort_mode")
        val DARK_FOLLOW = booleanPreferencesKey("dark_follow")
    }

    val fontSize: Flow<Float> = ds.data.map { it[Keys.FONT_SIZE] ?: 18f }
    val lineSpacing: Flow<Float> = ds.data.map { it[Keys.LINE_SPACING] ?: 1.4f }
    val margin: Flow<Float> = ds.data.map { it[Keys.MARGIN] ?: 16f }
    val themeBg: Flow<Long> = ds.data.map { it[Keys.THEME_BG] ?: 0xFFFFF8E7 }
    val themeFg: Flow<Long> = ds.data.map { it[Keys.THEME_FG] ?: 0xFF222222 }
    val brightness: Flow<Float> = ds.data.map { it[Keys.BRIGHTNESS] ?: 0f }
    val keepScreenOn: Flow<Boolean> = ds.data.map { it[Keys.KEEP_SCREEN_ON] ?: false }
    val volumeKeys: Flow<Boolean> = ds.data.map { it[Keys.VOLUME_KEYS] ?: true }
    val pageMode: Flow<String> = ds.data.map { it[Keys.PAGE_MODE] ?: "scroll" }
    val showLineNum: Flow<Boolean> = ds.data.map { it[Keys.SHOW_LINE_NUM] ?: false }
    val gestureLock: Flow<Boolean> = ds.data.map { it[Keys.GESTURE_LOCK] ?: false }
    val lockEnabled: Flow<Boolean> = ds.data.map { it[Keys.LOCK_ENABLED] ?: false }
    val lastDir: Flow<String> = ds.data.map { it[Keys.LAST_DIR] ?: "" }
    val sortMode: Flow<String> = ds.data.map { it[Keys.SORT_MODE] ?: "name_asc" }

    suspend fun get(key: Preferences.Key<String>, def: String = "") = ds.data.first()[key] ?: def
    suspend fun get(key: Preferences.Key<Boolean>, def: Boolean = false) = ds.data.first()[key] ?: def
    suspend fun get(key: Preferences.Key<Float>, def: Float) = ds.data.first()[key] ?: def
    suspend fun get(key: Preferences.Key<Long>, def: Long) = ds.data.first()[key] ?: def

    suspend fun set(key: Preferences.Key<String>, v: String) = ds.edit { it[key] = v }
    suspend fun set(key: Preferences.Key<Boolean>, v: Boolean) = ds.edit { it[key] = v }
    suspend fun set(key: Preferences.Key<Float>, v: Float) = ds.edit { it[key] = v }
    suspend fun set(key: Preferences.Key<Long>, v: Long) = ds.edit { it[key] = v }

    suspend fun snapshot(): Map<String, Any?> {
        val p = ds.data.first()
        return mapOf(
            "font_size" to (p[Keys.FONT_SIZE] ?: 18f),
            "line_spacing" to (p[Keys.LINE_SPACING] ?: 1.4f),
            "margin" to (p[Keys.MARGIN] ?: 16f),
            "theme_bg" to (p[Keys.THEME_BG] ?: 0xFFFFF8E7),
            "theme_fg" to (p[Keys.THEME_FG] ?: 0xFF222222),
            "brightness" to (p[Keys.BRIGHTNESS] ?: 0f),
            "keep_screen_on" to (p[Keys.KEEP_SCREEN_ON] ?: false),
            "volume_keys" to (p[Keys.VOLUME_KEYS] ?: true),
            "page_mode" to (p[Keys.PAGE_MODE] ?: "scroll"),
            "show_line_num" to (p[Keys.SHOW_LINE_NUM] ?: false),
            "gesture_lock" to (p[Keys.GESTURE_LOCK] ?: false),
            "lock_enabled" to (p[Keys.LOCK_ENABLED] ?: false),
            "lock_pin" to (p[Keys.LOCK_PIN] ?: ""),
            "lock_pattern" to (p[Keys.LOCK_PATTERN] ?: ""),
            "biometric" to (p[Keys.BIOMETRIC] ?: false),
            "last_dir" to (p[Keys.LAST_DIR] ?: ""),
            "sort_mode" to (p[Keys.SORT_MODE] ?: "name_asc"),
            "tz_top" to (p[Keys.TOUCH_ZONE_TOP] ?: 0.3f),
            "tz_bottom" to (p[Keys.TOUCH_ZONE_BOTTOM] ?: 0.3f)
        )
    }

    suspend fun restore(map: Map<String, Any?>) {
        ds.edit { e ->
            (map["font_size"] as? Number)?.toFloat()?.let { e[Keys.FONT_SIZE] = it }
            (map["line_spacing"] as? Number)?.toFloat()?.let { e[Keys.LINE_SPACING] = it }
            (map["margin"] as? Number)?.toFloat()?.let { e[Keys.MARGIN] = it }
            (map["theme_bg"] as? Number)?.toLong()?.let { e[Keys.THEME_BG] = it }
            (map["theme_fg"] as? Number)?.toLong()?.let { e[Keys.THEME_FG] = it }
            (map["brightness"] as? Number)?.toFloat()?.let { e[Keys.BRIGHTNESS] = it }
            (map["keep_screen_on"] as? Boolean)?.let { e[Keys.KEEP_SCREEN_ON] = it }
            (map["volume_keys"] as? Boolean)?.let { e[Keys.VOLUME_KEYS] = it }
            (map["page_mode"] as? String)?.let { e[Keys.PAGE_MODE] = it }
            (map["show_line_num"] as? Boolean)?.let { e[Keys.SHOW_LINE_NUM] = it }
            (map["gesture_lock"] as? Boolean)?.let { e[Keys.GESTURE_LOCK] = it }
            (map["lock_enabled"] as? Boolean)?.let { e[Keys.LOCK_ENABLED] = it }
            (map["lock_pin"] as? String)?.let { e[Keys.LOCK_PIN] = it }
            (map["lock_pattern"] as? String)?.let { e[Keys.LOCK_PATTERN] = it }
            (map["biometric"] as? Boolean)?.let { e[Keys.BIOMETRIC] = it }
            (map["last_dir"] as? String)?.let { e[Keys.LAST_DIR] = it }
            (map["sort_mode"] as? String)?.let { e[Keys.SORT_MODE] = it }
            (map["tz_top"] as? Number)?.toFloat()?.let { e[Keys.TOUCH_ZONE_TOP] = it }
            (map["tz_bottom"] as? Number)?.toFloat()?.let { e[Keys.TOUCH_ZONE_BOTTOM] = it }
        }
    }
}
