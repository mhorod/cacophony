package cacophony.graphs

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import kotlin.random.Random

class FirstFitGraphColoringTest {
    private fun validateFixedColorsAreRespected(fixedColors: Map<Int, Int>, coloring: Map<Int, Int>) {
        fixedColors.forEach { (v, c) ->
            assertThat(coloring).containsKey(v)
            assertThat(coloring[v]).isEqualTo(c)
        }
    }

    private fun validateColoringUsesAllowedColors(fixedColors: Map<Int, Int>, allowedColors: Collection<Int>, coloring: Map<Int, Int>) {
        coloring.forEach { (v, c) ->
            if (!fixedColors.containsKey(v)) {
                assertThat(allowedColors).contains(c)
            }
        }
    }

    private fun validateColoringIsProper(graph: Map<Int, Collection<Int>>, coloring: Map<Int, Int>) {
        graph.forEach { (v, neigh) ->
            neigh.forEach { u ->
                if (coloring.containsKey(v) && coloring.containsKey(u)) {
                    assertThat(coloring[v]).isNotEqualTo(coloring[u])
                }
            }
        }
    }

    private fun validateColoring(
        graph: Map<Int, Collection<Int>>,
        fixedColors: Map<Int, Int>,
        allowedColors: Collection<Int>,
        coloring: Map<Int, Int>,
    ) {
        validateFixedColorsAreRespected(fixedColors, coloring)
        validateColoringUsesAllowedColors(fixedColors, allowedColors, coloring)
        validateColoringIsProper(graph, coloring)
    }

    @Test
    fun `throws if graph contains loops`() {
        // given & when & then
        org.junit.jupiter.api.assertThrows<FirstFitColoringException> {
            FirstFitGraphColoring<Int, Int>().doColor(
                mapOf(0 to setOf(0)),
                mapOf(),
                mapOf(),
                setOf(),
            )
        }
    }

    @Test
    fun `properly colors empty graph with 0 available colors`() {
        // given
        val graph = mapOf<Int, Set<Int>>()
        val coalesce = mapOf<Int, Set<Int>>()
        val fixedColors = mapOf<Int, Int>()
        val allowedColors = setOf<Int>()

        // when
        val coloring =
            FirstFitGraphColoring<Int, Int>().doColor(
                graph,
                coalesce,
                fixedColors,
                allowedColors,
            )

        // then
        validateColoring(graph, fixedColors, allowedColors, coloring)
        assertThat(coloring).isEmpty()
    }

    @Test
    fun `properly colors with 1 register if there are no interferences`() {
        // given
        val graph = (1..50).associateWith { emptySet<Int>() }
        val coalesce = mapOf<Int, Set<Int>>()
        val fixedColors = mapOf<Int, Int>()
        val allowedColors = setOf(0)

        // when
        val coloring =
            FirstFitGraphColoring<Int, Int>().doColor(
                graph,
                coalesce,
                fixedColors,
                allowedColors,
            )

        // then
        validateColoring(graph, fixedColors, allowedColors, coloring)
        assertThat(coloring).containsExactlyInAnyOrderEntriesOf((1..50).associateWith { 0 })
    }

    @Test
    fun `2 vertices in coalesce relation are colored properly`() {
        // given
        val graph = mapOf<Int, Set<Int>>(0 to emptySet(), 1 to emptySet())
        val coalesce = mapOf(0 to setOf(1))
        val fixedColors = mapOf<Int, Int>()
        val allowedColors = setOf(0)

        // when
        val coloring =
            FirstFitGraphColoring<Int, Int>().doColor(
                graph,
                coalesce,
                fixedColors,
                allowedColors,
            )

        // then
        validateColoring(graph, fixedColors, allowedColors, coloring)
        assertThat(coloring).containsExactlyInAnyOrderEntriesOf(
            mapOf(
                0 to 0,
                1 to 0,
            ),
        )
    }

