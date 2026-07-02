package com.example.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Book(
    @Json(name = "Timestamp") val timestamp: String? = "",
    @Json(name = "Title") val title: String? = "",
    @Json(name = "Author") val author: String? = "",
    @Json(name = "Category") val category: String? = "",
    @Json(name = "Description") val description: String? = "",
    @Json(name = "Cover") val cover: String? = "",
    @Json(name = "url") val url: String? = "",
    @Json(name = "Language") val language: String? = ""
)
