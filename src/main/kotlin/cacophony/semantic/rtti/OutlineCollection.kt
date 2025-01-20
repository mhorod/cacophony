package cacophony.semantic.rtti

data class OutlineCollection(
    val objectOutlines: ObjectOutlines,
    val closureOutlines: LambdaOutlineLocation,
    val stackFrameOutlines: List<String>,
) {
    fun toAsm() = stackFrameOutlines + objectOutlines.asm + closureOutlines.values
}