    @Test
    fun `2 vertices with copy are colored with the same color`() {
        // given
        val graph = mapOf<Int, Set<Int>>(0 to emptySet(), 1 to emptySet())
        val coalesce = mapOf(0 to setOf(1))
        val fixedColors = mapOf<Int, Int>()
        val allowedColors = setOf(0, 1)

        // when
        val coloring =
            FirstFitGraphColoring<Int, Int>().doColor(
                graph,
                coalesce,
                fixedColors,
                allowedColors,
            )

        // then
        validateColoring(graph, fixedColors, allowedColors, coloring)
        assertThat(coloring.keys).containsExactlyInAnyOrder(0, 1)
        assertThat(coloring[0]).isEqualTo(coloring[1])
    }

    @Test
    fun `2 vertices with fixed colors are colored accordingly even if their colors are not in allowed colors`() {
        // given
        val graph = mapOf<Int, Set<Int>>(0 to emptySet(), 1 to emptySet())
        val coalesce = mapOf<Int, Set<Int>>()
        val fixedColors = mapOf(0 to 2, 1 to 3)
        val allowedColors = setOf<Int>()

        // when
        val coloring =
            FirstFitGraphColoring<Int, Int>().doColor(
                graph,
                coalesce,
                fixedColors,
                allowedColors,
            )

        // then
        validateColoring(graph, fixedColors, allowedColors, coloring)
        assertThat(coloring).containsExactlyInAnyOrderEntriesOf(
            mapOf(
                0 to 2,
                1 to 3,
            ),
        )
    }

    @Test
    fun `16 clique is 16 colorable`() {
        // given
        val graph = (0..15).associateWith { (0..15).minus(it) }
        val coalesce = mapOf<Int, Set<Int>>()
        val fixedColors = mapOf<Int, Int>()
        val allowedColors = (0..15).toSet()

        // when
        val coloring =
            FirstFitGraphColoring<Int, Int>().doColor(
                graph,
                coalesce,
                fixedColors,
                allowedColors,
            )

        // then
        validateColoring(graph, fixedColors, allowedColors, coloring)
        assertThat(coloring.keys).containsExactlyInAnyOrderElementsOf(0..15)
    }

    @Test
    fun `interference takes priority over copying`() {
        // given
        val graph = mapOf(0 to setOf(1), 1 to setOf(0))
        val coalesce = mapOf(0 to setOf(1))
        val fixedColors = mapOf<Int, Int>()
        val allowedColors = setOf(2)

        // when
        val coloring =
            FirstFitGraphColoring<Int, Int>().doColor(
                graph,
                coalesce,
                fixedColors,
                allowedColors,
            )

        // then
        validateColoring(graph, fixedColors, allowedColors, coloring)
        assertThat(coloring.keys).hasSize(1)
    }

    @ParameterizedTest
    @ValueSource(ints = [5, 7, 21])
    fun `4-clique blowup`(n: Int) {
        // given
        val graph = mutableMapOf<Int, MutableSet<Int>>()
        for (i in 0..<n) {
            for (j in 0..<n) {
                if (i % 4 != j % 4)
                    graph.getOrPut(i) { mutableSetOf() }.add(j)
            }
        }
        val coalesce = mapOf<Int, Set<Int>>()
        val fixedColors = mapOf<Int, Int>()
        val allowedColors = setOf(1, 2, 3)

        // when
        val coloring =
            FirstFitGraphColoring<Int, Int>().doColor(
                graph,
                coalesce,
                fixedColors,
                allowedColors,
            )

        // then
        validateColoring(graph, fixedColors, allowedColors, coloring)
        assertThat((0..<n).filter { !coloring.containsKey(it) }).hasSize(n / 4)
    }

    @ParameterizedTest
    @ValueSource(ints = [4, 8, 20])
    fun `clique with 3 available colors`(n: Int) {
        // given
        val graph = mutableMapOf<Int, MutableSet<Int>>()
        for (i in 0..<n) {
            for (j in 0..<n) {
                if (i != j)
                    graph.getOrPut(i) { mutableSetOf() }.add(j)
            }
        }
        val coalesce = mapOf<Int, Set<Int>>()
        val fixedColors = mapOf<Int, Int>()
        val allowedColors = setOf(1, 2, 3)

        // when
        val coloring =
            FirstFitGraphColoring<Int, Int>().doColor(
                graph,
                coalesce,
                fixedColors,
                allowedColors,
            )

        // then
        validateColoring(graph, fixedColors, allowedColors, coloring)
        assertThat((0..<n).filter { !coloring.containsKey(it) }).hasSize(n - 3)
    }

