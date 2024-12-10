package cacophony.controlflow

import cacophony.semantic.syntaxtree.Definition

sealed class Variable {
    data class SourceVariable(
        val definition: Definition,
    ) : Variable()

    sealed class AuxVariable : Variable() {
        class StaticLinkVariable : AuxVariable()
    }

    class PrimitiveVariable : Variable() {
        override fun equals(other: Any?): Boolean {
            return this === other
        }

        override fun hashCode(): Int {
            return System.identityHashCode(this)
        }
    }

    class StructVariable(val fields: Map<String, Variable>) : Variable()
}
