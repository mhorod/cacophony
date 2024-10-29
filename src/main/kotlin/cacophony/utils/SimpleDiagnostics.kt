package cacophony.utils

class SimpleDiagnostics(
    private val input: Input,
) : Diagnostics {
    class ReportedError(
        val message: String,
    )

    private val errors: MutableList<ReportedError> = ArrayList()

    override fun report(
        message: String,
        location: Location,
    ) {
        errors.add(ReportedError("${input.locationToString(location)}: $message"))
    }

    override fun report(
        message: String,
        range: Pair<Location, Location>,
    ) {
        errors.add(ReportedError("${input.locationRangeToString(range.first, range.second)}: $message"))
    }

    fun getErrors(): List<ReportedError> = errors
}
