package cacophony.controlflow.generation

import cacophony.controlflow.CFGNode
import cacophony.controlflow.Register

internal fun noOpOr(value: Layout, mode: EvalMode): Layout = if (mode is EvalMode.Value) value else SimpleLayout(CFGNode.NoOp)

internal fun noOpOrUnit(mode: EvalMode): Layout = noOpOr(SimpleLayout(CFGNode.UNIT), mode)

fun generateLayoutOfVirtualRegisters(layout: Layout): Layout =
    when (layout) {
        is SimpleLayout -> SimpleLayout(CFGNode.RegisterUse(Register.VirtualRegister()))
        is StructLayout -> StructLayout(layout.fields.mapValues { (_, subLayout) -> generateLayoutOfVirtualRegisters(subLayout) })
    }
