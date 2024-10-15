package cacophony.automata

interface DFA<DFAState> {
    // Checks if provided state is accepting
    fun isAccepting(state: DFAState): Boolean

    // Returns state produced from provided state and symbol, or null if it doesn't exist.
    fun getProduction(
        state: DFAState,
        symbol: Char,
    ): DFAState?

    // Returns all productions.
    // Returned value is map accepting current state and symbol, and returning new state, which may not exist.
    fun getProductions(): Map<Pair<DFAState, Char>, DFAState>
}
