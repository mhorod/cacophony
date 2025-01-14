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

// Stack content        direction
// [ret0]                       |
// [ret1]                       |
// [ret2]                       |
// ...                          |
// [arg0]                       |
// [arg1]                       |
// [arg2]                       |
// ...                          |
// [arg-1]                      |
// [ret address]                |
// padding                      |
// [outline address]            |
// [old rbp] <- current rbp     |
// [static link]                V
abstract class StackCallConvention(
    private val registerArgumentOrder: List<HardwareRegister>,
    private val registerReturnOrder: List<HardwareRegister>,
) : CallConvention {
    override fun argumentAllocation(index: Int): VariableAllocation =
        if (index < registerArgumentOrder.size) {
            VariableAllocation.InRegister(Register.FixedRegister(registerArgumentOrder[index]))
        } else {
            VariableAllocation.OnStack(-REGISTER_SIZE * (index - registerArgumentOrder.size + 4))
        }

    override fun returnAllocation(index: Int, argumentCount: Int): VariableAllocation =
        if (index < registerReturnOrder.size) {
            VariableAllocation.InRegister(Register.FixedRegister(registerReturnOrder[index]))
        } else {
            if (argumentCount <= registerArgumentOrder.size) {
                VariableAllocation.OnStack(-REGISTER_SIZE * (index - registerReturnOrder.size + 4))
            } else {
                VariableAllocation.OnStack(
                    -REGISTER_SIZE * (index - registerReturnOrder.size + argumentCount - registerArgumentOrder.size + 4),
                )
            }
        }
}

object SystemVAMD64CallConvention : StackCallConvention(REGISTER_ARGUMENT_ORDER, REGISTER_RETURN_ORDER) {
    private val preservedRegisters =
        listOf(HardwareRegister.RBX, HardwareRegister.R12, HardwareRegister.R13, HardwareRegister.R14, HardwareRegister.R15)

    override fun preservedRegisters(): List<HardwareRegister> = preservedRegisters
}

abstract class StacklessCallConvention(private val registerArgumentOrder: List<HardwareRegister>) : CallConvention
