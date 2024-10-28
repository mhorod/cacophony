package cacophony.utils

interface Diagnostics {
    fun report(
        message: String,
        location: Location,
    )

    fun report(
        message: String,
        range: Pair<Location, Location>,
    )
}
