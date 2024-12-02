package cacophony.controlflow

const val REGISTER_SIZE = 8

enum class HardwareRegister {
    RAX,
    RBX,
    RCX,
    RDX,
    RSI,
    RDI,
    RSP,
    RBP,
    R8,
    R9,
    R10,
    R11,
    R12,
    R13,
    R14,
    R15,
    ;

    override fun toString(): String = name.lowercase()
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
