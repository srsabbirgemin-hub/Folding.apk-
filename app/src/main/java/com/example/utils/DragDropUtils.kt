package com.example.utils

import android.content.ClipData
import android.content.ClipDescription
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Point
import android.graphics.RectF
import android.net.Uri
import android.os.Build
import android.view.DragEvent
import android.view.View
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import java.io.File

object DragDropUtils {

    fun getMimeType(file: File): String {
        val extension = file.extension.lowercase()
        val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
        return mimeType ?: when (extension) {
            "jpg", "jpeg", "png", "webp", "gif", "bmp", "heic" -> "image/*"
            "mp4", "mkv", "webm", "avi", "mov", "3gp" -> "video/*"
            "mp3", "wav", "ogg", "m4a", "flac" -> "audio/*"
            "pdf" -> "application/pdf"
            "zip" -> "application/zip"
            "7z" -> "application/x-7z-compressed"
            "rar" -> "application/x-rar-compressed"
            "txt", "log", "json", "xml", "html", "kt", "java", "py", "sh" -> "text/plain"
            else -> "*/*"
        }
    }

    fun getContentUri(context: Context, file: File): Uri {
        return try {
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (_: Exception) {
            Uri.fromFile(file)
        }
    }

    fun startGlobalDrag(
        view: View,
        files: List<File>,
        onDragStart: () -> Unit,
        onDragEnd: (success: Boolean) -> Unit
    ): Boolean {
        if (files.isEmpty()) return false
        val context = view.context

        // Prepare ClipData
        val firstFile = files.first()
        val firstUri = getContentUri(context, firstFile)
        val firstMime = getMimeType(firstFile)

        val clipDescription = ClipDescription(
            if (files.size > 1) "${files.size} files" else firstFile.name,
            arrayOf(firstMime, "text/uri-list", "*/*")
        )

        val clipData = ClipData(clipDescription, ClipData.Item(firstUri))
        for (i in 1 until files.size) {
            val file = files[i]
            val uri = getContentUri(context, file)
            clipData.addItem(ClipData.Item(uri))
        }

        // Custom Drag Shadow with file badge
        val shadowBuilder = CustomDragShadowBuilder(view, files)

        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            View.DRAG_FLAG_GLOBAL or View.DRAG_FLAG_GLOBAL_URI_READ
        } else {
            0
        }

        // Notify that drag has started so the floating window can become transparent/compact
        onDragStart()

        // Set drag listener to capture when drag ends
        view.setOnDragListener { _, event ->
            when (event.action) {
                DragEvent.ACTION_DRAG_ENDED -> {
                    val result = event.result
                    onDragEnd(result)
                    view.setOnDragListener(null)
                    true
                }
                else -> false
            }
        }

        return try {
            val started = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                view.startDragAndDrop(clipData, shadowBuilder, null, flags)
            } else {
                @Suppress("DEPRECATION")
                view.startDrag(clipData, shadowBuilder, null, flags)
            }
            if (!started) {
                onDragEnd(false)
                view.setOnDragListener(null)
            }
            started
        } catch (e: Exception) {
            e.printStackTrace()
            onDragEnd(false)
            view.setOnDragListener(null)
            false
        }
    }

    class CustomDragShadowBuilder(
        private val sourceView: View,
        private val files: List<File>
    ) : View.DragShadowBuilder(sourceView) {

        private val density = sourceView.resources.displayMetrics.density
        private val width = (110 * density).toInt()
        private val height = (90 * density).toInt()

        override fun onProvideShadowMetrics(outShadowSize: Point, outShadowTouchPoint: Point) {
            outShadowSize.set(width, height)
            outShadowTouchPoint.set(width / 2, height / 2)
        }

        override fun onDrawShadow(canvas: Canvas) {
            val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#E61E293B") // Dark blue card
                style = Paint.Style.FILL
            }
            val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#38BDF8") // Cyan border
                style = Paint.Style.STROKE
                strokeWidth = 2f * density
            }
            val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = 12f * density
                textAlign = Paint.Align.CENTER
            }
            val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#0284C7")
                style = Paint.Style.FILL
            }

            val rect = RectF(4f * density, 4f * density, width - 4f * density, height - 4f * density)
            val cornerRadius = 10f * density
            canvas.drawRoundRect(rect, cornerRadius, cornerRadius, bgPaint)
            canvas.drawRoundRect(rect, cornerRadius, cornerRadius, borderPaint)

            val firstFile = files.first()
            val label = if (files.size > 1) "${files.size} Files" else {
                if (firstFile.name.length > 10) firstFile.name.take(9) + "…" else firstFile.name
            }

            // Draw badge circle
            val circleY = height * 0.38f
            canvas.drawCircle(width / 2f, circleY, 16f * density, badgePaint)

            // Draw file type tag
            val typeText = when {
                firstFile.extension.lowercase() in setOf("jpg", "jpeg", "png", "webp") -> "IMG"
                firstFile.extension.lowercase() in setOf("mp4", "mkv", "mov", "avi") -> "VID"
                firstFile.extension.lowercase() in setOf("zip", "7z", "rar") -> "ZIP"
                else -> "FILE"
            }
            val iconTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = 10f * density
                isFakeBoldText = true
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText(typeText, width / 2f, circleY + 4f * density, iconTextPaint)

            // Draw filename or count
            canvas.drawText(label, width / 2f, height * 0.80f, textPaint)
        }
    }
}
