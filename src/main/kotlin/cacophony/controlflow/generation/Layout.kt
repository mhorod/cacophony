package cacophony.controlflow.generation

import cacophony.controlflow.CFGNode

sealed class Layout {
    abstract fun flatten(): List<CFGNode>
}

class SimpleLayout(val access: CFGNode) : Layout() {
    override fun flatten(): List<CFGNode> = listOf(access)
}

class StructLayout(val fields: Map<String, Layout>) : Layout() {
    override fun flatten(): List<CFGNode> =
        fields.entries
            .sortedBy { it.key }
            .map { it.value }
            .flatMap { it.flatten() }
            .toList()
}
