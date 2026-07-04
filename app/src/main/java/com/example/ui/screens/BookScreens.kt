package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.Image
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import androidx.compose.ui.viewinterop.AndroidView
import android.webkit.WebView
import android.webkit.WebViewClient
import java.net.URLEncoder
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.activity.compose.BackHandler
import android.app.Activity
import android.content.ContextWrapper
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import com.example.R
import com.example.data.Book
import com.example.ui.BookUiState
import com.example.ui.BookViewModel
import com.example.ui.DownloadState

@Composable
fun BookAppContent(
    viewModel: BookViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentReadingBook by viewModel.currentReadingBook.collectAsState()
    var selectedBook by remember { mutableStateOf<Book?>(null) }
    var onlineReadingBook by remember { mutableStateOf<Book?>(null) }
    var showExitDialog by remember { mutableStateOf(false) }
    var showNotifications by remember { mutableStateOf(false) }

    fun findActivity(ctx: android.content.Context): Activity? {
        var currentContext = ctx
        while (currentContext is ContextWrapper) {
            if (currentContext is Activity) return currentContext
            currentContext = currentContext.baseContext
        }
        return null
    }

    // Handle back buttons
    BackHandler(enabled = currentReadingBook != null) {
        viewModel.stopReadingBook()
    }

    BackHandler(enabled = currentReadingBook == null && onlineReadingBook != null) {
        onlineReadingBook = null
    }

    BackHandler(enabled = currentReadingBook == null && onlineReadingBook == null && selectedBook != null) {
        selectedBook = null
    }

    BackHandler(enabled = currentReadingBook == null && onlineReadingBook == null && selectedBook == null && showNotifications) {
        showNotifications = false
    }

    BackHandler(enabled = currentReadingBook == null && onlineReadingBook == null && selectedBook == null && !showNotifications) {
        showExitDialog = true
    }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = {
                Text(
                    text = stringResource(R.string.exit_app_title),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.exit_app_message),
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showExitDialog = false
                        findActivity(context)?.finish()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier.testTag("exit_confirm_btn")
                ) {
                    Text(stringResource(R.string.exit_btn), color = MaterialTheme.colorScheme.onError)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showExitDialog = false },
                    modifier = Modifier.testTag("exit_cancel_btn")
                ) {
                    Text(stringResource(R.string.cancel_btn))
                }
            },
            modifier = Modifier.testTag("exit_dialog")
        )
    }

    AnimatedContent(
        targetState = currentReadingBook,
        transitionSpec = {
            slideInVertically { height -> height } + fadeIn() togetherWith
                    slideOutVertically { height -> height } + fadeOut()
        },
        label = "ReaderTransition"
    ) { readingBook ->
        if (readingBook != null) {
            PdfReaderScreen(
                downloadedBook = readingBook,
                viewModel = viewModel,
                onClose = { viewModel.stopReadingBook() }
            )
        } else {
            var currentTab by remember { mutableStateOf("books") }

            if (onlineReadingBook != null) {
                OnlineReaderScreen(
                    book = onlineReadingBook!!,
                    viewModel = viewModel,
                    onClose = { onlineReadingBook = null }
                )
            } else {
                Scaffold(
                    bottomBar = {
                        if (selectedBook == null && !showNotifications) {
                            GeometricBottomNav(
                                currentTab = currentTab,
                                onTabSelected = { currentTab = it }
                            )
                        }
                    },
                    modifier = modifier.fillMaxSize()
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background)
                            .padding(innerPadding)
                    ) {
                        AnimatedContent(
                            targetState = if (showNotifications) "notifications" else if (selectedBook != null) "detail" else "tabs",
                            transitionSpec = {
                                slideInHorizontally { width -> width } + fadeIn() togetherWith
                                        slideOutHorizontally { width -> -width } + fadeOut()
                            },
                            label = "ScreenTransition"
                        ) { screenState ->
                            when (screenState) {
                                "notifications" -> {
                                    NotificationsScreen(
                                        viewModel = viewModel,
                                        onBack = { showNotifications = false }
                                    )
                                }
                                "detail" -> {
                                    selectedBook?.let { book ->
                                        BookDetailScreen(
                                            book = book,
                                            viewModel = viewModel,
                                            onBack = { selectedBook = null },
                                            onReadOnline = { onlineReadingBook = book }
                                        )
                                    }
                                }
                                "tabs" -> {
                                    Crossfade(targetState = currentTab, label = "TabTransition") { tab ->
                                        when (tab) {
                                            "books" -> BooksFeedTab(
                                                viewModel = viewModel,
                                                onBookSelected = { selectedBook = it },
                                                onOpenAbout = { currentTab = "about" },
                                                onOpenNotifications = { showNotifications = true }
                                            )
                                            "favorites" -> FavoritesTab(
                                                viewModel = viewModel,
                                                onBookSelected = { selectedBook = it }
                                            )
                                            "offline" -> OfflineTab(
                                                viewModel = viewModel,
                                                onBookSelected = { selectedBook = it },
                                                onNavigateToFeed = { currentTab = "books" }
                                            )
                                            "about" -> AboutTab(viewModel = viewModel)
                                        }
                                    }
                                }
                            }
                        }

                        DownloadProgressDialog(viewModel = viewModel)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BooksFeedTab(
    viewModel: BookViewModel,
    onBookSelected: (Book) -> Unit,
    onOpenAbout: () -> Unit,
    onOpenNotifications: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val filteredBooks by viewModel.filteredBooks.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val focusManager = LocalFocusManager.current
    val isDarkThemeOverride by viewModel.isDarkThemeOverride.collectAsState()
    val isGridView by viewModel.isGridView.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Logic Med Library",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = (-0.5).sp
                    )
                )
                Text(
                    text = "Medical Laboratory Library",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Layout Grid/List toggle Button
                IconButton(
                    onClick = { viewModel.toggleLayoutMode() },
                    modifier = Modifier.testTag("layout_toggle_feed")
                ) {
                    Icon(
                        imageVector = if (isGridView) Icons.Default.ViewList else Icons.Default.GridView,
                        contentDescription = "Toggle Layout Mode",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                // Notification Bell Icon Button with dynamic unread badge count!
                val unreadCount by viewModel.unreadNotificationsCount.collectAsState()
                IconButton(
                    onClick = onOpenNotifications,
                    modifier = Modifier.testTag("notifications_bell_feed")
                ) {
                    Box {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = stringResource(R.string.tab_notifications),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        if (unreadCount > 0) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .offset(x = 4.dp, y = (-4).dp)
                                    .background(MaterialTheme.colorScheme.error, CircleShape)
                                    .size(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (unreadCount > 9) "9+" else unreadCount.toString(),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onError
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        // Search Input
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.searchQuery.value = it },
            placeholder = {
                Text(
                    text = stringResource(R.string.search_placeholder),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.searchQuery.value = "" }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear search",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .testTag("search_input"),
            shape = CircleShape,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = Color.Transparent,
                errorBorderColor = Color.Transparent
            ),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() })
        )

        // Dynamic Categories Chips
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(categories) { category ->
                val isSelected = category.equals(selectedCategory, ignoreCase = true)
                Surface(
                    onClick = { viewModel.selectedCategory.value = category },
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (isSelected) Color.Transparent else MaterialTheme.colorScheme.outlineVariant
                    ),
                    modifier = Modifier.testTag("category_chip_$category")
                ) {
                    Text(
                        text = if (category.equals("All", ignoreCase = true)) stringResource(R.string.category_all) else category,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Medium
                        ),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
        }

        // Main Books list grid content
        val isRefreshing by viewModel.isRefreshing.collectAsState()

        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.refreshBooks() },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 16.dp)
                .testTag("pull_to_refresh_feed")
        ) {
            when (val state = uiState) {
                is BookUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
                is BookUiState.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp)
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "😞",
                            fontSize = 48.sp,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.fetchBooks() },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text(stringResource(R.string.retry_connection))
                        }
                    }
                }
                is BookUiState.Success -> {
                    if (filteredBooks.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp)
                                .verticalScroll(rememberScrollState()),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "📚",
                                fontSize = 48.sp,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )
                            Text(
                                text = stringResource(R.string.no_books_found),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        if (isGridView) {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(2),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                                contentPadding = PaddingValues(bottom = 24.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(filteredBooks) { book ->
                                    BookGridCard(
                                        book = book,
                                        viewModel = viewModel,
                                        onClick = { onBookSelected(book) }
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                contentPadding = PaddingValues(bottom = 24.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(filteredBooks) { book ->
                                    BookListCard(
                                        book = book,
                                        viewModel = viewModel,
                                        onClick = { onBookSelected(book) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FavoritesTab(
    viewModel: BookViewModel,
    onBookSelected: (Book) -> Unit
) {
    val favoriteBooks by viewModel.favoriteBooks.collectAsState()
    val isDarkThemeOverride by viewModel.isDarkThemeOverride.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.favorites_title),
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = (-0.5).sp
                    )
                )
                Text(
                    text = stringResource(R.string.favorites_subtitle),
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 16.dp)
        ) {
            if (favoriteBooks.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "💖",
                        fontSize = 54.sp,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    Text(
                        text = stringResource(R.string.favorites_empty_title),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.favorites_empty_desc),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(favoriteBooks) { book ->
                        BookGridCard(
                            book = book,
                            viewModel = viewModel,
                            onClick = { onBookSelected(book) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BookGridCard(
    book: Book,
    viewModel: BookViewModel,
    onClick: () -> Unit
) {
    val favoriteBooks by viewModel.favoriteBooks.collectAsState()
    val isFav = favoriteBooks.any { it.title.equals(book.title, ignoreCase = true) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("book_item_${book.title}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            // Book cover container (3:4 aspect ratio)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.75f)
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
            ) {
                if (!book.cover.isNullOrEmpty()) {
                    SubcomposeAsyncImage(
                        model = book.cover,
                        contentDescription = "Cover of ${book.title}",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        loading = {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        },
                        error = {
                            DefaultBookCoverFallback(title = book.title ?: "Medical Book")
                        }
                    )
                } else {
                    DefaultBookCoverFallback(title = book.title ?: "Medical Book")
                }

                // Interactive Favorite toggle heart button
                IconButton(
                    onClick = { viewModel.toggleFavorite(book) },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(36.dp)
                        .background(Color.White.copy(alpha = 0.85f), shape = CircleShape)
                        .testTag("fav_btn_${book.title}")
                ) {
                    Icon(
                        imageVector = if (isFav) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Toggle Favorite",
                        tint = if (isFav) Color.Red else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Title and author details
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Text(
                    text = book.title ?: "Untitled",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = book.author ?: "Unknown Author",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Language Label badge
                val languageStr = if (!book.language.isNullOrEmpty()) book.language else "Reference"
                Box(
                    modifier = Modifier
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = languageStr.uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun BookListCard(
    book: Book,
    viewModel: BookViewModel,
    onClick: () -> Unit
) {
    val favoriteBooks by viewModel.favoriteBooks.collectAsState()
    val isFav = favoriteBooks.any { it.title.equals(book.title, ignoreCase = true) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("book_item_list_${book.title}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Book cover container on the left
            Box(
                modifier = Modifier
                    .width(90.dp)
                    .height(120.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
            ) {
                if (!book.cover.isNullOrEmpty()) {
                    SubcomposeAsyncImage(
                        model = book.cover,
                        contentDescription = "Cover of ${book.title}",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        loading = {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        },
                        error = {
                            DefaultBookCoverFallback(title = book.title ?: "Medical Book")
                        }
                    )
                } else {
                    DefaultBookCoverFallback(title = book.title ?: "Medical Book")
                }
            }

            // Title, Author and Badge metadata on the right
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(12.dp)
                    .align(Alignment.CenterVertically)
            ) {
                Text(
                    text = book.title ?: "Untitled",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = book.author ?: "Unknown Author",
                    style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val languageStr = if (!book.language.isNullOrEmpty()) book.language else "Reference"
                    Box(
                        modifier = Modifier
                            .background(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = languageStr.uppercase(),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                    }

                    // Interactive Favorite toggle heart button
                    IconButton(
                        onClick = { viewModel.toggleFavorite(book) },
                        modifier = Modifier
                            .size(36.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), shape = CircleShape)
                            .testTag("fav_btn_list_${book.title}")
                    ) {
                        Icon(
                            imageVector = if (isFav) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = "Toggle Favorite",
                            tint = if (isFav) Color.Red else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DefaultBookCoverFallback(title: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "📚",
                fontSize = 36.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun BookDetailScreen(
    book: Book,
    viewModel: BookViewModel,
    onBack: () -> Unit,
    onReadOnline: () -> Unit
) {
    val favoriteBooks by viewModel.favoriteBooks.collectAsState()
    val isFav = favoriteBooks.any { it.title.equals(book.title, ignoreCase = true) }
    val downloadedBooks by viewModel.downloadedBooks.collectAsState()
    val downloadedBook = downloadedBooks.find { it.title.equals(book.title, ignoreCase = true) }
    val isDownloaded = downloadedBook != null
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val requestPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            viewModel.downloadBookPdf(book)
        } else {
            Toast.makeText(context, "Permission Denied: Cannot save download without storage access.", Toast.LENGTH_LONG).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        // Back toolbar with Favorite Button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    text = "Book Details",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            IconButton(
                onClick = { viewModel.toggleFavorite(book) },
                modifier = Modifier.testTag("detail_fav_button")
            ) {
                Icon(
                    imageVector = if (isFav) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = "Toggle Favorite",
                    tint = if (isFav) Color.Red else MaterialTheme.colorScheme.primary
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(scrollState)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Book cover card
            Card(
                modifier = Modifier
                    .width(180.dp)
                    .aspectRatio(0.75f),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                ) {
                    if (!book.cover.isNullOrEmpty()) {
                        SubcomposeAsyncImage(
                            model = book.cover,
                            contentDescription = "Cover for ${book.title}",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                            error = {
                                DefaultBookCoverFallback(title = book.title ?: "Medical Book")
                            }
                        )
                    } else {
                        DefaultBookCoverFallback(title = book.title ?: "Medical Book")
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Book credentials
            Text(
                text = book.title ?: "Untitled Book",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = "By ${book.author ?: "Unknown Author"}",
                style = MaterialTheme.typography.titleMedium.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Information grid of badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                DetailInfoItem(
                    label = "Category",
                    value = book.category ?: "Medical"
                )
                DetailInfoItem(
                    label = "Language",
                    value = book.language ?: "English"
                )
                DetailInfoItem(
                    label = "Posted",
                    value = book.timestamp?.split(" ")?.firstOrNull() ?: "Recent"
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Description Header and body
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Description",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (!book.description.isNullOrEmpty()) book.description else "No description available for this medical book.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        lineHeight = 22.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
            }

            Spacer(modifier = Modifier.height(36.dp))

            // Action Buttons
            if (!book.url.isNullOrEmpty()) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Read Online Button (Primary)
                    Button(
                        onClick = onReadOnline,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("read_online_button"),
                        shape = RoundedCornerShape(26.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MenuBook,
                            contentDescription = "Read Online"
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.read_online),
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }

                    // Download / Offline Button
                    if (isDownloaded) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Read Offline Button
                            Button(
                                onClick = {
                                    downloadedBook?.let { viewModel.startReadingBook(it) }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp)
                                    .testTag("read_offline_button"),
                                shape = RoundedCornerShape(26.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MenuBook,
                                    contentDescription = "Read Offline"
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.read_offline_btn),
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }

                            // Re-download Button
                            OutlinedButton(
                                onClick = {
                                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                                        viewModel.downloadBookPdf(book)
                                    } else {
                                        val hasPermission = androidx.core.content.ContextCompat.checkSelfPermission(
                                            context,
                                            android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                                        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                                        if (hasPermission) {
                                            viewModel.downloadBookPdf(book)
                                        } else {
                                            requestPermissionLauncher.launch(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .size(52.dp)
                                    .testTag("re_download_button"),
                                shape = RoundedCornerShape(26.dp),
                                contentPadding = PaddingValues(0.dp),
                                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Re-download",
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    } else {
                        // Download Button (Secondary / Outlined)
                        OutlinedButton(
                            onClick = {
                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                                    viewModel.downloadBookPdf(book)
                                } else {
                                    val hasPermission = androidx.core.content.ContextCompat.checkSelfPermission(
                                        context,
                                        android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                                    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                                    if (hasPermission) {
                                        viewModel.downloadBookPdf(book)
                                    } else {
                                        requestPermissionLauncher.launch(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("download_button"),
                            shape = RoundedCornerShape(26.dp),
                            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = "Download"
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.download_ref_book),
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OnlineReaderScreen(
    book: Book,
    viewModel: BookViewModel,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    var downloadProgress by remember { mutableStateOf(0f) }
    var isLoading by remember { mutableStateOf(true) }
    var hasError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    val rawUrl = book.url
    val title = book.title ?: "Untitled"

    LaunchedEffect(rawUrl) {
        if (rawUrl.isNullOrEmpty()) {
            hasError = true
            errorMessage = "Invalid book URL."
            isLoading = false
            return@LaunchedEffect
        }

        try {
            isLoading = true
            hasError = false
            downloadProgress = 0f

            withContext(Dispatchers.IO) {
                var formattedUrl = rawUrl.trim()
                if (!formattedUrl.startsWith("http://") && !formattedUrl.startsWith("https://")) {
                    formattedUrl = "https://$formattedUrl"
                }

                val urlConnection = java.net.URL(formattedUrl).openConnection() as java.net.HttpURLConnection
                urlConnection.connectTimeout = 20000
                urlConnection.readTimeout = 20000
                urlConnection.connect()

                val responseCode = urlConnection.responseCode
                if (responseCode != java.net.HttpURLConnection.HTTP_OK) {
                    throw Exception("Server returned HTTP $responseCode")
                }

                val fileLength = urlConnection.contentLength
                val inputStream = urlConnection.inputStream

                val sanitizedTitle = title.replace("[^a-zA-Z0-9]".toRegex(), "_")
                val tempFile = java.io.File(context.cacheDir, "${sanitizedTitle}_temp.pdf")
                if (tempFile.exists()) {
                    tempFile.delete()
                }
                val outputStream = java.io.FileOutputStream(tempFile)

                val data = ByteArray(8192) // Larger buffer for faster download
                var total: Long = 0
                var count: Int
                while (inputStream.read(data).also { count = it } != -1) {
                    total += count
                    if (fileLength > 0) {
                        downloadProgress = total.toFloat() / fileLength.toFloat()
                    } else {
                        downloadProgress = -1f // indeterminate
                    }
                    outputStream.write(data, 0, count)
                }

                outputStream.flush()
                outputStream.close()
                inputStream.close()

                val tempDownloadedBook = com.example.data.local.DownloadedBook(
                    title = book.title ?: "Untitled",
                    author = book.author,
                    category = book.category,
                    description = book.description,
                    cover = book.cover,
                    url = book.url,
                    language = book.language,
                    timestamp = book.timestamp,
                    localFilePath = tempFile.absolutePath
                )

                withContext(Dispatchers.Main) {
                    viewModel.startReadingBook(tempDownloadedBook)
                    onClose() // Dismiss OnlineReaderScreen loading overlay as we transition to native reader
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            withContext(Dispatchers.Main) {
                hasError = true
                errorMessage = e.localizedMessage ?: "Failed to retrieve reference book."
                isLoading = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .background(MaterialTheme.colorScheme.background),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (isLoading) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .padding(16.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(100.dp)
                    ) {
                        CircularProgressIndicator(
                            progress = if (downloadProgress >= 0f) downloadProgress else 0f,
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 6.dp,
                            modifier = Modifier.fillMaxSize()
                        )
                        if (downloadProgress >= 0f) {
                            Text(
                                text = "${(downloadProgress * 100).toInt()}%",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.MenuBook,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Text(
                        text = stringResource(R.string.loading_online_reader),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    OutlinedButton(
                        onClick = onClose,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(stringResource(R.string.cancel_btn))
                    }
                }
            }
        } else if (hasError) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .padding(16.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = "Error icon",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(48.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "Error Loading Book",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error),
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = onClose,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(stringResource(R.string.cancel_btn))
                        }
                        
                        Button(
                            onClick = {
                                isLoading = true
                                hasError = false
                                downloadProgress = 0f
                            },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Retry")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DetailInfoItem(
    label: String,
    value: String
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun AboutTab(viewModel: BookViewModel) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val isDarkThemeOverride by viewModel.isDarkThemeOverride.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .statusBarsPadding()
            .padding(20.dp)
    ) {
        // App Identity Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(18.dp))
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_launcher_background),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize()
                )
                Image(
                    painter = painterResource(id = R.drawable.ic_launcher_foreground),
                    contentDescription = "App Icon",
                    modifier = Modifier.fillMaxSize()
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            )
            Text(
                text = "Version 1.1.0",
                style = MaterialTheme.typography.labelMedium.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Appearance Mode Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = stringResource(R.string.app_theme),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.theme_sub),
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val states = listOf(
                        Triple(null, stringResource(R.string.theme_system), Icons.Default.BrightnessAuto),
                        Triple(false, stringResource(R.string.theme_light), Icons.Default.LightMode),
                        Triple(true, stringResource(R.string.theme_dark), Icons.Default.DarkMode)
                    )

                    states.forEach { (value, label, icon) ->
                        val isSelected = isDarkThemeOverride == value
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .clickable { viewModel.isDarkThemeOverride.value = value }
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    shape = RoundedCornerShape(10.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                             ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = label,
                                    tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Color Palette Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = stringResource(R.string.theme_style_title),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.theme_style_sub),
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
                Spacer(modifier = Modifier.height(16.dp))

                val currentThemeStyle by viewModel.selectedThemeStyle.collectAsState()

                val styleOptions = listOf(
                    Triple("purple", stringResource(R.string.theme_style_purple), Color(0xFF6750A4)),
                    Triple("blue", stringResource(R.string.theme_style_blue), Color(0xFF0061A4)),
                    Triple("green", stringResource(R.string.theme_style_green), Color(0xFF006D40)),
                    Triple("orange", stringResource(R.string.theme_style_orange), Color(0xFF8B5000)),
                    Triple("red", stringResource(R.string.theme_style_red), Color(0xFFBA1A1A))
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    styleOptions.forEach { (styleKey, styleLabel, styleColor) ->
                        val isSelected = currentThemeStyle == styleKey
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .clickable { viewModel.setThemeStyle(styleKey) }
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .testTag("theme_style_btn_$styleKey"),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .background(styleColor, shape = CircleShape)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = styleLabel,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Language Select Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = stringResource(R.string.app_language),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.lang_sub),
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
                Spacer(modifier = Modifier.height(16.dp))

                val currentLang by viewModel.languageCode.collectAsState()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val languages = listOf(
                        "en" to stringResource(R.string.lang_en),
                        "ku" to stringResource(R.string.lang_ku)
                    )

                    languages.forEach { (code, label) ->
                        val isSelected = currentLang == code
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .clickable { viewModel.setLanguage(code) }
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .testTag("lang_btn_$code"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // App Mission Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = stringResource(R.string.about_title),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.about_desc),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        lineHeight = 22.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Developer Profile Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .background(MaterialTheme.colorScheme.primary, shape = CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "👨‍💻",
                            fontSize = 24.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "Muhammad Bakr Hasan",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        )
                        Text(
                            text = "Medical Laboratory Technologist & Developer",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.12f))
                Spacer(modifier = Modifier.height(16.dp))

                // Academic Information Section
                Text(
                    text = stringResource(R.string.academic_background_title),
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.academic_background_desc),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Quick Email Action Button
                Button(
                    onClick = {
                        val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("mailto:logicgram2019@gmail.com")
                            putExtra(Intent.EXTRA_SUBJECT, "Inquiry about Logic Med Library")
                        }
                        try {
                            context.startActivity(emailIntent)
                        } catch (e: Exception) {
                            Toast.makeText(context, context.getString(R.string.email_client_error), Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("email_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(
                        imageVector = Icons.Default.Email,
                        contentDescription = "Email developer"
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.contact_developer),
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun GeometricBottomNav(
    currentTab: String,
    onTabSelected: (String) -> Unit
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 8.dp,
        modifier = Modifier.height(80.dp)
    ) {
        NavigationBarItem(
            selected = currentTab == "books",
            onClick = { onTabSelected("books") },
            icon = {
                Icon(
                    imageVector = if (currentTab == "books") Icons.AutoMirrored.Filled.MenuBook else Icons.AutoMirrored.Outlined.MenuBook,
                    contentDescription = stringResource(R.string.tab_books)
                )
            },
            label = {
                Text(
                    text = stringResource(R.string.tab_books),
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = if (currentTab == "books") FontWeight.Bold else FontWeight.Normal
                    )
                )
            },
            modifier = Modifier.testTag("tab_books")
        )

        NavigationBarItem(
            selected = currentTab == "favorites",
            onClick = { onTabSelected("favorites") },
            icon = {
                Icon(
                    imageVector = if (currentTab == "favorites") Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = stringResource(R.string.tab_favorites)
                )
            },
            label = {
                Text(
                    text = stringResource(R.string.tab_favorites),
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = if (currentTab == "favorites") FontWeight.Bold else FontWeight.Normal
                    )
                )
            },
            modifier = Modifier.testTag("tab_favorites")
        )

        NavigationBarItem(
            selected = currentTab == "offline",
            onClick = { onTabSelected("offline") },
            icon = {
                Icon(
                    imageVector = if (currentTab == "offline") Icons.Default.CloudDownload else Icons.Default.CloudDownload,
                    contentDescription = stringResource(R.string.tab_offline)
                )
            },
            label = {
                Text(
                    text = stringResource(R.string.tab_offline),
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = if (currentTab == "offline") FontWeight.Bold else FontWeight.Normal
                    )
                )
            },
            modifier = Modifier.testTag("tab_offline")
        )

        NavigationBarItem(
            selected = currentTab == "about",
            onClick = { onTabSelected("about") },
            icon = {
                Icon(
                    imageVector = if (currentTab == "about") Icons.Filled.Info else Icons.Outlined.Info,
                    contentDescription = stringResource(R.string.tab_about)
                )
            },
            label = {
                Text(
                    text = stringResource(R.string.tab_about),
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = if (currentTab == "about") FontWeight.Bold else FontWeight.Normal
                    )
                )
            },
            modifier = Modifier.testTag("tab_about")
        )
    }
}

@Composable
fun DownloadProgressDialog(
    viewModel: BookViewModel
) {
    val downloadState by viewModel.downloadState.collectAsState()
    val context = LocalContext.current

    if (downloadState != DownloadState.Idle) {
        AlertDialog(
            onDismissRequest = {
                if (downloadState is DownloadState.Success || downloadState is DownloadState.Error) {
                    viewModel.clearDownloadState()
                }
            },
            title = {
                Text(
                    text = when (downloadState) {
                        is DownloadState.Downloading -> "Downloading PDF"
                        is DownloadState.Success -> "Download Finished"
                        is DownloadState.Error -> "Download Failed"
                        else -> ""
                    },
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    when (val state = downloadState) {
                        is DownloadState.Downloading -> {
                            Spacer(modifier = Modifier.height(8.dp))
                            if (state.progress >= 0) {
                                LinearProgressIndicator(
                                    progress = { state.progress / 100f },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "${state.progress}% downloaded",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            } else {
                                LinearProgressIndicator(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Downloading reference book...",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }
                        is DownloadState.Success -> {
                            Text(
                                text = "The PDF \"${state.book.title ?: "Untitled"}\" was successfully downloaded to your Downloads folder and is ready for offline reading.",
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        is DownloadState.Error -> {
                            Text(
                                text = state.message,
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        else -> {}
                    }
                }
            },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    when (val state = downloadState) {
                        is DownloadState.Success -> {
                            TextButton(
                                onClick = { viewModel.clearDownloadState() }
                            ) {
                                Text("Close")
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            TextButton(
                                onClick = {
                                    val book = state.book
                                    // Instantly trigger re-download of this book
                                    viewModel.downloadBookPdf(book)
                                }
                            ) {
                                Text("Re-download")
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    val path = state.filePath
                                    viewModel.clearDownloadState()
                                    val downloadedBook = com.example.data.local.DownloadedBook(
                                        title = state.book.title ?: "Untitled",
                                        author = state.book.author,
                                        category = state.book.category,
                                        description = state.book.description,
                                        cover = state.book.cover,
                                        url = state.book.url,
                                        language = state.book.language,
                                        timestamp = state.book.timestamp,
                                        localFilePath = path
                                    )
                                    viewModel.startReadingBook(downloadedBook)
                                }
                            ) {
                                Text("Open Book")
                            }
                        }
                        is DownloadState.Error -> {
                            Button(
                                onClick = { viewModel.clearDownloadState() }
                            ) {
                                Text("Dismiss")
                            }
                        }
                        else -> {
                            TextButton(
                                onClick = { viewModel.clearDownloadState() }
                            ) {
                                Text("Cancel")
                            }
                        }
                    }
                }
            }
        )
    }
}

@Composable
fun OfflineTab(
    viewModel: BookViewModel,
    onBookSelected: (Book) -> Unit,
    onNavigateToFeed: () -> Unit
) {
    val downloadedBooks by viewModel.downloadedBooks.collectAsState()
    val context = LocalContext.current
    val isDarkThemeOverride by viewModel.isDarkThemeOverride.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.offline_title),
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = (-0.5).sp
                    )
                )
                Text(
                    text = stringResource(R.string.offline_subtitle),
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 16.dp)
        ) {
            if (downloadedBooks.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "📥",
                        fontSize = 54.sp,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    Text(
                        text = stringResource(R.string.offline_empty_title),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.offline_empty_desc),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = onNavigateToFeed,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Search, contentDescription = stringResource(R.string.browse_books_btn))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.browse_books_btn))
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(downloadedBooks) { downloaded ->
                        val book = Book(
                            title = downloaded.title,
                            author = downloaded.author,
                            category = downloaded.category,
                            description = downloaded.description,
                            cover = downloaded.cover,
                            url = downloaded.url,
                            language = downloaded.language,
                            timestamp = downloaded.timestamp
                        )
                        OfflineBookRow(
                            downloaded = downloaded,
                            book = book,
                            onClick = { viewModel.startReadingBook(downloaded) },
                            onOpenPdf = { viewModel.startReadingBook(downloaded) },
                            onDelete = {
                                viewModel.removeDownloadedBookByTitle(downloaded.title)
                                Toast.makeText(context, "\"${downloaded.title}\" removed from offline books.", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun OfflineBookRow(
    downloaded: com.example.data.local.DownloadedBook,
    book: Book,
    onClick: () -> Unit,
    onOpenPdf: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("offline_book_card_${downloaded.title}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Book cover (Aspect ratio 3:4)
            Box(
                modifier = Modifier
                    .size(width = 75.dp, height = 100.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
            ) {
                if (!downloaded.cover.isNullOrEmpty()) {
                    SubcomposeAsyncImage(
                        model = downloaded.cover,
                        contentDescription = "Cover of ${downloaded.title}",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        loading = {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        },
                        error = {
                            DefaultBookCoverFallback(title = downloaded.title)
                        }
                    )
                } else {
                    DefaultBookCoverFallback(title = downloaded.title)
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Text Info & Actions
            Column(
                modifier = Modifier.weight(1f)
            ) {
                val categoryStr = if (!downloaded.category.isNullOrEmpty()) downloaded.category else "General"
                Text(
                    text = categoryStr.uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = downloaded.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = downloaded.author ?: "Unknown Author",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onOpenPdf,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.height(32.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.MenuBook,
                            contentDescription = "Open PDF",
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Open PDF",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Offline Download",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PdfReaderScreen(
    downloadedBook: com.example.data.local.DownloadedBook,
    viewModel: BookViewModel,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    var pdfRenderer by remember { mutableStateOf<PdfRenderer?>(null) }
    var pageCount by remember { mutableStateOf(0) }
    var pdfError by remember { mutableStateOf<String?>(null) }

    // Read preferences
    val savedPage = remember(downloadedBook.title) { viewModel.getReadingProgress(downloadedBook.title) }
    var currentPageIndex by remember { mutableStateOf(savedPage) }
    
    // Theme options: "light", "dark", "sepia"
    var readingTheme by remember { mutableStateOf("light") }
    // Rotation options: 0, 90, 180, 270 degrees
    var rotationDegrees by remember { mutableStateOf(0) }
    
    var isFullscreen by remember { mutableStateOf(false) }
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }

    // Dialog for bookmark list
    var showBookmarksDialog by remember { mutableStateOf(false) }
    // Text field input for direct page jump
    var pageInputText by remember { mutableStateOf("") }
    
    // Load PDF
    LaunchedEffect(downloadedBook.localFilePath) {
        try {
            val fileDescriptor = if (downloadedBook.localFilePath.startsWith("content://")) {
                context.contentResolver.openFileDescriptor(Uri.parse(downloadedBook.localFilePath), "r")
            } else {
                ParcelFileDescriptor.open(java.io.File(downloadedBook.localFilePath), ParcelFileDescriptor.MODE_READ_ONLY)
            }
            if (fileDescriptor != null) {
                val renderer = PdfRenderer(fileDescriptor)
                pdfRenderer = renderer
                pageCount = renderer.pageCount
                // Bounds safety check
                if (currentPageIndex < 0 || currentPageIndex >= pageCount) {
                    currentPageIndex = 0
                }
            } else {
                pdfError = "Unable to open PDF file source descriptor."
            }
        } catch (e: Exception) {
            e.printStackTrace()
            pdfError = "Could not open offline PDF: ${e.localizedMessage}"
        }
    }

    // Dispose
    DisposableEffect(downloadedBook.localFilePath) {
        onDispose {
            try {
                pdfRenderer?.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Keep Page rendering in a LaunchedEffect
    var pageBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isRendering by remember { mutableStateOf(false) }
    var renderingError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(currentPageIndex, pdfRenderer) {
        val renderer = pdfRenderer ?: return@LaunchedEffect
        if (currentPageIndex < 0 || currentPageIndex >= pageCount) return@LaunchedEffect

        isRendering = true
        renderingError = null
        kotlinx.coroutines.withContext(Dispatchers.IO) {
            try {
                val page = renderer.openPage(currentPageIndex)
                
                // Render at high resolution (e.g. 2.5x original size for great crispness)
                val targetWidth = (page.width * 2.5f).toInt()
                val targetHeight = (page.height * 2.5f).toInt()
                
                val bitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()

                kotlinx.coroutines.withContext(Dispatchers.Main) {
                    pageBitmap?.recycle()
                    pageBitmap = bitmap
                    isRendering = false
                    viewModel.saveReadingProgress(downloadedBook.title, currentPageIndex)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                kotlinx.coroutines.withContext(Dispatchers.Main) {
                    renderingError = "Failed to render PDF page: ${e.localizedMessage}"
                    isRendering = false
                }
            }
        }
    }

    // Theme Colors
    val readerBgColor = when (readingTheme) {
        "dark" -> Color(0xFF121212)
        "sepia" -> Color(0xFFF4ECD8)
        else -> Color(0xFFFFFFFF)
    }
    val readerTextColor = when (readingTheme) {
        "dark" -> Color(0xFFE0E0E0)
        "sepia" -> Color(0xFF433422)
        else -> Color(0xFF1E1E1E)
    }

    // Color Filters for the PDF Pages
    val invertMatrix = ColorMatrix(floatArrayOf(
        -1.0f,  0.0f,  0.0f, 0.0f, 255.0f,
         0.0f, -1.0f,  0.0f, 0.0f, 255.0f,
         0.0f,  0.0f, -1.0f, 0.0f, 255.0f,
         0.0f,  0.0f,  0.0f, 1.0f,   0.0f
    ))
    
    val sepiaMatrix = ColorMatrix(floatArrayOf(
        0.393f, 0.769f, 0.189f, 0f, 0f,
        0.349f, 0.686f, 0.168f, 0f, 0f,
        0.272f, 0.534f, 0.131f, 0f, 0f,
        0f,     0f,     0f,     1f, 0f
    ))

    val colorFilter = when (readingTheme) {
        "dark" -> ColorFilter.colorMatrix(invertMatrix)
        "sepia" -> ColorFilter.colorMatrix(sepiaMatrix)
        else -> null
    }

    val state = rememberTransformableState { zoomChange, offsetChange, _ ->
        scale = (scale * zoomChange).coerceIn(1f, 5f)
        offset = if (scale > 1f) offset + offsetChange else androidx.compose.ui.geometry.Offset.Zero
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(readerBgColor)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Main content (PDF page)
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (pdfError != null) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(imageVector = Icons.Default.Error, contentDescription = "Error", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = pdfError!!, color = readerTextColor, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = onClose) {
                        Text("Go Back")
                    }
                }
            } else if (pdfRenderer == null || isRendering && pageBitmap == null) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = if (readingTheme == "dark") Color.White else MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Loading Offline Book Pages...", color = readerTextColor, style = MaterialTheme.typography.bodyMedium)
                }
            } else {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onDoubleTap = {
                                    if (scale > 1f) {
                                        scale = 1f
                                        offset = androidx.compose.ui.geometry.Offset.Zero
                                    } else {
                                        scale = 2.5f
                                    }
                                },
                                onTap = {
                                    isFullscreen = !isFullscreen
                                }
                            )
                        }
                        .transformable(state = state)
                        .clip(RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (pageBitmap != null) {
                        androidx.compose.foundation.Image(
                            bitmap = pageBitmap!!.asImageBitmap(),
                            contentDescription = "PDF Page ${currentPageIndex + 1}",
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer(
                                    scaleX = scale,
                                    scaleY = scale,
                                    translationX = offset.x,
                                    translationY = offset.y,
                                    rotationZ = rotationDegrees.toFloat()
                                ),
                            contentScale = ContentScale.Fit,
                            colorFilter = colorFilter
                        )
                    }

                    if (isRendering) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.3f)),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = Color.White)
                        }
                    }

                    if (renderingError != null) {
                        Text(
                            text = renderingError!!,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(16.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        // Top Toolbar
        AnimatedVisibility(
            visible = !isFullscreen,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (readingTheme == "dark") Color(0xFF1E1E1E) else MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onClose) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Close PDF Reader",
                            tint = if (readingTheme == "dark") Color.White else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Column(modifier = Modifier.weight(1f).padding(horizontal = 8.dp)) {
                        Text(
                            text = downloadedBook.title,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (readingTheme == "dark") Color.White else MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = downloadedBook.author ?: "Unknown Author",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (readingTheme == "dark") Color.LightGray else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Bookmarks List button
                    IconButton(onClick = { showBookmarksDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Bookmarks,
                            contentDescription = "View Bookmarks",
                            tint = if (readingTheme == "dark") Color.White else MaterialTheme.colorScheme.primary
                        )
                    }

                    // Bookmark toggle
                    val isPageBookmarked = viewModel.isBookmarked(downloadedBook.title, currentPageIndex)
                    IconButton(
                        onClick = {
                            if (isPageBookmarked) {
                                viewModel.removeBookmark(downloadedBook.title, currentPageIndex)
                            } else {
                                viewModel.addBookmark(downloadedBook.title, currentPageIndex)
                            }
                        }
                    ) {
                        Icon(
                            imageVector = if (isPageBookmarked) Icons.Default.Bookmark else Icons.Outlined.Bookmark,
                            contentDescription = "Bookmark this page",
                            tint = if (isPageBookmarked) MaterialTheme.colorScheme.error else (if (readingTheme == "dark") Color.White else MaterialTheme.colorScheme.onSurface)
                        )
                    }

                    // Theme selector button
                    IconButton(
                        onClick = {
                            readingTheme = when (readingTheme) {
                                "light" -> "sepia"
                                "sepia" -> "dark"
                                else -> "light"
                            }
                        }
                    ) {
                        Icon(
                            imageVector = when (readingTheme) {
                                "sepia" -> Icons.Default.WbSunny
                                "dark" -> Icons.Default.NightsStay
                                else -> Icons.AutoMirrored.Outlined.MenuBook
                            },
                            contentDescription = "Toggle Reading Theme",
                            tint = if (readingTheme == "dark") Color.Yellow else MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // Bottom Controls
        AnimatedVisibility(
            visible = !isFullscreen,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (readingTheme == "dark") Color(0xFF1E1E1E) else MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                ) {
                    // Page scrubber Slider
                    if (pageCount > 1) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "1",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (readingTheme == "dark") Color.LightGray else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Slider(
                                value = currentPageIndex.toFloat(),
                                onValueChange = { currentPageIndex = it.toInt().coerceIn(0, pageCount - 1) },
                                valueRange = 0f..(pageCount - 1).toFloat(),
                                steps = if (pageCount > 2) pageCount - 2 else 0,
                                modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                                colors = SliderDefaults.colors(
                                    thumbColor = MaterialTheme.colorScheme.primary,
                                    activeTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                                )
                            )
                            Text(
                                text = pageCount.toString(),
                                style = MaterialTheme.typography.bodySmall,
                                color = if (readingTheme == "dark") Color.LightGray else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left navigation buttons & Rotation
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = {
                                    if (currentPageIndex > 0) currentPageIndex--
                                },
                                enabled = currentPageIndex > 0
                            ) {
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowLeft,
                                    contentDescription = "Previous Page",
                                    tint = if (readingTheme == "dark") Color.White else MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Text(
                                text = "Page ${currentPageIndex + 1} of $pageCount",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (readingTheme == "dark") Color.White else MaterialTheme.colorScheme.onSurface
                            )

                            IconButton(
                                onClick = {
                                    if (currentPageIndex < pageCount - 1) currentPageIndex++
                                },
                                enabled = currentPageIndex < pageCount - 1
                            ) {
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowRight,
                                    contentDescription = "Next Page",
                                    tint = if (readingTheme == "dark") Color.White else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        // Right utility actions: Rotate, Zoom Reset, Direct Input Jump
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Rotate Page button
                            IconButton(
                                onClick = {
                                    rotationDegrees = (rotationDegrees + 90) % 360
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.RotateRight,
                                    contentDescription = "Rotate Page",
                                    tint = if (readingTheme == "dark") Color.White else MaterialTheme.colorScheme.onSurface
                                )
                            }

                            // Reset zoom button
                            if (scale > 1f) {
                                IconButton(
                                    onClick = {
                                        scale = 1f
                                        offset = androidx.compose.ui.geometry.Offset.Zero
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ZoomOut,
                                        contentDescription = "Reset Zoom",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            // Jump text input
                            OutlinedTextField(
                                value = pageInputText,
                                onValueChange = { pageInputText = it.filter { char -> char.isDigit() } },
                                modifier = Modifier
                                    .width(70.dp)
                                    .height(52.dp),
                                placeholder = { Text("#", fontSize = 11.sp) },
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Number,
                                    imeAction = ImeAction.Go
                                ),
                                keyboardActions = KeyboardActions(
                                    onGo = {
                                        val entered = pageInputText.toIntOrNull()
                                        if (entered != null && entered in 1..pageCount) {
                                            currentPageIndex = entered - 1
                                            pageInputText = ""
                                        }
                                    }
                                ),
                                textStyle = MaterialTheme.typography.bodySmall.copy(
                                    textAlign = TextAlign.Center,
                                    color = if (readingTheme == "dark") Color.White else MaterialTheme.colorScheme.onSurface
                                ),
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // Bookmarks Dialog
    if (showBookmarksDialog) {
        val bookmarks = viewModel.getBookmarks(downloadedBook.title)
            .mapNotNull { it.toIntOrNull() }
            .sorted()

        AlertDialog(
            onDismissRequest = { showBookmarksDialog = false },
            title = {
                Text(
                    text = "Bookmarked Pages",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                if (bookmarks.isEmpty()) {
                    Text(
                        text = "You haven't bookmarked any pages yet. Click the bookmark icon in the top bar to save pages.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 300.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(bookmarks) { bPageIndex ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        currentPageIndex = bPageIndex
                                        showBookmarksDialog = false
                                    },
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Bookmark,
                                            contentDescription = "Bookmark",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Page ${bPageIndex + 1}",
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                                        )
                                    }

                                    IconButton(
                                        onClick = {
                                            viewModel.removeBookmark(downloadedBook.title, bPageIndex)
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete bookmark",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showBookmarksDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    viewModel: BookViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val notifications by viewModel.notifications.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.notifications_title),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = stringResource(R.string.notifications_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("notifications_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    if (notifications.isNotEmpty()) {
                        IconButton(
                            onClick = { viewModel.markAllNotificationsAsRead() },
                            modifier = Modifier.testTag("notifications_mark_all_read")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = stringResource(R.string.mark_all_read),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(
                            onClick = { viewModel.clearAllNotifications() },
                            modifier = Modifier.testTag("notifications_clear_all")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = stringResource(R.string.clear_all),
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        if (notifications.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .background(
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = stringResource(R.string.notifications_empty_title),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.notifications_empty_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.widthIn(max = 320.dp)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(notifications, key = { it.id }) { item ->
                    NotificationCard(
                        notification = item,
                        onMarkAsRead = { viewModel.markNotificationAsRead(item.id) },
                        onDelete = {
                            viewModel.deleteNotification(item.id)
                            Toast.makeText(context, context.getString(R.string.notification_deleted), Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun NotificationCard(
    notification: com.example.data.local.Notification,
    onMarkAsRead: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isRead = notification.isRead
    val cardColor = if (isRead) {
        MaterialTheme.colorScheme.surface
    } else {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
    }
    val borderColor = if (isRead) {
        MaterialTheme.colorScheme.outlineVariant
    } else {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
    }

    val timeString = remember(notification.timestamp) {
        val sdf = java.text.SimpleDateFormat("MMM dd, yyyy - hh:mm a", java.util.Locale.getDefault())
        sdf.format(java.util.Date(notification.timestamp))
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("notification_card_${notification.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Unread indicator or category icon
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = if (isRead) {
                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                        } else {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                        },
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (notification.category.lowercase()) {
                        "system" -> Icons.Default.Settings
                        "updates" -> Icons.Default.Refresh
                        "tutorial" -> Icons.AutoMirrored.Filled.MenuBook
                        else -> Icons.Default.Notifications
                    },
                    contentDescription = notification.category,
                    modifier = Modifier.size(20.dp),
                    tint = if (isRead) {
                        MaterialTheme.colorScheme.onSecondaryContainer
                    } else {
                        MaterialTheme.colorScheme.primary
                    }
                )
            }

            // Notification content
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Category Badge
                    Surface(
                        color = when (notification.category.lowercase()) {
                            "system" -> MaterialTheme.colorScheme.tertiaryContainer
                            "updates" -> MaterialTheme.colorScheme.errorContainer
                            "tutorial" -> MaterialTheme.colorScheme.secondaryContainer
                            else -> MaterialTheme.colorScheme.primaryContainer
                        },
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = notification.category,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            color = when (notification.category.lowercase()) {
                                "system" -> MaterialTheme.colorScheme.onTertiaryContainer
                                "updates" -> MaterialTheme.colorScheme.onErrorContainer
                                "tutorial" -> MaterialTheme.colorScheme.onSecondaryContainer
                                else -> MaterialTheme.colorScheme.onPrimaryContainer
                            }
                        )
                    }

                    // Timestamp
                    Text(
                        text = timeString,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Title
                Text(
                    text = notification.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = if (isRead) FontWeight.Medium else FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Message
                Text(
                    text = notification.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (!isRead) {
                    Spacer(modifier = Modifier.height(12.dp))
                    TextButton(
                        onClick = onMarkAsRead,
                        modifier = Modifier
                            .align(Alignment.End)
                            .testTag("mark_read_btn_${notification.id}"),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.mark_as_read),
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }

            // Individual Delete Button
            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .align(Alignment.Top)
                    .size(32.dp)
                    .testTag("delete_notification_btn_${notification.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.Clear,
                    contentDescription = "Delete",
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }
    }
}
