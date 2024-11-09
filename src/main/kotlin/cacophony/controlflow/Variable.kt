package cacophony.controlflow

sealed class Variable {
    class SourceVariable : Variable()

    class AuxVariable : Variable()
}

sealed class Register {
    class VirtualRegister : Register()

    class FixedRegister(
        val hardwareRegister: X64Register,
    ) : Register()
}
