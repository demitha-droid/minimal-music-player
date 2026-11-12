package com.minimal.music.data.source

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LyricsHelperTest {

    @Test
    fun testParseSyncedLrc_standard() {
        val lrc = """
            [ti:Test Track]
            [ar:Test Artist]
            [00:01.50]Line 1
            [00:04.20]Line 2
            [00:10.00]Line 3
        """.trimIndent()

        val parsed = LyricsHelper.parseLyrics(lrc)
        assertTrue(parsed.isSynced)
        assertEquals(3, parsed.lines.size)

        assertEquals(1500L, parsed.lines[0].timeMs)
        assertEquals("Line 1", parsed.lines[0].text)

        assertEquals(4200L, parsed.lines[1].timeMs)
        assertEquals("Line 2", parsed.lines[1].text)

        assertEquals(10000L, parsed.lines[2].timeMs)
        assertEquals("Line 3", parsed.lines[2].text)
    }

    @Test
    fun testParseSyncedLrc_threeDigitMs() {
        val lrc = """
            [01:15.500]Guitar solo begins
        """.trimIndent()

        val parsed = LyricsHelper.parseLyrics(lrc)
        assertTrue(parsed.isSynced)
        assertEquals(1, parsed.lines.size)
        assertEquals(75500L, parsed.lines[0].timeMs)
        assertEquals("Guitar solo begins", parsed.lines[0].text)
    }

    @Test
    fun testParseSyncedLrc_multipleTimestamps() {
        val lrc = """
            [00:05.00][00:15.00]Repeated chorus
        """.trimIndent()

        val parsed = LyricsHelper.parseLyrics(lrc)
        assertTrue(parsed.isSynced)
        assertEquals(2, parsed.lines.size)
        assertEquals(5000L, parsed.lines[0].timeMs)
        assertEquals("Repeated chorus", parsed.lines[0].text)
        assertEquals(15000L, parsed.lines[1].timeMs)
        assertEquals("Repeated chorus", parsed.lines[1].text)
    }

    @Test
    fun testGetCurrentLineIndex() {
        val lrc = """
            [00:02.00]First
            [00:05.00]Second
            [00:10.00]Third
        """.trimIndent()

        val parsed = LyricsHelper.parseLyrics(lrc)

        // Before any line starts
        assertEquals(-1, parsed.getCurrentLineIndex(1000L))

        // At exactly first line
        assertEquals(0, parsed.getCurrentLineIndex(2000L))

        // In between first and second
        assertEquals(0, parsed.getCurrentLineIndex(3500L))

        // At second line
        assertEquals(1, parsed.getCurrentLineIndex(5000L))

        // At third line
        assertEquals(2, parsed.getCurrentLineIndex(12000L))
    }

    @Test
    fun testParsePlainLyrics() {
        val plain = """
            Verse 1
            This is plain text without any timestamps
            Chorus
            Another line
        """.trimIndent()

        val parsed = LyricsHelper.parseLyrics(plain)
        assertFalse(parsed.isSynced)
        assertEquals(4, parsed.lines.size)
        assertEquals(-1L, parsed.lines[0].timeMs)
        assertEquals("Verse 1", parsed.lines[0].text)
    }

    @Test
    fun testParseNullOrEmptyLyrics() {
        val emptyParsed = LyricsHelper.parseLyrics("")
        assertFalse(emptyParsed.isSynced)
        assertTrue(emptyParsed.lines.isEmpty())

        val nullParsed = LyricsHelper.parseLyrics(null)
        assertFalse(nullParsed.isSynced)
        assertTrue(nullParsed.lines.isEmpty())
    }
}
