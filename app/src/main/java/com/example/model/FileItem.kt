package com.example.model

import java.io.File

data class FileItem(
    val file: File,
    val name: String,
    val isDirectory: Boolean,
    val size: Long,
    val lastModified: Long,
    val extension: String,
    val isHidden: Boolean,
    val childCount: Int = 0
) {
    val isArchive: Boolean
        get() = extension.lowercase() in setOf("zip", "7z", "rar", "tar", "gz")

    val isTextFile: Boolean
        get() = extension.lowercase() in setOf(
            "txt", "json", "xml", "html", "htm", "kt", "java", "py", "js", "ts", "css",
            "md", "csv", "log", "properties", "gradle", "sh", "yml", "yaml", "c", "cpp", "h"
        )

    val isImage: Boolean
        get() = extension.lowercase() in setOf("jpg", "jpeg", "png", "webp", "gif", "bmp", "heic")

    val isVideo: Boolean
        get() = extension.lowercase() in setOf("mp4", "mkv", "webm", "avi", "3gp", "mov", "ts")
}

enum class SortBy {
    NAME_ASC,
    NAME_DESC,
    DATE_DESC,
    DATE_ASC,
    SIZE_DESC,
    SIZE_ASC
}
