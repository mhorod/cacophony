package cacophony.controlflow.generation

import cacophony.controlflow.CFGNode
import cacophony.controlflow.Variable

data class LayoutAccessInfo(val access: CFGNode, val holdsReference: Boolean)

sealed class Layout {
    abstract fun flatten(): List<LayoutAccessInfo>
}

class SimpleLayout(val access: CFGNode, val holdsReference: Boolean = false) : Layout() {
    override fun flatten(): List<LayoutAccessInfo> = listOf(LayoutAccessInfo(access, holdsReference))
}

// NOTE: link is either the static link or closure link
class FunctionLayout(val code: SimpleLayout, val link: SimpleLayout) : Layout() {
    override fun flatten() = listOf(code, link).map { LayoutAccessInfo(it.access, it.holdsReference) }
}

class StructLayout(val fields: Map<String, Layout>) : Layout() {
    override fun flatten(): List<LayoutAccessInfo> =
        fields.entries
            .sortedBy { it.key }
            .map { it.value }
            .flatMap { it.flatten() }
}

class ClosureLayout(val vars: Map<Variable.PrimitiveVariable, SimpleLayout>) : Layout() {
    override fun flatten() =
        vars.entries
            .sortedBy { it.hashCode() }
            .map { it.value }
            .flatMap(Layout::flatten)
}

class VoidLayout : Layout() {
    override fun flatten(): List<LayoutAccessInfo> = emptyList()
}
