package cacophony.codegen.patterns

import cacophony.codegen.BlockLabel
import cacophony.codegen.instructions.Instruction
import cacophony.controlflow.*
import kotlin.reflect.cast

// TODO refactor slot fill construction
data class SlotFill(
    val valueFill: Map<ValueLabel, Register>,
    val registerFill: Map<RegisterLabel, Register>,
    val constantFill: Map<ConstantLabel, CFGNode.Constant>,
    val functionFill: Map<FunctionLabel, CFGNode.Function>,
    val nodeFill: Map<NodeLabel, CFGNode>
) {
    fun <T : CFGNode> getNodeForNodeSlot(slot: CFGNode.NodeSlot<T>): T {
        return slot.clazz.cast(nodeFill[slot.label]!!)
    }
}

sealed interface Pattern {
    val tree: CFGNode
    fun priority(): Int = 0
}

interface SideEffectPattern : Pattern {
    fun makeInstance(fill: SlotFill): List<Instruction>
}

interface ValuePattern : Pattern {
    fun makeInstance(fill: SlotFill, destination: Register): List<Instruction>
}

interface ConditionPattern : Pattern {
    fun makeInstance(fill: SlotFill, destinationLabel: BlockLabel, jumpIf: Boolean = true): List<Instruction>
}
