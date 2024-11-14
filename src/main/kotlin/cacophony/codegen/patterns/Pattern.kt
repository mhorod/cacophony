package cacophony.codegen.patterns

import cacophony.codegen.instructions.Instruction
import cacophony.codegen.instructions.InstructionLabel
import cacophony.controlflow.*

data class SlotFill(
    val valueFill: Map<ValueLabel, CFGNode.Value>,
    val registerFill: Map<RegisterLabel, Register>,
    val registerFill: Map<RegisterLabel, Register>,
) {

}
//typealias ValueSlotFill = Map<SlotLabel, CFGNode.Value>
//typealias RegisterFill = Map<SlotLabel, Register>

sealed class Pattern(val tree: CFGNode)

abstract class SideEffectPattern(tree: CFGNode) : Pattern(tree) {
    abstract fun makeInstance(fill: SlotFill): List<Instruction>
}

abstract class ValuePattern(tree: CFGNode) : Pattern(tree) {
    abstract fun makeInstance(
        fill: SlotFill,
        destination: Register,
    ): List<Instruction>
}

abstract class ConditionPattern(tree: CFGNode) : Pattern(tree) {
    abstract fun makeInstance(
        fill: SlotFill,
        destinationLabel: InstructionLabel,
    ): List<Instruction>
}
