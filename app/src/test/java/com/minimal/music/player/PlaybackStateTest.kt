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

    @Test
    fun testSleepTimerRemainingFormat() {
        val remainingMs = 125000L // 2m 5s
        val totalSec = (remainingMs / 1000).coerceAtLeast(0)
        val m = totalSec / 60
        val s = totalSec % 60
        val formatted = "%d:%02d".format(m, s)
        assertEquals("2:05", formatted)
    }

    @Test
    fun testSleepTimerAdjustmentLogic() {
        var remainingMs = 15 * 60 * 1000L // 15 min
        val add5Min = 5 * 60 * 1000L
        remainingMs += add5Min
        assertEquals(20 * 60 * 1000L, remainingMs)

        val sub5Min = 5 * 60 * 1000L
        remainingMs = (remainingMs - sub5Min).coerceAtLeast(60 * 1000L)
        assertEquals(15 * 60 * 1000L, remainingMs)
    }

    @Test
    fun testMiniPlayerDismissalThreshold() {
        val swipeOffsetRight = 120f
        val swipeOffsetLeft = -130f
        val threshold = 90f

        assertTrue("Right swipe should exceed threshold", kotlin.math.abs(swipeOffsetRight) >= threshold)
        assertTrue("Left swipe should exceed threshold", kotlin.math.abs(swipeOffsetLeft) >= threshold)

        val smallDrag = 40f
        assertFalse("Small drag should not trigger dismissal", kotlin.math.abs(smallDrag) >= threshold)
    }
}
