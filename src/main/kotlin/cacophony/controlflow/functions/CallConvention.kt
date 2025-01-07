package cacophony.controlflow.functions

import cacophony.controlflow.*

interface CallConvention {
    // Where one can find the argument with the given index
    // assuming that `push rbp` and `move rbp, rsp` happened
    fun argumentAllocation(index: Int): VariableAllocation

    // Where the returned value should be put into
    fun returnAllocation(index: Int, argumentCount: Int): VariableAllocation

    // Which GPR registers should be preserved by the function call (except RSP and RBP!)
    fun preservedRegisters(): List<HardwareRegister>
}

abstract class StackCallConvention(
    private val registerArgumentOrder: List<HardwareRegister>,
    private val registerReturnOrder: List<HardwareRegister>,
) : CallConvention {
    override fun argumentAllocation(index: Int): VariableAllocation =
        if (index < registerArgumentOrder.size) {
            VariableAllocation.InRegister(Register.FixedRegister(registerArgumentOrder[index]))
        } else {
            // Stack is
            // [arg2]
            // [arg1]
            // [arg0] + 24
            // [ret address] + 16
            // [old rbp] + 8
            // [static link] <- curr rbp
            VariableAllocation.OnStack(-REGISTER_SIZE * (index - registerArgumentOrder.size + 3))
        }

    override fun returnAllocation(index: Int, argumentCount: Int): VariableAllocation =
        if (index < registerReturnOrder.size) {
            VariableAllocation.InRegister(Register.FixedRegister(registerReturnOrder[index]))
        } else {
            if (argumentCount <= registerArgumentOrder.size) {
                VariableAllocation.OnStack(-REGISTER_SIZE * (index - registerReturnOrder.size + 3))
            } else {
                // TODO: why does it need to be +4 instead of +3?
                VariableAllocation.OnStack(
                    -REGISTER_SIZE * (index - registerReturnOrder.size + argumentCount - registerArgumentOrder.size + 3),
                )
            }
        }
}

object SystemVAMD64CallConvention : StackCallConvention(REGISTER_ARGUMENT_ORDER, REGISTER_RETURN_ORDER) {
    private val preservedRegisters =
        listOf(HardwareRegister.RBX, HardwareRegister.R12, HardwareRegister.R13, HardwareRegister.R14, HardwareRegister.R15)

    override fun preservedRegisters(): List<HardwareRegister> = preservedRegisters
}
