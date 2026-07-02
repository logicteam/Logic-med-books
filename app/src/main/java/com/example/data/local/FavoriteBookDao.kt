package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteBookDao {
    @Query("SELECT * FROM favorite_books ORDER BY title ASC")
    fun getAllFavorites(): Flow<List<FavoriteBook>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(book: FavoriteBook)

    @Query("DELETE FROM favorite_books WHERE title = :title")
    suspend fun deleteFavoriteByTitle(title: String)

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_books WHERE title = :title)")
    fun isFavorite(title: String): Flow<Boolean>
}
