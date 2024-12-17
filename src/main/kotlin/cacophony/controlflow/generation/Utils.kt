package cacophony.controlflow.generation

import cacophony.controlflow.CFGNode
import cacophony.controlflow.Register
import cacophony.controlflow.Variable
import cacophony.controlflow.functions.FunctionHandler
import cacophony.semantic.types.BuiltinType
import cacophony.semantic.types.FunctionType
import cacophony.semantic.types.StructType
import cacophony.semantic.types.TypeExpr

internal fun noOpOr(value: Layout, mode: EvalMode): Layout = if (mode is EvalMode.Value) value else SimpleLayout(CFGNode.NoOp)

internal fun noOpOrUnit(mode: EvalMode): Layout = noOpOr(SimpleLayout(CFGNode.UNIT), mode)

fun generateLayoutOfVirtualRegisters(layout: Layout): Layout =
    when (layout) {
        is SimpleLayout -> SimpleLayout(CFGNode.RegisterUse(Register.VirtualRegister()))
        is StructLayout -> StructLayout(layout.fields.mapValues { (_, subLayout) -> generateLayoutOfVirtualRegisters(subLayout) })
    }

fun generateLayoutOfVirtualRegisters(type: TypeExpr): Layout =
    when (type) {
        BuiltinType.BooleanType -> SimpleLayout(CFGNode.RegisterUse(Register.VirtualRegister()))
        BuiltinType.IntegerType -> SimpleLayout(CFGNode.RegisterUse(Register.VirtualRegister()))
        BuiltinType.UnitType -> SimpleLayout(CFGNode.RegisterUse(Register.VirtualRegister()))
        is StructType -> StructLayout(type.fields.mapValues { (_, fieldType) -> generateLayoutOfVirtualRegisters(fieldType) })
        TypeExpr.VoidType -> SimpleLayout(CFGNode.RegisterUse(Register.VirtualRegister())) // TODO: ???
        is FunctionType -> throw IllegalArgumentException("No layout for function types")
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
