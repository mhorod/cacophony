package cacophony.codegen.patterns.cacophonyPatterns

import cacophony.codegen.BlockLabel
import cacophony.codegen.instructions.Instruction
import cacophony.codegen.patterns.ConditionPattern
import cacophony.codegen.patterns.SlotFill
import cacophony.controlflow.eq
import cacophony.controlflow.geq
import cacophony.controlflow.gt
import cacophony.controlflow.leq
import cacophony.controlflow.lt
import cacophony.controlflow.neq
import cacophony.controlflow.not

val conditionPatterns =
    listOf(
        EqualsConditionPattern,
        NotEqualsConditionPattern,
        LessConditionPattern,
        LessEqualConditionPattern,
        GreaterConditionPattern,
        GreaterEqualConditionPattern,
        LogicalNotConditionPattern,
    )

object EqualsConditionPattern : ConditionPattern, BinaryOpPattern() {
    override val tree = lhsSlot eq rhsSlot

    override fun makeInstance(fill: SlotFill, destinationLabel: BlockLabel, jumpIf: Boolean): List<Instruction> {
        TODO("Not yet implemented")
    }
}

object NotEqualsConditionPattern : ConditionPattern, BinaryOpPattern() {
    override val tree = lhsSlot neq rhsSlot

    override fun makeInstance(fill: SlotFill, destinationLabel: BlockLabel, jumpIf: Boolean): List<Instruction> {
        TODO("Not yet implemented")
    }
}

object LessConditionPattern : ConditionPattern, BinaryOpPattern() {
    override val tree = lhsSlot lt rhsSlot

    override fun makeInstance(fill: SlotFill, destinationLabel: BlockLabel, jumpIf: Boolean): List<Instruction> {
        TODO("Not yet implemented")
    }
}

object LessEqualConditionPattern : ConditionPattern, BinaryOpPattern() {
    override val tree = lhsSlot leq rhsSlot

    override fun makeInstance(fill: SlotFill, destinationLabel: BlockLabel, jumpIf: Boolean): List<Instruction> {
        TODO("Not yet implemented")
    }
}

object GreaterConditionPattern : ConditionPattern, BinaryOpPattern() {
    override val tree = lhsSlot gt rhsSlot

    override fun makeInstance(fill: SlotFill, destinationLabel: BlockLabel, jumpIf: Boolean): List<Instruction> {
        TODO("Not yet implemented")
    }
}

object GreaterEqualConditionPattern : ConditionPattern, BinaryOpPattern() {
    override val tree = lhsSlot geq rhsSlot

    override fun makeInstance(fill: SlotFill, destinationLabel: BlockLabel, jumpIf: Boolean): List<Instruction> {
        TODO("Not yet implemented")
    }
}

object LogicalNotConditionPattern : ConditionPattern, UnaryOpPattern() {
    override val tree = not(childSlot)

    override fun makeInstance(fill: SlotFill, destinationLabel: BlockLabel, jumpIf: Boolean): List<Instruction> {
        TODO("Not yet implemented")
    }
}
