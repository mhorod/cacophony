package cacophony.automata

fun createDFA(
    starting: Int,
    accepting: Set<Int>,
    productions: Map<Pair<Int, Char>, Int>,
): DFA<Int> {
    val allStates =
        setOf(starting)
            .union(accepting)
            .union(productions.keys.map { it.first })
            .union(productions.values)
            .toList()

    return object : DFA<Int> {
        override fun getStartingState(): Int = starting

        override fun getAllStates(): List<Int> = allStates

        override fun getProductions(): Map<Pair<Int, Char>, Int> = productions

        override fun getProduction(
            state: Int,
            symbol: Char,
        ): Int? = productions[state via symbol]

        override fun isAccepting(state: Int): Boolean = accepting.contains(state)
    }
}

infix fun Int.via(label: Char): Pair<Int, Char> = Pair(this, label)
