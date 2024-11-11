package cacophony.automata

interface DFA<StateT, AtomT, ResultT> {
    // Returns starting state
    fun getStartingState(): StateT

    // Checks if provided state is accepting
    fun isAccepting(state: StateT): Boolean

    // Returns specific result
    fun result(state: StateT): ResultT?

    // Returns all DFA states
    fun getAllStates(): List<StateT>

    // Returns state produced from provided state and symbol, or null if it doesn't exist.
    fun getProduction(
        state: StateT,
        symbol: AtomT,
    ): StateT?

    // Returns all productions.
    // Returned value is map accepting current state and symbol, and returning new state, which may not exist.
    fun getProductions(): Map<Pair<StateT, AtomT>, StateT>
}

class SimpleDFA<StateT, AtomT, ResultT>(
    private val start: StateT,
    private val prod: Map<Pair<StateT, AtomT>, StateT>,
    private val results: Map<StateT, ResultT>,
) : DFA<StateT, AtomT, ResultT> {
    val all =
        (
            prod.keys
                .unzip()
                .first
                .toSet() union setOf(start) union results.keys.toSet() union prod.values.toSet()
        ).toList()

    override fun getStartingState() = start

    override fun isAccepting(state: StateT) = state in results

    override fun result(state: StateT) = results[state]

    override fun getAllStates() = all

    override fun getProductions() = prod

    override fun getProduction(
        state: StateT,
        symbol: AtomT,
    ) = prod[state to symbol]
}
