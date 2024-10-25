package cacophony.automata.minimalization

import cacophony.automata.DFA
import cacophony.automata.SimpleDFA
import cacophony.automata.buildNFAFromRegex
import cacophony.automata.determinize
import cacophony.utils.AlgebraicRegex

class DFAPreimagesCalculator<DFAState, AtomType>(
    dfa: DFA<DFAState, AtomType, *>,
) {
    private val cache: Map<DFAState, Map<AtomType, Set<DFAState>>> =
        dfa
            .getProductions()
            .entries
            .groupBy({ it.value }, { it.key })
            .mapValues { entry ->
                entry.value.groupBy({ it.second }, { it.first }).mapValues { it.value.toSet() }
            }

    // getPreimages(S)[c] is a set of all states t, such that a production (t, c, element from S) exists.
    fun getPreimages(states: Set<DFAState>): Map<AtomType, Set<DFAState>> =
        states
            .mapNotNull { state -> cache[state]?.let { state to it } }
            .flatMap { it.second.entries }
            .flatMap { entry -> entry.value.map { entry.key to it } }
            .groupBy({ it.first }, { it.second })
            .mapValues { it.value.toSet() }
}

// Returns a set of all elements reachable from a collection of nodes in a directed graph.
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

// Returns a set of states reachable from starting state.
fun <DFAState> DFA<DFAState, *, *>.getReachableStates(): Set<DFAState> {
    val graph = getProductions().map { Pair(it.key.first, it.value) }.groupBy({ it.first }, { it.second })
    return getReachableFrom(listOf(getStartingState()), graph)
}

// Returns a set of alive states (states such that there exist a path to some accepting state).
fun <DFAState> DFA<DFAState, *, *>.getAliveStates(): Set<DFAState> {
    val graph = getProductions().map { Pair(it.key.first, it.value) }.groupBy({ it.second }, { it.first })
    return getReachableFrom(getAllStates().filter(this::isAccepting), graph)
}

// Returns a copy of this DFA with only alive and reachable states.
fun <DFAState, AtomType, ResultType> DFA<DFAState, AtomType, ResultType>.withAliveReachableStates(): DFA<DFAState, AtomType, ResultType> =
    withStates(
        getAliveStates() intersect getReachableStates(),
    )

// Returns a copy of this DFA with all states not belonging to retain set removed.
// IllegalArgumentException iff retain does not contain starting state
fun <DFAState, AtomType, ResultType> DFA<DFAState, AtomType, ResultType>.withStates(
    retain: Set<DFAState>,
): DFA<DFAState, AtomType, ResultType> {
    if (!retain.contains(getStartingState())) {
        throw IllegalArgumentException("Cannot remove starting state")
    }

    val productions = getProductions().filter { retain.contains(it.key.first) && retain.contains(it.value) }
    val fullDFA = this

    return SimpleDFA(
        fullDFA.getStartingState(),
        productions,
        retain.mapNotNull {
            val result = fullDFA.result(it)
            if (result != null) it to result else null
        }.toMap(),
    )
}

// TODO: During refactor, move all of this to some utils file outside of minimalization directory.

infix fun <DFAState, AtomType> DFAState.via(label: AtomType): Pair<DFAState, AtomType> = Pair(this, label)

// Some utility functions, to not write whole pipeline each time
fun <AtomType> buildDFAFromRegex(regex: AlgebraicRegex<AtomType>) = determinize(buildNFAFromRegex(regex)).minimalize().makeIntDfa()

fun buildDFAFromRegex(regex: String) = buildDFAFromRegex(AlgebraicRegex.fromString(regex))

fun <DFAState, AtomType, ResultType> DFA<DFAState, AtomType, ResultType>.makeIntDfa(): DFA<Int, AtomType, ResultType> {
    val oldToNew: MutableMap<DFAState, Int> = mutableMapOf()
    var stateCounter = 0
    // It is not necessary, but it is nice to have 0 as starting state.
    oldToNew[getStartingState()] = stateCounter++
    for (state in getAllStates()) oldToNew.computeIfAbsent(state) { stateCounter++ }
    val productions = getProductions().map { (key, value) -> oldToNew[key.first]!! via key.second to oldToNew[value]!! }.toMap()
    val results = getAllStates().mapNotNull { state -> result(state)?.let { res -> oldToNew[state]!! to res } }.toMap()
    return SimpleDFA(0, productions, results)
}
