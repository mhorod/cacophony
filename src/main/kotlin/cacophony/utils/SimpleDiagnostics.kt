package cacophony.utils

import cacophony.diagnostics.DiagnosticMessage
import cacophony.diagnostics.Diagnostics

class SimpleDiagnostics(
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

    fun getErrors(): List<ReportedError> = errors

    fun extractErrors(): List<String> = errors.map { it.toString() }
}
