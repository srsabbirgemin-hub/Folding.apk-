package com.example.ui.floating

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.MediaItem
import com.example.ui.screens.FileManagerScreen
import com.example.ui.screens.PhotoDetailViewer
import com.example.ui.screens.PhotosScreen
import com.example.ui.screens.VideoPlayerView
import com.example.ui.screens.VideosScreen

enum class FloatingTab {
    PHOTOS,
    VIDEOS,
    FILES
}

@Composable
fun FloatingWindowRoot(
    isMinimized: Boolean,
    isFullScreen: Boolean,
    onDrag: (dx: Float, dy: Float) -> Unit,
    onResize: (dw: Float, dh: Float) -> Unit,
    onToggleMinimize: () -> Unit,
    onToggleFullScreen: () -> Unit,
    onClose: () -> Unit,
    onExternalDragStart: () -> Unit = {},
    onExternalDragEnd: (Boolean) -> Unit = {}
) {
    if (isMinimized) {
        // Floating Bubble mode
        FloatingBubbleView(
            onDrag = onDrag,
            onClick = onToggleMinimize
        )
    } else {
        // Floating Window mode
        FloatingWindowContent(
            isFullScreen = isFullScreen,
            onDrag = onDrag,
            onResize = onResize,
            onToggleMinimize = onToggleMinimize,
            onToggleFullScreen = onToggleFullScreen,
            onClose = onClose,
            onExternalDragStart = onExternalDragStart,
            onExternalDragEnd = onExternalDragEnd
        )
    }
}

@Composable
fun FloatingBubbleView(
    onDrag: (dx: Float, dy: Float) -> Unit,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(60.dp)
            .shadow(10.dp, CircleShape)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary)
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    onDrag(dragAmount.x, dragAmount.y)
                }
            }
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Layers,
            contentDescription = "Expand Floating Window",
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(32.dp)
        )
    }
}

