package cacophony.controlflow.generation

import cacophony.controlflow.CFGLabel
import cacophony.controlflow.CFGNode
import cacophony.controlflow.CFGVertex

internal sealed class GeneralCFGVertex(val label: CFGLabel) {
    internal abstract fun toVertex(): CFGVertex

    internal class UnconditionalVertex(private val node: CFGNode.Unconditional, label: CFGLabel) : GeneralCFGVertex(label) {
        private var outgoing: CFGLabel? = null

        internal fun connect(label: CFGLabel) {
            check(outgoing == null) { "Vertex is already connected" }
            outgoing = label
        }

        override fun toVertex(): CFGVertex.Jump = CFGVertex.Jump(node, outgoing!!)
    }

    internal class ConditionalVertex(private val node: CFGNode, label: CFGLabel) : GeneralCFGVertex(label) {
        private var outgoingTrue: CFGLabel? = null
        private var outgoingFalse: CFGLabel? = null

        internal fun connectTrue(label: CFGLabel) {
            check(outgoingTrue == null) { "Vertex true output is already connected" }
            outgoingTrue = label
        }

        internal fun connectFalse(label: CFGLabel) {
            check(outgoingFalse == null) { "Vertex false output is already connected" }
            outgoingFalse = label
        }

        override fun toVertex() = CFGVertex.Conditional(node, outgoingTrue!!, outgoingFalse!!)
    }

    internal class FinalVertex(private val node: CFGNode, label: CFGLabel) : GeneralCFGVertex(label) {
        override fun toVertex() = CFGVertex.Final(node)
    }
}
