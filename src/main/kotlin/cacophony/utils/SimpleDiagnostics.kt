package cacophony.utils

import cacophony.token.Token

class SimpleDiagnostics : Diagnostics {
    class ReportedError(
        message: String,
    )

    private val errors: MutableList<ReportedError> = ArrayList()

    override fun report(
        message: String,
        input: Input,
        location: Location,
    ) {
        errors.add(ReportedError("${input.locationToString(location)}: $message"))
    }

    override fun <TC : Enum<TC>> report(
        message: String,
        input: Input,
        token: Token<TC>,
    ) {
        errors.add(ReportedError("${input.locationRangeToString(token.rangeFrom, token.rangeTo)}: $message"))
    }

    fun getErrors(): List<ReportedError> = errors
}
