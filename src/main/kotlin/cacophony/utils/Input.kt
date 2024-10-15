package cacophony.util

interface Input {
    // Retrieves next character from the input and advances the cursor.
    // Returns null if the end is reached.
    fun next(): Char?

    // Gets the current position of the cursor in the input.
    fun getLocation(): Location

    // Sets the current position of the cursor in the input.
    fun setLocation(loc: Location)

    // Converts current cursor to human-readable string that may contain additional information.
    fun locationToString(loc: Location): String
}
