package cacophony.automata

import kotlin.collections.mutableMapOf
import kotlin.collections.mutableSetOf
import kotlin.collections.setOf

public fun <StateType, AtomType> determinize(nfa: NFA<StateType, AtomType>): DFA<Int, AtomType, Boolean> =
    determinize(
        nfa,
        nfa.getProductions().keys.fold(mutableSetOf()) { atomset, (_, atom) ->
            atomset.add(atom)
            atomset
        },
    )

private fun <StateType, AtomType> determinize(
    nfa: NFA<StateType, AtomType>,
    atomset: Iterable<AtomType>,
): DFA<Int, AtomType, Boolean> {
    val startingState: Set<StateType> = nfa.epsilonClosure(setOf(nfa.getStartingState()))
    var createdStates = mutableSetOf(startingState)
    var worklist = ArrayDeque(listOf(startingState))
    var dfaProductions = mutableMapOf<Set<StateType>, MutableMap<AtomType, Set<StateType>>>()

    while (!worklist.isEmpty()) {
        val states = worklist.removeFirst()

        dfaProductions.getOrPut(states) { mutableMapOf() }.run {
            atomset.forEach {
                nfa.getSetEdge(states, it).also { neighbor ->
                    this[it] = neighbor
                    createdStates.add(neighbor) && worklist.add(neighbor)
                }
            }
        }
    }

    val setToInt = createdStates.mapIndexed { index, state -> state to index }.toMap()
    val acceptingStates =
        createdStates
            .filter { it.any { nfa.isAccepting(it) } }
            .map { setToInt[it]!! }
            .associate { it to true }
    val productions =
        dfaProductions
            .flatMap { (state, edges) ->
                edges.map { Pair(setToInt[state]!!, it.key) to setToInt[it.value]!! }
            }.toMap()
    return SimpleDFA(setToInt[startingState]!!, productions, acceptingStates)
}

private fun <StateType> NFA<StateType, *>.epsilonClosure(states: Collection<StateType>): MutableSet<StateType> {
    var queue = ArrayDeque(states)
    var visited = states.toMutableSet()

    val epsilonProductions = this.getEpsilonProductions()

    while (!queue.isEmpty()) {
        queue.removeFirst().run { epsilonProductions.get(this) }?.forEach {
            visited.add(it) && queue.add(it)
        }
    }

    return visited
}

private fun <StateType, AtomType> NFA<StateType, AtomType>.getSetEdge(
    states: Set<StateType>,
    symbol: AtomType,
): Set<StateType> =
    this.epsilonClosure(states).let { closure ->
        this.epsilonClosure(closure.flatMap { this.getProductions(it, symbol) }.toSet())
    }
