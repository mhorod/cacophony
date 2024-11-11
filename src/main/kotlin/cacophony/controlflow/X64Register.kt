package cacophony.controlflow

enum class X64Register(val isCallPreserved: Boolean) {
    RAX(false),
    RBX(true),
    RCX(false),
    RDX(false),
    RSI(false),
    RDI(false),
    RSP(true),
    RBP(true),
    R8(false),
    R9(false),
    R10(false),
    R11(false),
    R12(true),
    R13(true),
    R14(true),
    R15(true),
}

val REGISTER_ARGUMENT_ORDER =
    listOf(
        X64Register.RDI,
        X64Register.RSI,
        X64Register.RDX,
        X64Register.RCX,
        X64Register.R8,
        X64Register.R9,
    )
