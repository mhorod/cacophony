package cacophony.semantic.rtti

import cacophony.semantic.types.*
import kotlin.math.absoluteValue

typealias ObjectOutlineLocation = Map<TypeExpr, String>

/**
 * Object outline consists of 8B blocks:
 * - The first block denotes object size (in 8B blocks, e.g. Int or a reference has size 1).
 * - The next ceil(size / 64) blocks contain info if a field in the struct is a pointer.
 *   To check if i-th field in flattened structure is a pointer, check block
 *   number i / 64 on bit position i % 64.
 */

class ObjectOutlinesCreator {
    private val locations: MutableMap<TypeExpr, String> = mutableMapOf()
    private val asmDataSectionEntries = mutableListOf<String>()

    private fun structToLabel(type: StructType): String = "${type.fields.keys.sorted().joinToString("_")}_${type.hashCode().absoluteValue}"

    private fun toIsPointerList(type: TypeExpr): List<Boolean> =
        when (type) {
            is StructType -> {
                type.fields.entries.sortedBy { it.key }.map { it.value }.flatMap { toIsPointerList(it) }.toList()
            }
            is ReferentialType -> listOf(true)
            is BuiltinType -> listOf(false)
            is FunctionType -> listOf(true) // maybe update in the future
            is TypeExpr.VoidType -> emptyList()
        }

    private fun toIsPointerMaskChunks(type: TypeExpr): List<ULong> {
        val isPointerList = toIsPointerList(type)
        assert(isPointerList.size == type.size())

        val numberOfChunks = (isPointerList.size + ULong.SIZE_BITS - 1) / ULong.SIZE_BITS
        val chunks = Array(numberOfChunks) { 0UL }
        for ((index, isPointer) in isPointerList.withIndex()) {
            if (isPointer) {
                chunks[index / ULong.SIZE_BITS] += 1UL shl (index % ULong.SIZE_BITS)
            }
        }
        return chunks.toList()
    }

    fun add(type: TypeExpr) {
        if (type !is StructType) return

        val label = structToLabel(type)
        val outline = listOf(type.size().toULong()) + toIsPointerMaskChunks(type)
        val asmEntry = "$label: dq ${outline.joinToString(", ")}"

        locations[type] = label
        asmDataSectionEntries.add(asmEntry)
    }

    fun add(types: List<TypeExpr>) {
        types.forEach { add(it) }
    }

    fun getLocations(): ObjectOutlineLocation = locations

    fun getAsm(): List<String> = asmDataSectionEntries
}
