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

/**
 * A collection of patterns that can be used to generate value-producing instructions.
 *
 * LogicalAnd, LogicalOr, and LogicalNot are not included here because cfg does not generate them in condition mode.
 */
val conditionPatterns: List<ConditionPattern> =
    listOf(
        EqualsConditionPattern,
        NotEqualsConditionPattern,
        LessConditionPattern,
        LessEqualConditionPattern,
        GreaterConditionPattern,
        GreaterEqualConditionPattern,
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
