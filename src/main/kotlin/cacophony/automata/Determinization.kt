package cacophony.automata

import kotlin.collections.mutableMapOf
import kotlin.collections.mutableSetOf
import kotlin.collections.setOf

fun <StateT, AtomT> determinize(nfa: NFA<StateT, AtomT>): DFA<Int, AtomT, Unit> =
    determinize(
        nfa,
        nfa.getProductions().keys.fold(mutableSetOf()) { atoms, (_, atom) ->
            atoms.add(atom)
            atoms
        },
    )

private fun <StateT, AtomT> determinize(nfa: NFA<StateT, AtomT>, atoms: Iterable<AtomT>): DFA<Int, AtomT, Unit> {
    val startingState: Set<StateT> = nfa.epsilonClosure(setOf(nfa.getStartingState()))
    val createdStates = mutableSetOf(startingState)
    val worklist = ArrayDeque(listOf(startingState))
    val dfaProductions = mutableMapOf<Set<StateT>, MutableMap<AtomT, Set<StateT>>>()

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

private fun <StateT> NFA<StateT, *>.epsilonClosure(states: Collection<StateT>): MutableSet<StateT> {
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

private fun <StateT, AtomT> NFA<StateT, AtomT>.getSetEdge(states: Set<StateT>, symbol: AtomT): Set<StateT> =
    this.epsilonClosure(states).let { closure ->
        this.epsilonClosure(closure.flatMap { this.getProductions(it, symbol) }.toSet())
    }
