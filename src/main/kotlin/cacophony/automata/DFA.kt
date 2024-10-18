package cacophony.automata

interface DFA<DFAState> {
    // Returns starting state
    fun getStartingState(): DFAState

    // Checks if provided state is accepting
    fun isAccepting(state: DFAState): Boolean

    // Returns all DFA states
    fun getAllStates(): List<DFAState>

    // Returns state produced from provided state and symbol, or null if it doesn't exist.
    fun getProduction(
        state: DFAState,
        symbol: Char,
    ): DFAState?

    // Returns all productions.
    // Returned value is map accepting current state and symbol, and returning new state, which may not exist.
    fun getProductions(): Map<Pair<DFAState, Char>, DFAState>
}

public class SimpleDFA<StateType>(
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
