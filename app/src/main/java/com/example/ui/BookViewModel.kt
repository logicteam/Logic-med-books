package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.Book
import com.example.data.BookRepository
import com.example.data.local.DownloadedBook
import com.example.util.SecurityUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface BookUiState {
    object Loading : BookUiState
    data class Success(val books: List<Book>) : BookUiState
    data class Error(val message: String) : BookUiState
}

sealed interface DownloadState {
    object Idle : DownloadState
    data class Downloading(val book: Book, val progress: Int) : DownloadState
    data class Success(val book: Book, val filePath: String) : DownloadState
    data class Error(val book: Book?, val message: String) : DownloadState
}

class BookViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = BookRepository(application)

    private val _books = MutableStateFlow<List<Book>>(emptyList())
    private val _uiState = MutableStateFlow<BookUiState>(BookUiState.Loading)
    val uiState: StateFlow<BookUiState> = _uiState

    private val _downloadState = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val downloadState: StateFlow<DownloadState> = _downloadState

    private val _currentReadingBook = MutableStateFlow<DownloadedBook?>(null)
    val currentReadingBook: StateFlow<DownloadedBook?> = _currentReadingBook

    fun startReadingBook(book: DownloadedBook) {
        _currentReadingBook.value = book
    }

    fun stopReadingBook() {
        _currentReadingBook.value = null
    }

    private val readerPrefs = application.getSharedPreferences("pdf_reader_prefs", Context.MODE_PRIVATE)

    fun getReadingProgress(title: String): Int {
        return readerPrefs.getInt("progress_$title", 0)
    }

    fun saveReadingProgress(title: String, page: Int) {
        readerPrefs.edit().putInt("progress_$title", page).apply()
    }

    fun getBookmarks(title: String): Set<String> {
        return readerPrefs.getStringSet("bookmarks_$title", emptySet()) ?: emptySet()
    }

    fun addBookmark(title: String, pageIndex: Int) {
        val bookmarks = getBookmarks(title).toMutableSet()
        bookmarks.add(pageIndex.toString())
        readerPrefs.edit().putStringSet("bookmarks_$title", bookmarks).apply()
    }

    fun removeBookmark(title: String, pageIndex: Int) {
        val bookmarks = getBookmarks(title).toMutableSet()
        bookmarks.remove(pageIndex.toString())
        readerPrefs.edit().putStringSet("bookmarks_$title", bookmarks).apply()
    }

    fun isBookmarked(title: String, pageIndex: Int): Boolean {
        return getBookmarks(title).contains(pageIndex.toString())
    }

    private val prefs = application.getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)

    val searchQuery = MutableStateFlow("")
    val selectedCategory = MutableStateFlow("All")
    val isDarkThemeOverride = MutableStateFlow<Boolean?>(
        when (prefs.getString("theme_override", "system")) {
            "light" -> false
            "dark" -> true
            else -> null
        }
    )

    private val _categories = MutableStateFlow<List<String>>(listOf("All"))
    val categories: StateFlow<List<String>> = _categories

    // Reactive favorites list from Room database
    val favoriteBooks: StateFlow<List<Book>> = repository.allFavorites
        .map { favList ->
            favList.map { fav ->
                Book(
                    timestamp = fav.timestamp,
                    title = fav.title,
                    author = fav.author,
                    category = fav.category,
                    description = fav.description,
                    cover = fav.cover,
                    url = fav.url,
                    language = fav.language
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Reactive downloaded books list from Room database
    val downloadedBooks: StateFlow<List<DownloadedBook>> = repository.allDownloaded
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun removeDownloadedBookByTitle(title: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.removeDownloadedBookByTitle(title)
        }
    }

    val filteredBooks: StateFlow<List<Book>> = combine(
        _books,
        searchQuery,
        selectedCategory
    ) { booksList, query, category ->
        booksList.filter { book ->
            val matchesQuery = query.isEmpty() ||
                    (book.title?.contains(query, ignoreCase = true) == true) ||
                    (book.author?.contains(query, ignoreCase = true) == true)

            val matchesCategory = category == "All" ||
                    (book.category?.trim()?.equals(category.trim(), ignoreCase = true) == true)

            matchesQuery && matchesCategory
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    init {
        viewModelScope.launch {
            isDarkThemeOverride.collect { override ->
                val strValue = when (override) {
                    false -> "light"
                    true -> "dark"
                    else -> "system"
                }
                prefs.edit().putString("theme_override", strValue).apply()
            }
        }
        fetchBooks()
    }

    fun fetchBooks() {
        viewModelScope.launch {
            _uiState.value = BookUiState.Loading
            try {
                val url = SecurityUtils.getDecryptedUrl()
                val fetchedBooks = repository.getBooksFromApi(url)
                _books.value = fetchedBooks
                
                val distinctCategories = fetchedBooks
                    .mapNotNull { it.category?.trim() }
                    .filter { it.isNotBlank() }
                    .distinct()
                    .sorted()
                
                _categories.value = listOf("All") + distinctCategories
                _uiState.value = BookUiState.Success(fetchedBooks)
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = BookUiState.Error(e.message ?: "Failed to load medical books")
            }
        }
    }

    fun toggleFavorite(book: Book) {
        viewModelScope.launch {
            val title = book.title ?: "Untitled"
            val isFav = favoriteBooks.value.any { it.title.equals(title, ignoreCase = true) }
            if (isFav) {
                repository.removeFavorite(title)
            } else {
                repository.addFavorite(book)
            }
        }
    }

    fun isBookFavorite(title: String): StateFlow<Boolean> {
        return repository.isFavorite(title)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = false
            )
    }

    fun downloadBookPdf(book: Book) {
        val rawUrl = book.url
        if (rawUrl.isNullOrEmpty()) {
            _downloadState.value = DownloadState.Error(book, "Invalid download URL.")
            return
        }
        val title = book.title ?: "Untitled"
        _downloadState.value = DownloadState.Downloading(book, 0)

        viewModelScope.launch(Dispatchers.IO) {
            try {
                var formattedUrl = rawUrl.trim()
                if (!formattedUrl.startsWith("http://") && !formattedUrl.startsWith("https://")) {
                    formattedUrl = "https://$formattedUrl"
                }

                val urlConnection = java.net.URL(formattedUrl).openConnection() as java.net.HttpURLConnection
                urlConnection.connectTimeout = 15000
                urlConnection.readTimeout = 15000
                urlConnection.connect()

                val responseCode = urlConnection.responseCode
                if (responseCode != java.net.HttpURLConnection.HTTP_OK) {
                    throw Exception("Server returned HTTP $responseCode")
                }

                val fileLength = urlConnection.contentLength
                val inputStream = urlConnection.inputStream
                
                // Sanitize file name
                val sanitizedTitle = title.replace("[^a-zA-Z0-9]".toRegex(), "_")
                val fileName = "$sanitizedTitle.pdf"

                val context = getApplication<Application>()
                val outputStream: java.io.OutputStream
                val resultPathOrUri: String

                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    val contentResolver = context.contentResolver
                    val contentValues = android.content.ContentValues().apply {
                        put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                        put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                        put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS)
                    }

                    val uri = contentResolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                        ?: throw Exception("Failed to create MediaStore entry in Downloads.")
                    outputStream = contentResolver.openOutputStream(uri)
                        ?: throw Exception("Failed to open output stream.")
                    resultPathOrUri = uri.toString()
                } else {
                    // Check write permission for SDK < 29
                    val hasPermission = androidx.core.content.ContextCompat.checkSelfPermission(
                        context,
                        android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ) == android.content.pm.PackageManager.PERMISSION_GRANTED

                    if (!hasPermission) {
                        inputStream.close()
                        _downloadState.value = DownloadState.Error(book, "Permission Denied: Write External Storage permission is required on Android 9 and below.")
                        return@launch
                    }

                    val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
                    if (!downloadsDir.exists()) {
                        downloadsDir.mkdirs()
                    }
                    val outputFile = java.io.File(downloadsDir, fileName)
                    outputStream = java.io.FileOutputStream(outputFile)
                    resultPathOrUri = outputFile.absolutePath
                }

                val data = ByteArray(4096)
                var total: Long = 0
                var count: Int
                while (inputStream.read(data).also { count = it } != -1) {
                    total += count
                    if (fileLength > 0) {
                        val progress = ((total * 100) / fileLength).toInt()
                        _downloadState.value = DownloadState.Downloading(book, progress)
                    } else {
                        _downloadState.value = DownloadState.Downloading(book, -1)
                    }
                    outputStream.write(data, 0, count)
                }

                outputStream.flush()
                outputStream.close()
                inputStream.close()

                val downloadedBook = DownloadedBook(
                    title = book.title ?: "Untitled",
                    author = book.author,
                    category = book.category,
                    description = book.description,
                    cover = book.cover,
                    url = book.url,
                    language = book.language,
                    timestamp = book.timestamp,
                    localFilePath = resultPathOrUri
                )
                repository.addDownloadedBook(downloadedBook)

                _downloadState.value = DownloadState.Success(book, resultPathOrUri)
            } catch (e: Exception) {
                e.printStackTrace()
                _downloadState.value = DownloadState.Error(book, e.localizedMessage ?: "Failed to download PDF file.")
            }
        }
    }

    fun clearDownloadState() {
        _downloadState.value = DownloadState.Idle
    }
}
