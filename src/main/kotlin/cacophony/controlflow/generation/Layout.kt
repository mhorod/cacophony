package cacophony.controlflow.generation

import cacophony.controlflow.CFGNode
import cacophony.semantic.syntaxtree.BaseType
import cacophony.semantic.syntaxtree.Type

data class LayoutAccessInfo(val access: CFGNode, val holdsReference: Boolean)

sealed class Layout {
    abstract fun flatten(): List<LayoutAccessInfo>

    abstract fun matchesType(type: Type): Boolean
}

class SimpleLayout(val access: CFGNode, val holdsReference: Boolean = false) : Layout() {
    override fun flatten(): List<LayoutAccessInfo> = listOf(LayoutAccessInfo(access, holdsReference))

    override fun matchesType(type: Type): Boolean =
        when (type) {
            is BaseType.Basic, is BaseType.Referential -> true
            is BaseType.Functional, is BaseType.Structural -> false
        }
}

class StructLayout(val fields: Map<String, Layout>) : Layout() {
    override fun flatten(): List<LayoutAccessInfo> =
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

class VoidLayout : Layout() {
    override fun flatten(): List<LayoutAccessInfo> = emptyList()

    override fun matchesType(type: Type): Boolean = true
}
