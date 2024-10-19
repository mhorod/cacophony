package cacophony.utils

class StringInput(private val text: String) : Input {
    // current position of the cursor in string
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
}
