package io.github.karino2.itsunani

import org.junit.Test

import org.junit.Assert.*

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class LinesTest {
    val testContent = """
        Hello
        - 2021-04-19 19:53 line 1
        - 2021-04-21 13:22 line 2
        
        other line
        
        - 2021-05-07 15:33 line 3
        - 2021-05-20 16:24 line 4
        
        last
    """.trimIndent()


    @Test
    fun itsuNaniLines_splitCorrect() {
        val target = Lines(testContent)
        val iLines = target.itsuNaniLines

        assertEquals(4, iLines.count)
        val line = iLines.at(0)
        assertEquals("2021-04-19 19:53", line.date)
        assertEquals("line 1", line.body)
    }

    @Test
    fun itsuNaniLines_update() {
        val target = Lines(testContent)
        val iLines = target.itsuNaniLines

        iLines.updateEntry(2, "hogehoge")

        val actual = iLines.at(2)
        assertEquals("2021-05-07 15:33", actual.date)
        assertEquals("hogehoge", actual.body)
    }

/*
        Hello
        - 2021-04-19 19:53 line 1
        - 2021-04-21 13:22 line 2

        other line

        - 2021-05-07 15:33 line 3
        - 2021-05-20 16:24 line 4

        last

 */
    @Test
    fun allLines_recoverOriginal() {
        val target = Lines(testContent)
        val actual = target.allLines.toList()
        assertEquals(10, actual.size)
        assertEquals("Hello", actual[0])
        assertEquals("- 2021-04-19 19:53 line 1", actual[1])
        assertEquals(testContent, actual.joinToString("\n"))
    }

    @Test
    fun allLines_modified_correct() {
        val target = Lines(testContent)
        target.itsuNaniLines.updateEntry(1, "hogehoge")
        val actual = target.allLines.toList()
        assertEquals(10, actual.size)
        assertEquals("Hello", actual[0])
        assertEquals("- 2021-04-21 13:22 hogehoge", actual[2])
    }

}
