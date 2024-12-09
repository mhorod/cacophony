package cacophony.semantic.types

import cacophony.diagnostics.Diagnostics
import cacophony.semantic.syntaxtree.BaseType
import cacophony.semantic.syntaxtree.Type

private val builtinTypes = BuiltinType::class.sealedSubclasses.associate { it.objectInstance!!.name to it.objectInstance!! }

sealed class TypeExpr(
    val name: String,
) {
    override fun toString(): String = name

    override fun equals(other: Any?): Boolean {
        if (other == null || other !is TypeExpr) return false
        return name == other.name
    }

    override fun hashCode(): Int = name.hashCode()

    object VoidType : TypeExpr("Void")
}

sealed class BuiltinType private constructor(
    name: String,
) : TypeExpr(name) {
    object BooleanType : BuiltinType("Bool")

    object IntegerType : BuiltinType("Int")

    object UnitType : BuiltinType("Unit")
}

class FunctionType(
    val args: List<TypeExpr>,
    val result: TypeExpr,
) : TypeExpr(args.joinToString(", ", "[", "] -> ${result.name}"))

class StructType(
    val fields: Map<String, TypeExpr>,
) : TypeExpr(
        fields.map {
            it.key + ": " + it.value.toString()
        }.joinToString(", ", "{", "}"),
    )

class TypeTranslator(
    diagnostics: Diagnostics,
) {
    private val error = ErrorHandler(diagnostics)
    private val basicTypes: Map<String, TypeExpr> = builtinTypes

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
                    type.fields.map {
                        it.key to (translateType(it.value) ?: return null)
                    }.toMap()
                StructType(
                    fields,
                )
            }
        }
    }
}
