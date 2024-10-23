package cacophony.automata

// TODO: Changes here are only till Jan merges his PR

interface GenericDFA<DFAState, Atom, Result> {
    // Returns starting state
    fun getStartingState(): DFAState

    // Checks if provided state is accepting
    fun isAccepting(state: DFAState): Boolean

    // Returns resulting type of state
    fun result(state: DFAState): Result

    // Returns all DFA states
    fun getAllStates(): List<DFAState>

    // Returns state produced from provided state and symbol, or null if it doesn't exist.
    fun getProduction(
        state: DFAState,
        symbol: Atom,
    ): DFAState?

    // Returns all productions.
    // Returned value is map accepting current state and symbol, and returning new state, which may not exist.
    fun getProductions(): Map<Pair<DFAState, Atom>, DFAState>
}

interface DFA<DFAState> : GenericDFA<DFAState, Char, Boolean> {
    override fun result(state: DFAState): Boolean = isAccepting(state)
}

public data class SimpleDFA<StateType>(
    private val start: StateType,
    private val prod: Map<Pair<StateType, Char>, StateType>,
    private val accept: Set<StateType>,
) : DFA<StateType> {
    val all =
        (
            prod.keys
                .unzip()
                .first
                .toSet() union setOf(start) union accept union prod.values.toSet()
        ).toList()

    public override fun getStartingState() = start

    public override fun isAccepting(state: StateType) = state in accept

    public override fun getAllStates() = all

    public override fun getProductions() = prod

    public override fun getProduction(
        state: StateType,
        symbol: Char,
    ) = prod[state to symbol]
}
