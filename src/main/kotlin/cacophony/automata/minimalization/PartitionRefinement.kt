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

    // Refines the partition
    // Returns a list of pairs (OldId, NewId), indicating that the OldId set has been split into two sets (OldId, NewId)
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

    fun getPartitionId(e: E): PartitionId = elementToPartitionId[e] ?: throw IllegalArgumentException("This element is not in the base set")

    fun getElements(id: PartitionId): Set<E> = partitionToElements[id] ?: throw IllegalArgumentException("This partition does not exist")

    fun getAllPartitions(): Collection<Set<E>> = partitionToElements.values
}
