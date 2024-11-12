package cacophony.controlflow

import cacophony.semantic.syntaxtree.Definition

typealias CFGVertices = Map<CFGLabel, CFGVertex>

/**
 * Control-Flow Graph generated for a single function
 *
 * @property vertices vertices of the graph
 * @property initialLabel entry point to the computation
 */
data class CFGFragment(val vertices: CFGVertices, val initialLabel: CFGLabel)

fun programCfgToGraphviz(cfg: Map<Definition.FunctionDeclaration, CFGFragment>): String {
    var nextNodeId = 0
    var clusterId = 0
    val builder = StringBuilder()
    builder.append("strict digraph {\n")
    cfg.entries.forEach { (function, cfg) ->
        builder.append("subgraph cluster_$clusterId {\n")
        builder.append("label=\"${function.identifier}\"\n")
        builder.append("color=black\n")

        builder.append(cfgGraphvizVertices(cfg, nextNodeId))
        nextNodeId += cfg.vertices.size
        clusterId++
        builder.append("}\n")
    }
    builder.append("}")

    return builder.toString()
}

fun cfgFragmentToGraphviz(cfg: CFGFragment): String {
    val builder = StringBuilder()
    builder.append("strict digraph {\n")
    builder.append(cfgGraphvizVertices(cfg, 0))
    builder.append("}")
    return builder.toString()
}

private fun cfgGraphvizVertices(
    cfg: CFGFragment,
    nextNodeId: Int,
): String {
    var nextId = nextNodeId
    val ids = cfg.vertices.mapValues { nextId++ }

    val builder = StringBuilder()

    cfg.vertices.forEach { (label, vertex) ->
        val id = ids[label]
        if (label == cfg.initialLabel) {
            builder.append("  node$id [label=\"${vertex.tree}\", style=\"filled\", fillcolor=\"green\"]\n")
        } else if (vertex is CFGVertex.Final) {
            builder.append("  node$id [label=\"${vertex.tree}\", style=\"filled\", fillcolor=\"red\"]\n")
        } else {
            builder.append("  node$id [label=\"${vertex.tree}\"]\n")
        }

        when (vertex) {
            is CFGVertex.Conditional -> {
                builder.append("  node$id -> node${ids[vertex.trueDestination]} [color=\"green\"]\n")
                builder.append("  node$id -> node${ids[vertex.falseDestination]} [color=\"red\"]\n")
            }

            is CFGVertex.Jump -> builder.append("  node$id -> node${ids[vertex.destination]}\n")
            is CFGVertex.Final -> { // final has no edges
            }
        }
        builder.append("\n")
    }
    return builder.toString()
}
