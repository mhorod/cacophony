package cacophony.controlflow

import cacophony.semantic.syntaxtree.Definition

sealed class Variable {
    class SourceVariable(val definition: Definition) : Variable()

    class AuxVariable : Variable()
}

sealed class Register {
    class VirtualRegister : Register()

    class FixedRegister(
        val hardwareRegister: X64Register,
    ) : Register()
}
