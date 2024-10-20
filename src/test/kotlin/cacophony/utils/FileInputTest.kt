package cacophony.utils

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.File

class FileInputTest {
    private fun createFileWithContent(content: String): String {
        val file = File.createTempFile("test_file", ".cac")
        file.deleteOnExit()
        file.writeText(content)
        return file.path
    }

    @Test
    fun `next simple`() {
        val path = createFileWithContent("abcde")
        val input = FileInput(path)

        assertEquals('a', input.next())
        assertEquals('b', input.next())
        assertEquals('c', input.next())
        assertEquals('d', input.next())
        assertEquals('e', input.next())
        // not checking first character after last - it is '\n'
        input.next()
        assertEquals(null, input.next())
        assertEquals(null, input.next())
    }

    @Test
    fun `peek simple`() {
        val path = createFileWithContent("abcde")
        val input = FileInput(path)

        assertEquals('a', input.peek())
        input.next()
        assertEquals('b', input.peek())
        input.next()
        assertEquals('c', input.peek())
        input.next()
        assertEquals('d', input.peek())
        input.next()
        assertEquals('e', input.peek())
        input.next()
        input.next()
        assertEquals(null, input.peek())
        input.next()
        assertEquals(null, input.peek())
    }

    @Test
    fun `skip simple`() {
        val path = createFileWithContent("aab bab\nacc\ndbd ab\n")
        val input = FileInput(path)

        input.skip('b')
        assertEquals('b', input.peek())
        input.skip(' ')
        assertEquals(' ', input.peek())
        input.skip('\n')
        assertEquals('\n', input.peek())
        input.skip('c')
        assertEquals('c', input.peek())
        input.skip('d')
        assertEquals('d', input.peek())
        input.skip('a')
        assertEquals('a', input.peek())
        input.skip('d')
        assertEquals(null, input.peek())
    }

    @Test
    fun `next with whitespaces`() {
        val path = createFileWithContent("abc de\nf g\n")
        val input = FileInput(path)

        assertEquals('a', input.next())
        assertEquals('b', input.next())
        assertEquals('c', input.next())
        assertEquals(' ', input.next())
        assertEquals('d', input.next())
        assertEquals('e', input.next())
        assertEquals('\n', input.next())
        assertEquals('f', input.next())
        assertEquals(' ', input.next())
        assertEquals('g', input.next())
        assertEquals('\n', input.next())
        assertEquals(null, input.next())
        assertEquals(null, input.next())
    }

    @Test
    fun `get and set location in the first character`() {
        val path = createFileWithContent("abcde")
        val input = FileInput(path)

        val locA = input.getLocation()
        input.next()
        input.next()

        input.setLocation(locA)
        assertEquals('a', input.next())
    }

    @Test
    fun `get and set location in a single line`() {
        val path = createFileWithContent("abcde")
        val input = FileInput(path)

        input.next()
        val locB = input.getLocation()
        input.next()
        input.next()
        val locD = input.getLocation()

        input.setLocation(locB)
        assertEquals('b', input.next())
        input.setLocation(locD)
        assertEquals('d', input.next())
        input.setLocation(locB)
        assertEquals('b', input.next())
    }

    @Test
    fun `get and set location in a multiple lines`() {
        val path = createFileWithContent("abc de\nf g\nhijk\n")
        val input = FileInput(path)

        input.next()
        val locB = input.getLocation()
        input.next()
        input.next()
        input.next()
        val locD = input.getLocation()
        input.next()
        input.next()
        val locNewLine = input.getLocation()
        input.next()
        input.next()
        val locSpace = input.getLocation()
        input.next()
        val locG = input.getLocation()
        input.next()
        input.next()
        val locH = input.getLocation()
        input.next()
        input.next()
        val locJ = input.getLocation()
        input.next()
        input.next()

        input.setLocation(locG)
        assertEquals('g', input.next())
        input.setLocation(locNewLine)
        assertEquals('\n', input.next())
        input.setLocation(locB)
        assertEquals('b', input.next())
        input.setLocation(locJ)
        assertEquals('j', input.next())
        input.setLocation(locSpace)
        assertEquals(' ', input.next())
        input.setLocation(locG)
        assertEquals('g', input.next())
        input.setLocation(locD)
        assertEquals('d', input.next())
        input.setLocation(locH)
        assertEquals('h', input.next())
    }

