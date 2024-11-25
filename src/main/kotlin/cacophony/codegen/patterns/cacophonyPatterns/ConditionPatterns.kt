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
import cacophony.controlflow.not

/**
 * A collection of patterns that can be used to generate value-producing instructions.
 *
 * LogicalAnd, LogicalOr are not included here because cfg does not generate them in condition mode.
 */
val conditionPatterns =
    listOf(
        EqualsConditionPattern,
        NotEqualsConditionPattern,
        LessConditionPattern,
        LessEqualConditionPattern,
        GreaterConditionPattern,
        GreaterEqualConditionPattern,
        LogicalNotConditionPattern,
        // negated versions
        NegatedEqualsConditionPattern,
        NegatedNotEqualsConditionPattern,
        NegatedLessConditionPattern,
        NegatedLessEqualConditionPattern,
        NegatedGreaterConditionPattern,
        NegatedGreaterEqualConditionPattern,
        NegatedLogicalNotConditionPattern,
    )

object EqualsConditionPattern : ConditionPattern, BinaryOpPattern() {
    override val tree = lhsSlot eq rhsSlot

    override fun makeInstance(fill: SlotFill, destinationLabel: BlockLabel, jumpIf: Boolean) =
        if (jumpIf) equalsInstance(fill, destinationLabel)
        else notEqualsInstance(fill, destinationLabel)
}

object NotEqualsConditionPattern : ConditionPattern, BinaryOpPattern() {
    override val tree = lhsSlot neq rhsSlot

    override fun makeInstance(fill: SlotFill, destinationLabel: BlockLabel, jumpIf: Boolean) =
        if (jumpIf) notEqualsInstance(fill, destinationLabel)
        else equalsInstance(fill, destinationLabel)
}

object LessConditionPattern : ConditionPattern, BinaryOpPattern() {
    override val tree = lhsSlot lt rhsSlot

    override fun makeInstance(fill: SlotFill, destinationLabel: BlockLabel, jumpIf: Boolean) =
        if (jumpIf) lessInstance(fill, destinationLabel)
        else greaterEqualInstance(fill, destinationLabel)
}

object LessEqualConditionPattern : ConditionPattern, BinaryOpPattern() {
    override val tree = lhsSlot leq rhsSlot

    override fun makeInstance(fill: SlotFill, destinationLabel: BlockLabel, jumpIf: Boolean) =
        if (jumpIf) lessEqualInstance(fill, destinationLabel)
        else greaterInstance(fill, destinationLabel)
}

object GreaterConditionPattern : ConditionPattern, BinaryOpPattern() {
    override val tree = lhsSlot gt rhsSlot

    override fun makeInstance(fill: SlotFill, destinationLabel: BlockLabel, jumpIf: Boolean) =
        if (jumpIf) greaterInstance(fill, destinationLabel)
        else lessEqualInstance(fill, destinationLabel)
}

object GreaterEqualConditionPattern : ConditionPattern, BinaryOpPattern() {
    override val tree = lhsSlot geq rhsSlot

    override fun makeInstance(fill: SlotFill, destinationLabel: BlockLabel, jumpIf: Boolean) =
        if (jumpIf) greaterEqualInstance(fill, destinationLabel)
        else lessInstance(fill, destinationLabel)
}

object LogicalNotConditionPattern : ConditionPattern, UnaryOpPattern() {
    override val tree = not(childSlot)

    override fun makeInstance(fill: SlotFill, destinationLabel: BlockLabel, jumpIf: Boolean) =
        instructions(fill) {
            test(reg(lhsLabel), reg(lhsLabel))
            if (jumpIf) jz(destinationLabel)
            else jnz(destinationLabel)
        }
}

object NegatedEqualsConditionPattern : ConditionPattern, BinaryOpPattern() {
    override val tree = not(lhsSlot eq rhsSlot)

