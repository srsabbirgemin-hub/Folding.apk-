package com.example.model

import android.net.Uri

data class MediaItem(
    val id: Long,
    val name: String,
    val uri: Uri,
    val path: String,
    val size: Long,
    val dateModified: Long,
    val duration: Long = 0L, // for videos in ms
    val bucketName: String = "",
    val mimeType: String = ""
)
