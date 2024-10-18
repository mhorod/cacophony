package cacophony.automata

import kotlin.collections.mutableMapOf
import kotlin.collections.mutableSetOf
import kotlin.collections.setOf

public fun <StateType> determinize(nfa: NFA<StateType>): DFA<Int> =
    determinize(
        nfa,
        nfa.getProductions().keys.fold(mutableSetOf()) { charset, (_, char) ->
            charset.add(char)
            charset
        },
    )

private fun <StateType> determinize(
    nfa: NFA<StateType>,
    charset: Iterable<Char>,
): DFA<Int> {
    val startingState = setOf(nfa.getStartingState())
    var createdStates = mutableSetOf<Set<StateType>>(startingState)
    var worklist = ArrayDeque(listOf(startingState))
    var dfaProductions = mutableMapOf<Set<StateType>, MutableMap<Char, Set<StateType>>>()

    while (!worklist.isEmpty()) {
        val states = worklist.removeFirst()

        dfaProductions.getOrPut(states) { mutableMapOf() }.run {
            charset.forEach {
                nfa.getSetEdge(states, it).also { neighbor ->
                    this[it] = neighbor
                    createdStates.add(neighbor) && worklist.add(neighbor)
                }
            }
        }
    }

    val setToInt = createdStates.mapIndexed { index, states -> states to index }.toMap()
    val acceptingStates =
        createdStates
            .filter { it.any { nfa.isAccepting(it) } }
            .map { setToInt[it]!! }
            .toSet()
    val productions =
        dfaProductions
            .flatMap { (state, edges) ->
                edges.map { Pair(setToInt[state]!!, it.key) to setToInt[it.value]!! }
            }.toMap()
    return SimpleDFA(setToInt[startingState]!!, productions, acceptingStates)
}

private fun <StateType> NFA<StateType>.epsilonClosure(states: Collection<StateType>): MutableSet<StateType> {
    var queue = ArrayDeque(states)
    var visited = states.toMutableSet()
    var res = states.toMutableSet()

    val epsilonProductions = this.getEpsilonProductions()

    while (!queue.isEmpty()) {
        queue.removeFirst().run { epsilonProductions.get(this) }?.forEach {
            it.takeIf { visited.add(it) }?.let {
                res.add(it)
                queue.add(it)
            }
        }
    }

    return res
}

private fun <StateType> NFA<StateType>.getSetEdge(
    states: Set<StateType>,
    symbol: Char,
): Set<StateType> =
    this.epsilonClosure(states).also { closure ->
        closure.addAll(this.epsilonClosure(closure.flatMap { this.getProductions(it, symbol) }))
    }
