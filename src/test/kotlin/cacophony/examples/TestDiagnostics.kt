package cacophony.examples

import cacophony.utils.Diagnostics
import cacophony.utils.Input
import cacophony.utils.Location

class TestDiagnostics : Diagnostics {
    class ReportedError(message: String, input: Input, location: Location)

    private val errors: MutableList<ReportedError> = ArrayList()

    override fun report(
        message: String,
        input: Input,
        location: Location,
    ) {
        errors.add(ReportedError(message, input, location))
    }

    fun reportFatal(
        message: String,
        input: Input,
        location: Location,
    ) {
        errors.add(ReportedError(message, input, location))
    }

    fun errors(): List<ReportedError> {
        return errors
    }
}
