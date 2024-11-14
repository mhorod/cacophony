package cacophony.controlflow.generation

import cacophony.controlflow.CFGNode

internal fun noOpOr(
    value: CFGNode,
    mode: EvalMode,
): CFGNode = if (mode is EvalMode.Value) value else CFGNode.NoOp

internal fun noOpOrUnit(mode: EvalMode): CFGNode = noOpOr(CFGNode.UNIT, mode)
