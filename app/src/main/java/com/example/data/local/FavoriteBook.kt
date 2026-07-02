package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorite_books")
data class FavoriteBook(
    @PrimaryKey val title: String,
    val author: String?,
    val category: String?,
    val description: String?,
    val cover: String?,
    val url: String?,
    val language: String?,
    val timestamp: String?
)