@Composable
fun FloatingWindowContent(
    isFullScreen: Boolean,
    onDrag: (dx: Float, dy: Float) -> Unit,
    onResize: (dw: Float, dh: Float) -> Unit,
    onToggleMinimize: () -> Unit,
    onToggleFullScreen: () -> Unit,
    onClose: () -> Unit,
    onExternalDragStart: () -> Unit,
    onExternalDragEnd: (Boolean) -> Unit
) {
    var selectedTab by remember { mutableStateOf(FloatingTab.FILES) }
    var directPhotoViewerItem by remember { mutableStateOf<MediaItem?>(null) }
    var directVideoPlayerItem by remember { mutableStateOf<MediaItem?>(null) }

    val cornerRadius = if (isFullScreen) 0.dp else 14.dp

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .shadow(if (isFullScreen) 0.dp else 12.dp, RoundedCornerShape(cornerRadius))
            .border(
                width = if (isFullScreen) 0.dp else 1.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(cornerRadius)
            ),
        shape = RoundedCornerShape(cornerRadius),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Title Bar (Draggable)
                TitleBar(
                    selectedTab = selectedTab,
                    isFullScreen = isFullScreen,
                    onDrag = onDrag,
                    onMinimize = onToggleMinimize,
                    onFullScreen = onToggleFullScreen,
                    onClose = onClose
                )

                // Main Content Body
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    when {
                        directPhotoViewerItem != null -> {
                            PhotoDetailViewer(
                                photos = listOf(directPhotoViewerItem!!),
                                initialIndex = 0,
                                onClose = { directPhotoViewerItem = null },
                                onExternalDragStart = onExternalDragStart,
                                onExternalDragEnd = onExternalDragEnd
                            )
                        }
                        directVideoPlayerItem != null -> {
                            VideoPlayerView(
                                video = directVideoPlayerItem!!,
                                onClose = { directVideoPlayerItem = null },
                                onExternalDragStart = onExternalDragStart,
                                onExternalDragEnd = onExternalDragEnd
                            )
                        }
                        else -> {
                            when (selectedTab) {
                                FloatingTab.PHOTOS -> PhotosScreen(
                                    onExternalDragStart = onExternalDragStart,
                                    onExternalDragEnd = onExternalDragEnd
                                )
                                FloatingTab.VIDEOS -> VideosScreen(
                                    onExternalDragStart = onExternalDragStart,
                                    onExternalDragEnd = onExternalDragEnd
                                )
                                FloatingTab.FILES -> FileManagerScreen(
                                    onOpenPhoto = { directPhotoViewerItem = it },
                                    onOpenVideo = { directVideoPlayerItem = it },
                                    onExternalDragStart = onExternalDragStart,
                                    onExternalDragEnd = onExternalDragEnd
                                )
                            }
                        }
                    }
                }

                // Bottom Tab Navigation Bar (3 buttons: ছবি, ভিডিও, ফাইল)
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    tonalElevation = 3.dp,
                    modifier = Modifier.height(58.dp)
                ) {
                    NavigationBarItem(
                        selected = selectedTab == FloatingTab.PHOTOS,
                        onClick = {
                            directPhotoViewerItem = null
                            directVideoPlayerItem = null
                            selectedTab = FloatingTab.PHOTOS
                        },
                        icon = { Icon(Icons.Default.PhotoLibrary, contentDescription = "ছবি", modifier = Modifier.size(20.dp)) },
                        label = { Text("ছবি", fontSize = 11.sp) }
                    )
                    NavigationBarItem(
                        selected = selectedTab == FloatingTab.VIDEOS,
                        onClick = {
                            directPhotoViewerItem = null
                            directVideoPlayerItem = null
                            selectedTab = FloatingTab.VIDEOS
                        },
                        icon = { Icon(Icons.Default.VideoLibrary, contentDescription = "ভিডিও", modifier = Modifier.size(20.dp)) },
                        label = { Text("ভিডিও", fontSize = 11.sp) }
                    )
                    NavigationBarItem(
                        selected = selectedTab == FloatingTab.FILES,
                        onClick = {
                            directPhotoViewerItem = null
                            directVideoPlayerItem = null
                            selectedTab = FloatingTab.FILES
                        },
                        icon = { Icon(Icons.Default.FolderOpen, contentDescription = "ফাইল", modifier = Modifier.size(20.dp)) },
                        label = { Text("ফাইল", fontSize = 11.sp) }
                    )
                }
            }

            // Bottom-Right Corner Resize Handle (Hidden in Fullscreen)
            if (!isFullScreen) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .align(Alignment.BottomEnd)
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                onResize(dragAmount.x, dragAmount.y)
                            }
                        },
                    contentAlignment = Alignment.BottomEnd
                ) {
                    Icon(
                        imageVector = Icons.Default.OpenInFull,
                        contentDescription = "Resize Window",
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                        modifier = Modifier
                            .padding(4.dp)
                            .size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun TitleBar(
    selectedTab: FloatingTab,
    isFullScreen: Boolean,
    onDrag: (dx: Float, dy: Float) -> Unit,
    onMinimize: () -> Unit,
    onFullScreen: () -> Unit,
    onClose: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    onDrag(dragAmount.x, dragAmount.y)
                }
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.DragIndicator,
                contentDescription = "Drag Window",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )

            Spacer(modifier = Modifier.width(6.dp))

            val tabTitle = when (selectedTab) {
                FloatingTab.PHOTOS -> "ছবি ভিউয়ার"
                FloatingTab.VIDEOS -> "ভিডিও প্লেয়ার"
                FloatingTab.FILES -> "ফাইল ম্যানেজার"
            }

            Text(
                text = tabTitle,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            // Minimize Button
            IconButton(
                onClick = onMinimize,
                modifier = Modifier.size(30.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Remove,
                    contentDescription = "Minimize",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Fullscreen Toggle Button
            IconButton(
                onClick = onFullScreen,
                modifier = Modifier.size(30.dp)
            ) {
                Icon(
                    imageVector = if (isFullScreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                    contentDescription = if (isFullScreen) "Restore" else "Maximize",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Close Button
            IconButton(
                onClick = onClose,
                modifier = Modifier.size(30.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
