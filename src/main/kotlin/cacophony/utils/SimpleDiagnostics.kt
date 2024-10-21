package cacophony.utils

class SimpleDiagnostics : Diagnostics {
    class ReportedError(message: String, input: Input, location: Location)

    private val errors: MutableList<ReportedError> = ArrayList()

    override fun report(
        message: String,
        input: Input,
        location: Location,
    ) {
        errors.add(ReportedError(message, input, location))
    }

    fun getErrors(): List<ReportedError> {
        return errors
    }
}
