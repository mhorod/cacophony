package cacophony.utils
import cacophony.token.Token

interface Diagnostics {
    fun report(
        message: String,
        input: Input,
        location: Location,
    )

    fun <TC : Enum<TC>> report(
        message: String,
        input: Input,
        token: Token<TC>,
    )
}
