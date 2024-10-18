package cacophony.automata.minimalization

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import kotlin.random.Random

class PartitionRefinementTest {
    @Test
    fun refineTest1() {
        val pr = PartitionRefinement(listOf(0, 1, 2, 3, 4))
        pr.refine(listOf(0, 2, 3, 4))

        val expected = setOf(setOf(0, 2, 3, 4), setOf(1))
        val actual = pr.getAllPartitions().toSet()
        assertEquals(expected, actual)
    }

    @Test
    fun refineTest2() {
        val pr = PartitionRefinement(listOf(0, 1, 2, 3, 4))
        pr.refine(listOf(0, 2, 3, 4))
        pr.refine(listOf(3))

        val expected = setOf(setOf(0, 2, 4), setOf(1), setOf(3))
        val actual = pr.getAllPartitions().toSet()
        assertEquals(expected, actual)
    }

    @Test
    fun refineWithDuplicates() {
        val pr = PartitionRefinement(listOf(0, 1))
        val refinements = pr.refine(listOf(0, 0))
        assertEquals(1, refinements.size)
    }

    private fun <E> refineOnce(
        partitions: Set<Set<E>>,
        refiningSet: Set<E>,
    ): Set<Set<E>> {
        return partitions
            .flatMap { listOf(it.intersect(refiningSet), it.subtract(refiningSet)) }
            .filter { it.isNotEmpty() }
            .toSet()
    }

    private fun getAllPartitionsLargeRandomOnce(
        n: Int,
        seed: Int,
    ) {
        val random = Random(seed)
        val base = (0..n).toSet()
        val pr = PartitionRefinement(base)
        var correct = setOf(base)

        for (it in 0..20) {
            assertEquals(correct, pr.getAllPartitions().toSet())

            val refiningSet = base.filter { _ -> random.nextBoolean() }.toSet()
            pr.refine(refiningSet)
            correct = refineOnce(correct, refiningSet)
        }
    }

    @Test
    fun getAllPartitionsLargeRandom() {
        (0..15).forEach { getAllPartitionsLargeRandomOnce(500, it) }
        (0..150).forEach { getAllPartitionsLargeRandomOnce(25, it) }
    }
}
