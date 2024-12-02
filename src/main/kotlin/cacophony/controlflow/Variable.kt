package cacophony.controlflow

import cacophony.semantic.syntaxtree.Definition

sealed class Variable {
    data class SourceVariable(
        val definition: Definition,
    ) : Variable()

    sealed class AuxVariable : Variable() {
        class StaticLinkVariable : AuxVariable()
    }
}
