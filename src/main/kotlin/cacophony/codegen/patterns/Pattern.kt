package cacophony.codegen.patterns

import cacophony.codegen.BlockLabel
import cacophony.codegen.instructions.Instruction
import cacophony.controlflow.*

data class SlotFill(
    val valueFill: Map<ValueLabel, Register>,
    val registerFill: Map<RegisterLabel, Register>,
    val constantFill: Map<ConstantLabel, CFGNode.Constant>,
    val functionFill: Map<FunctionLabel, CFGNode.Function>,
)

sealed interface Pattern {
    val tree: CFGNode
}

interface SideEffectPattern : Pattern {
    fun makeInstance(fill: SlotFill): List<Instruction>
}

interface NoTemporaryRegistersPattern : SideEffectPattern

interface ValuePattern : Pattern {
    fun makeInstance(fill: SlotFill, destination: Register): List<Instruction>
}

interface ConditionPattern : Pattern {
    fun makeInstance(fill: SlotFill, destinationLabel: BlockLabel, jumpIf: Boolean = true): List<Instruction>
}
