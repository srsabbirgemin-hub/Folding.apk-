package com.example

import com.example.model.FileItem
import com.example.model.MediaItem
import com.example.utils.FileUtils
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class ExampleUnitTest {
    @Test
    fun testFormatFileSize() {
        assertEquals("0 B", FileUtils.formatFileSize(0))
        assertEquals("1 KB", FileUtils.formatFileSize(1024))
        assertEquals("1 MB", FileUtils.formatFileSize(1024 * 1024))
    }

    @Test
    fun testFormatDuration() {
        assertEquals("00:15", FileUtils.formatDuration(15000))
        assertEquals("02:30", FileUtils.formatDuration(150000))
        assertEquals("1:01:05", FileUtils.formatDuration(3665000))
    }

    @Test
    fun testFileItemExtensions() {
        val zipFile = FileItem(
            file = File("archive.zip"),
            name = "archive.zip",
            isDirectory = false,
            size = 100,
            lastModified = 0,
            extension = "zip",
            isHidden = false
        )
        assertTrue(zipFile.isArchive)
        assertFalse(zipFile.isTextFile)

        val txtFile = FileItem(
            file = File("notes.txt"),
            name = "notes.txt",
            isDirectory = false,
            size = 50,
            lastModified = 0,
            extension = "txt",
            isHidden = false
        )
        assertTrue(txtFile.isTextFile)
        assertFalse(txtFile.isArchive)
    }
}
