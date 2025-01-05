package cacophony.controlflow.generation

import cacophony.controlflow.CFGNode
import cacophony.controlflow.Register
import cacophony.controlflow.Variable
import cacophony.controlflow.functions.FunctionHandler
import cacophony.controlflow.registerUse
import cacophony.semantic.syntaxtree.BaseType
import cacophony.semantic.syntaxtree.Type
import cacophony.semantic.types.*

internal fun noOpOr(value: Layout, mode: EvalMode): Layout = if (mode is EvalMode.Value) value else SimpleLayout(CFGNode.NoOp)

internal fun noOpOrUnit(mode: EvalMode): Layout = noOpOr(SimpleLayout(CFGNode.UNIT), mode)

fun generateLayoutOfVirtualRegisters(layout: Layout): Layout =
    when (layout) {
        is SimpleLayout -> SimpleLayout(CFGNode.RegisterUse(Register.VirtualRegister()))
        is StructLayout -> StructLayout(layout.fields.mapValues { (_, subLayout) -> generateLayoutOfVirtualRegisters(subLayout) })
    }

fun generateLayoutOfVirtualRegisters(type: TypeExpr): Layout =
    when (type) {
        BuiltinType.BooleanType -> SimpleLayout(registerUse(Register.VirtualRegister()))
        BuiltinType.IntegerType -> SimpleLayout(registerUse(Register.VirtualRegister()))
        BuiltinType.UnitType -> SimpleLayout(registerUse(Register.VirtualRegister()))
        is StructType -> StructLayout(type.fields.mapValues { (_, fieldType) -> generateLayoutOfVirtualRegisters(fieldType) })
        TypeExpr.VoidType -> StructLayout(emptyMap())
        is FunctionType -> throw IllegalArgumentException("No layout for function types")
        is ReferentialType -> TODO()
    }

fun generateLayoutOfVirtualRegisters(type: Type): Layout =
    when (type) {
        is BaseType.Basic -> SimpleLayout(registerUse(Register.VirtualRegister()))
        is BaseType.Structural -> StructLayout(type.fields.mapValues { (_, fieldType) -> generateLayoutOfVirtualRegisters(fieldType) })
        is BaseType.Functional -> throw IllegalArgumentException("No layout for function types")
        is BaseType.Referential -> TODO()
    }

fun generateSubLayout(layout: Layout, type: TypeExpr): Layout {
    if (layout is StructLayout && type is StructType) {
        return StructLayout(
            type.fields.mapValues {
                generateSubLayout(
                    layout.fields[it.key] ?: throw IllegalArgumentException("layout does not match type"),
                    it.value,
                )
            },
        )
    } else if (layout is SimpleLayout && type !is StructType) {
        return layout
    }
    throw IllegalArgumentException("layout does not match type")
}

fun getVariableLayout(handler: FunctionHandler, variable: Variable): Layout =
    when (variable) {
        is Variable.PrimitiveVariable -> SimpleLayout(handler.generateVariableAccess(variable))
        is Variable.StructVariable -> StructLayout(variable.fields.mapValues { (_, subfield) -> getVariableLayout(handler, subfield) })
    }

fun flattenLayout(layout: Layout): List<CFGNode> =
    when (layout) {
        is SimpleLayout -> listOf(layout.access)
        is StructLayout ->
            layout.fields
                .toSortedMap()
                .map { (_, subLayout) -> flattenLayout(subLayout) }
                .flatten()
    }

fun generateLayoutOfHeapObject(base: CFGNode, type: TypeExpr): Layout {
    TODO()
}
