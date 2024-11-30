package cacophony.controlflow.functions

import cacophony.controlflow.*
import cacophony.utils.CompileException

class GenerateVariableAccessException(
    reason: String,
) : CompileException(reason)

fun defaultCallConvention(index: Int): CFGNode {
    if (index < REGISTER_ARGUMENT_ORDER.size)
        return registerUse(Register.FixedRegister(REGISTER_ARGUMENT_ORDER[index]))
    val over = index - REGISTER_ARGUMENT_ORDER.size
    // Assumes that the RSP has not changed after `call f`
    return memoryAccess(registerUse(rsp) add integer((over + 1) * REGISTER_SIZE))
}
