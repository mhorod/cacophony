package cacophony.utils

import cacophony.graphs.getProperTransitiveClosure
import cacophony.graphs.getReachableFrom
import cacophony.graphs.getStronglyConnectedComponents
import cacophony.graphs.getTransitiveClosure
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class GraphUtilsTest {
    private val graph =
        mapOf(
            1 to setOf(2, 3),
            2 to setOf(3, 4),
            3 to setOf(),
            4 to setOf(5),
            5 to setOf(4, 6),
            50 to setOf(60),
            60 to setOf(50),
            100 to setOf(100),
            200 to setOf(),
        )

    @Test
    fun `set of elements reachable from empty set is empty`() {
        assertThat(getReachableFrom(emptySet(), graph)).isEmpty()
    }

    @Test
    fun `getReachableFrom handles duplicates correctly`() {
        assertThat(getReachableFrom(listOf(2, 2, 100, 2, 100), graph)).containsExactlyInAnyOrder(2, 3, 4, 5, 6, 100)
    }

    @Test
    fun `getTransitiveClosure works correctly`() {
        val actual = getTransitiveClosure(graph)
        val expected =
            mapOf(
                1 to setOf(1, 2, 3, 4, 5, 6),
                2 to setOf(2, 3, 4, 5, 6),
                3 to setOf(3),
                4 to setOf(4, 5, 6),
                5 to setOf(4, 5, 6),
                6 to setOf(6),
                50 to setOf(50, 60),
                60 to setOf(50, 60),
                100 to setOf(100),
                200 to setOf(200),
            )
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `getProperTransitiveClosure works correctly`() {
        val actual = getProperTransitiveClosure(graph)
        val expected =
            mapOf(
                1 to setOf(2, 3, 4, 5, 6),
                2 to setOf(3, 4, 5, 6),
                3 to setOf(),
                4 to setOf(4, 5, 6),
                5 to setOf(4, 5, 6),
                6 to setOf(),
                50 to setOf(50, 60),
                60 to setOf(50, 60),
                100 to setOf(100),
                200 to setOf(),
            )
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `getStronglyConnectedComponents works correctly`() {
        val components = getStronglyConnectedComponents(graph)
        val componentId = components.withIndex().flatMap { (id, component) -> component.map { Pair(it, id) } }.toMap()

        assertThat(components).hasSize(8)
        assertThat(components[componentId[1]!!]).containsExactlyInAnyOrder(1)
        assertThat(components[componentId[2]!!]).containsExactlyInAnyOrder(2)
        assertThat(components[componentId[3]!!]).containsExactlyInAnyOrder(3)
        assertThat(components[componentId[4]!!]).containsExactlyInAnyOrder(4, 5)
        assertThat(components[componentId[5]!!]).containsExactlyInAnyOrder(4, 5)
        assertThat(components[componentId[6]!!]).containsExactlyInAnyOrder(6)
        assertThat(components[componentId[50]!!]).containsExactlyInAnyOrder(50, 60)
        assertThat(components[componentId[60]!!]).containsExactlyInAnyOrder(50, 60)
        assertThat(components[componentId[100]!!]).containsExactlyInAnyOrder(100)
        assertThat(components[componentId[200]!!]).containsExactlyInAnyOrder(200)

        for (i in graph.keys) {
            for (j in graph[i]!!) {
                assertThat(componentId[i]!!).isGreaterThanOrEqualTo(componentId[j]!!)
            }
        }
    }
}
