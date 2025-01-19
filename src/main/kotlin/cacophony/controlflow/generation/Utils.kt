package cacophony.controlflow.generation

import cacophony.controlflow.*
import cacophony.controlflow.functions.CallableHandler
import cacophony.semantic.syntaxtree.BaseType
import cacophony.semantic.syntaxtree.Definition
import cacophony.semantic.syntaxtree.Type
import cacophony.semantic.types.*

internal fun noOpOr(value: Layout, mode: EvalMode): Layout = if (mode !is EvalMode.SideEffect) value else SimpleLayout(CFGNode.NoOp, false)

internal fun noOpOrUnit(mode: EvalMode): Layout = noOpOr(SimpleLayout(CFGNode.UNIT, false), mode)

fun generateLayoutOfVirtualRegisters(layout: Layout): Layout =
    when (layout) {
        is SimpleLayout ->
            SimpleLayout(
                CFGNode.RegisterUse(Register.VirtualRegister(layout.holdsReference), layout.holdsReference),
                layout.holdsReference,
            )
        is StructLayout ->
            StructLayout(layout.fields.mapValues { (_, subLayout) -> generateLayoutOfVirtualRegisters(subLayout) })
        is FunctionLayout ->
            FunctionLayout(
                generateLayoutOfVirtualRegisters(layout.code) as SimpleLayout,
                generateLayoutOfVirtualRegisters(layout.link) as SimpleLayout,
            )
        is VoidLayout -> VoidLayout()
    }

fun generateLayoutOfVirtualRegisters(type: TypeExpr): Layout =
    when (type) {
        is BuiltinType -> SimpleLayout(registerUse(Register.VirtualRegister(), false), false)
        is ReferentialType -> SimpleLayout(registerUse(Register.VirtualRegister(true), true), true)
        is StructType -> StructLayout(type.fields.mapValues { (_, fieldType) -> generateLayoutOfVirtualRegisters(fieldType) })
        is FunctionType ->
            FunctionLayout(
                SimpleLayout(registerUse(Register.VirtualRegister(), false), false),
                SimpleLayout(registerUse(Register.VirtualRegister(), true), true),
            )
        TypeExpr.VoidType -> VoidLayout()
    }

fun generateLayoutOfVirtualRegisters(type: Type): Layout =
    when (type) {
        is BaseType.Basic -> SimpleLayout(registerUse(Register.VirtualRegister(), false), false)
        is BaseType.Referential -> SimpleLayout(registerUse(Register.VirtualRegister(true), true), true)
        is BaseType.Structural -> StructLayout(type.fields.mapValues { (_, fieldType) -> generateLayoutOfVirtualRegisters(fieldType) })
        is BaseType.Functional -> throw IllegalArgumentException("No layout for function types")
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

fun getVariableLayout(handler: CallableHandler, variable: Variable): Layout =
    when (variable) {
        is Variable.PrimitiveVariable -> SimpleLayout(handler.generateVariableAccess(variable), variable.holdsReference)
        is Variable.StructVariable -> StructLayout(variable.fields.mapValues { (_, subfield) -> getVariableLayout(handler, subfield) })
        is Variable.FunctionVariable ->
            FunctionLayout(
                getVariableLayout(handler, variable.code) as SimpleLayout,
                getVariableLayout(handler, variable.link) as SimpleLayout,
            )
        is Variable.Heap -> throw IllegalArgumentException("`Heap` is a special marker `Variable` and has no layout")
    }

fun flattenLayout(layout: Layout): List<CFGNode> = layout.flatten().map { it.access }

private fun generateLayoutOfHeapObjectImpl(base: CFGNode, type: TypeExpr, offset: Int): Layout {
    var offsetInternal = offset
    return when (type) {
        is BuiltinType -> SimpleLayout(memoryAccess(base add integer(offset * REGISTER_SIZE), false), false)
        is ReferentialType -> SimpleLayout(memoryAccess(base add integer(offset * REGISTER_SIZE), true), true)
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

        is FunctionType ->
            FunctionLayout(
                SimpleLayout(memoryAccess(base add integer(offset * REGISTER_SIZE), false), false),
                SimpleLayout(memoryAccess(base add integer((offset + 1) * REGISTER_SIZE), true), true),
            )
        is TypeExpr.VoidType -> StructLayout(emptyMap()) // void type on the heap?
    }
}

fun generateLayoutOfHeapObject(base: CFGNode, type: TypeExpr): Layout = generateLayoutOfHeapObjectImpl(base, type, 0)

fun getForeignFunctionLayout(function: Definition.ForeignFunctionDeclaration) =
    FunctionLayout(SimpleLayout(dataLabel(function.getLabel())), SimpleLayout(integer(0)))
