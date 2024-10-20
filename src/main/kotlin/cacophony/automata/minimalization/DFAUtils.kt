package cacophony.automata.minimalization

import cacophony.automata.DFA
import cacophony.automata.SimpleDFA

class DFAPreimagesCalculator<DFAState>(
    dfa: DFA<DFAState>,
) {
    private val cache: Map<DFAState, Map<Char, Set<DFAState>>> =
        dfa
            .getProductions()
            .entries
            .groupBy({ it.value }, { it.key })
            .mapValues { entry ->
                entry.value.groupBy({ it.second }, { it.first }).mapValues { it.value.toSet() }
            }

    fun getPreimages(states: Set<DFAState>): Map<Char, Set<DFAState>> =
        states
            .mapNotNull { state -> cache[state]?.let { state to it } }
            .flatMap { it.second.entries }
            .flatMap { entry -> entry.value.map { entry.key to it } }
            .groupBy({ it.first }, { it.second })
            .mapValues { it.value.toSet() }
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

fun <DFAState> DFA<DFAState>.withAliveReachableStates(): DFA<DFAState> = withStates(getAliveStates() intersect getReachableStates())

fun <DFAState> DFA<DFAState>.withStates(retain: Set<DFAState>): DFA<DFAState> {
    if (!retain.contains(getStartingState())) {
        return getDFAForEmptyLanguage()
    }

    val productions = getProductions().filter { retain.contains(it.key.first) && retain.contains(it.value) }
    val fullDFA = this

    return SimpleDFA(
        fullDFA.getStartingState(),
        productions,
        retain.filter { fullDFA.isAccepting(it) }.toSet(),
    )
}

infix fun <DFAState> DFAState.via(label: Char): Pair<DFAState, Char> = Pair(this, label)
