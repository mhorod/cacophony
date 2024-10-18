package cacophony.automata

import kotlin.collections.mutableListOf
import kotlin.collections.mutableMapOf
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
    var list = mutableListOf(startingState)
    var dfaProductions = mutableMapOf<Set<StateType>, MutableMap<Char, Set<StateType>>>()

    while (!list.isEmpty()) {
        val states = list.removeFirst()

        dfaProductions.getOrPut(states) { mutableMapOf() }.run {
            charset.forEach { this[it] = nfa.getSetEdge(states, it) }
        }
    }

    val setToInt = dfaProductions.keys.mapIndexed { index, states -> states to index }.toMap()
    val acceptingStates = dfaProductions.keys.filter { it.any { nfa.isAccepting(it) } }.map { setToInt[it]!! }.toSet()
    val productions =
        dfaProductions
            .flatMap { (state, edges) -> edges.map { Pair(setToInt[state]!!, it.key) to setToInt[it.value]!! } }
            .toMap()
    return SimpleDFA(setToInt[startingState]!!, productions, acceptingStates)
}

private fun <StateType> NFA<StateType>.epsilonClosure(states: Iterable<StateType>): MutableSet<StateType> {
    var queue = states.toMutableList()
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

private class SimpleDFA<StateType>(
    private val start: StateType,
    private val prod: Map<Pair<StateType, Char>, StateType>,
    private val accept: Set<StateType>,
) : DFA<StateType> {
    val all = prod.keys.map { (state, _) -> state }

    public override fun getStartingState() = start

    public override fun isAccepting(state: StateType) = state in accept

    public override fun getAllStates() = all

    public override fun getProductions() = prod

    public override fun getProduction(
        state: StateType,
        symbol: Char,
    ) = prod[state to symbol]
}
