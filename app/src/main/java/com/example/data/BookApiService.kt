package com.example.data

import retrofit2.http.GET
import retrofit2.http.Url

interface BookApiService {
    @GET
    suspend fun getBooks(@Url url: String): List<Book>
}
