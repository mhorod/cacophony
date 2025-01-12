package cacophony.semantic.rtti

private fun toMask(isRef: List<Boolean>): Array<ULong> {
    val numberOfChunks = (isRef.size + ULong.SIZE_BITS - 1) / ULong.SIZE_BITS
    val chunks = Array(numberOfChunks) { 0UL }
    for ((index, isPointer) in isRef.withIndex()) {
        if (isPointer) {
            chunks[index / ULong.SIZE_BITS] += 1UL shl (index % ULong.SIZE_BITS)
        }
    }
    return chunks
}

internal fun toAsm(label: String, isRef: List<Boolean>) = "$label: dq ${(listOf(isRef.size) + toMask(isRef)).joinToString(", ")}"
