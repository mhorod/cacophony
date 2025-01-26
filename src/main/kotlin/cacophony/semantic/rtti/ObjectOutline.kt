package cacophony.semantic.rtti

import cacophony.semantic.types.*

typealias ObjectOutlineLocation = Map<TypeExpr, String>

data class ObjectOutlines(
    val locations: ObjectOutlineLocation,
    val asm: List<String>,
)

fun createObjectOutlines(types: List<TypeExpr>): ObjectOutlines {
    val objectOutlinesCreator = ObjectOutlinesCreator()
    objectOutlinesCreator.addTypes(types)
    return ObjectOutlines(
        objectOutlinesCreator.getLocations(),
        objectOutlinesCreator.getAsm(),
    )
}

/**
 * Object outline consists of 8B blocks:
 * - The first block denotes object size (in 8B blocks, e.g. Int or a reference has size 1).
 * - The next ceil(size / 64) blocks contain info if a field in the struct is a pointer.
 *   To check if i-th field (fields are sorted by name) in flattened structure is a pointer,
 *   check block number i / 64 on bit position i % 64.
 */

internal class ObjectOutlinesCreator {
    private val locations: MutableMap<TypeExpr, String> = mutableMapOf()
    private val labels: MutableSet<String> = mutableSetOf()
    private val asmDataSectionEntries = mutableListOf<String>()

    private fun getReferenceDepth(type: TypeExpr): Int =
        when (type) {
            is ReferentialType -> 1 + getReferenceDepth((type.type))
            else -> 0
        }

    private fun getReferenceBase(type: TypeExpr): TypeExpr =
        when (type) {
            is ReferentialType -> getReferenceBase(type.type)
            else -> type
        }

    private fun typeToLabel(type: TypeExpr): String =
        when (type) {
            is StructType -> "S${type.hashCode().toUInt()}"
            is BuiltinType.BooleanType -> "B${type.hashCode().toUInt()}"
            is BuiltinType.IntegerType -> "I${type.hashCode().toUInt()}"
            is BuiltinType.UnitType -> "U${type.hashCode().toUInt()}"
            is FunctionType -> "F"
            is ReferentialType -> "${getReferenceDepth(type)}R${typeToLabel(getReferenceBase(type))}"
            is TypeExpr.VoidType -> "V${type.hashCode().toUInt()}"
        }

    private fun toIsPointerList(type: TypeExpr): List<Boolean> =
        when (type) {
            is StructType ->
                type.fields.entries
                    .sortedBy { it.key }
                    .map { it.value }
                    .flatMap { toIsPointerList(it) }
                    .toList()
            is ReferentialType -> listOf(true)
            is BuiltinType -> listOf(false)
            is FunctionType -> listOf(false, true)
            is TypeExpr.VoidType -> emptyList()
        }

    private fun add(type: TypeExpr) {
        if (locations.containsKey(type)) return

        val label = "outline_${typeToLabel(type)}"
        locations[type] = label

        if (!labels.add(label)) {
            return
        }

        val asmEntry = toAsm(label, toIsPointerList(type))
        asmDataSectionEntries.add(asmEntry)
    }

    fun addTypes(types: List<TypeExpr>) {
        types.forEach { add(it) }
    }

    fun getLocations(): ObjectOutlineLocation = locations

    fun getAsm(): List<String> = asmDataSectionEntries
}
