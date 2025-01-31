package cacophony.semantic.types

import cacophony.diagnostics.Diagnostics
import cacophony.semantic.syntaxtree.BaseType
import cacophony.semantic.syntaxtree.Type

val BUILTIN_TYPES = BuiltinType::class.sealedSubclasses.associate { it.objectInstance!!.name to it.objectInstance!! }
val NON_ALLOCATABLE_TYPES = listOf(TypeExpr.VoidType, BuiltinType.UnitType)

sealed class TypeExpr(
    val name: String,
) {
    override fun toString(): String = name

    override fun equals(other: Any?): Boolean {
        if (other == null || other !is TypeExpr) return false
        return name == other.name
    }

    override fun hashCode(): Int = name.hashCode()

    abstract fun size(): Int

    object VoidType : TypeExpr("Void") {
        override fun size(): Int = 0
    }
}

sealed class BuiltinType private constructor(
    name: String,
) : TypeExpr(name) {
    override fun size(): Int = 1

    object BooleanType : BuiltinType("Bool")

    object IntegerType : BuiltinType("Int")

    object UnitType : BuiltinType("Unit")
}

class FunctionType(
    val args: List<TypeExpr>,
    val result: TypeExpr,
) : TypeExpr(args.joinToString(", ", "[", "] -> ${result.name}")) {
    override fun size(): Int = 2
}

class StructType(
    val fields: Map<String, TypeExpr>,
) : TypeExpr(
        fields
            .map { it.key + ": " + it.value.toString() }
            .joinToString(", ", "{", "}"),
    ) {
    override fun size(): Int = fields.values.sumOf { it.size() }
}

data class ReferentialType(val type: TypeExpr) : TypeExpr("&${type.name}") {
    override fun size(): Int = 1

    override fun toString(): String = super.toString()
}

class TypeTranslator(
    diagnostics: Diagnostics,
) {
    private val error = ErrorHandler(diagnostics)
    private val basicTypes: Map<String, TypeExpr> = BUILTIN_TYPES

    internal fun translateType(type: Type): TypeExpr? {
        return when (type) {
            is BaseType.Basic ->
                basicTypes[type.identifier] ?: run {
                    error.unknownType(type.range)
                    null
                }

            is BaseType.Functional -> {
                val args = type.argumentsType.map { translateType(it) }
                val ret = translateType(type.returnType)
                FunctionType(
                    args.map { it ?: return null },
                    ret ?: return null,
                )
            }

            is BaseType.Structural -> {
                val fields =
                    type.fields
                        .map { it.key to (translateType(it.value) ?: return null) }
                        .toMap()
                StructType(fields)
            }

            is BaseType.Referential -> ReferentialType(translateType(type.type) ?: return null)
        }
    }
}
