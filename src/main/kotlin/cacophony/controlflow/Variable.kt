package cacophony.controlflow

import cacophony.semantic.syntaxtree.Definition

sealed interface Variable

class SourceVariable(
    val definition: Definition,
) : Variable

sealed class Register() : Variable {
    class Virtual

    class Fixed {
        // TODO
    }
}
