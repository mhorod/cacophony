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
            // [arg0] + 16
            // [ret address] +8
            // [old rbp] <- curr rbp
            // so do your math
            VariableAllocation.OnStack(-REGISTER_SIZE * (index - registerOrder.size + 2))
        }
}

object SystemVAMD64CallConvention : StackCallConvention(REGISTER_ARGUMENT_ORDER) {
    private val preservedRegisters =
        HardwareRegister.entries.filter {
            it != HardwareRegister.RSP &&
                it != HardwareRegister.RBP &&
                it.isCallPreserved
        }

    override fun returnRegister(): HardwareRegister = HardwareRegister.RAX

    override fun preservedRegisters(): List<HardwareRegister> = preservedRegisters
}
