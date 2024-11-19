package cacophony.controlflow.generation

import cacophony.controlflow.CFGLabel
import cacophony.controlflow.CFGNode
import cacophony.controlflow.CFGVertex

/**
 * Represents a mutable vertex of the Control Flow Graph
 *
 * @property label Unique label of this vertex
 * @property node CFG Node computed by this vertex
 */
internal sealed class GeneralCFGVertex(val label: CFGLabel, open val node: CFGNode) {
    internal abstract fun toVertex(): CFGVertex

    internal abstract fun replaceLabel(label: CFGLabel, newLabel: CFGLabel)

    internal abstract fun getConnections(): List<CFGLabel>

    /**
     * Vertex that executes unconditional computation that then continues at another vertex
     */
    internal class UnconditionalVertex(override val node: CFGNode, label: CFGLabel) : GeneralCFGVertex(label, node) {
        private var outgoing: CFGLabel? = null

        internal fun connect(label: CFGLabel) {
            check(outgoing == null) { "Vertex $this is already connected" }
            outgoing = label
        }

        override fun getConnections() = listOf(outgoing ?: error("Vertex $this is not connected"))

        override fun replaceLabel(label: CFGLabel, newLabel: CFGLabel) {
            if (outgoing == label) outgoing = newLabel
        }

        override fun toVertex(): CFGVertex.Jump = CFGVertex.Jump(node, outgoing ?: CFGLabel())
    }

    /**
     * Vertex that checks a condition given by its node and then jumps to either true or false branch
     */
    internal class ConditionalVertex(override val node: CFGNode, label: CFGLabel) : GeneralCFGVertex(label, node) {
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

        override fun getConnections() =
            listOf(
                outgoingTrue ?: error("Vertex $this true output is not connected"),
                outgoingFalse ?: error("Vertex $this false output is not connected"),
            )

        override fun replaceLabel(label: CFGLabel, newLabel: CFGLabel) {
            if (outgoingTrue == label) outgoingTrue = newLabel
            if (outgoingFalse == label) outgoingFalse = newLabel
        }

        override fun toVertex(): CFGVertex.Conditional = CFGVertex.Conditional(node, outgoingTrue!!, outgoingFalse!!)
    }

    /**
     * Final vertex of a computation, that doesn't continue at other vertices
     */
    internal class FinalVertex(override val node: CFGNode, label: CFGLabel) : GeneralCFGVertex(label, node) {
        override fun toVertex() = CFGVertex.Final(node)

        override fun replaceLabel(label: CFGLabel, newLabel: CFGLabel) = Unit

        override fun getConnections() = emptyList<CFGLabel>()
    }
}
