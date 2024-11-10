package cacophony.controlflow

sealed class Variable {
    class SourceVariable : Variable()

    sealed class AuxVariable : Variable() {
        class StaticLinkVariable() : AuxVariable()
    }
}

sealed class Register {
    class VirtualRegister : Register()

    class FixedRegister(
        id: String,
    ) : Register()
}
