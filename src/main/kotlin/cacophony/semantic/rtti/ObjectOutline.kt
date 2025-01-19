package cacophony.semantic.rtti

import cacophony.semantic.syntaxtree.LambdaExpression
import cacophony.semantic.types.*
import kotlin.math.absoluteValue

typealias ObjectOutlineLocation = Map<TypeExpr, String>
typealias LambdaOutlineLocation = Map<LambdaExpression, String>

data class ObjectOutlines(
    val locations: ObjectOutlineLocation,
    val lambdaLocations: LambdaOutlineLocation,
    val asm: List<String>,
)

fun createObjectOutlines(types: List<TypeExpr>, lambdas: List<LambdaExpression>): ObjectOutlines {
    val objectOutlinesCreator = ObjectOutlinesCreator()
    // all internal structures, like closures, must later be added here
    objectOutlinesCreator.addTypes(types)
    objectOutlinesCreator.addLambdas(lambdas)
    return ObjectOutlines(
        objectOutlinesCreator.getLocations(),
        emptyMap(), // TODO
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
            is StructType -> "S${type.hashCode().absoluteValue}"
            is BuiltinType.BooleanType -> "B${type.hashCode().absoluteValue}"
            is BuiltinType.IntegerType -> "I${type.hashCode().absoluteValue}"
            is BuiltinType.UnitType -> "U${type.hashCode().absoluteValue}"
            is FunctionType -> "F${type.hashCode().absoluteValue}"
            is ReferentialType -> "${getReferenceDepth(type)}R${typeToLabel(getReferenceBase(type))}"
            is TypeExpr.VoidType -> "V${type.hashCode().absoluteValue}"
        }

    private fun toIsPointerList(type: TypeExpr): List<Boolean> =
        when (type) {
            is StructType -> type.fields.entries.sortedBy { it.key }.map { it.value }.flatMap { toIsPointerList(it) }.toList()
            is ReferentialType -> listOf(true)
            is BuiltinType -> listOf(false)
            is FunctionType -> listOf(true) // maybe update in the future
            is TypeExpr.VoidType -> emptyList()
        }

    private fun add(type: TypeExpr) {
        if (locations.containsKey(type)) return
        if (type is FunctionType) return // functional types not supported for now

        val label = "outline_${typeToLabel(type)}"
        val asmEntry = toAsm(label, toIsPointerList(type))

        locations[type] = label
        asmDataSectionEntries.add(asmEntry)
    }

    private fun add(lambda: LambdaExpression) {
        // TODO
    }

    fun addTypes(types: List<TypeExpr>) {
        types.forEach { add(it) }
    }

    fun addLambdas(lambdas: List<LambdaExpression>) {
        lambdas.forEach { add(it) }
    }

    fun getLocations(): ObjectOutlineLocation = locations

    fun getAsm(): List<String> = asmDataSectionEntries
}
