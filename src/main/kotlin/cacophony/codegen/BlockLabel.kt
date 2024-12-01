package cacophony.codegen

data class BlockLabel(val name: String) {
    override fun toString() = ".${name}_${hashCode()}"
}
