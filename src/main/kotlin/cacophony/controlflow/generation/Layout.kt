package cacophony.controlflow.generation

import cacophony.controlflow.CFGNode

// TODO: make the LValueLayout class and get rid of `require(something is CFGNode.LValue)`

sealed class Layout

class SimpleLayout(val access: CFGNode) : Layout() {
    override fun toString(): String = "SimpleLayout($access)"
}

class StructLayout(val fields: Map<String, Layout>) : Layout() {
    override fun toString(): String = "StructLayout($fields)"
}
