package cacophony.examples

import cacophony.diagnostics.DiagnosticMessage
import cacophony.diagnostics.Diagnostics
import cacophony.utils.Location

class TestDiagnostics : Diagnostics {
    open class ReportedError(
        open val message: DiagnosticMessage,
    )

    class LexerError(
        message: DiagnosticMessage,
        location: Location,
    ) : ReportedError(message)

    class ParserError(
        message: DiagnosticMessage,
        range: Pair<Location, Location>,
    ) : ReportedError(message)

    private val errors: MutableList<ReportedError> = ArrayList()

    fun errors(): List<ReportedError> = errors

    override fun report(
        message: DiagnosticMessage,
        range: Pair<Location, Location>,
    ) {
        errors.add(ParserError(message, range))
    }

    override fun report(
        message: DiagnosticMessage,
        location: Location,
    ) {
        errors.add(LexerError(message, location))
    }
}
