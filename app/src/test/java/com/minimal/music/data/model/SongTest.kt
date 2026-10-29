package com.minimal.music.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SongTest {

    @Test
    fun testFormattedDuration_standard() {
        val song = Song(
            id = 1L,
            title = "Test Song",
            artist = "Test Artist",
            durationMs = 215000L, // 3m 35s
            mediaUri = "https://example.com/test.mp3"
        )
        assertEquals("3:35", song.formattedDuration)
    }

    @Test
    fun testFormattedDuration_short() {
        val song = Song(
            id = 2L,
            title = "Short Song",
            artist = "Test Artist",
            durationMs = 8000L, // 0m 08s
            mediaUri = "https://example.com/test2.mp3"
        )
        assertEquals("0:08", song.formattedDuration)
    }

    @Test
    fun testFormattedDuration_zero() {
        val song = Song(
            id = 3L,
            title = "Zero Song",
            artist = "Test Artist",
            durationMs = 0L,
            mediaUri = "https://example.com/test3.mp3"
        )
        assertEquals("0:00", song.formattedDuration)
    }

    @Test
    fun testFavoriteToggle() {
        val song = Song(
            id = 4L,
            title = "Fav Song",
            artist = "Test Artist",
            mediaUri = "https://example.com/fav.mp3",
            isFavorite = false
        )
        assertFalse(song.isFavorite)

        val updated = song.copy(isFavorite = true)
        assertTrue(updated.isFavorite)
    }

    @Test
    fun testSongFolderProperties() {
        val song = Song(
            id = 5L,
            title = "Folder Track",
            artist = "Local Artist",
            mediaUri = "file:///storage/emulated/0/Music/Rock/track.mp3",
            folderName = "Rock",
            folderPath = "/storage/emulated/0/Music/Rock"
        )
        assertEquals("Rock", song.folderName)
        assertEquals("/storage/emulated/0/Music/Rock", song.folderPath)
    }

    @Test
    fun testFolderSongsPrioritizedOverStandardSongs() {
        val nonFolderSong = Song(
            id = 1L,
            title = "A Non Folder Song",
            artist = "Artist A",
            mediaUri = "https://example.com/a.mp3"
        )
        val folderSong = Song(
            id = 2L,
            title = "Z Folder Song",
            artist = "Artist Z",
            mediaUri = "file:///storage/emulated/0/Music/Rock/z.mp3",
            folderName = "Rock",
            folderPath = "/storage/emulated/0/Music/Rock"
        )

        val list = listOf(nonFolderSong, folderSong)
        val sorted = list.sortedWith(
            compareBy<Song> { if (it.folderPath.isNullOrBlank()) 1 else 0 }
                .thenBy { it.folderName ?: "" }
                .thenBy { it.title }
        )

        assertEquals("Z Folder Song", sorted[0].title)
        assertEquals("A Non Folder Song", sorted[1].title)
    }
}
