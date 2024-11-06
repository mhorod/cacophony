package cacophony.automata

import cacophony.automata.minimization.minimize
import cacophony.utils.AlgebraicRegex

infix fun <DFAState, AtomType> DFAState.via(label: AtomType): Pair<DFAState, AtomType> = Pair(this, label)

// Some utility functions, to not write whole pipeline each time
fun <AtomType> buildDFAFromRegex(regex: AlgebraicRegex<AtomType>) = determinize(buildNFAFromRegex(regex)).minimize().makeIntDfa()

fun <AtomType, ResulType> buildDFAFromRegex(
    regex: AlgebraicRegex<AtomType>,
    result: ResulType,
) = joinAutomata(listOf(buildDFAFromRegex(regex) to result))

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

// Returns map from state to its predecessors per atom on edge.
fun <StateType, AtomType, ResultType> reverse(dfa: DFA<StateType, AtomType, ResultType>): Map<StateType, Map<AtomType, Set<StateType>>> =
    dfa
        .getProductions()
        .asSequence()
        .map { (it.value to it.key.second) to it.key.first }
        .groupBy({ it.first }, { it.second })
        .map { it.key.first to (it.key.second to it.value.toSet()) }
        .groupBy({ it.first }, { it.second })
        .map { it.key to it.value.toMap() }
        .toMap()
