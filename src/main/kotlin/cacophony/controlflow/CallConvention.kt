package cacophony.controlflow

interface CallConvention {
    // Where one can find the argument with the given index
    // assuming that `push rbp` and `move rbp, rsp` happened
    fun argumentAllocation(index: Int): VariableAllocation

    // Where the returned value should be put into
    fun returnRegister(): HardwareRegister

    // Which GPR registers should be preserved by the function call (except RSP and RBP!)
    fun preservedRegisters(): List<HardwareRegister>
}

abstract class StackCallConvention(private val registerOrder: List<HardwareRegister>) : CallConvention {
    override fun argumentAllocation(index: Int): VariableAllocation =
        if (index < registerOrder.size) {
            VariableAllocation.InRegister(Register.FixedRegister(registerOrder[index]))
        } else {
            // Stack is
            // [arg2]
            // [arg1]
            // [arg0] + 24
            // [ret address] + 16
            // [old rbp] + 8
            // [static link] <- curr rbp
            VariableAllocation.OnStack(-REGISTER_SIZE * (index - registerOrder.size + 3))
        }
}

object SystemVAMD64CallConvention : StackCallConvention(REGISTER_ARGUMENT_ORDER) {
    private val preservedRegisters =
        listOf(HardwareRegister.RBX, HardwareRegister.R12, HardwareRegister.R13, HardwareRegister.R14, HardwareRegister.R15)

    override fun returnRegister(): HardwareRegister = HardwareRegister.RAX

    override fun preservedRegisters(): List<HardwareRegister> = preservedRegisters
}
