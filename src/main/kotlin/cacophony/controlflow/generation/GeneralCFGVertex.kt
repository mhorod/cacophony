package cacophony.controlflow.generation

import cacophony.controlflow.CFGLabel
import cacophony.controlflow.CFGNode

sealed class GeneralCFGVertex(val node: CFGNode, val label: CFGLabel) {
    class UnconditionalVertex(node: CFGNode, label: CFGLabel) : GeneralCFGVertex(node, label) {
        private var outgoing: CFGLabel? = null

        fun connect(label: CFGLabel) {
            check(outgoing == null) { "Vertex is already connected" }
        }

        fun getConnection(): CFGLabel {
            check(outgoing != null) { "Vertex is not connected" }
            return outgoing!!
        }
    }

    class ConditionalVertex(node: CFGNode, label: CFGLabel) : GeneralCFGVertex(node, label) {
        private var outgoingTrue: CFGLabel? = null
        private var outgoingFalse: CFGLabel? = null

        fun connectTrue(label: CFGLabel) {
            check(outgoingTrue == null) { "Vertex true output is already connected" }
            outgoingTrue = label
        }

        fun connectFalse(label: CFGLabel) {
            check(outgoingFalse == null) { "Vertex false output is already connected" }
            outgoingFalse = label
        }

        fun getTrueConnection(): CFGLabel {
            check(outgoingTrue != null && outgoingFalse != null) { "Vertex outputs are not fully connected" }
            return outgoingTrue!!
        }

        fun getFalseConnection(): CFGLabel {
            check(outgoingTrue != null && outgoingFalse != null) { "Vertex outputs are not fully connected" }
            return outgoingFalse!!
        }
    }

    class FinalVertex(node: CFGNode, label: CFGLabel) : GeneralCFGVertex(node, label)
}
