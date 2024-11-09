package cacophony.automata

import cacophony.automata.minimization.minimize
import cacophony.utils.AlgebraicRegex

infix fun <DFAState, AtomT> DFAState.via(label: AtomT): Pair<DFAState, AtomT> = Pair(this, label)

// Some utility functions, to not write whole pipeline each time
fun <AtomT> buildDFAFromRegex(regex: AlgebraicRegex<AtomT>) = determinize(buildNFAFromRegex(regex)).minimize().makeIntDfa()

fun buildDFAFromRegex(regex: String) = buildDFAFromRegex(AlgebraicRegex.fromString(regex))

fun <DFAState, AtomT, ResultT> DFA<DFAState, AtomT, ResultT>.makeIntDfa(): DFA<Int, AtomT, ResultT> {
    val oldToNew: MutableMap<DFAState, Int> = mutableMapOf()
    var stateCounter = 0
    // It is not necessary, but it is nice to have 0 as starting state.
    oldToNew[getStartingState()] = stateCounter++
    for (state in getAllStates()) oldToNew.computeIfAbsent(state) { stateCounter++ }
    val productions = getProductions().map { (key, value) -> oldToNew[key.first]!! via key.second to oldToNew[value]!! }.toMap()
    val results = getAllStates().mapNotNull { state -> result(state)?.let { res -> oldToNew[state]!! to res } }.toMap()
    return SimpleDFA(0, productions, results)
}

// Returns map from state to its predecessors per atom on edge.
fun <StateT, AtomT, ResultT> reverse(dfa: DFA<StateT, AtomT, ResultT>): Map<StateT, Map<AtomT, Set<StateT>>> =
    dfa
        .getProductions()
        .asSequence()
        .map { (it.value to it.key.second) to it.key.first }
        .groupBy({ it.first }, { it.second })
        .map { it.key.first to (it.key.second to it.value.toSet()) }
        .groupBy({ it.first }, { it.second })
        .map { it.key to it.value.toMap() }
        .toMap()
