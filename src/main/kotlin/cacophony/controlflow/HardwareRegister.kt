package cacophony.controlflow

const val REGISTER_SIZE = 8

val PRESERVED_REGISTERS = HardwareRegister.entries.filter { it != HardwareRegister.RSP && it.isCallPreserved }

enum class HardwareRegister(val isCallPreserved: Boolean) {
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
    R15(true), ;

    override fun toString(): String = name
}

enum class HardwareRegisterByte(private val hardwareRegister: HardwareRegister) {
    AL(HardwareRegister.RAX),
    BL(HardwareRegister.RBX),
    CL(HardwareRegister.RCX),
    DL(HardwareRegister.RDX),
    SIL(HardwareRegister.RSI),
    DIL(HardwareRegister.RDI),
    SPL(HardwareRegister.RSP),
    BPL(HardwareRegister.RBP),
    R8B(HardwareRegister.R8),
    R9B(HardwareRegister.R9),
    R10B(HardwareRegister.R10),
    R11B(HardwareRegister.R11),
    R12B(HardwareRegister.R12),
    R13B(HardwareRegister.R13),
    R14B(HardwareRegister.R14),
    R15B(HardwareRegister.R15),
    ;

    override fun toString(): String = name

    companion object {
        fun fromRegister(register: HardwareRegister): HardwareRegisterByte =
            entries.find { it.hardwareRegister == register } ?: error("No byte register for $register")
    }
}

val REGISTER_ARGUMENT_ORDER =
    listOf(
        HardwareRegister.RDI,
        HardwareRegister.RSI,
        HardwareRegister.RDX,
        HardwareRegister.RCX,
        HardwareRegister.R8,
        HardwareRegister.R9,
    )
