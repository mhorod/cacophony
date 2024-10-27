package cacophony.examples

import cacophony.token.Token
import cacophony.utils.Diagnostics
import cacophony.utils.Input
import cacophony.utils.Location

class TestDiagnostics : Diagnostics {
    open class ReportedError

    class LexerError(
        message: String,
        input: Input,
        location: Location,
    ) : ReportedError()

    class ParserError<TC : Enum<TC>>(
        message: String,
        input: Input,
        token: Token<TC>,
    ) : ReportedError()

    private val errors: MutableList<ReportedError> = ArrayList()

    override fun report(
        message: String,
        input: Input,
        location: Location,
    ) {
        errors.add(LexerError(message, input, location))
    }

    override fun <TC : Enum<TC>> report(
        message: String,
        input: Input,
        token: Token<TC>,
    ) {
        errors.add(ParserError(message, input, token))
    }

    fun reportFatal(
        message: String,
        input: Input,
        location: Location,
    ) {
        errors.add(LexerError(message, input, location))
    }

    fun errors(): List<ReportedError> = errors
}
