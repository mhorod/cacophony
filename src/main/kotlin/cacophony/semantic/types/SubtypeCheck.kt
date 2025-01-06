package cacophony.semantic.types

fun isSubtype(subtype: TypeExpr, type: TypeExpr): Boolean {
    if (subtype == TypeExpr.VoidType) return true
    return when (type) {
        is BuiltinType -> isSubtypeBuiltin(subtype, type)
        is FunctionType -> isSubtypeFunction(subtype, type)
        is StructType -> isSubtypeStruct(subtype, type)
        is ReferentialType -> subtype == type
        else -> false
    }
}

fun isSubtypeBuiltin(subtype: TypeExpr, builtinType: BuiltinType): Boolean {
    return when (builtinType) {
        is BuiltinType.IntegerType -> subtype is BuiltinType.IntegerType
        is BuiltinType.BooleanType -> subtype is BuiltinType.BooleanType
        is BuiltinType.UnitType -> subtype is BuiltinType.UnitType
    }
}

fun isSubtypeFunction(subtype: TypeExpr, functionType: FunctionType): Boolean {
    if (subtype !is FunctionType) return false
    if (!isSubtype(subtype.result, functionType.result)) return false
    val functionArgs = functionType.args
    val subtypeArgs = subtype.args
    if (subtypeArgs.size != functionArgs.size) return false
    return subtypeArgs.zip(functionArgs).all { (argSubtype, argFunction) ->
        isSubtype(argFunction, argSubtype) // contravariance
    }
}

fun isSubtypeStruct(subtype: TypeExpr, structType: StructType): Boolean {
    if (subtype !is StructType) return false
    return structType.fields.all { (identifier, type) ->
        isSubtype((subtype.fields[identifier] ?: return false), type)
    }
}
