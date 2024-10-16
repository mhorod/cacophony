package cacophony.automata

interface DFAHelper<StateA, StateB> {
    fun areDistinguishable(a: StateA, b: StateB): Boolean

    fun areEquivalent(a: StateA, b: StateB): Boolean {
        return !areDistinguishable(a, b)
    }
}

fun <StateA, StateB> areEquivalent(dfaA: DFA<StateA>, dfaB: DFA<StateB>): Boolean {
    return createHelper(dfaA, dfaB).areEquivalent(dfaA.getStartingState(), dfaB.getStartingState())
}

fun <StateA, StateB> createHelper(dfaA: DFA<StateA>, dfaB: DFA<StateB>): DFAHelper<StateA, StateB> {
    val symbols = getSymbols(dfaA.getProductions()) union getSymbols(dfaB.getProductions())
    val invA = invertProductions(dfaA, symbols)
    val invB = invertProductions(dfaB, symbols)
    val distinguishable = mutableSetOf<Pair<StateA?, StateB?>>()
    for (a in dfaA.getAllStates())
        for (b in dfaB.getAllStates())
            if (dfaA.isAccepting(a) != dfaB.isAccepting(b)) {
                distinguishable.add(Pair(a, b))
            }
    dfaA.getAllStates().filter { dfaA.isAccepting(it) }.forEach { distinguishable.add(Pair(it, null)) }
    dfaB.getAllStates().filter { dfaB.isAccepting(it) }.forEach { distinguishable.add(Pair(null, it)) }
    val toVisit = ArrayDeque(distinguishable)
    while (!toVisit.isEmpty()) {
        val (currentA, currentB) = toVisit.removeFirst()
        for (symbol in symbols) {
            for (a in invA.getOrDefault(Pair(currentA, symbol), setOf())) {
                for (b in invB.getOrDefault(Pair(currentB, symbol), setOf())) {
                    if (!distinguishable.contains(Pair(a, b))) {
                        val discovered = Pair(a, b)
                        distinguishable.add(discovered)
                        toVisit.add(discovered)
                    }
                }
            }
        }
    }
    return object : DFAHelper<StateA, StateB> {
        override fun areDistinguishable(a: StateA, b: StateB): Boolean {
            return distinguishable.contains(Pair(a, b))
        }
    }
}

private fun <State> getSymbols(productions: Map<Pair<State, Char>, State>): Set<Char> {
    return productions.keys.map { it.second }.toSet()
}

private fun <State> invertProductions(dfa: DFA<State>, symbols: Set<Char>): Map<Pair<State?, Char>, Set<State?>> {
    val inverted = mutableMapOf<Pair<State?, Char>, MutableSet<State?>>()
    for (symbol in symbols) {
        for (state in dfa.getAllStates()) {
            val newState = dfa.getProduction(state, symbol)
            inverted.getOrPut(Pair(newState, symbol)) { mutableSetOf() }.add(state)
        }
        inverted.getOrPut(Pair(null, symbol)) { mutableSetOf() }.add(null)
    }
    return inverted
}
