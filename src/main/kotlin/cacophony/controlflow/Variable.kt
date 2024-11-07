package cacophony.controlflow

import cacophony.semantic.syntaxtree.Definition

sealed interface Variable

sealed interface RealVariable : Variable

class SourceVariable(
    val definition: Definition,
) : RealVariable

class StackVariable() : RealVariable

sealed class Register() : Variable {
    class Virtual

    class Fixed {
        // TODO
    }
}
