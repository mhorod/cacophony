package cacophony.controlflow.generation

import cacophony.controlflow.CFGNode

typealias CFGReferenceAnalysis = Map<CFGNode, Boolean>

fun analyzeCFGReferences(node: CFGNode): CFGReferenceAnalysis {
    return ReferenceMapBuilder().build(node)
}

private class ReferenceMapBuilder() {
    private val holdsReference = mutableMapOf<CFGNode, Boolean>()
    fun build(node: CFGNode): Map<CFGNode, Boolean> {
        visit(node)
        return holdsReference
    }

    fun visit(node: CFGNode) {
        node.children().forEach { visit(it) }
        val holdsRef = when (node) {
            !is CFGNode.Value -> false
            is CFGNode.RegisterUse -> node.holdsReference
            is CFGNode.MemoryAccess -> node.holdsReference
            is CFGNode.Assignment -> holdsReference[node.value]!!
            is CFGNode.Addition -> holdsReference[node.lhs]!! || holdsReference[node.rhs]!!
            is CFGNode.AdditionAssignment -> holdsReference[node.lhs]!!
            is CFGNode.SubtractionAssignment -> holdsReference[node.lhs]!!
            is CFGNode.Subtraction -> holdsReference[node.lhs]!!
            else -> false
        }
        holdsReference[node] = holdsRef
    }
}