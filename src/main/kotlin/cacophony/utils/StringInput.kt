package cacophony.utils

import kotlin.math.min

class StringInput(private val text: String) : Input {
    // Current position of the cursor in string.
    private var position = 0

    override fun next(): Char? {
        val nextChar = text.elementAtOrNull(position)
        position++
        return nextChar
    }

    override fun peek(): Char? {
        return text.elementAtOrNull(position)
    }

    override fun skip(c: Char) {
        while ((peek() != null) and (peek() != c)) {
            position++
        }
    }

    override fun getLocation(): Location {
        return Location(position)
    }

    override fun setLocation(loc: Location) {
        position = loc.value
    }

    override fun locationToString(loc: Location): String {
        return "position ${loc.value} with '${text.elementAtOrNull(loc.value)}'"
    }

    override fun locationRangeToString(
        locBegin: Location,
        locEnd: Location,
    ): String {
        assert(locBegin.value <= locEnd.value)

        val content = text.substring(locBegin.value, min(locEnd.value, text.length))
        return "positions from ${locBegin.value} to ${locEnd.value} with \"$content\""
    }
}
