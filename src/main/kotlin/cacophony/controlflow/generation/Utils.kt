package cacophony.controlflow.generation

import cacophony.controlflow.CFGNode

internal fun noOpOr(
    value: CFGNode.Unconditional,
    mode: EvalMode,
): CFGNode.Unconditional = if (mode is EvalMode.Value) value else CFGNode.NoOp

internal fun noOpOrUnit(mode: EvalMode): CFGNode.Unconditional = noOpOr(CFGNode.UNIT, mode)
