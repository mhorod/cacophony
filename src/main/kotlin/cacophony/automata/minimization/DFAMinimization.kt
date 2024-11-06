package cacophony.automata.minimization

import cacophony.automata.DFA
import cacophony.automata.SimpleDFA
import cacophony.automata.via

private fun <E> PartitionRefinement<E>.smallerSet(
    a: PartitionId,
    b: PartitionId,
): PartitionId = if (getElements(a).size < getElements(b).size) a else b

// This is class and not dataclass to make equals() and hashcode() test for object identity,
// which is sufficient in our case and makes sure there are no checks for the list equality.
class ContractedDFAState<DFAState>(
    states: List<DFAState>,
) {
    val originalStates: List<DFAState> = states

    override fun toString(): String = "ContractedDFAState(originalStates=$originalStates)"
}

// Returns a minimized copy of this DFA, with dead/unreachable states removed.
// Throws IllegalArgumentException if DFA is invalid (i.e. it does not accept any word).
fun <DFAState, AtomType, ResultType> DFA<DFAState, AtomType, ResultType>.minimize():
    DFA<ContractedDFAState<DFAState>, AtomType, ResultType> =
    minimizeImpl(
        withAliveReachableStates(),
    )

// minimize() helper function. Assumes dfa contains only alive and reachable states.
private fun <DFAState, AtomType, ResultType> minimizeImpl(
    dfa: DFA<DFAState, AtomType, ResultType>,
): DFA<ContractedDFAState<DFAState>, AtomType, ResultType> {
    val preimagesCalculator = DFAPreimagesCalculator(dfa)

    val refineStructure = PartitionRefinement(dfa.getAllStates())

    val initialPartitions = dfa.getAllStates().groupBy { dfa.result(it) }.values
    initialPartitions.forEach {
        refineStructure.refine(it)
    }

    val queue = dfa.getAllStates().map { refineStructure.getPartitionId(it) }.toMutableSet()

    while (queue.isNotEmpty()) {
        val partitionId = queue.first().also { queue.remove(it) }
        val preimages: Map<AtomType, Set<DFAState>> = preimagesCalculator.getPreimages(refineStructure.getElements(partitionId))

        for (preimageClass in preimages.values) {
            for ((oldId, newId) in refineStructure.refine(preimageClass)) {
                if (queue.contains(oldId)) {
                    queue.add(newId)
                } else {
                    queue.add(refineStructure.smallerSet(oldId, newId))
                }
            }
        }
    }

    val toNewState: MutableMap<DFAState, ContractedDFAState<DFAState>> = HashMap()
    val allNewStates =
        refineStructure.getAllPartitions().map {
            val newState = ContractedDFAState(it.toList())
            for (e in newState.originalStates) toNewState[e] = newState
            return@map newState
        }

    val newResults =
        allNewStates
            .mapNotNull {
                val result = dfa.result(it.originalStates[0])
                if (result != null) it to result else null
            }.toMap()
    val newStartingState = toNewState[dfa.getStartingState()]!!
    val newProductions =
        dfa
            .getProductions()
            .map { (kv, result) ->
                val (from, symbol) = kv
                val newFrom = toNewState[from]!!
                val newResult = toNewState[result]!!
                return@map (newFrom via symbol to newResult)
            }.toMap()

    return SimpleDFA(
        newStartingState,
        newProductions,
        newResults,
    )
}
