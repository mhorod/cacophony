package cacophony.automata

import kotlin.collections.mutableMapOf
import kotlin.collections.mutableSetOf
import kotlin.collections.setOf

public fun <StateType, AtomType> determinize(nfa: NFA<StateType, AtomType>): DFA<Int, AtomType, Unit> =
    determinize(
        nfa,
        nfa.getProductions().keys.fold(mutableSetOf()) { atoms, (_, atom) ->
            atoms.add(atom)
            atoms
        },
    )

private fun <StateType, AtomType> determinize(
    nfa: NFA<StateType, AtomType>,
    atoms: Iterable<AtomType>,
): DFA<Int, AtomType, Unit> {
    val startingState: Set<StateType> = nfa.epsilonClosure(setOf(nfa.getStartingState()))
    val createdStates = mutableSetOf(startingState)
    val worklist = ArrayDeque(listOf(startingState))
    val dfaProductions = mutableMapOf<Set<StateType>, MutableMap<AtomType, Set<StateType>>>()

    while (!worklist.isEmpty()) {
        val states = worklist.removeFirst()

        dfaProductions.getOrPut(states) { mutableMapOf() }.run {
            atoms.forEach {
                nfa.getSetEdge(states, it).also { neighbor ->
                    this[it] = neighbor
                    createdStates.add(neighbor) && worklist.add(neighbor)
                }
            }
        }
    }

    val setToInt = createdStates.mapIndexed { index, state -> state to index }.toMap()
    val results =
        createdStates
            .filter { it.any { state -> nfa.isAccepting(state) } }
            .map { setToInt[it]!! }
            .associate { it to Unit }
    val productions =
        dfaProductions
            .flatMap { (state, edges) ->
                edges.map { setToInt[state]!! via it.key to setToInt[it.value]!! }
            }.toMap()
    return SimpleDFA(setToInt[startingState]!!, productions, results)
}

private fun <StateType> NFA<StateType, *>.epsilonClosure(states: Collection<StateType>): MutableSet<StateType> {
    val queue = ArrayDeque(states)
    val visited = states.toMutableSet()

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
