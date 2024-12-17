package cacophony.controlflow.generation

import cacophony.controlflow.CFGNode
import cacophony.semantic.syntaxtree.BaseType
import cacophony.semantic.syntaxtree.Type

sealed class Layout {
    abstract fun flatten(): List<CFGNode>

    abstract fun matchesType(type: Type): Boolean
}

class SimpleLayout(val access: CFGNode) : Layout() {
    override fun flatten(): List<CFGNode> = listOf(access)

    override fun matchesType(type: Type): Boolean =
        when (type) {
            is BaseType.Basic -> true
            else -> false
        }
}

class StructLayout(val fields: Map<String, Layout>) : Layout() {
    override fun flatten(): List<CFGNode> =
        fields.entries
            .sortedBy { it.key }
            .map { it.value }
            .flatMap { it.flatten() }
            .toList()

    override fun matchesType(type: Type): Boolean =
        when (type) {
            is BaseType.Structural ->
                fields.keys.containsAll(type.fields.keys) &&
                    type.fields.keys.containsAll(fields.keys) &&
                    fields.all { (name, layout) -> layout.matchesType(type.fields[name]!!) }

            else -> false
        }
}
