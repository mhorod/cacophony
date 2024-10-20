package cacophony.automata.minimalization

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class PartitionRefinementTest {
    private fun initRefinement(n: Int) = PartitionRefinement((0..<n).toList())

    private val part =
        object {
            operator fun get(vararg values: Int): MutableSet<Set<Int>> = mutableSetOf(values.toSet())
        }

    private operator fun MutableSet<Set<Int>>.get(vararg values: Int): MutableSet<Set<Int>> =
        apply {
            this.add(values.toSet())
        }

    @Test
    fun `getAllPartitions works after three refinements`() {
        val pr = initRefinement(7)
        pr.refine(listOf(0, 1, 2, 3))
        assertEquals(part[0, 1, 2, 3][4, 5, 6], pr.getAllPartitions().toSet())
        pr.refine(listOf(2, 3, 4))
        assertEquals(part[0, 1][2, 3][4][5, 6], pr.getAllPartitions().toSet())
        pr.refine(listOf(1))
        assertEquals(part[0][1][2, 3][4][5, 6], pr.getAllPartitions().toSet())
    }

    @Test
    fun `refinement with duplicates`() {
        val pr = initRefinement(2)
        val refinements = pr.refine(listOf(0, 0))
        assertEquals(1, refinements.size)
        assertEquals(part[0][1], pr.getAllPartitions().toSet())
    }

    @Test
    fun `refine zero sets`() {
        val pr = initRefinement(4)
        pr.refine(listOf(0, 1))
        val refinements = pr.refine(listOf(0, 1))
        assertEquals(0, refinements.size)
    }

    @Test
    fun `refine two sets`() {
        val pr = initRefinement(4)
        pr.refine(listOf(0, 1))
        val refinements = pr.refine(listOf(1, 2))
        assertEquals(2, refinements.size)
    }

    @Test
    fun `getPartitionId identifies a partition`() {
        val pr = initRefinement(7)
        pr.refine(listOf(0, 1, 2, 3, 4))
        pr.refine(listOf(2, 3, 4))

        assertEquals(part[0, 1][2, 3, 4][5, 6], pr.getAllPartitions().toSet())

        assertEquals(pr.getPartitionId(0), pr.getPartitionId(1))
        assertEquals(pr.getPartitionId(2), pr.getPartitionId(3))
        assertEquals(pr.getPartitionId(2), pr.getPartitionId(4))
        assertEquals(pr.getPartitionId(5), pr.getPartitionId(6))

        assertNotEquals(pr.getPartitionId(0), pr.getPartitionId(2))
        assertNotEquals(pr.getPartitionId(2), pr.getPartitionId(5))
        assertNotEquals(pr.getPartitionId(0), pr.getPartitionId(5))
    }

    @Test
    fun `refine returns partition numbers`() {
        val pr = initRefinement(4)
        val refinement = pr.refine(listOf(0, 1))

        assertEquals(1, refinement.size)

        (0..<4).forEach {
            assertTrue(pr.getPartitionId(it) in refinement[0].toList())
        }
    }

    @Test
    fun `refine returns different numbers`() {
        val pr = initRefinement(8)
        pr.refine(listOf(0, 1, 2, 3))
        pr.refine(listOf(0, 1, 4, 5))
        val refinement = pr.refine(listOf(0, 2, 4, 6))

        val partitionIds = refinement.flatMap { it.toList() }.toSet()
        assertEquals(8, partitionIds.size)
    }

    @Test
    fun `refine returns consistent partition ids between iterations`() {
        val pr = initRefinement(8)
        pr.refine(listOf(0, 1, 2, 3))
        val ref1 = pr.refine(listOf(0, 1, 4, 5)).toMap()
        val ref2 = pr.refine(listOf(0, 2, 4, 6)).toMap()

        ref2.keys.forEach {
            assertTrue(it in ref1.keys || it in ref1.values)
        }

        ref2.values.forEach {
            assertTrue(it !in ref1.keys && it !in ref1.values)
        }
    }

    @Test
    fun `refine returns subsets of original partition`() {
        val pr = initRefinement(8)
        pr.refine(listOf(0, 1, 2, 3))
        val oldElementsByPartId = (0..<8).groupBy { pr.getPartitionId(it) }.mapValues { it.value.toSet() }
        val refinement = pr.refine(listOf(0, 1, 5))
        val newElementsByPartId = (0..<8).groupBy { pr.getPartitionId(it) }.mapValues { it.value.toSet() }

        refinement.forEach {
            assertTrue(oldElementsByPartId[it.first]!!.containsAll(newElementsByPartId[it.first]!!))
            assertTrue(oldElementsByPartId[it.first]!!.containsAll(newElementsByPartId[it.second]!!))
        }
    }

    @Test
    fun `getElements is consistent with getPartitionId`() {
        val pr = initRefinement(8)
        pr.refine(listOf(0, 1, 2, 3))
        pr.refine(listOf(0, 1, 5))

        (0..<8).forEach {
            assertTrue(it in pr.getElements(pr.getPartitionId(it)))
        }
    }

    @Test
    fun `getElements returns elements of partition`() {
        val pr = initRefinement(8)
        pr.refine(listOf(0, 1, 2, 3))
        pr.refine(listOf(0, 1, 5))

        assertEquals(setOf(0, 1), pr.getElements(pr.getPartitionId(0)))
        assertEquals(setOf(2, 3), pr.getElements(pr.getPartitionId(2)))
        assertEquals(setOf(4, 6, 7), pr.getElements(pr.getPartitionId(4)))
        assertEquals(setOf(5), pr.getElements(pr.getPartitionId(5)))
    }

    @Test
    fun `getPartitionId throws IllegalArgumentException on invalid element`() {
        val pr = initRefinement(42)
        assertThrows<IllegalArgumentException> {
            pr.getPartitionId(42)
        }
    }

    @Test
    fun `getElements throws IllegalArgumentException on invalid partitionId`() {
        val pr = initRefinement(2)
        pr.refine(listOf(0))

        val invalidPartitionIds = mutableSetOf(0, 1, 2)
        invalidPartitionIds.remove(pr.getPartitionId(0).id)
        invalidPartitionIds.remove(pr.getPartitionId(1).id)
        val invalidId = PartitionId(invalidPartitionIds.first())

        assertThrows<IllegalArgumentException> {
            pr.getElements(invalidId)
        }
    }
}
