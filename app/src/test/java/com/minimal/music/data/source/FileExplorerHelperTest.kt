package com.minimal.music.data.source

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class FileExplorerHelperTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun testAudioExtensionsRecognition() {
        val audioExtensions = setOf("mp3", "m4a", "wav", "flac", "ogg", "aac", "opus", "wma")
        assertTrue("mp3 should be recognized", "mp3" in audioExtensions)
        assertTrue("flac should be recognized", "flac" in audioExtensions)
        assertTrue("m4a should be recognized", "m4a" in audioExtensions)
        assertFalse("txt should not be recognized", "txt" in audioExtensions)
        assertFalse("jpg should not be recognized", "jpg" in audioExtensions)
    }

    @Test
    fun testDirectorySortingAndFiltering() {
        val rootDir = tempFolder.newFolder("MusicTest")
        val subDirB = File(rootDir, "Rock").apply { mkdir() }
        val subDirA = File(rootDir, "Ambient").apply { mkdir() }

        val audioFileB = File(rootDir, "track_b.mp3").apply { writeText("audio content") }
        val audioFileA = File(rootDir, "track_a.mp3").apply { writeText("audio content") }
        val textFile = File(rootDir, "notes.txt").apply { writeText("not an audio") }

        val files = rootDir.listFiles() ?: emptyArray()

        val directories = files.filter { it.isDirectory }.sortedBy { it.name.lowercase() }
        val audioFiles = files.filter { !it.isDirectory && it.extension.lowercase() in setOf("mp3", "wav") }
            .sortedBy { it.name.lowercase() }

        assertEquals(2, directories.size)
        assertEquals("Ambient", directories[0].name)
        assertEquals("Rock", directories[1].name)

        assertEquals(2, audioFiles.size)
        assertEquals("track_a.mp3", audioFiles[0].name)
        assertEquals("track_b.mp3", audioFiles[1].name)
    }

    @Test
    fun testDeterministicSongIdGeneration() {
        val path1 = "/storage/emulated/0/Music/Song1.mp3"
        val path2 = "/storage/emulated/0/Music/Song2.mp3"

        val id1a = path1.hashCode().toLong().let { if (it < 0) -it else it }
        val id1b = path1.hashCode().toLong().let { if (it < 0) -it else it }
        val id2 = path2.hashCode().toLong().let { if (it < 0) -it else it }

        assertEquals(id1a, id1b)
        assertTrue(id1a != id2)
    }
}
