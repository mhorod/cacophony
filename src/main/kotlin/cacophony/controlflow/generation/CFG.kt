package cacophony.controlflow.generation

import cacophony.controlflow.CFGFragment
import cacophony.controlflow.CFGLabel
import cacophony.controlflow.CFGNode

internal class CFG {
    private val cfg = mutableMapOf<CFGLabel, GeneralCFGVertex>()

    internal fun addUnconditionalVertex(node: CFGNode.Unconditional): GeneralCFGVertex.UnconditionalVertex {
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

    internal fun getCFGFragment(): CFGFragment = cfg.mapValues { (_, value) -> value.toVertex() }
}
