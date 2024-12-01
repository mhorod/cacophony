package cacophony.controlflow.functions

import cacophony.controlflow.*
import cacophony.utils.CompileException

class GenerateVariableAccessException(
    reason: String,
) : CompileException(reason)
