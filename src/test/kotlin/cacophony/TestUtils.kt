package cacophony

import cacophony.automata.DFA
import cacophony.utils.Input
import cacophony.utils.Location

fun createDFA(
    starting: Int,
    productions: Map<Pair<Int, Char>, Int>,
    accepting: Set<Int>,
): DFA<Int> {
    val allStates =
        (
            setOf(starting) union accepting union
                productions.keys
                    .map { it.first }
                    .toSet() union productions.values
        ).toList()
    return object : DFA<Int> {
        override fun getStartingState(): Int = starting

        override fun getAllStates(): List<Int> = allStates

        override fun getProductions(): Map<Pair<Int, Char>, Int> = productions

        override fun getProduction(
            state: Int,
            symbol: Char,
        ): Int? = productions[Pair(state, symbol)]

        override fun isAccepting(state: Int): Boolean = accepting.contains(state)
    }
}

// TODO: This can be removed once Rafał implements inputs
fun createStringInput(str: String): Input {
    var cursor = -1

    return object : Input {
        override fun next(): Char? {
            if (cursor + 1 < str.length) {
                cursor += 1
                return str[cursor]
            }
            return null
        }

        override fun peek(): Char? {
            if (cursor + 1 < str.length) {
                return str[cursor + 1]
            }
            return null
        }

        override fun skip(c: Char) {
            while (cursor < str.length && str[cursor] != c) {
                cursor += 1
            }
        }

        override fun getLocation(): Location = Location(cursor)

        override fun setLocation(loc: Location) {
            cursor = loc.value
        }

        override fun locationToString(loc: Location): String = cursor.toString()
    }
}
