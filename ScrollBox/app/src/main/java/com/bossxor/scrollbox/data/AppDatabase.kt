package com.bossxor.scrollbox.data

import androidx.room.*

@Entity(tableName = "reading_progress")
data class ReadingProgress(
    @PrimaryKey val path: String,
    val offset: Int = 0,
    val pageMode: String = "scroll",
    val encoding: String = "auto",
    val lastOpened: Long = System.currentTimeMillis(),
    val percent: Float = 0f
)

@Entity(tableName = "bookmarks")
data class Bookmark(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val path: String,
    val offset: Int,
    val label: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "favorites")
data class Favorite(
    @PrimaryKey val path: String,
    val name: String,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "highlights")
data class Highlight(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val path: String,
    val start: Int,
    val end: Int,
    val color: Long = 0xFFFFFF00,
    val memo: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Dao
interface ProgressDao {
    @Query("SELECT * FROM reading_progress ORDER BY lastOpened DESC LIMIT :limit")
    suspend fun recent(limit: Int = 50): List<ReadingProgress>

    @Query("SELECT * FROM reading_progress WHERE path = :path")
    suspend fun get(path: String): ReadingProgress?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(p: ReadingProgress)

    @Query("DELETE FROM reading_progress WHERE path = :path")
    suspend fun delete(path: String)

    @Query("SELECT * FROM reading_progress")
    suspend fun all(): List<ReadingProgress>
}

@Dao
interface BookmarkDao {
    @Query("SELECT * FROM bookmarks WHERE path = :path ORDER BY offset")
    suspend fun forPath(path: String): List<Bookmark>

    @Insert
    suspend fun insert(b: Bookmark): Long

    @Delete
    suspend fun delete(b: Bookmark)

    @Query("SELECT * FROM bookmarks")
    suspend fun all(): List<Bookmark>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(list: List<Bookmark>)
}

@Dao
interface FavoriteDao {
    @Query("SELECT * FROM favorites ORDER BY addedAt DESC")
    suspend fun all(): List<Favorite>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(f: Favorite)

    @Query("DELETE FROM favorites WHERE path = :path")
    suspend fun delete(path: String)

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE path = :path)")
    suspend fun isFavorite(path: String): Boolean
}

@Dao
interface HighlightDao {
    @Query("SELECT * FROM highlights WHERE path = :path ORDER BY start")
    suspend fun forPath(path: String): List<Highlight>

    @Insert
    suspend fun insert(h: Highlight): Long

    @Update
    suspend fun update(h: Highlight)

    @Delete
    suspend fun delete(h: Highlight)

    @Query("SELECT * FROM highlights")
    suspend fun all(): List<Highlight>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(list: List<Highlight>)
}

@Database(
    entities = [ReadingProgress::class, Bookmark::class, Favorite::class, Highlight::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun progress(): ProgressDao
    abstract fun bookmarks(): BookmarkDao
    abstract fun favorites(): FavoriteDao
    abstract fun highlights(): HighlightDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null
        fun get(ctx: android.content.Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    ctx.applicationContext,
                    AppDatabase::class.java,
                    "scrollbox.db"
                ).build().also { INSTANCE = it }
            }
    }
}
