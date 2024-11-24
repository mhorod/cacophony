package cacophony.codegen.patterns.cacophonyPatterns

import cacophony.codegen.BlockLabel
import cacophony.codegen.patterns.ConditionPattern
import cacophony.codegen.patterns.SlotFill
import cacophony.codegen.patterns.cacophonyPatterns.LessEqualValuePattern.lhsLabel
import cacophony.codegen.patterns.cacophonyPatterns.LessEqualValuePattern.rhsLabel
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
val conditionPatterns =
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

    override fun makeInstance(fill: SlotFill, destinationLabel: BlockLabel, jumpIf: Boolean) =
        if (jumpIf) {
            makeBinaryLogicalOperatorInstance(fill, destinationLabel, InstructionBuilder::je)
        } else {
            makeBinaryLogicalOperatorInstance(fill, destinationLabel, InstructionBuilder::jne)
        }
}

object NotEqualsConditionPattern : ConditionPattern, BinaryOpPattern() {
    override val tree = lhsSlot neq rhsSlot

    override fun makeInstance(fill: SlotFill, destinationLabel: BlockLabel, jumpIf: Boolean) =
        if (jumpIf) {
            makeBinaryLogicalOperatorInstance(fill, destinationLabel, InstructionBuilder::jne)
        } else {
            makeBinaryLogicalOperatorInstance(fill, destinationLabel, InstructionBuilder::je)
        }
}

object LessConditionPattern : ConditionPattern, BinaryOpPattern() {
    override val tree = lhsSlot lt rhsSlot

    override fun makeInstance(fill: SlotFill, destinationLabel: BlockLabel, jumpIf: Boolean) =
        if (jumpIf) {
            makeBinaryLogicalOperatorInstance(fill, destinationLabel, InstructionBuilder::jl)
        } else {
            makeBinaryLogicalOperatorInstance(fill, destinationLabel, InstructionBuilder::jge)
        }
}

object LessEqualConditionPattern : ConditionPattern, BinaryOpPattern() {
    override val tree = lhsSlot leq rhsSlot

    override fun makeInstance(fill: SlotFill, destinationLabel: BlockLabel, jumpIf: Boolean) =
        if (jumpIf) {
            makeBinaryLogicalOperatorInstance(fill, destinationLabel, InstructionBuilder::jle)
        } else {
            makeBinaryLogicalOperatorInstance(fill, destinationLabel, InstructionBuilder::jg)
        }
}

object GreaterConditionPattern : ConditionPattern, BinaryOpPattern() {
    override val tree = lhsSlot gt rhsSlot

    override fun makeInstance(fill: SlotFill, destinationLabel: BlockLabel, jumpIf: Boolean) =
        if (jumpIf) {
            makeBinaryLogicalOperatorInstance(fill, destinationLabel, InstructionBuilder::jg)
        } else {
            makeBinaryLogicalOperatorInstance(fill, destinationLabel, InstructionBuilder::jle)
        }
}

object GreaterEqualConditionPattern : ConditionPattern, BinaryOpPattern() {
    override val tree = lhsSlot geq rhsSlot

    override fun makeInstance(fill: SlotFill, destinationLabel: BlockLabel, jumpIf: Boolean) =
        if (jumpIf) {
            makeBinaryLogicalOperatorInstance(fill, destinationLabel, InstructionBuilder::jge)
        } else {
            makeBinaryLogicalOperatorInstance(fill, destinationLabel, InstructionBuilder::jl)
        }
}

private fun makeBinaryLogicalOperatorInstance(fill: SlotFill, destinationLabel: BlockLabel, jcc: InstructionBuilder.(BlockLabel) -> Unit) =
    instructions(fill) {
        cmp(reg(lhsLabel), reg(rhsLabel))
        jcc(destinationLabel)
    }
