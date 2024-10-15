package cacophony.automata

interface NFA<NFAState> {
    // Returns starting state
    fun getStartingState(): NFAState

    // Returns accepting state - it has to be unique
    fun getAcceptingState(): NFAState

    // Checks if provided state is accepting
    fun isAccepting(state: NFAState): Boolean

    // Returns all NFA states
    fun getAllStates(): List<NFAState>

    // Returns list of reachable states, returned list may be empty.
    fun getProductions(
        state: NFAState,
        symbol: Char,
    ): List<NFAState>

    // Returns all non-epsilon productions.
    // Returned value is map accepting current state and symbol, and returning all reachable states.
    fun getProductions(): Map<Pair<NFAState, Char>, List<NFAState>>

    // Returns all epsilon productions.
    // Returned value is map accepting current state, and returning all states reachable by single epsilon production.
    fun getEpsilonProductions(): Map<NFAState, List<NFAState>>
}
