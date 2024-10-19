package cacophony.automata.minimalization

// Represents a set of the partition and allow for quick comparisons
data class PartitionId(
    val id: Int,
)

class PartitionRefinement<E>(
    baseSet: Collection<E>,
) {
    private val elementToPartitionId: MutableMap<E, PartitionId> = HashMap()
    private val partitionToElements: MutableMap<PartitionId, MutableSet<E>> = HashMap()

    init {
        baseSet.associateWithTo(elementToPartitionId) { PartitionId(0) }
        partitionToElements[PartitionId(0)] = baseSet.toMutableSet()
    }

    // Refines the partition. For each set U of partition, such that U \cap refineBy and U \ refineBy are both not empty,
    // set U is removed from partition, and sets U \cap refineBy and U \ refineBy are added to partition.
    //
    // Returns a list of pairs. Pair (U_ID, V_ID) indicates that the set with id U_ID has been removed from partition,
    // U \cap refineBy is represented by new id V_ID,
    // U \ refineBy reuses the old id U_ID (due to complexity).
    fun refine(refineBy: Collection<E>): List<Pair<PartitionId, PartitionId>> {
        return refineBy
            .groupBy(this::getPartitionId)
            .map { (oldId, elements) ->
                val oldPartition = partitionToElements[oldId]!!
                val newPartition = elements.toMutableSet()
                if (oldPartition.size == newPartition.size) return@map null

                val newId = PartitionId(partitionToElements.size)

                partitionToElements[newId] = newPartition
                oldPartition.removeAll(newPartition)
                for (e in newPartition) elementToPartitionId[e] = newId

                return@map Pair(oldId, newId)
            }.filterNotNull()
    }

    fun getPartitionId(e: E): PartitionId = elementToPartitionId[e] ?: throw IllegalArgumentException("Non existing element")

    fun getElements(id: PartitionId): Set<E> = partitionToElements[id] ?: throw IllegalArgumentException("Non existing partition")

    fun getAllPartitions(): Collection<Set<E>> = partitionToElements.values
}
