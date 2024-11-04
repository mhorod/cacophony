package cacophony.utils

// Represents error in a cacophony program.
open class CompileException(
    reason: String,
) : Exception(reason)