    @Test
    fun `copying chain with one edge`() {
        // given
        val graph =
            mapOf(
                0 to emptySet(),
                1 to setOf(3),
                2 to emptySet(),
                3 to setOf(1),
                4 to emptySet(),
            )
        val coalesce = (1..4).associateWith { setOf(it - 1) }
        val fixedColors = mapOf<Int, Int>()
        val allowedColors = setOf(1, 2)

        // when
        val coloring =
            FirstFitGraphColoring<Int, Int>().doColor(
                graph,
                coalesce,
                fixedColors,
                allowedColors,
            )

        // then
        validateColoring(graph, fixedColors, allowedColors, coloring)
        assertThat(coloring.keys).containsExactlyInAnyOrder(0, 1, 2, 3, 4)
        assertThat(coloring[1]).isNotEqualTo(coloring[3])
        assertThat(coloring[0]).isEqualTo(coloring[2])
        assertThat(coloring[2]).isEqualTo(coloring[4])
    }

    @Test
    fun `copying chain with fixed colors`() {
        // given
        val graph = (0..4).associateWith { emptySet<Int>() }
        val coalesce = (1..4).associateWith { setOf(it - 1) }
        val fixedColors = mapOf(1 to 9, 3 to 10)
        val allowedColors = setOf(9, 10)

        // when
        val coloring =
            FirstFitGraphColoring<Int, Int>().doColor(
                graph,
                coalesce,
                fixedColors,
                allowedColors,
            )

        // then
        validateColoring(graph, fixedColors, allowedColors, coloring)
        assertThat(coloring.keys).containsExactlyInAnyOrder(0, 1, 2, 3, 4)
        assertThat(coloring[1]).isEqualTo(9)
        assertThat(coloring[3]).isEqualTo(10)
        assertThat(setOf(coloring[0], coloring[2], coloring[4])).hasSize(1)
    }

    @Test
    fun `bipartite clique`() {
        // given
        val graph =
            (0..<20).associateWith { v ->
                (0..<20)
                    .filter { u ->
                        u / 10 != v / 10
                    }.toSet()
            }
        val coalesce = mapOf<Int, Set<Int>>()
        val fixedColors = mapOf<Int, Int>()
        val allowedColors = setOf(0, 1)

        // when
        val coloring =
            FirstFitGraphColoring<Int, Int>().doColor(
                graph,
                coalesce,
                fixedColors,
                allowedColors,
            )

        // then
        validateColoring(graph, fixedColors, allowedColors, coloring)
        assertThat(coloring.keys).containsExactlyInAnyOrderElementsOf((0..<20))
    }

    @ParameterizedTest
    @ValueSource(ints = [1, 2, 3, 4, 5, 6])
    fun `half of bipartite clique`(seed: Int) {
        val n = 20
        val rnd = Random(seed)
        val vertices = (0..<n).shuffled(rnd)
        val graph = vertices.associateWith { mutableSetOf<Int>() }
        for (i in 0..<n / 2) {
            for (j in n / 2..<n) {
                if (rnd.nextBoolean()) {
                    graph[vertices[i]]!!.add(vertices[j])
                    graph[vertices[j]]!!.add(vertices[i])
                }
            }
        }
        val coalesce = mapOf<Int, Set<Int>>()
        val fixedColors = mapOf<Int, Int>()
        val allowedColors = setOf(0, 1)

        // when
        val coloring =
            FirstFitGraphColoring<Int, Int>().doColor(
                graph,
                coalesce,
                fixedColors,
                allowedColors,
            )

        // then
        validateColoring(graph, fixedColors, allowedColors, coloring)
        assertThat(coloring.keys).containsExactlyInAnyOrderElementsOf((0..<n))
    }

    @Test
    fun `tree of copies`() {
        val graph =
            mapOf(
                1 to setOf(2),
                2 to setOf(1),
                3 to setOf(4),
                4 to setOf(3),
                5 to setOf(6),
                6 to setOf(5),
            )
        val coalesce =
            mapOf(
                1 to setOf(0),
                2 to setOf(0),
                3 to setOf(1),
                4 to setOf(1),
                5 to setOf(2),
                6 to setOf(2),
            )
        val fixedColors = mapOf<Int, Int>()
        val allowedColors = setOf(0, 1)

        // when
        val coloring =
            FirstFitGraphColoring<Int, Int>().doColor(
                graph,
                coalesce,
                fixedColors,
                allowedColors,
            )

        // then
        validateColoring(graph, fixedColors, allowedColors, coloring)
        assertThat(coloring.keys).containsExactlyInAnyOrder(1, 2, 3, 4, 5, 6)
    }

