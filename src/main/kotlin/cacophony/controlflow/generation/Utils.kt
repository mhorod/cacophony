package cacophony.controlflow.generation

import cacophony.controlflow.*
import cacophony.controlflow.functions.FunctionHandler
import cacophony.semantic.syntaxtree.BaseType
import cacophony.semantic.syntaxtree.Type
import cacophony.semantic.types.*

internal fun noOpOr(value: Layout, mode: EvalMode): Layout = if (mode !is EvalMode.SideEffect) value else SimpleLayout(CFGNode.NoOp)

internal fun noOpOrUnit(mode: EvalMode): Layout = noOpOr(SimpleLayout(CFGNode.UNIT), mode)

fun generateLayoutOfVirtualRegisters(layout: Layout): Layout =
    when (layout) {
        is SimpleLayout -> SimpleLayout(CFGNode.RegisterUse(Register.VirtualRegister()))
        is StructLayout -> StructLayout(layout.fields.mapValues { (_, subLayout) -> generateLayoutOfVirtualRegisters(subLayout) })
    }

fun generateLayoutOfVirtualRegisters(type: TypeExpr): Layout =
    when (type) {
        is BuiltinType, is ReferentialType -> SimpleLayout(registerUse(Register.VirtualRegister()))
        is StructType -> StructLayout(type.fields.mapValues { (_, fieldType) -> generateLayoutOfVirtualRegisters(fieldType) })
        TypeExpr.VoidType -> StructLayout(emptyMap())
        is FunctionType -> throw IllegalArgumentException("No layout for function types")
    }

fun generateLayoutOfVirtualRegisters(type: Type): Layout =
    when (type) {
        is BaseType.Basic, is BaseType.Referential -> SimpleLayout(registerUse(Register.VirtualRegister()))
        is BaseType.Structural -> StructLayout(type.fields.mapValues { (_, fieldType) -> generateLayoutOfVirtualRegisters(fieldType) })
        is BaseType.Functional -> throw IllegalArgumentException("No layout for function types")
    }

fun getVariableLayout(handler: FunctionHandler, variable: Variable): Layout =
    when (variable) {
        is Variable.PrimitiveVariable -> SimpleLayout(handler.generateVariableAccess(variable))
        is Variable.StructVariable -> StructLayout(variable.fields.mapValues { (_, subfield) -> getVariableLayout(handler, subfield) })
        is Variable.Heap -> throw IllegalArgumentException("`Heap` is a special marker `Variable` and has no layout")
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

private fun generateLayoutOfHeapObjectImpl(base: CFGNode, type: TypeExpr, offset: Int): Layout {
    var offsetInternal = offset
    return when (type) {
        is BuiltinType, is ReferentialType -> SimpleLayout(memoryAccess(base add integer(offset * REGISTER_SIZE)))
        is StructType ->
            StructLayout(
                type.fields.entries
                    .sortedBy { it.key }
                    .associate { (name, fieldType) ->
                        val layout = generateLayoutOfHeapObjectImpl(base, fieldType, offsetInternal)
                        offsetInternal += fieldType.size()
                        Pair(name, layout)
                    },
            )

        is FunctionType -> throw IllegalArgumentException("No layout for function types")
        is TypeExpr.VoidType -> StructLayout(emptyMap()) // void type on the heap?
    }
}

fun generateLayoutOfHeapObject(base: CFGNode, type: TypeExpr): Layout = generateLayoutOfHeapObjectImpl(base, type, 0)
