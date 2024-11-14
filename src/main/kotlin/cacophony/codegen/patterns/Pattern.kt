package cacophony.codegen.patterns

import cacophony.codegen.instructions.Instruction
import cacophony.codegen.instructions.InstructionLabel
import cacophony.controlflow.CFGNode
import cacophony.controlflow.Register
import cacophony.controlflow.SlotLabel

typealias SlotFill = Map<SlotLabel, CFGNode>

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
