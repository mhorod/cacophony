package cacophony.controlflow.generation

import cacophony.controlflow.CFGNode
import cacophony.controlflow.Register

internal fun noOpOr(value: CFGNode, mode: EvalMode): CFGNode = if (mode is EvalMode.Value) value else CFGNode.NoOp

internal fun noOpOrUnit(mode: EvalMode): CFGNode = noOpOr(CFGNode.UNIT, mode)

fun generateLayoutOfVirtualRegisters(layout: Layout): Layout =
    when (layout) {
        is SimpleLayout -> SimpleLayout(CFGNode.RegisterUse(Register.VirtualRegister()))
        is StructLayout -> StructLayout(layout.fields.mapValues { (_, subLayout) -> generateLayoutOfVirtualRegisters(subLayout) })
    }
