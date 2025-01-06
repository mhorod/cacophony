package cacophony.controlflow.generation

import cacophony.controlflow.CFGNode
import cacophony.controlflow.Register
import cacophony.controlflow.Variable
import cacophony.controlflow.functions.FunctionHandler
import cacophony.controlflow.registerUse
import cacophony.semantic.syntaxtree.BaseType
import cacophony.semantic.syntaxtree.Type
import cacophony.semantic.types.*

internal fun noOpOr(value: Layout, mode: EvalMode): Layout = if (mode is EvalMode.Value) value else SimpleLayout(CFGNode.NoOp, false)

internal fun noOpOrUnit(mode: EvalMode): Layout = noOpOr(SimpleLayout(CFGNode.UNIT, false), mode)

fun generateLayoutOfVirtualRegisters(layout: Layout): Layout =
    when (layout) {
        is SimpleLayout -> SimpleLayout(CFGNode.RegisterUse(Register.VirtualRegister(layout.holdsReference), layout.holdsReference), layout.holdsReference)
        is StructLayout -> StructLayout(layout.fields.mapValues { (_, subLayout) -> generateLayoutOfVirtualRegisters(subLayout) })
    }

fun generateLayoutOfVirtualRegisters(type: TypeExpr): Layout =
    when (type) {
        BuiltinType.BooleanType -> SimpleLayout(registerUse(Register.VirtualRegister(), false), false)
        BuiltinType.IntegerType -> SimpleLayout(registerUse(Register.VirtualRegister(), false), false)
        BuiltinType.UnitType -> SimpleLayout(registerUse(Register.VirtualRegister(), false), false)
        is StructType -> StructLayout(type.fields.mapValues { (_, fieldType) -> generateLayoutOfVirtualRegisters(fieldType) })
        TypeExpr.VoidType -> StructLayout(emptyMap())
        is FunctionType -> throw IllegalArgumentException("No layout for function types")
        is ReferentialType -> TODO()
    }

fun generateLayoutOfVirtualRegisters(type: Type): Layout =
    when (type) {
        is BaseType.Basic -> SimpleLayout(registerUse(Register.VirtualRegister(), false), false)
        is BaseType.Structural -> StructLayout(type.fields.mapValues { (_, fieldType) -> generateLayoutOfVirtualRegisters(fieldType) })
        is BaseType.Functional -> throw IllegalArgumentException("No layout for function types")
        is BaseType.Referential -> TODO()
    }

fun getVariableLayout(handler: FunctionHandler, variable: Variable): Layout =
    when (variable) {
        is Variable.PrimitiveVariable -> SimpleLayout(handler.generateVariableAccess(variable), variable.holdsReference)
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
