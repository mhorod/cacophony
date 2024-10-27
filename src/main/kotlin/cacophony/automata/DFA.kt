package cacophony.automata

interface DFA<DFAState, AtomType, ResultType> {
    // Returns starting state
    fun getStartingState(): DFAState

    // Checks if provided state is accepting
    fun isAccepting(state: DFAState): Boolean

    // Returns specific result
    fun result(state: DFAState): ResultType?

    // Returns all DFA states
    fun getAllStates(): List<DFAState>

    // Returns state produced from provided state and symbol, or null if it doesn't exist.
    fun getProduction(
        state: DFAState,
        symbol: AtomType,
    ): DFAState?

    // Returns all productions.
    // Returned value is map accepting current state and symbol, and returning new state, which may not exist.
    fun getProductions(): Map<Pair<DFAState, AtomType>, DFAState>
}

public data class SimpleDFA<StateType, AtomType, ResultType>(
    private val start: StateType,
    private val prod: Map<Pair<StateType, AtomType>, StateType>,
    private val results: Map<StateType, ResultType>,
) : DFA<StateType, AtomType, ResultType> {
    val all =
        (
            prod.keys
                .unzip()
                .first
                .toSet() union setOf(start) union results.keys.toSet() union prod.values.toSet()
        ).toList()

    public override fun getStartingState() = start

    public override fun isAccepting(state: StateType) = state in results

    public override fun result(state: StateType) = results[state]

    public override fun getAllStates() = all

    public override fun getProductions() = prod

    public override fun getProduction(
        state: StateType,
        symbol: AtomType,
    ) = prod[state to symbol]

    override fun equals(other: Any?): Boolean {
        return this === other
    }
}
