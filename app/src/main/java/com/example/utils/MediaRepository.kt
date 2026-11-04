package com.example.utils

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.os.Build
import android.provider.MediaStore
import com.example.model.MediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object MediaRepository {

    suspend fun loadPhotos(context: Context): List<MediaItem> = withContext(Dispatchers.IO) {
        val list = mutableListOf<MediaItem>()
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }

        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATA,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.DATE_MODIFIED,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Images.Media.MIME_TYPE
        )

        val sortOrder = "${MediaStore.Images.Media.DATE_MODIFIED} DESC"

        try {
            val cursor: Cursor? = context.contentResolver.query(collection, projection, null, null, sortOrder)
            cursor?.use {
                val idColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val nameColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                val dataColumn = it.getColumnIndex(MediaStore.Images.Media.DATA)
                val sizeColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
                val dateColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_MODIFIED)
                val bucketColumn = it.getColumnIndex(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
                val mimeColumn = it.getColumnIndex(MediaStore.Images.Media.MIME_TYPE)

                while (it.moveToNext()) {
                    val id = it.getLong(idColumn)
                    val name = it.getString(nameColumn) ?: "Photo_$id"
                    val path = if (dataColumn >= 0) it.getString(dataColumn) ?: "" else ""
                    val size = it.getLong(sizeColumn)
                    val dateModified = it.getLong(dateColumn) * 1000
                    val bucket = if (bucketColumn >= 0) it.getString(bucketColumn) ?: "Other" else "Other"
                    val mime = if (mimeColumn >= 0) it.getString(mimeColumn) ?: "image/*" else "image/*"
                    val contentUri = ContentUris.withAppendedId(collection, id)

                    list.add(
                        MediaItem(
                            id = id,
                            name = name,
                            uri = contentUri,
                            path = path,
                            size = size,
                            dateModified = dateModified,
                            bucketName = bucket,
                            mimeType = mime
                        )
                    )
                }
            }
        } catch (_: Exception) {}

        list
    }

    suspend fun loadVideos(context: Context): List<MediaItem> = withContext(Dispatchers.IO) {
        val list = mutableListOf<MediaItem>()
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }

        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DATA,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DATE_MODIFIED,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Video.Media.MIME_TYPE
        )

        val sortOrder = "${MediaStore.Video.Media.DATE_MODIFIED} DESC"

        try {
            val cursor: Cursor? = context.contentResolver.query(collection, projection, null, null, sortOrder)
            cursor?.use {
                val idColumn = it.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val nameColumn = it.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
                val dataColumn = it.getColumnIndex(MediaStore.Video.Media.DATA)
                val sizeColumn = it.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
                val dateColumn = it.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_MODIFIED)
                val durColumn = it.getColumnIndex(MediaStore.Video.Media.DURATION)
                val bucketColumn = it.getColumnIndex(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)
                val mimeColumn = it.getColumnIndex(MediaStore.Video.Media.MIME_TYPE)

                while (it.moveToNext()) {
                    val id = it.getLong(idColumn)
                    val name = it.getString(nameColumn) ?: "Video_$id"
                    val path = if (dataColumn >= 0) it.getString(dataColumn) ?: "" else ""
                    val size = it.getLong(sizeColumn)
                    val dateModified = it.getLong(dateColumn) * 1000
                    val duration = if (durColumn >= 0) it.getLong(durColumn) else 0L
                    val bucket = if (bucketColumn >= 0) it.getString(bucketColumn) ?: "Other" else "Other"
                    val mime = if (mimeColumn >= 0) it.getString(mimeColumn) ?: "video/*" else "video/*"
                    val contentUri = ContentUris.withAppendedId(collection, id)

                    list.add(
                        MediaItem(
                            id = id,
                            name = name,
                            uri = contentUri,
                            path = path,
                            size = size,
                            dateModified = dateModified,
                            duration = duration,
                            bucketName = bucket,
                            mimeType = mime
                        )
                    )
                }
            }
        } catch (_: Exception) {}

        list
    }
}
