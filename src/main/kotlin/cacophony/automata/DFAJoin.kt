package cacophony.automata

import cacophony.automata.minimalization.via

// Joins given list of the automata into a single DFA with accepting states marked
// with a Result corresponding to the source DFAs.
// Throws if any state would be marked by many results, i.e. when any two DFAs accept the same word.
fun <State, Atom, Result> joinAutomata(
    automataAndResults: List<Pair<GenericDFA<State, Atom, *>, Result>>,
): GenericDFA<Int, Atom, Result?> {
    if (automataAndResults.isEmpty()) throw IllegalArgumentException("Provided empty list of automaton")
    val automata = automataAndResults.unzip().first

    // Get result for the given state or null if it is not accepting.
    // Throws if the result is ambiguous.
    val resultForState = fun(states: List<State?>): Result? {
        val accept = (automataAndResults zip states).filter { (pair, state) -> state != null && pair.first.isAccepting(state) }
        if (accept.size > 1) throw IllegalArgumentException("Provided automata are ambiguous")
        val res = accept.firstOrNull() ?: return null
        return res.first.second
    }

    // Apply the transition to every element of list.
    val transition = fun(
        states: List<State?>,
        atom: Atom,
    ): List<State?> = (automata zip states).map { (automaton, state) -> state?.let { automaton.getProduction(it, atom) } }

    // Parts of the final automaton.
    val productions: MutableMap<Pair<Int, Atom>, Int> = mutableMapOf()
    val results: MutableMap<Int, Result?> = mutableMapOf()

    // Preparation to the graph search.
    val viableTransitions = automata.flatMap { it.getProductions().keys.map { it.second } }.toSet()
    val oldStateToNew: MutableMap<List<State?>, Int> = mutableMapOf()
    val queue = ArrayDeque<Pair<List<State?>, Int>>()
    val startingState = automata.map { it.getStartingState() }
    var stateCounter = 0
    queue.add(startingState to 0)
    oldStateToNew[startingState] = stateCounter++
    while (!queue.isEmpty()) {
        val (currentState, currentName) = queue.removeFirst()
        // Throws if currentState has an ambiguous Result
        results[currentName] = resultForState(currentState)
        for (atom in viableTransitions) {
            val nextState = transition(currentState, atom)
            val nextName =
                oldStateToNew.getOrPut(nextState) {
                    queue.addLast(nextState to stateCounter)
                    stateCounter++
                }
            productions[currentName via atom] = nextName
        }
    }

    // TODO: Do we need to clear dead states?
    // TODO: minimalize?
    return JoinedDFA(0, stateCounter, productions, results)
}

private class JoinedDFA<Atom, Result>(
    private val start: Int,
    size: Int,
    private val productions: Map<Pair<Int, Atom>, Int>,
    private val results: Map<Int, Result?>,
) : GenericDFA<Int, Atom, Result?> {
    private val allStates = (start..<size).toList()

    override fun getStartingState(): Int = start

    override fun getAllStates(): List<Int> = allStates

    override fun getProductions(): Map<Pair<Int, Atom>, Int> = productions

    override fun getProduction(
        state: Int,
        symbol: Atom,
    ): Int? = productions[state via symbol]

    override fun result(state: Int): Result? = results[state]

    override fun isAccepting(state: Int): Boolean = results[state] != null
}
