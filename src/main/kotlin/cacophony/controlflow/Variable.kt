package cacophony.controlflow

import cacophony.semantic.syntaxtree.Definition

sealed class Variable

class SourceVariable(
    val definition: Definition,
) : Variable()

sealed class Register() : Variable()

class VirtualRegister : Register()

class FixedRegister : Register() {
    // TODO
}
