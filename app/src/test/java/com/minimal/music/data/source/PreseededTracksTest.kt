package com.minimal.music.data.source

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PreseededTracksTest {

    @Test
    fun testPreseededTracksAreNotEmpty() {
        val tracks = PreseededTracks.tracks
        assertTrue("Preseeded tracks should not be empty", tracks.isNotEmpty())
        assertEquals(8, tracks.size)
    }

    @Test
    fun testPreseededTracksHaveUniqueIds() {
        val tracks = PreseededTracks.tracks
        val ids = tracks.map { it.id }
        assertEquals("All track IDs must be unique", ids.size, ids.toSet().size)
    }

    @Test
    fun testPreseededTracksHaveValidData() {
        PreseededTracks.tracks.forEach { track ->
            assertTrue("Title must not be blank", track.title.isNotBlank())
            assertTrue("Artist must not be blank", track.artist.isNotBlank())
            assertTrue("Media URI must not be blank", track.mediaUri.isNotBlank())
            assertTrue("Duration must be positive", track.durationMs > 0)
        }
    }
}
