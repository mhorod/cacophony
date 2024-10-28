package cacophony.examples

import cacophony.utils.Diagnostics
import cacophony.utils.Input
import cacophony.utils.Location

class TestDiagnostics : Diagnostics {
    open class ReportedError

    class LexerError(
        message: String,
        location: Location,
    ) : ReportedError()

    class ParserError(
        message: String,
        range: Pair<Location, Location>,
    ) : ReportedError()

    private val errors: MutableList<ReportedError> = ArrayList()

    override fun report(
        message: String,
        location: Location,
    ) {
        errors.add(LexerError(message, location))
    }

    override fun report(
        message: String,
        range: Pair<Location, Location>,
    ) {
        errors.add(ParserError(message, range))
    }

    fun reportFatal(
        message: String,
        input: Input,
        location: Location,
    ) {
        errors.add(LexerError(message, location))
    }

    fun errors(): List<ReportedError> = errors
}