    override fun makeInstance(fill: SlotFill, destinationLabel: BlockLabel, jumpIf: Boolean) =
        if (jumpIf) notEqualsInstance(fill, destinationLabel)
        else equalsInstance(fill, destinationLabel)
}

object NegatedNotEqualsConditionPattern : ConditionPattern, BinaryOpPattern() {
    override val tree = not(lhsSlot neq rhsSlot)

    override fun makeInstance(fill: SlotFill, destinationLabel: BlockLabel, jumpIf: Boolean) =
        if (jumpIf) equalsInstance(fill, destinationLabel)
        else notEqualsInstance(fill, destinationLabel)
}

object NegatedLessConditionPattern : ConditionPattern, BinaryOpPattern() {
    override val tree = not(lhsSlot lt rhsSlot)

    override fun makeInstance(fill: SlotFill, destinationLabel: BlockLabel, jumpIf: Boolean) =
        if (jumpIf) greaterEqualInstance(fill, destinationLabel)
        else lessInstance(fill, destinationLabel)
}

object NegatedLessEqualConditionPattern : ConditionPattern, BinaryOpPattern() {
    override val tree = not(lhsSlot leq rhsSlot)

    override fun makeInstance(fill: SlotFill, destinationLabel: BlockLabel, jumpIf: Boolean) =
        if (jumpIf) greaterInstance(fill, destinationLabel)
        else lessEqualInstance(fill, destinationLabel)
}

object NegatedGreaterConditionPattern : ConditionPattern, BinaryOpPattern() {
    override val tree = not(lhsSlot gt rhsSlot)

    override fun makeInstance(fill: SlotFill, destinationLabel: BlockLabel, jumpIf: Boolean) =
        if (jumpIf) lessEqualInstance(fill, destinationLabel)
        else greaterInstance(fill, destinationLabel)
}

object NegatedGreaterEqualConditionPattern : ConditionPattern, BinaryOpPattern() {
    override val tree = not(lhsSlot geq rhsSlot)

    override fun makeInstance(fill: SlotFill, destinationLabel: BlockLabel, jumpIf: Boolean) =
        if (jumpIf) lessInstance(fill, destinationLabel)
        else greaterEqualInstance(fill, destinationLabel)
}

object NegatedLogicalNotConditionPattern : ConditionPattern, UnaryOpPattern() {
    override val tree = not(not(childSlot))

    override fun makeInstance(fill: SlotFill, destinationLabel: BlockLabel, jumpIf: Boolean) =
        instructions(fill) {
            test(reg(lhsLabel), reg(lhsLabel))
            if (jumpIf) jnz(destinationLabel)
            else jz(destinationLabel)
        }
}

private fun equalsInstance(fill: SlotFill, destinationLabel: BlockLabel) =
    instructions(fill) {
        cmp(reg(lhsLabel), reg(rhsLabel))
        je(destinationLabel)
    }

private fun notEqualsInstance(fill: SlotFill, destinationLabel: BlockLabel) =
    instructions(fill) {
        cmp(reg(lhsLabel), reg(rhsLabel))
        jne(destinationLabel)
    }

private fun lessInstance(fill: SlotFill, destinationLabel: BlockLabel) =
    instructions(fill) {
        cmp(reg(lhsLabel), reg(rhsLabel))
        jl(destinationLabel)
    }

private fun lessEqualInstance(fill: SlotFill, destinationLabel: BlockLabel) =
    instructions(fill) {
        cmp(reg(lhsLabel), reg(rhsLabel))
        jle(destinationLabel)
    }

private fun greaterInstance(fill: SlotFill, destinationLabel: BlockLabel) =
    instructions(fill) {
        cmp(reg(lhsLabel), reg(rhsLabel))
        jg(destinationLabel)
    }

private fun greaterEqualInstance(fill: SlotFill, destinationLabel: BlockLabel) =
    instructions(fill) {
        cmp(reg(lhsLabel), reg(rhsLabel))
        jge(destinationLabel)
    }
