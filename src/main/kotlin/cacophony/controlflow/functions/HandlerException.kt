package cacophony.controlflow.functions

import cacophony.utils.CompileException

class GenerateVariableAccessException(
    reason: String,
) : CompileException(reason)

class GenerateAccessToFramePointerException(
    reason: String,
) : CompileException(reason)
