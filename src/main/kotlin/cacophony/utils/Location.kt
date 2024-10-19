package cacophony.utils

data class Location(
    val value: Int,
) : Comparable<Location> {
    override fun compareTo(other: Location): Int = value.compareTo(other.value)
}
