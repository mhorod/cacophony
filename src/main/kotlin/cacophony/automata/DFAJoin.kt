package cacophony.automata

import cacophony.automata.minimization.minimize

// Joins given list of the automata into a single DFA with accepting states marked
// with a Result corresponding to the source DFAs.
// Throws if any state would be marked by many results, i.e. when any two DFAs accept the same word.
fun <StateT, AtomT, ResultT> joinAutomata(automataAndResults: List<Pair<DFA<StateT, AtomT, *>, ResultT>>): DFA<Int, AtomT, ResultT> {
    if (automataAndResults.isEmpty()) throw IllegalArgumentException("Provided empty list of automaton")
    val automata = automataAndResults.unzip().first

    // Get result for the given state or null if it is not accepting.
    // Throws if the result is ambiguous.
    val resultForState = fun(states: List<StateT?>): ResultT? {
        val accept = (automataAndResults zip states).filter { (pair, state) -> state != null && pair.first.isAccepting(state) }
        if (accept.size > 1) throw IllegalArgumentException("Provided automata are ambiguous")
        val res = accept.firstOrNull() ?: return null
        return res.first.second
    }

    // Apply the transition to every element of list.
    val transition = fun(
        states: List<StateT?>,
        atom: AtomT,
    ): List<StateT?> = (automata zip states).map { (automaton, state) -> state?.let { automaton.getProduction(it, atom) } }

    // Parts of the final automaton.
    val productions: MutableMap<Pair<Int, AtomT>, Int> = mutableMapOf()
    val results: MutableMap<Int, ResultT> = mutableMapOf()

    // Preparation to the graph search.
    val viableTransitions = automata.flatMap { it.getProductions().keys.map { (_, atom) -> atom } }.toSet()
    val oldStateToNew: MutableMap<List<StateT?>, Int> = mutableMapOf()
    val queue = ArrayDeque<Pair<List<StateT?>, Int>>()
    val startingState = automata.map { it.getStartingState() }
    var stateCounter = 0
    queue.add(startingState to 0)
    oldStateToNew[startingState] = stateCounter++
    while (!queue.isEmpty()) {
        val (currentState, currentName) = queue.removeFirst()
        // Throws if the currentState has an ambiguous Result.
        resultForState(currentState)?.let { results[currentName] = it }
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

    return SimpleDFA(0, productions, results).minimize().makeIntDfa()
}
