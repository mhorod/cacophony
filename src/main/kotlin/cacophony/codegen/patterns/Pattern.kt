package cacophony.codegen.patterns

import cacophony.codegen.BlockLabel
import cacophony.codegen.instructions.Instruction
import cacophony.controlflow.*

data class SlotFill(
    val valueFill: Map<ValueLabel, CFGNode.Value>,
    val registerFill: Map<RegisterLabel, CFGNode.RegisterUse>,
    val constantFill: Map<ConstantLabel, CFGNode.Constant>,
)

sealed interface Pattern {
    val tree: CFGNode
}

interface SideEffectPattern : Pattern {
    fun makeInstance(fill: SlotFill): List<Instruction>
}

interface ValuePattern : Pattern {
    fun makeInstance(fill: SlotFill, destination: Register): List<Instruction>
}

interface ConditionPattern : Pattern {
    fun makeInstance(fill: SlotFill, destinationLabel: BlockLabel): List<Instruction>
}
