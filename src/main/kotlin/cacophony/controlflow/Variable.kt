package cacophony.controlflow

import cacophony.semantic.syntaxtree.Definition

sealed class Variable

class SourceVariable(
    val definition: Definition,
) : Variable()

sealed class Register() : Variable()

data object VirtualRegister : Register()

data object FixedRegister : Register() {
    // TODO
}
