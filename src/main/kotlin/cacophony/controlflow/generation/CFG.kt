package cacophony.controlflow.generation

import cacophony.controlflow.CFGFragment
import cacophony.controlflow.CFGLabel
import cacophony.controlflow.CFGNode
import cacophony.graphs.getReachableFrom

/**
 * Internal mutable representation of CFG that is being built
 */
internal class CFG {
    private val cfg = mutableMapOf<CFGLabel, GeneralCFGVertex>()

    internal fun addUnconditionalVertex(node: CFGNode): GeneralCFGVertex.UnconditionalVertex {
        val vertex = GeneralCFGVertex.UnconditionalVertex(node, CFGLabel())
        cfg[vertex.label] = vertex
        return vertex
    }

    internal fun addFinalVertex(node: CFGNode): GeneralCFGVertex.FinalVertex {
        val vertex = GeneralCFGVertex.FinalVertex(node, CFGLabel())
        cfg[vertex.label] = vertex
        return vertex
    }

    internal fun addConditionalVertex(node: CFGNode): GeneralCFGVertex.ConditionalVertex {
        val vertex = GeneralCFGVertex.ConditionalVertex(node, CFGLabel())
        cfg[vertex.label] = vertex
        return vertex
    }

    internal fun cfgFragment(entryLabel: CFGLabel): CFGFragment {
        val newEntryLabel = findNonNoOpFromEntry(entryLabel)
        filterNoOps(newEntryLabel)
        filterUnreachable(newEntryLabel)
        return CFGFragment(cfg.mapValues { (_, value) -> value.toVertex() }, newEntryLabel)
    }

    private fun filterUnreachable(entryLabel: CFGLabel) {
        val graph = cfg.mapValues { it.value.getConnections() }
        val reachableLabels = getReachableFrom(setOf(entryLabel), graph)
        cfg.keys.retainAll(reachableLabels)
    }

    private fun findNonNoOpFromEntry(entryLabel: CFGLabel): CFGLabel {
        var newLabel = entryLabel
        while (true) {
            val vertex = cfg[newLabel]
            if (vertex != null && vertex.node is CFGNode.NoOp) {
                newLabel = vertex.getConnections().first()
                if (newLabel == entryLabel) return newLabel
            } else {
                return newLabel
            }
        }
    }

    private fun filterNoOps(entryLabel: CFGLabel) {
        val ingoingEdges = mutableMapOf<CFGLabel, MutableSet<GeneralCFGVertex>>()
        cfg.values.forEach { vertex ->

            vertex.getConnections().forEach {
                ingoingEdges.computeIfAbsent(it) { _ -> mutableSetOf() }.add(vertex)
            }
        }

        cfg.entries.forEach { (label, vertex) ->
            run {
                if (vertex is GeneralCFGVertex.UnconditionalVertex && vertex.node is CFGNode.NoOp) {
                    val edges = ingoingEdges.getOrDefault(label, listOf())
                    val next = vertex.getConnections().first()
                    if (next != label) {
                        edges.forEach { v ->
                            v.replaceLabel(label, next)
                        }
                        ingoingEdges[next]!!.addAll(edges)
                        ingoingEdges.remove(label)
                    }
                }
            }
        }
    }
}
