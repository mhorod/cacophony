package cacophony.automata

interface DFAEquivalenceHelper<StateA, StateB> {
    fun areDistinguishable(
        a: StateA,
        b: StateB,
    ): Boolean

    fun areEquivalent(
        a: StateA,
        b: StateB,
    ): Boolean {
        return !areDistinguishable(a, b)
    }
}

fun <StateA, StateB, AtomType, ResultType> areEquivalent(
    dfaA: DFA<StateA, AtomType, ResultType>,
    dfaB: DFA<StateB, AtomType, ResultType>,
): Boolean {
    return createDFAEquivalenceHelper(dfaA, dfaB).areEquivalent(dfaA.getStartingState(), dfaB.getStartingState())
}

fun <StateA, StateB, AtomType, ResultType> createDFAEquivalenceHelper(
    dfaA: DFA<StateA, AtomType, ResultType>,
    dfaB: DFA<StateB, AtomType, ResultType>,
): DFAEquivalenceHelper<StateA, StateB> {
    val distinguishable = initializeDistinguishableStates(dfaA, dfaB)
    return object : DFAEquivalenceHelper<StateA, StateB> {
        override fun areDistinguishable(
            a: StateA,
            b: StateB,
        ): Boolean {
            return distinguishable.contains(Pair(a, b))
        }
    }
}

private fun <StateA, StateB, AtomType, ResultType> initializeDistinguishableStates(
    dfaA: DFA<StateA, AtomType, ResultType>,
    dfaB: DFA<StateB, AtomType, ResultType>,
): Set<Pair<StateA?, StateB?>> {
    val symbols = getSymbols(dfaA.getProductions()) union getSymbols(dfaB.getProductions())
    val invA = invertProductions(dfaA, symbols)
    val invB = invertProductions(dfaB, symbols)
    val distinguishable = mutableSetOf<Pair<StateA?, StateB?>>()
    for (a in dfaA.getAllStates())
        for (b in dfaB.getAllStates())
            if (dfaA.result(a) != dfaB.result(b)) {
                distinguishable.add(Pair(a, b))
            }
    dfaA.getAllStates().filter { dfaA.result(it) != null }.forEach { distinguishable.add(Pair(it, null)) }
    dfaB.getAllStates().filter { dfaB.result(it) != null }.forEach { distinguishable.add(Pair(null, it)) }
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
    return distinguishable
}

private fun <State, AtomType> getSymbols(productions: Map<Pair<State, AtomType>, State>): Set<AtomType> {
    return productions.keys.map { it.second }.toSet()
}

private fun <State, AtomType> invertProductions(
    dfa: DFA<State, AtomType, *>,
    symbols: Set<AtomType>,
): Map<Pair<State?, AtomType>, Set<State?>> {
    val inverted = mutableMapOf<Pair<State?, AtomType>, MutableSet<State?>>()
    for (symbol in symbols) {
        for (state in dfa.getAllStates()) {
            val newState = dfa.getProduction(state, symbol)
            inverted.getOrPut(Pair(newState, symbol)) { mutableSetOf() }.add(state)
        }
        inverted.getOrPut(Pair(null, symbol)) { mutableSetOf() }.add(null)
    }
    return inverted
}
