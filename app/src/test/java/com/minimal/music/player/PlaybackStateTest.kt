package com.minimal.music.player

import com.minimal.music.data.model.Song
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackStateTest {

    @Test
    fun testProgressCalculation() {
        val song = Song(
            id = 1L,
            title = "Test",
            artist = "Artist",
            durationMs = 100000L,
            mediaUri = "https://example.com/test.mp3"
        )

        val state = PlaybackState(
            currentSong = song,
            durationMs = 100000L,
            currentPositionMs = 25000L
        )

        assertEquals(0.25f, state.progress, 0.001f)
    }

    @Test
    fun testProgressWithZeroDuration() {
        val state = PlaybackState(
            currentPositionMs = 1000L,
            durationMs = 0L
        )
        assertEquals(0f, state.progress, 0.001f)
    }

    @Test
    fun testRepeatModeTransitions() {
        var mode = RepeatMode.OFF
        mode = when (mode) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
        assertEquals(RepeatMode.ALL, mode)

        mode = when (mode) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
        assertEquals(RepeatMode.ONE, mode)

        mode = when (mode) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
        assertEquals(RepeatMode.OFF, mode)
    }
}