    @Test
    fun `bipartite clique with some copies`() {
        // given
        val vertices = (0..8)
        val graph = vertices.associateWith { setOf<Int>() }.toMutableMap()
        val coalesce = vertices.associateWith { mutableSetOf<Int>() }

        val a = (0..2).toSet()
        val b = (3..5).toSet()
        a.forEach { graph[it] = b }
        b.forEach { graph[it] = a }
        coalesce[6]!!.add(0)
        coalesce[7]!!.add(5)
        coalesce[8]!!.add(1)

        val fixedColors = mapOf<Int, Int>()
        val allowedColors = setOf(0, 1)

        // when
        val coloring =
            FirstFitGraphColoring<Int, Int>().doColor(
                graph,
                coalesce,
                fixedColors,
                allowedColors,
            )

        // then
        validateColoring(graph, fixedColors, allowedColors, coloring)
        assertThat(coloring.keys).containsExactlyInAnyOrder(0, 1, 2, 3, 4, 5, 6, 7, 8)
        assertThat(coloring[6]).isEqualTo(coloring[0])
        assertThat(coloring[7]).isEqualTo(coloring[5])
        assertThat(coloring[8]).isEqualTo(coloring[1])
    }

    @Test
    fun `copying with fixed colors`() {
        // given
        val graph = (0..3).associateWith { emptySet<Int>() }
        val coalesce = mapOf(0 to setOf(1), 3 to setOf(2))
        val fixedColors = mapOf(0 to 2, 2 to 4)
        val allowedColors = setOf(2, 4)

        // when
        val coloring =
            FirstFitGraphColoring<Int, Int>().doColor(
                graph,
                coalesce,
                fixedColors,
                allowedColors,
            )

        // then
        validateColoring(graph, fixedColors, allowedColors, coloring)
        assertThat(coloring.keys).containsExactlyInAnyOrder(0, 1, 2, 3)
    }

    @Test
    fun `two copies with interference`() {
        // given
        val graph = mapOf(1 to setOf(), 2 to setOf(3), 3 to setOf(2))
        val coalesce = mapOf(1 to setOf(2), 3 to setOf(1))
        val fixedColors = mapOf<Int, Int>()
        val allowedColors = setOf(0, 1, 2)

        // when
        val coloring =
            FirstFitGraphColoring<Int, Int>().doColor(
                graph,
                coalesce,
                fixedColors,
                allowedColors,
            )

        // then
        validateColoring(graph, fixedColors, allowedColors, coloring)
        assertThat(coloring.keys).containsExactlyInAnyOrder(1, 2, 3)
        assertThat(coloring[1]).isIn(coloring[2], coloring[3])
    }

    @Test
    fun `bipartite clique with some copies and fixed register`() {
        // given
        val vertices = (0..8)
        val graph = vertices.associateWith { setOf<Int>() }.toMutableMap()
        val a = (0..2).toSet()
        val b = (3..5).toSet()
        a.forEach { graph[it] = b }
        b.forEach { graph[it] = a }
        val coalesce =
            mapOf(
                6 to setOf(0),
                7 to setOf(5),
                8 to setOf(1),
            )
        val fixedColors = mapOf(0 to 0)
        val allowedColors = setOf(0, 1, 2)

        // when
        val coloring =
            FirstFitGraphColoring<Int, Int>().doColor(
                graph,
                coalesce,
                fixedColors,
                allowedColors,
            )

        // then
        validateColoring(graph, fixedColors, allowedColors, coloring)
        assertThat(coloring.keys).containsExactlyInAnyOrder(0, 1, 2, 3, 4, 5, 6, 7, 8)
        assertThat(coloring[6]).isEqualTo(coloring[0])
        assertThat(coloring[7]).isEqualTo(coloring[5])
        assertThat(coloring[8]).isEqualTo(coloring[1])
    }
}
