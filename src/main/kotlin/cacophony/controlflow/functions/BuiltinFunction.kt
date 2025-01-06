package cacophony.controlflow.functions

import cacophony.semantic.syntaxtree.BaseType
import cacophony.semantic.syntaxtree.Definition
import cacophony.semantic.syntaxtree.Type
import cacophony.utils.Location

internal fun mockLocation() = Pair(Location(-1), Location(-1))

internal fun builtin(identifier: String, argumentsType: List<Type>, returnType: Type): Definition.FunctionDeclaration =
    Definition.ForeignFunctionDeclaration(
        mockLocation(),
        identifier,
        BaseType.Functional(mockLocation(), argumentsType, returnType),
        returnType,
    )

private val intType = BaseType.Basic(mockLocation(), "Int")

object Builtin {
    val allocStruct = builtin("alloc_struct", listOf(intType), intType)

    val all = listOf(allocStruct)
}
