package cacophony.graphs

import cacophony.controlflow.Register

class FirstFitColoringException(reason: String) : Exception(reason)

/**
 * Assumes that keys of graph map are exactly all vertices in graph.
 * @throws FirstFitColoringException if graphs contains loops.
 */
class FirstFitGraphColoring<VertexT, ColorT> : GraphColoring<VertexT, ColorT> {
    override fun doColor(
        graph: Map<VertexT, Collection<VertexT>>,
        coalesce: Map<VertexT, Collection<VertexT>>,
        fixedColors: Map<VertexT, ColorT>,
        allowedColors: Collection<ColorT>,
    ): Map<VertexT, ColorT> = ColoringProblemInstance(graph, coalesce, fixedColors, allowedColors).doColor()

    private class ColoringProblemInstance<VertexT, ColorT>(
        graph: Map<VertexT, Collection<VertexT>>,
        coalesce: Map<VertexT, Collection<VertexT>>,
        private val fixedColors: Map<VertexT, ColorT>,
        private val allowedColors: Collection<ColorT>,
    ) {
        init {
            // Verify there are no loops in graph.
            if (graph.any { it.value.contains(it.key) }) {
                throw FirstFitColoringException("Cannot color graph with loops.")
            }
        }

        private val originalGraph = getSymmetricClosure(graph).mapValues { (_, neigh) -> neigh.toMutableSet() }
        private val copying = getProperTransitiveSymmetricClosure(coalesce)
        private val vertices = originalGraph.keys.toMutableSet()

        private fun originalNeighbors(v: VertexT) = originalGraph.getOrDefault(v, emptySet())

        private fun copies(v: VertexT) = (copying.getOrDefault(v, emptySet())).filter { it in vertices }

        init {
            // Add graph edges between vertices with different fixed colors.
            vertices.forEach { v ->
                vertices.forEach { u ->
                    if (fixedColors.containsKey(v) && fixedColors.containsKey(u) && fixedColors[v] != fixedColors[u]) {
                        originalGraph[v]!!.add(u)
                        originalGraph[u]!!.add(v)
                    }
                }
            }
        }

        // Deep copy of originalGraph.
        private val currentGraph = originalGraph.keys.associateWith { originalGraph[it]!!.toMutableSet() }.toMutableMap()

        private val stack = mutableListOf<Set<VertexT>>()
        private val copyGroups = mutableMapOf<VertexT, Set<VertexT>>()

        private fun neighbors(v: VertexT) = currentGraph.getOrDefault(v, emptySet())

        private fun copyGroup(v: VertexT) = (copyGroups.getOrDefault(v, emptySet())) + setOf(v)

        private fun deposit(v: VertexT) {
            stack.add(copyGroup(v))
            vertices.removeAll(copyGroup(v))
            currentGraph[v]?.forEach {
                currentGraph[it]?.remove(v)
            }
        }

        private fun coalesce(v: VertexT, u: VertexT) {
            copyGroups[v] = copyGroup(v) + copyGroup(u)
            vertices.remove(u)
            currentGraph[u]?.forEach {
                currentGraph[it]?.remove(u)
                currentGraph[it]?.add(v)
            }
            currentGraph.getOrPut(v) { mutableSetOf() }.addAll(currentGraph[u] ?: emptySet())
        }

        // Uses George criterion for safety
        private fun isCoalesceSafe(v: VertexT, u: VertexT): Boolean =
            neighbors(v).all {
                it in neighbors(u) ||
                    neighbors(it).size < allowedColors.size
            }

        private fun shouldCoalesce(v: VertexT, u: VertexT): Boolean =
            u !in neighbors(v) &&
                u in copies(v) &&
                isCoalesceSafe(v, u)

        private fun vertexToCoalesce(v: VertexT) = copies(v).find { shouldCoalesce(v, it) }

        private fun generateFirstFitOrder() {
            while (vertices.isNotEmpty()) {
                while (true) {
                    val y =
                        vertices
                            .map { it to vertexToCoalesce(it) }
                            .firstOrNull { it.second != null }
                            ?.also { (a, b) -> coalesce(a, b!!) }
                    if (y != null) continue

                    val x = vertices.find { neighbors(it).size < allowedColors.size && vertexToCoalesce(it) == null }?.also { deposit(it) }
                    if (x == null) break
                }
                if (vertices.isNotEmpty())
                    deposit(vertices.minBy { if (it is Register.FixedRegister) Int.MAX_VALUE else neighbors(it).size })
            }
        }

        fun doColor(): Map<VertexT, ColorT> {
            generateFirstFitOrder()

            val coloring = mutableMapOf<VertexT, ColorT>()

            stack.forEach { group ->
                val vertexWithFixedColor = group.find { fixedColors.containsKey(it) }
                if (vertexWithFixedColor != null) {
                    group.forEach {
                        coloring[it] = fixedColors[vertexWithFixedColor]!!
                    }
                }
            }

            stack
                .reversed()
                .filter { !coloring.containsKey(it.first()) }
                .forEach { group ->
                    val forbiddenColors =
                        group
                            .map { v ->
                                originalNeighbors(v).mapNotNull { coloring[it] }
                            }.flatten()
                            .toSet()
                    val color = (allowedColors - forbiddenColors).firstOrNull()
                    if (color != null) {
                        group.forEach { coloring[it] = color }
                    }
                }

            return coloring
        }
    }
}
