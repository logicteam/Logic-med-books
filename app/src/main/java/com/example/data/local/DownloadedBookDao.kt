package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadedBookDao {
    @Query("SELECT * FROM downloaded_books ORDER BY title ASC")
    fun getAllDownloadedBooks(): Flow<List<DownloadedBook>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDownloadedBook(book: DownloadedBook)

    @Query("DELETE FROM downloaded_books WHERE title = :title")
    suspend fun deleteDownloadedBookByTitle(title: String)

    @Query("SELECT EXISTS(SELECT 1 FROM downloaded_books WHERE title = :title)")
    fun isDownloaded(title: String): Flow<Boolean>
}
