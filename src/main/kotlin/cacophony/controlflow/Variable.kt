package cacophony.controlflow

import cacophony.semantic.syntaxtree.Definition

sealed class Variable {
    data class SourceVariable(
        val definition: Definition,
    ) : Variable()

    sealed class AuxVariable : Variable() {
        class StaticLinkVariable : AuxVariable()
    }

    class PrimitiveVariable : Variable()

    class StructVariable(val fields: Map<String, Variable>) : Variable()
}

// TODO: Next week it should be
//  sealed class Variable {
//     class PrimitiveVariable : Variable()
//     class StructVariable(val fields: Map<String, Variable>) : Variable()
//  }
