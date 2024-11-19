package cacophony.automata

interface NFA<StateT, AtomT> {
    // Returns starting state
    fun getStartingState(): StateT

    // Returns accepting state - it has to be unique
    fun getAcceptingState(): StateT

    // Checks if provided state is accepting
    fun isAccepting(state: StateT): Boolean

    // Returns all NFA states
    fun getAllStates(): List<StateT>

    // Returns list of reachable states, returned list may be empty.
    fun getProductions(state: StateT, symbol: AtomT): List<StateT>

    // Returns all non-epsilon productions.
    // Returned value is map accepting current state and symbol, and returning all reachable states.
    fun getProductions(): Map<Pair<StateT, AtomT>, List<StateT>>

    // Returns all epsilon productions.
    // Returned value is map accepting current state, and returning all states reachable by single epsilon production.
    fun getEpsilonProductions(): Map<StateT, List<StateT>>
}

data class SimpleNFA<AtomT>(
    private val start: Int,
    private val prod: Map<Pair<Int, AtomT>, List<Int>>,
    private val epsProd: Map<Int, List<Int>>,
    private val accept: Int,
) : NFA<Int, AtomT> {
    private val all =
        (
            setOf(start, accept) union
                prod.keys
                    .unzip()
                    .first
                    .toSet() union prod.values.flatten().toSet() union epsProd.keys union
                epsProd.values
                    .flatten()
                    .toSet()
        ).toList()

    override fun getStartingState() = start

    override fun getAcceptingState() = accept

    override fun isAccepting(state: Int): Boolean = state == accept

    override fun getAllStates() = all

    override fun getProductions() = prod

    override fun getProductions(state: Int, symbol: AtomT) = prod[state to symbol] ?: emptyList()

    override fun getEpsilonProductions(): Map<Int, List<Int>> = epsProd
}
