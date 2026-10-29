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
}
