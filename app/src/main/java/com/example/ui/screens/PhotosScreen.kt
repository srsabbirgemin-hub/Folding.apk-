package com.example.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.model.MediaItem
import com.example.utils.DragDropUtils
import com.example.utils.FileUtils
import com.example.utils.MediaRepository
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotosScreen(
    onNavigateBack: (() -> Unit)? = null,
    onExternalDragStart: () -> Unit = {},
    onExternalDragEnd: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val view = LocalView.current
    var photos by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedAlbum by remember { mutableStateOf("All") }
    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        isLoading = true
        photos = MediaRepository.loadPhotos(context)
        isLoading = false
    }

    val albums = remember(photos) {
        listOf("All") + photos.map { it.bucketName }.filter { it.isNotBlank() }.distinct().sorted()
    }

    val filteredPhotos = remember(photos, selectedAlbum) {
        if (selectedAlbum == "All") photos
        else photos.filter { it.bucketName == selectedAlbum }
    }

    if (selectedIndex != null && filteredPhotos.isNotEmpty()) {
        val safeIndex = selectedIndex!!.coerceIn(0, filteredPhotos.size - 1)
        PhotoDetailViewer(
            photos = filteredPhotos,
            initialIndex = safeIndex,
            onClose = { selectedIndex = null },
            onExternalDragStart = onExternalDragStart,
            onExternalDragEnd = onExternalDragEnd
        )
    } else {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Album Chips Filter Bar
            if (albums.size > 1) {
                ScrollableTabRow(
                    selectedTabIndex = albums.indexOf(selectedAlbum).coerceAtLeast(0),
                    edgePadding = 8.dp,
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    contentColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxWidth().height(42.dp)
                ) {
                    albums.forEach { album ->
                        Tab(
                            selected = album == selectedAlbum,
                            onClick = { selectedAlbum = album },
                            text = {
                                Text(
                                    text = album,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        )
                    }
                }
            }

            Box(modifier = Modifier.fillMaxSize()) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                } else if (filteredPhotos.isEmpty()) {
                    Column(
                        modifier = Modifier.align(Alignment.Center).padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoLibrary,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "কোনো ছবি পাওয়া যায়নি",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        FilledTonalButton(
                            onClick = {
                                scope.launch {
                                    isLoading = true
                                    photos = MediaRepository.loadPhotos(context)
                                    isLoading = false
                                }
                            }
                        ) {
                            Text("রিফ্রেশ করুন")
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 90.dp),
                        contentPadding = PaddingValues(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        itemsIndexed(filteredPhotos, key = { _, item -> item.id }) { index, item ->
                            PhotoGridItem(
                                item = item,
                                onClick = { selectedIndex = index },
                                onLongClick = {
                                    val file = File(item.path)
                                    if (file.exists()) {
                                        DragDropUtils.startGlobalDrag(
                                            view = view,
                                            files = listOf(file),
                                            onDragStart = onExternalDragStart,
                                            onDragEnd = { success ->
                                                onExternalDragEnd(success)
                                                if (success) {
                                                    Toast.makeText(context, "শেয়ার সম্পন্ন!", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        )
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PhotoGridItem(
    item: MediaItem,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val context = LocalContext.current
    var showMenu by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(6.dp),
        modifier = Modifier
            .aspectRatio(1f)
            .combinedClickable(
                onClick = onClick,
                onLongClick = {
                    onLongClick()
                }
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(item.uri)
                    .size(250, 250)
                    .allowHardware(true)
                    .crossfade(150)
                    .build(),
                contentDescription = item.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Small 3-dot fallback menu at corner
            IconButton(
                onClick = { showMenu = true },
                modifier = Modifier
                    .size(24.dp)
                    .align(Alignment.TopEnd)
                    .padding(2.dp)
                    .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Options",
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }

            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text("শেয়ার (Share)") },
                    leadingIcon = { Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    onClick = {
                        showMenu = false
                        val file = File(item.path)
                        if (file.exists()) FileUtils.shareFile(context, file)
                    }
                )
                DropdownMenuItem(
                    text = { Text("কপি (Copy)") },
                    leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    onClick = {
                        showMenu = false
                        val file = File(item.path)
                        if (file.exists()) {
                            FileUtils.copyToClipboard(context, file)
                            Toast.makeText(context, "ক্লিপবোর্ডে কপি করা হয়েছে", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PhotoDetailViewer(
    photos: List<MediaItem>,
    initialIndex: Int,
    onClose: () -> Unit,
    onExternalDragStart: () -> Unit = {},
    onExternalDragEnd: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    val view = LocalView.current
    val pagerState = rememberPagerState(initialPage = initialIndex, pageCount = { photos.size })
    var showInfo by remember { mutableStateOf(false) }

    BackHandler {
        onClose()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val photo = photos[page]
            ZoomableImage(
                uri = photo.uri,
                name = photo.name,
                onLongClick = {
                    val file = File(photo.path)
                    if (file.exists()) {
                        DragDropUtils.startGlobalDrag(
                            view = view,
                            files = listOf(file),
                            onDragStart = onExternalDragStart,
                            onDragEnd = onExternalDragEnd
                        )
                    }
                }
            )
        }

        // Top Toolbar Overlay
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.6f))
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .align(Alignment.TopCenter),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            Text(
                text = "${pagerState.currentPage + 1} / ${photos.size}",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
            val currentPhoto = photos.getOrNull(pagerState.currentPage)
            if (currentPhoto != null) {
                // Drag button
                IconButton(onClick = {
                    val file = File(currentPhoto.path)
                    if (file.exists()) {
                        DragDropUtils.startGlobalDrag(
                            view = view,
                            files = listOf(file),
                            onDragStart = onExternalDragStart,
                            onDragEnd = onExternalDragEnd
                        )
                    }
                }) {
                    Icon(
                        imageVector = Icons.Default.DragIndicator,
                        contentDescription = "Drag to other app",
                        tint = Color.White
                    )
                }
                IconButton(onClick = {
                    val file = File(currentPhoto.path)
                    if (file.exists()) {
                        FileUtils.shareFile(context, file)
                    }
                }) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share",
                        tint = Color.White
                    )
                }
                IconButton(onClick = {
                    val file = File(currentPhoto.path)
                    if (file.exists()) {
                        FileUtils.copyToClipboard(context, file)
                        Toast.makeText(context, "কপি করা হয়েছে", Toast.LENGTH_SHORT).show()
                    }
                }) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy",
                        tint = Color.White
                    )
                }
                IconButton(onClick = { showInfo = !showInfo }) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Info",
                        tint = Color.White
                    )
                }
            }
        }

        // Bottom Info Card
        val current = photos.getOrNull(pagerState.currentPage)
        if (showInfo && current != null) {
            Surface(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = current.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "আকার: ${FileUtils.formatFileSize(current.size)}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "তারিখ: ${FileUtils.formatDate(current.dateModified)}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    if (current.path.isNotEmpty()) {
                        Text(
                            text = "পাথ: ${current.path}",
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ZoomableImage(
    uri: Uri,
    name: String,
    onLongClick: () -> Unit = {}
) {
    val context = LocalContext.current
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .combinedClickable(
                onClick = {},
                onLongClick = onLongClick
            )
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(1f, 5f)
                    if (scale > 1f) {
                        val maxPanX = (size.width * (scale - 1)) / 2
                        val maxPanY = (size.height * (scale - 1)) / 2
                        offsetX = (offsetX + pan.x).coerceIn(-maxPanX, maxPanX)
                        offsetY = (offsetY + pan.y).coerceIn(-maxPanY, maxPanY)
                    } else {
                        offsetX = 0f
                        offsetY = 0f
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(uri)
                .crossfade(true)
                .build(),
            contentDescription = name,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offsetX,
                    translationY = offsetY
                )
        )
    }
}
