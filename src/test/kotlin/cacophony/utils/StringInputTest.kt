package cacophony.utils

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class StringInputTest {
    @Test
    fun `next simple`() {
        val input = StringInput("abcde")

        assertEquals('a', input.next())
        assertEquals('b', input.next())
        assertEquals('c', input.next())
        assertEquals('d', input.next())
        assertEquals('e', input.next())
        assertEquals(null, input.next())
        assertEquals(null, input.next())
    }

    @Test
    fun `peek simple`() {
        val input = StringInput("abcde")

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
        assertEquals(null, input.peek())
        input.next()
        assertEquals(null, input.peek())
    }

    @Test
    fun `skip simple`() {
        val input = StringInput("aababaccdbdab")

        input.skip('b')
        assertEquals('b', input.peek())
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
    fun `get and set location`() {
        val input = StringInput("abcde")

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
    fun `next after set location`() {
        val input = StringInput("abcde")

        input.next()
        val locB = input.getLocation()
        input.next()
        input.next()
        val locD = input.getLocation()

        input.setLocation(locB)
        input.next()
        assertEquals('c', input.next())
        input.setLocation(locD)
        input.next()
        input.next()
        assertEquals(null, input.next())
    }

    @Test
    fun `locationToString simple`() {
        val input = StringInput("abcd e")

        input.next()
        val locB = input.getLocation()
        input.next()
        input.next()
        val locD = input.getLocation()
        input.next()
        val locSpace = input.getLocation()
        input.next()
        input.next()
        val locNull = input.getLocation()
        input.next()

        assertEquals("position 1 with 'b'", input.locationToString(locB))
        assertEquals("position 3 with 'd'", input.locationToString(locD))
        assertEquals("position 4 with ' '", input.locationToString(locSpace))
        assertEquals("position 6 with 'null'", input.locationToString(locNull))
    }

    @Test
    fun `locationRangeToString simple`() {
        val input = StringInput("abcd efg")

        input.next()
        input.next()
        val locC = input.getLocation()
        input.next()
        input.next()
        input.next()
        input.next()
        input.next()
        val locG = input.getLocation()
        input.next()
        input.next()
        val locNull = input.getLocation()

        assertEquals("positions from 2 to 7 with \"cd efg\"", input.locationRangeToString(locC, locG))
        assertEquals("positions from 2 to 9 with \"cd efg\"", input.locationRangeToString(locC, locNull))
    }

    @Test
    fun `all functionalities together`() {
        val input = StringInput("abc de\nf g\nhijk\n")

        input.next()
        input.next()
        assertEquals('c', input.next())
        input.next()
        val locD = input.getLocation()
        input.next()
        input.next()
        input.next()
        assertEquals("position 4 with 'd'", input.locationToString(locD))
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
        assertEquals("positions from 11 to 13 with \"hij\"", input.locationRangeToString(locH, locJ))
        input.next()
        input.setLocation(locSpace)
        input.next()
        assertEquals('g', input.next())
        assertEquals("position 13 with 'j'", input.locationToString(locJ))
        input.setLocation(locD)
        assertEquals('d', input.next())
    }
}