    @Test
    fun `next after set location`() {
        val path = createFileWithContent("abc de\nf g\nhijk\n")
        val input = FileInput(path)

        input.next()
        val locB = input.getLocation()
        input.next()
        input.next()
        input.next()
        val locD = input.getLocation()
        input.next()
        input.next()
        val locNewLine = input.getLocation()
        input.next()
        input.next()
        val locSpace = input.getLocation()
        input.next()
        val locG = input.getLocation()
        input.next()
        input.next()
        input.next()
        input.next()
        val locJ = input.getLocation()
        input.next()
        input.next()

        input.setLocation(locG)
        input.next()
        input.next()
        assertEquals('h', input.next())
        input.setLocation(locNewLine)
        input.next()
        assertEquals('f', input.next())
        input.setLocation(locB)
        input.next()
        assertEquals('c', input.next())
        input.setLocation(locJ)
        input.next()
        assertEquals('k', input.next())
        input.setLocation(locSpace)
        input.next()
        input.next()
        input.next()
        assertEquals('h', input.next())
        input.setLocation(locD)
        input.next()
        input.next()
        assertEquals('\n', input.next())
    }

    @Test
    fun `locationToString in a single line`() {
        val path = createFileWithContent("abcd e")
        val input = FileInput(path)

        input.next()
        val locB = input.getLocation()
        input.next()
        input.next()
        val locD = input.getLocation()
        input.next()
        val locSpace = input.getLocation()

        assertEquals("line 0, position 1 with 'b'", input.locationToString(locB))
        assertEquals("line 0, position 3 with 'd'", input.locationToString(locD))
        assertEquals("line 0, position 4 with ' '", input.locationToString(locSpace))
    }

    @Test
    fun `locationToString in multiple lines`() {
        val path = createFileWithContent("abc de\nf g\nhijk\n")
        val input = FileInput(path)

        input.next()
        val locB = input.getLocation()
        input.next()
        input.next()
        input.next()
        val locD = input.getLocation()
        input.next()
        input.next()
        val locNewLine = input.getLocation()
        input.next()
        input.next()
        val locSpace = input.getLocation()
        input.next()
        val locG = input.getLocation()
        input.next()
        input.next()
        val locH = input.getLocation()
        input.next()
        input.next()
        val locJ = input.getLocation()
        input.next()
        input.next()

        assertEquals("line 0, position 1 with 'b'", input.locationToString(locB))
        assertEquals("line 0, position 4 with 'd'", input.locationToString(locD))
        assertEquals("line 0, position 6 with '\n'", input.locationToString(locNewLine))
        assertEquals("line 1, position 1 with ' '", input.locationToString(locSpace))
        assertEquals("line 1, position 2 with 'g'", input.locationToString(locG))
        assertEquals("line 2, position 0 with 'h'", input.locationToString(locH))
        assertEquals("line 2, position 2 with 'j'", input.locationToString(locJ))
    }

    @Test
    fun `locationRangeToString in a single line`() {
        val path = createFileWithContent("abcd ef")
        val input = FileInput(path)

        input.next()
        val locB = input.getLocation()
        input.next()
        input.next()
        input.next()
        input.next()
        val locE = input.getLocation()
        input.next()

        assertEquals(
            "from line 0, position 1 to line 0, position 5 with \"bcd \"",
            input.locationRangeToString(locB, locE),
        )
    }

    @Test
    fun `locationRangeToString in multiple lines`() {
        val path = createFileWithContent("abc de\nf g\nhijk\n")
        val input = FileInput(path)

        input.next()
        val locB = input.getLocation()
        for (i in 0..11) {
            input.next()
        }
        val locJ = input.getLocation()
        input.next()
        input.next()
        input.next()
        input.next()
        input.next()
        val locNull = input.getLocation()

        assertEquals(
            "from line 0, position 1 to line 2, position 2 with \"bc de\nf g\nhi\"",
            input.locationRangeToString(locB, locJ),
        )
        assertEquals(
            "from line 2, position 2 to line 3, position 0 with \"jk\n\"",
            input.locationRangeToString(locJ, locNull),
        )
    }

    @Test
    fun `all functionalities together`() {
        val path = createFileWithContent("abc de\nf g\nhijk\n")
        val input = FileInput(path)

        input.next()
        input.next()
        assertEquals('c', input.next())
        input.next()
        val locD = input.getLocation()
        input.next()
        input.next()
        input.next()
        assertEquals("line 0, position 4 with 'd'", input.locationToString(locD))
        input.next()
        val locSpace = input.getLocation()
        input.next()
        assertEquals('g', input.peek())
        input.skip('h')
        assertEquals('h', input.peek())
        val locH = input.getLocation()
        input.next()
        input.next()
        val locJ = input.getLocation()
        input.next()
        assertEquals(
            "from line 2, position 0 to line 2, position 2 with \"hi\"",
            input.locationRangeToString(locH, locJ),
        )
        input.next()
        input.setLocation(locSpace)
        input.next()
        assertEquals('g', input.next())
        assertEquals("line 2, position 2 with 'j'", input.locationToString(locJ))
        input.setLocation(locD)
        assertEquals('d', input.next())
    }
}
