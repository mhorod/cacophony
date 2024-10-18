package cacophony.automata.minimalization

import cacophony.automata.DFA

class DFAPreimagesCalculator<DFAState>(dfa: DFA<DFAState>) {
    private val cache: Map<DFAState, Map<Char, Set<DFAState>>>

    init {
        val result: MutableMap<DFAState, MutableMap<Char, MutableSet<DFAState>>> = mutableMapOf()
        dfa.getProductions().forEach {
            val inner = result.getOrPut(it.value) { mutableMapOf() }
            inner.getOrPut(it.key.second) { mutableSetOf() }.add(it.key.first)
        }
        cache = result
    }

    fun getPreimages(states: Set<DFAState>): Map<Char, Set<DFAState>> {
        val result: MutableMap<Char, MutableSet<DFAState>> = mutableMapOf()
        for (state in states) {
            cache[state]?.forEach { result.getOrPut(it.key) { mutableSetOf() }.addAll(it.value) }
        }
        return result
    }
}

fun <DFAState> getDFAForEmptyLanguage(): DFA<DFAState> = TODO("DFA interface does not allow empty languages")

fun <E> getReachableFrom(
    from: Collection<E>,
    graph: Map<E, Collection<E>>,
): Set<E> {
    val reachable = from.toMutableSet()
    val workList = from.toMutableList()
    while (workList.isNotEmpty()) {
        val u = workList.removeLast()
        for (v in graph.getOrDefault(u, listOf())) {
            if (!reachable.contains(v)) {
                reachable.add(v)
                workList.add(v)
            }
        }
    }
    return reachable
}

fun <DFAState> DFA<DFAState>.getReachableStates(): Set<DFAState> {
    val graph = getProductions().map { Pair(it.key.first, it.value) }.groupBy({ it.first }, { it.second })
    return getReachableFrom(listOf(getStartingState()), graph)
}

fun <DFAState> DFA<DFAState>.getAliveStates(): Set<DFAState> {
    val graph = getProductions().map { Pair(it.key.first, it.value) }.groupBy({ it.second }, { it.first })
    return getReachableFrom(getAllStates().filter(this::isAccepting), graph)
}

fun <DFAState> DFA<DFAState>.withAliveReachableStates(): DFA<DFAState> {
    return withStates(getAliveStates() intersect getReachableStates())
}

fun <DFAState> DFA<DFAState>.withStates(retain: Set<DFAState>): DFA<DFAState> {
    if (!retain.contains(getStartingState())) {
        return getDFAForEmptyLanguage()
    }

    val productions = getProductions().filter { retain.contains(it.key.first) && retain.contains(it.value) }
    val retainList = retain.toList()
    val fullDFA = this

    return object : DFA<DFAState> {
        override fun getStartingState(): DFAState {
            return fullDFA.getStartingState()
        }

        override fun getAllStates(): List<DFAState> {
            return retainList
        }

        override fun getProductions(): Map<Pair<DFAState, Char>, DFAState> {
            return productions
        }

        override fun getProduction(
            state: DFAState,
            symbol: Char,
        ): DFAState? {
            return productions[Pair(state, symbol)]
        }

        override fun isAccepting(state: DFAState): Boolean {
            return fullDFA.isAccepting(state)
        }
    }
}
