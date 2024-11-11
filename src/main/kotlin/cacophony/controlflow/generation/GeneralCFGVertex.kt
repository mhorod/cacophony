package cacophony.controlflow.generation

import cacophony.controlflow.CFGLabel
import cacophony.controlflow.CFGNode

internal class GeneralCFGVertex(val node: CFGNode, val label: CFGLabel) {
    val outgoing = mutableListOf<CFGLabel>()

    fun connect(vararg labels: CFGLabel) {
        outgoing.addAll(labels)
    }
}
