package cacophony.diagnostics

import cacophony.utils.CompileException
import cacophony.utils.Input
import cacophony.utils.Location

class CacophonyDiagnostics(
    private val input: Input,
) : Diagnostics {
    inner class ReportedError(
        val message: DiagnosticMessage,
        val range: Pair<Location, Location>,
    ) {
        override fun toString(): String = "${input.locationRangeToString(range.first, range.second)}: ${message.getMessage()}"
    }

    private val errors: MutableList<ReportedError> = ArrayList()

    override fun report(
        message: DiagnosticMessage,
        range: Pair<Location, Location>,
    ) {
        errors.add(ReportedError(message, range))
    }

    override fun fatal() = CompileException("Compilation failed")

    override fun getErrors(): List<String> = errors.map(ReportedError::toString)

    fun extractErrors(): List<ReportedError> = errors
}
