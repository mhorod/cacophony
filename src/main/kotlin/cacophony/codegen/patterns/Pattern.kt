package cacophony.codegen.patterns

import cacophony.codegen.BlockLabel
import cacophony.codegen.instructions.Instruction
import cacophony.controlflow.*

data class SlotFill(
    val valueFill: Map<ValueLabel, Register>,
    val registerFill: Map<RegisterLabel, Register>,
    val constantFill: Map<ConstantLabel, CFGNode.Constant>,
    val nodeFill: Map<NodeLabel, CFGNode>,
)

sealed interface Pattern {
    val tree: CFGNode

    // Used to break ties in case two patterns cover the same amount of nodes.
    fun priority(): Int = 0
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
