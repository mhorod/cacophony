package cacophony.semantic.types

// Returns true iff type <: other (type is a subtype of other).
fun isSubtype(type: TypeExpr, other: TypeExpr): Boolean {
    if (type == TypeExpr.VoidType) return true
    return when (other) {
        is BuiltinType -> isSubtypeBuiltin(type, other)
        is FunctionType -> isSubtypeFunction(type, other)
        is StructType -> isSubtypeStruct(type, other)
        is ReferentialType -> isSubtypeReferential(type, other)
        else -> false
    }
}

fun isSubtypeBuiltin(type: TypeExpr, other: BuiltinType): Boolean =
    when (other) {
        is BuiltinType.IntegerType -> type is BuiltinType.IntegerType
        is BuiltinType.BooleanType -> type is BuiltinType.BooleanType
        is BuiltinType.UnitType -> type is BuiltinType.UnitType
    }

fun isSubtypeFunction(type: TypeExpr, other: FunctionType): Boolean {
    if (type !is FunctionType) return false
    if (type.result != other.result) return false
    val functionArgs = other.args
    val typeArgs = type.args
    if (typeArgs.size != functionArgs.size) return false
    return typeArgs.zip(functionArgs).all { (argSubtype, argFunction) ->
        argFunction == argSubtype
    }
}

fun isSubtypeStruct(type: TypeExpr, other: StructType): Boolean {
    if (type !is StructType) return false
    return other.fields.all { (identifier, fieldType) ->
        isSubtype((type.fields[identifier] ?: return false), fieldType)
    }
}

fun isSubtypeReferential(type: TypeExpr, other: ReferentialType): Boolean {
    if (type !is ReferentialType) return false
    return type.type == other.type
}
