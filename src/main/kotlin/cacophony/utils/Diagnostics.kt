package cacophony.utils

interface Diagnostics {
    fun report(
        message: String,
        input: Input,
        location: Location,
    )
}
