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

sealed class Register {
    class VirtualRegister : Register()

    data class FixedRegister(
        val hardwareRegister: HardwareRegister,
    ) : Register()
}
