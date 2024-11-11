package cacophony.utils

import java.io.File

class FileInput(
    filePath: String,
) : Input {
    private val file = File(filePath)
    private val lines = file.readLines().toMutableList()
    private val lineBeginPos = mutableListOf<Int>()
    private val totalLength: Int
    private var curLineInd = 0
    private var curPos = 0

    init {
        // Add new line character to every line (even if it might not have been present in the last one).
        lines.forEachIndexed { ind, line ->
            lines[ind] = line + "\n"
        }

        // Calculate position of each begin of line in the string formed by the concatenation of all lines.
        // Used to encode Location.
        var linesLengthSum = 0
        lineBeginPos.add(0)
        for (line in lines) {
            linesLengthSum += line.length
            lineBeginPos.add(linesLengthSum)
        }
        totalLength = linesLengthSum
    }

    private fun advancePosition() {
        if (curLineInd >= lines.size) return

        curPos++
        if (curPos >= lines[curLineInd].length) {
            curPos = 0
            curLineInd++
        }
    }

    private fun getCharAtPosition(
        lineInd: Int,
        pos: Int,
    ): Char? {
        if (lines.elementAtOrNull(lineInd) == null) return null
        return lines[lineInd].elementAtOrNull(pos)
    }

    // Location is encoded as the position in the string formed by the concatenation of all lines.
    private fun encodeLocation(
        lineInd: Int,
        pos: Int,
    ): Location = Location(lineBeginPos[lineInd] + pos)

    // Returns first after last position if not in range.
    private fun decodeLocation(loc: Location): Pair<Int, Int> {
        if (loc.value < 0 || loc.value >= totalLength) {
            return Pair(lines.size, 0)
        }

        var lineInd = lineBeginPos.binarySearch(loc.value)
        if (lineInd < 0) lineInd = -(lineInd + 2)
        return Pair(lineInd, loc.value - lineBeginPos[lineInd])
    }

    override fun next(): Char? {
        val nextChar = getCharAtPosition(curLineInd, curPos)
        advancePosition()
        return nextChar
    }

    override fun peek(): Char? = getCharAtPosition(curLineInd, curPos)

    override fun skip(c: Char) {
        while ((peek() != null) and (peek() != c)) {
            advancePosition()
        }
    }

    override fun getLocation(): Location = encodeLocation(curLineInd, curPos)

    override fun setLocation(loc: Location) {
        val (lineInd, pos) = decodeLocation(loc)
        curLineInd = lineInd
        curPos = pos
    }

    override fun locationToString(loc: Location): String {
        val (lineInd, pos) = decodeLocation(loc)
        val charAtLoc = getCharAtPosition(lineInd, pos)
        return "line $lineInd, position $pos with '$charAtLoc'"
    }

    override fun locationRangeToString(
        locBegin: Location,
        locEnd: Location,
    ): String {
        if (locBegin == locEnd) return locationToString(locBegin)

        val (lineIndBegin, posBegin) = decodeLocation(locBegin)
        val (lineIndEnd, posEnd) = decodeLocation(locEnd)

        assert(lineIndBegin < lineIndEnd || posBegin <= posEnd)

        var content = ""
        if (lineIndBegin == lineIndEnd) {
            if (lines.elementAtOrNull(lineIndBegin) != null) {
                content = lines[lineIndBegin].substring(posBegin, posEnd)
            }
        } else {
            content = lines[lineIndBegin].substring(posBegin)
            for (lineInd in lineIndBegin + 1..<lineIndEnd) {
                content += lines[lineInd]
            }
            if (lines.elementAtOrNull(lineIndEnd) != null) {
                content += lines[lineIndEnd].substring(0, posEnd)
            }
        }

        return "from line $lineIndBegin, position $posBegin to line $lineIndEnd, position $posEnd with \"$content\""
    }
}
