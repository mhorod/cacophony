package cacophony.diagnostics

import cacophony.utils.Location

interface Diagnostics {
    fun report(
        message: DiagnosticMessage,
        location: Location,
    ) = report(message, location to location)

    fun report(
        message: DiagnosticMessage,
        range: Pair<Location, Location>,
    )
}
