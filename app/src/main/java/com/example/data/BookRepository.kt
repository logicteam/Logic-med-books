package com.example.data

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.local.DownloadedBook
import com.example.data.local.DownloadedBookDao
import com.example.data.local.FavoriteBook
import com.example.data.local.FavoriteBookDao
import kotlinx.coroutines.flow.Flow

import com.example.data.local.Notification
import com.example.data.local.NotificationDao

class BookRepository(context: Context) {
    private val favoriteBookDao: FavoriteBookDao = AppDatabase.getDatabase(context).favoriteBookDao()
    private val downloadedBookDao: DownloadedBookDao = AppDatabase.getDatabase(context).downloadedBookDao()
    private val notificationDao: NotificationDao = AppDatabase.getDatabase(context).notificationDao()

    val allNotifications: Flow<List<Notification>> = notificationDao.getAllNotifications()
    val unreadNotificationsCount: Flow<Int> = notificationDao.getUnreadCount()

    suspend fun addNotification(notification: Notification) {
        notificationDao.insertNotification(notification)
    }

    suspend fun markNotificationAsRead(id: Int) {
        notificationDao.markAsRead(id)
    }

    suspend fun markAllNotificationsAsRead() {
        notificationDao.markAllAsRead()
    }

    suspend fun deleteNotificationById(id: Int) {
        notificationDao.deleteNotification(id)
    }

    suspend fun deleteDefaultNotifications() {
        notificationDao.deleteDefaultNotifications()
    }

    suspend fun clearAllNotifications() {
        notificationDao.clearAllNotifications()
    }

    suspend fun getBooksFromApi(url: String): List<Book> {
        return NetworkModule.apiService.getBooks(url)
    }

    val allFavorites: Flow<List<FavoriteBook>> = favoriteBookDao.getAllFavorites()
    val allDownloaded: Flow<List<DownloadedBook>> = downloadedBookDao.getAllDownloadedBooks()

    suspend fun addFavorite(book: Book) {
        val favorite = FavoriteBook(
            title = book.title ?: "Untitled",
            author = book.author,
            category = book.category,
            description = book.description,
            cover = book.cover,
            url = book.url,
            language = book.language,
            timestamp = book.timestamp
        )
        favoriteBookDao.insertFavorite(favorite)
    }

    suspend fun removeFavorite(title: String) {
        favoriteBookDao.deleteFavoriteByTitle(title)
    }

    fun isFavorite(title: String): Flow<Boolean> {
        return favoriteBookDao.isFavorite(title)
    }

    suspend fun addDownloadedBook(downloadedBook: DownloadedBook) {
        downloadedBookDao.insertDownloadedBook(downloadedBook)
    }

    suspend fun removeDownloadedBookByTitle(title: String) {
        downloadedBookDao.deleteDownloadedBookByTitle(title)
    }

    fun isDownloaded(title: String): Flow<Boolean> {
        return downloadedBookDao.isDownloaded(title)
    }
}
