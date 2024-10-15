package cacophony.token

import cacophony.utils.Location

data class Token<TC : Enum<TC>>(val category: TC, val context: String, val rangeFrom: Location, val rangeTo: Location)
