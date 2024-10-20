package cacophony.automata.minimalization

import cacophony.automata.DFA
import cacophony.automata.SimpleDFA

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

// Returns a minimalized copy of this DFA, with dead/unreachable states removed.
fun <DFAState> DFA<DFAState>.minimalize(): DFA<ContractedDFAState<DFAState>> = minimalizeImpl(withAliveReachableStates())

// minimalize() helper function. Assumes dfa contains only alive and reachable states.
private fun <DFAState> minimalizeImpl(dfa: DFA<DFAState>): DFA<ContractedDFAState<DFAState>> {
    val preimagesCalculator = DFAPreimagesCalculator(dfa)

    val acceptingStates = dfa.getAllStates().filter(dfa::isAccepting)
    val refineStructure = PartitionRefinement(dfa.getAllStates())

    refineStructure.refine(acceptingStates)
    val queue = dfa.getAllStates().map { refineStructure.getPartitionId(it) }.toMutableSet()

    while (queue.isNotEmpty()) {
        val partitionId = queue.first().also { queue.remove(it) }
        val preimages: Map<Char, Set<DFAState>> = preimagesCalculator.getPreimages(refineStructure.getElements(partitionId))

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

    val newAcceptingStates = acceptingStates.map { toNewState[it]!! }.toSet()
    val newStartingState = toNewState[dfa.getStartingState()]!!
    val newProductions =
        dfa
            .getProductions()
            .map { (kv, result) ->
                val (from, symbol) = kv
                val newFrom = toNewState[from]!!
                val newResult = toNewState[result]!!
                return@map Pair(Pair(newFrom, symbol), newResult)
            }.toMap()

    return SimpleDFA(
        newStartingState,
        newProductions,
        newAcceptingStates,
    )
}
