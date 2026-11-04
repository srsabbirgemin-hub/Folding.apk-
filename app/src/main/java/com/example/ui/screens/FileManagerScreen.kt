package com.example.ui.screens

import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.FileItem
import com.example.model.MediaItem
import com.example.model.SortBy
import com.example.utils.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

import androidx.compose.ui.platform.LocalView
import com.example.utils.DragDropUtils

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun FileManagerScreen(
    onOpenPhoto: (MediaItem) -> Unit,
    onOpenVideo: (MediaItem) -> Unit,
    onExternalDragStart: () -> Unit = {},
    onExternalDragEnd: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()

    val rootDir = remember { Environment.getExternalStorageDirectory() }
    var currentDir by remember { mutableStateOf(rootDir) }
    var files by remember { mutableStateOf<List<FileItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }

    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    var showHiddenFiles by remember { mutableStateOf(false) }
    var sortBy by remember { mutableStateOf(SortBy.NAME_ASC) }

    // Multi-selection state
    val selectedFiles = remember { mutableStateListOf<File>() }
    val isSelectionMode by remember { derivedStateOf { selectedFiles.isNotEmpty() } }

    // Clipboard operation for Copy/Cut
    var clipboardFiles by remember { mutableStateOf<List<File>>(emptyList()) }
    var isCutOperation by remember { mutableStateOf(false) }

    // Dialogs
    var showNewFolderDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf<File?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showPropertiesDialog by remember { mutableStateOf<FileItem?>(null) }
    var showZipDialog by remember { mutableStateOf(false) }
    var showExtractDialog by remember { mutableStateOf<File?>(null) }
    var textEditorFile by remember { mutableStateOf<File?>(null) }

    // Load directory files
    fun refreshDirectory() {
        scope.launch {
            isLoading = true
            val list = withContext(Dispatchers.IO) {
                try {
                    val rawFiles = currentDir.listFiles() ?: emptyArray()
                    rawFiles.map { file ->
                        FileItem(
                            file = file,
                            name = file.name,
                            isDirectory = file.isDirectory,
                            size = if (file.isDirectory) 0L else file.length(),
                            lastModified = file.lastModified(),
                            extension = file.extension,
                            isHidden = file.name.startsWith("."),
                            childCount = if (file.isDirectory) file.list()?.size ?: 0 else 0
                        )
                    }
                } catch (e: Exception) {
                    emptyList()
                }
            }

            // Filtering & Sorting
            var processed = list.filter {
                (showHiddenFiles || !it.isHidden) &&
                        (searchQuery.isEmpty() || it.name.contains(searchQuery, ignoreCase = true))
            }

            // Folders first, then apply sort
            val (dirs, nonDirs) = processed.partition { it.isDirectory }
            val sortedDirs = when (sortBy) {
                SortBy.NAME_ASC -> dirs.sortedBy { it.name.lowercase() }
                SortBy.NAME_DESC -> dirs.sortedByDescending { it.name.lowercase() }
                SortBy.DATE_DESC -> dirs.sortedByDescending { it.lastModified }
                SortBy.DATE_ASC -> dirs.sortedBy { it.lastModified }
                SortBy.SIZE_DESC -> dirs.sortedByDescending { it.childCount }
                SortBy.SIZE_ASC -> dirs.sortedBy { it.childCount }
            }
            val sortedFiles = when (sortBy) {
                SortBy.NAME_ASC -> nonDirs.sortedBy { it.name.lowercase() }
                SortBy.NAME_DESC -> nonDirs.sortedByDescending { it.name.lowercase() }
                SortBy.DATE_DESC -> nonDirs.sortedByDescending { it.lastModified }
                SortBy.DATE_ASC -> nonDirs.sortedBy { it.lastModified }
                SortBy.SIZE_DESC -> nonDirs.sortedByDescending { it.size }
                SortBy.SIZE_ASC -> nonDirs.sortedBy { it.size }
            }

            files = sortedDirs + sortedFiles
            isLoading = false
        }
    }

    LaunchedEffect(currentDir, showHiddenFiles, sortBy, searchQuery) {
        refreshDirectory()
    }

    BackHandler(enabled = currentDir != rootDir || isSelectionMode || isSearchActive) {
        when {
            isSelectionMode -> selectedFiles.clear()
            isSearchActive -> {
                isSearchActive = false
                searchQuery = ""
            }
            currentDir.parentFile != null && currentDir != rootDir -> {
                currentDir = currentDir.parentFile!!
            }
        }
    }

    // Text Editor Screen
    if (textEditorFile != null) {
        TextEditorScreen(
            file = textEditorFile!!,
            onClose = {
                textEditorFile = null
                refreshDirectory()
            }
        )
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Path Breadcrumb & Actions Bar
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainer,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (currentDir != rootDir) {
                        IconButton(
                            onClick = {
                                if (currentDir.parentFile != null) {
                                    currentDir = currentDir.parentFile!!
                                    selectedFiles.clear()
                                }
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }

                    Text(
                        text = if (currentDir == rootDir) "Internal Storage" else currentDir.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    // Search Button
                    IconButton(
                        onClick = { isSearchActive = !isSearchActive },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(if (isSearchActive) Icons.Default.Close else Icons.Default.Search, contentDescription = "Search")
                    }

                    // Sort Menu
                    var showSortMenu by remember { mutableStateOf(false) }
                    IconButton(
                        onClick = { showSortMenu = true },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.Sort, contentDescription = "Sort")
                    }
                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("নাম (A-Z)") },
                            onClick = { sortBy = SortBy.NAME_ASC; showSortMenu = false }
                        )
                        DropdownMenuItem(
                            text = { Text("নাম (Z-A)") },
                            onClick = { sortBy = SortBy.NAME_DESC; showSortMenu = false }
                        )
                        DropdownMenuItem(
                            text = { Text("তারিখ (নতুন আগে)") },
                            onClick = { sortBy = SortBy.DATE_DESC; showSortMenu = false }
                        )
                        DropdownMenuItem(
                            text = { Text("সাইজ (বড় আগে)") },
                            onClick = { sortBy = SortBy.SIZE_DESC; showSortMenu = false }
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text(if (showHiddenFiles) "হিডেন ফাইল লুকান" else "হিডেন ফাইল দেখান") },
                            onClick = {
                                showHiddenFiles = !showHiddenFiles
                                showSortMenu = false
                            }
                        )
                    }

                    // New Folder Button
                    IconButton(
                        onClick = { showNewFolderDialog = true },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.CreateNewFolder, contentDescription = "New Folder")
                    }
                }

                // Search Bar Input if active
                if (isSearchActive) {
                    TextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("ফাইল অনুসন্ধান...", fontSize = 12.sp) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                }
            }
        }

        // Selection Action Bar or Clipboard Paste Bar
        AnimatedVisibility(visible = isSelectionMode) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${selectedFiles.size} টি সিলেক্টেড",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Drag & Drop out to external app
                        IconButton(
                            onClick = {
                                DragDropUtils.startGlobalDrag(
                                    view = view,
                                    files = selectedFiles.toList(),
                                    onDragStart = onExternalDragStart,
                                    onDragEnd = { success ->
                                        onExternalDragEnd(success)
                                        if (success) {
                                            Toast.makeText(context, "${selectedFiles.size} টি ফাইল শেয়ার সম্পন্ন!", Toast.LENGTH_SHORT).show()
                                            selectedFiles.clear()
                                        }
                                    }
                                )
                            },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(Icons.Default.DragIndicator, contentDescription = "Drag to other app", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                        }
                        // Copy
                        IconButton(
                            onClick = {
                                clipboardFiles = selectedFiles.toList()
                                isCutOperation = false
                                Toast.makeText(context, "${selectedFiles.size} টি কপি করা হয়েছে", Toast.LENGTH_SHORT).show()
                                selectedFiles.clear()
                            },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(18.dp))
                        }
                        // Cut
                        IconButton(
                            onClick = {
                                clipboardFiles = selectedFiles.toList()
                                isCutOperation = true
                                Toast.makeText(context, "${selectedFiles.size} টি কাট করা হয়েছে", Toast.LENGTH_SHORT).show()
                                selectedFiles.clear()
                            },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(Icons.Default.ContentCut, contentDescription = "Cut", modifier = Modifier.size(18.dp))
                        }
                        // Zip Archive
                        IconButton(
                            onClick = { showZipDialog = true },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(Icons.Default.FolderZip, contentDescription = "Zip", modifier = Modifier.size(18.dp))
                        }
                        // Delete
                        IconButton(
                            onClick = { showDeleteConfirmDialog = true },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error)
                        }
                        // Close Selection
                        IconButton(
                            onClick = { selectedFiles.clear() },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Cancel", modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }

        // Paste Bar if Clipboard has items
        if (clipboardFiles.isNotEmpty() && !isSelectionMode) {
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${clipboardFiles.size} টি পেস্ট করার জন্য প্রস্তুত (${if (isCutOperation) "Move" else "Copy"})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Row {
                        TextButton(
                            onClick = {
                                scope.launch(Dispatchers.IO) {
                                    try {
                                        for (src in clipboardFiles) {
                                            val dest = File(currentDir, src.name)
                                            if (isCutOperation) {
                                                src.renameTo(dest)
                                            } else {
                                                FileUtils.copyFileOrDirectory(src, dest)
                                            }
                                        }
                                        withContext(Dispatchers.Main) {
                                            clipboardFiles = emptyList()
                                            Toast.makeText(context, "সফলভাবে পেস্ট করা হয়েছে", Toast.LENGTH_SHORT).show()
                                            refreshDirectory()
                                        }
                                    } catch (e: Exception) {
                                        withContext(Dispatchers.Main) {
                                            Toast.makeText(context, "ত্রুটি: ${e.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            }
                        ) {
                            Text("পেস্ট", fontWeight = FontWeight.Bold)
                        }
                        IconButton(
                            onClick = { clipboardFiles = emptyList() },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Cancel Clipboard")
                        }
                    }
                }
            }
        }

        // File List
        Box(modifier = Modifier.fillMaxSize()) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (files.isEmpty()) {
                Text(
                    text = "ফোল্ডার খালি",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    items(files, key = { it.file.absolutePath }) { fileItem ->
                        val isSelected = selectedFiles.contains(fileItem.file)
                        FileListItem(
                            fileItem = fileItem,
                            isSelected = isSelected,
                            onItemClick = {
                                if (isSelectionMode) {
                                    if (isSelected) selectedFiles.remove(fileItem.file)
                                    else selectedFiles.add(fileItem.file)
                                } else {
                                    if (fileItem.isDirectory) {
                                        currentDir = fileItem.file
                                    } else if (fileItem.isImage) {
                                        onOpenPhoto(
                                            MediaItem(
                                                id = fileItem.file.hashCode().toLong(),
                                                name = fileItem.name,
                                                uri = Uri.fromFile(fileItem.file),
                                                path = fileItem.file.absolutePath,
                                                size = fileItem.size,
                                                dateModified = fileItem.lastModified
                                            )
                                        )
                                    } else if (fileItem.isVideo) {
                                        onOpenVideo(
                                            MediaItem(
                                                id = fileItem.file.hashCode().toLong(),
                                                name = fileItem.name,
                                                uri = Uri.fromFile(fileItem.file),
                                                path = fileItem.file.absolutePath,
                                                size = fileItem.size,
                                                dateModified = fileItem.lastModified
                                            )
                                        )
                                    } else if (fileItem.isArchive) {
                                        showExtractDialog = fileItem.file
                                    } else if (fileItem.isTextFile) {
                                        textEditorFile = fileItem.file
                                    } else {
                                        FileUtils.shareFile(context, fileItem.file)
                                    }
                                }
                            },
                            onItemLongClick = {
                                if (isSelectionMode) {
                                    if (isSelected) {
                                        selectedFiles.remove(fileItem.file)
                                    } else {
                                        selectedFiles.add(fileItem.file)
                                    }
                                } else {
                                    // Start global drag immediately on long-press
                                    DragDropUtils.startGlobalDrag(
                                        view = view,
                                        files = listOf(fileItem.file),
                                        onDragStart = onExternalDragStart,
                                        onDragEnd = { success ->
                                            onExternalDragEnd(success)
                                            if (success) {
                                                Toast.makeText(context, "${fileItem.name} শেয়ার সম্পন্ন!", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    )
                                }
                            },
                            onActionClick = { action ->
                                when (action) {
                                    "drag" -> {
                                        DragDropUtils.startGlobalDrag(
                                            view = view,
                                            files = listOf(fileItem.file),
                                            onDragStart = onExternalDragStart,
                                            onDragEnd = onExternalDragEnd
                                        )
                                    }
                                    "copy" -> {
                                        FileUtils.copyToClipboard(context, fileItem.file)
                                        Toast.makeText(context, "ক্লিপবোর্ডে কপি করা হয়েছে", Toast.LENGTH_SHORT).show()
                                    }
                                    "rename" -> showRenameDialog = fileItem.file
                                    "delete" -> {
                                        selectedFiles.clear()
                                        selectedFiles.add(fileItem.file)
                                        showDeleteConfirmDialog = true
                                    }
                                    "properties" -> showPropertiesDialog = fileItem
                                    "share" -> FileUtils.shareFile(context, fileItem.file)
                                    "extract" -> showExtractDialog = fileItem.file
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    // Dialog: Create New Folder
    if (showNewFolderDialog) {
        var folderName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showNewFolderDialog = false },
            title = { Text("নতুন ফোল্ডার") },
            text = {
                OutlinedTextField(
                    value = folderName,
                    onValueChange = { folderName = it },
                    label = { Text("ফোল্ডারের নাম") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (folderName.isNotBlank()) {
                            val newDir = File(currentDir, folderName.trim())
                            if (newDir.mkdir()) {
                                Toast.makeText(context, "ফোল্ডার তৈরি হয়েছে", Toast.LENGTH_SHORT).show()
                                refreshDirectory()
                            } else {
                                Toast.makeText(context, "ফোল্ডার তৈরি করা যায়নি", Toast.LENGTH_SHORT).show()
                            }
                        }
                        showNewFolderDialog = false
                    }
                ) {
                    Text("তৈরি করুন")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewFolderDialog = false }) {
                    Text("বাতিল")
                }
            }
        )
    }

    // Dialog: Rename
    if (showRenameDialog != null) {
        val targetFile = showRenameDialog!!
        var newName by remember { mutableStateOf(targetFile.name) }
        AlertDialog(
            onDismissRequest = { showRenameDialog = null },
            title = { Text("নাম পরিবর্তন") },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("নতুন নাম") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newName.isNotBlank()) {
                            val dest = File(targetFile.parentFile, newName.trim())
                            if (targetFile.renameTo(dest)) {
                                Toast.makeText(context, "নাম পরিবর্তিত হয়েছে", Toast.LENGTH_SHORT).show()
                                refreshDirectory()
                            } else {
                                Toast.makeText(context, "ব্যর্থ হয়েছে", Toast.LENGTH_SHORT).show()
                            }
                        }
                        showRenameDialog = null
                    }
                ) {
                    Text("সেভ")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = null }) {
                    Text("বাতিল")
                }
            }
        )
    }

    // Dialog: Delete Confirm
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("ডিলিট নিশ্চিত করুন") },
            text = { Text("${selectedFiles.size} টি আইটেম মুছে ফেলা হবে?") },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    onClick = {
                        scope.launch(Dispatchers.IO) {
                            for (file in selectedFiles) {
                                if (file.isDirectory) file.deleteRecursively() else file.delete()
                            }
                            withContext(Dispatchers.Main) {
                                selectedFiles.clear()
                                showDeleteConfirmDialog = false
                                Toast.makeText(context, "ডিলিট সম্পন্ন", Toast.LENGTH_SHORT).show()
                                refreshDirectory()
                            }
                        }
                    }
                ) {
                    Text("ডিলিট")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("বাতিল")
                }
            }
        )
    }

    // Dialog: Properties
    if (showPropertiesDialog != null) {
        val item = showPropertiesDialog!!
        AlertDialog(
            onDismissRequest = { showPropertiesDialog = null },
            title = { Text("প্রপার্টিজ") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("নাম: ${item.name}", fontWeight = FontWeight.SemiBold)
                    Text("পাথ: ${item.file.absolutePath}", fontSize = 12.sp)
                    Text("সাইজ: ${if (item.isDirectory) "${item.childCount} items" else FileUtils.formatFileSize(item.size)}")
                    Text("পরিবর্তন: ${FileUtils.formatDate(item.lastModified)}")
                    Text("রিডেবল: ${item.file.canRead()}, রাইটেবল: ${item.file.canWrite()}")
                }
            },
            confirmButton = {
                TextButton(onClick = { showPropertiesDialog = null }) {
                    Text("ঠিক আছে")
                }
            }
        )
    }

    // Dialog: Create ZIP
    if (showZipDialog) {
        var zipName by remember { mutableStateOf("archive.zip") }
        var password by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showZipDialog = false },
            title = { Text("ZIP আর্কাইভ তৈরি") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = zipName,
                        onValueChange = { zipName = it },
                        label = { Text("ফাইলের নাম (.zip)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("পাসওয়ার্ড (ঐচ্ছিক)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val finalName = if (zipName.endsWith(".zip")) zipName else "$zipName.zip"
                        val destZip = File(currentDir, finalName)
                        val filesToZip = selectedFiles.toList()
                        showZipDialog = false
                        selectedFiles.clear()

                        scope.launch(Dispatchers.IO) {
                            try {
                                FileUtils.createZipArchive(filesToZip, destZip, password.ifEmpty { null })
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(context, "আর্কাইভ তৈরি হয়েছে", Toast.LENGTH_SHORT).show()
                                    refreshDirectory()
                                }
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(context, "ত্রুটি: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    }
                ) {
                    Text("কম্প্রেস করুন")
                }
            },
            dismissButton = {
                TextButton(onClick = { showZipDialog = false }) {
                    Text("বাতিল")
                }
            }
        )
    }

    // Dialog: Extract Archive
    if (showExtractDialog != null) {
        val archiveFile = showExtractDialog!!
        var extractFolderName by remember { mutableStateOf(archiveFile.nameWithoutExtension) }
        var password by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showExtractDialog = null },
            title = { Text("আর্কাইভ এক্সট্র্যাক্ট") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("ফাইল: ${archiveFile.name}", style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(
                        value = extractFolderName,
                        onValueChange = { extractFolderName = it },
                        label = { Text("এক্সট্র্যাক্ট ফোল্ডারের নাম") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("পাসওয়ার্ড (যদি থাকে)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val destFolder = File(currentDir, extractFolderName)
                        showExtractDialog = null
                        scope.launch(Dispatchers.IO) {
                            try {
                                FileUtils.extractZipArchive(archiveFile, destFolder, password.ifEmpty { null })
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(context, "এক্সট্র্যাক্ট সম্পন্ন হয়েছে", Toast.LENGTH_SHORT).show()
                                    refreshDirectory()
                                }
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(context, "এক্সট্র্যাক্ট ব্যর্থ: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    }
                ) {
                    Text("এক্সট্র্যাক্ট")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExtractDialog = null }) {
                    Text("বাতিল")
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileListItem(
    fileItem: FileItem,
    isSelected: Boolean,
    onItemClick: () -> Unit,
    onItemLongClick: () -> Unit,
    onActionClick: (String) -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Surface(
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else Color.Transparent,
        shape = RoundedCornerShape(6.dp),
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onItemClick,
                onLongClick = onItemLongClick
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            val icon = when {
                fileItem.isDirectory -> Icons.Default.Folder
                fileItem.isArchive -> Icons.Default.FolderZip
                fileItem.isImage -> Icons.Default.Image
                fileItem.isVideo -> Icons.Default.VideoFile
                fileItem.isTextFile -> Icons.Default.EditNote
                else -> Icons.AutoMirrored.Filled.InsertDriveFile
            }
            val iconTint = when {
                fileItem.isDirectory -> MaterialTheme.colorScheme.primary
                fileItem.isArchive -> MaterialTheme.colorScheme.tertiary
                fileItem.isImage -> Color(0xFF4CAF50)
                fileItem.isVideo -> Color(0xFFFF9800)
                fileItem.isTextFile -> Color(0xFF2196F3)
                else -> MaterialTheme.colorScheme.outline
            }

            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(28.dp)
            )

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = fileItem.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (fileItem.isDirectory) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (fileItem.isDirectory) {
                        "${fileItem.childCount} items  •  ${FileUtils.formatDate(fileItem.lastModified)}"
                    } else {
                        "${FileUtils.formatFileSize(fileItem.size)}  •  ${FileUtils.formatDate(fileItem.lastModified)}"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp
                )
            }

            Box {
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.MoreVert, contentDescription = "More", modifier = Modifier.size(18.dp))
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    if (fileItem.isArchive) {
                        DropdownMenuItem(
                            text = { Text("এক্সট্র্যাক্ট করুন") },
                            onClick = { showMenu = false; onActionClick("extract") }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("বাইরে ড্র্যাগ করুন (Drag out)") },
                        leadingIcon = { Icon(Icons.Default.DragIndicator, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        onClick = { showMenu = false; onActionClick("drag") }
                    )
                    DropdownMenuItem(
                        text = { Text("শেয়ার (Share)") },
                        leadingIcon = { Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        onClick = { showMenu = false; onActionClick("share") }
                    )
                    DropdownMenuItem(
                        text = { Text("কপি (Copy)") },
                        leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        onClick = { showMenu = false; onActionClick("copy") }
                    )
                    DropdownMenuItem(
                        text = { Text("রিনেম") },
                        onClick = { showMenu = false; onActionClick("rename") }
                    )
                    DropdownMenuItem(
                        text = { Text("প্রপার্টিজ") },
                        onClick = { showMenu = false; onActionClick("properties") }
                    )
                    DropdownMenuItem(
                        text = { Text("ডিলিট", color = MaterialTheme.colorScheme.error) },
                        onClick = { showMenu = false; onActionClick("delete") }
                    )
                }
            }
        }
    }
}

@Composable
fun TextEditorScreen(
    file: File,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var textContent by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }

    LaunchedEffect(file) {
        isLoading = true
        textContent = withContext(Dispatchers.IO) {
            try {
                file.readText()
            } catch (e: Exception) {
                "ফাইল পড়তে সমস্যা হয়েছে: ${e.message}"
            }
        }
        isLoading = false
    }

    BackHandler {
        onClose()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Editor Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onClose, modifier = Modifier.size(36.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text(
                text = file.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Button(
                onClick = {
                    scope.launch {
                        isSaving = true
                        withContext(Dispatchers.IO) {
                            try {
                                file.writeText(textContent)
                            } catch (_: Exception) {}
                        }
                        isSaving = false
                        Toast.makeText(context, "সংরক্ষণ করা হয়েছে", Toast.LENGTH_SHORT).show()
                    }
                },
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                modifier = Modifier.height(34.dp)
            ) {
                Text("সেভ", fontSize = 12.sp)
            }
        }

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize()) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        } else {
            TextField(
                value = textContent,
                onValueChange = { textContent = it },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                textStyle = LocalTextStyle.current.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp
                ),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    }
}
