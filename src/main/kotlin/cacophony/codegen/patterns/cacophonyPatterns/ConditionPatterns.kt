package cacophony.codegen.patterns.cacophonyPatterns

import cacophony.codegen.BlockLabel
import cacophony.codegen.patterns.ConditionPattern
import cacophony.codegen.patterns.SlotFill
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
val conditionPatterns: List<ConditionPattern> =
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

abstract class BinaryConditionPattern : ConditionPattern, BinaryOpPattern() {
    protected fun equalsInstance(fill: SlotFill, destinationLabel: BlockLabel) =
        instructions(fill) {
            cmp(reg(lhsLabel), reg(rhsLabel))
            je(destinationLabel)
        }

    protected fun notEqualsInstance(fill: SlotFill, destinationLabel: BlockLabel) =
        instructions(fill) {
            cmp(reg(lhsLabel), reg(rhsLabel))
            jne(destinationLabel)
        }

    protected fun lessInstance(fill: SlotFill, destinationLabel: BlockLabel) =
        instructions(fill) {
            cmp(reg(lhsLabel), reg(rhsLabel))
            jl(destinationLabel)
        }

    protected fun lessEqualInstance(fill: SlotFill, destinationLabel: BlockLabel) =
        instructions(fill) {
            cmp(reg(lhsLabel), reg(rhsLabel))
            jle(destinationLabel)
        }

    protected fun greaterInstance(fill: SlotFill, destinationLabel: BlockLabel) =
        instructions(fill) {
            cmp(reg(lhsLabel), reg(rhsLabel))
            jg(destinationLabel)
        }

    protected fun greaterEqualInstance(fill: SlotFill, destinationLabel: BlockLabel) =
        instructions(fill) {
            cmp(reg(lhsLabel), reg(rhsLabel))
            jge(destinationLabel)
        }
}

object EqualsConditionPattern : BinaryConditionPattern() {
    override val tree = lhsSlot eq rhsSlot

    override fun makeInstance(fill: SlotFill, destinationLabel: BlockLabel, jumpIf: Boolean) =
        if (jumpIf) equalsInstance(fill, destinationLabel)
        else notEqualsInstance(fill, destinationLabel)
}

object NotEqualsConditionPattern : BinaryConditionPattern() {
    override val tree = lhsSlot neq rhsSlot

    override fun makeInstance(fill: SlotFill, destinationLabel: BlockLabel, jumpIf: Boolean) =
        if (jumpIf) notEqualsInstance(fill, destinationLabel)
        else equalsInstance(fill, destinationLabel)
}

object LessConditionPattern : BinaryConditionPattern() {
    override val tree = lhsSlot lt rhsSlot

    override fun makeInstance(fill: SlotFill, destinationLabel: BlockLabel, jumpIf: Boolean) =
        if (jumpIf) lessInstance(fill, destinationLabel)
        else greaterEqualInstance(fill, destinationLabel)
}

object LessEqualConditionPattern : BinaryConditionPattern() {
    override val tree = lhsSlot leq rhsSlot

    override fun makeInstance(fill: SlotFill, destinationLabel: BlockLabel, jumpIf: Boolean) =
        if (jumpIf) lessEqualInstance(fill, destinationLabel)
        else greaterInstance(fill, destinationLabel)
}

object GreaterConditionPattern : BinaryConditionPattern() {
    override val tree = lhsSlot gt rhsSlot

    override fun makeInstance(fill: SlotFill, destinationLabel: BlockLabel, jumpIf: Boolean) =
        if (jumpIf) greaterInstance(fill, destinationLabel)
        else lessEqualInstance(fill, destinationLabel)
}

object GreaterEqualConditionPattern : BinaryConditionPattern() {
    override val tree = lhsSlot geq rhsSlot

    override fun makeInstance(fill: SlotFill, destinationLabel: BlockLabel, jumpIf: Boolean) =
        if (jumpIf) greaterEqualInstance(fill, destinationLabel)
        else lessInstance(fill, destinationLabel)
}

object LogicalNotConditionPattern : UnaryOpPattern(), ConditionPattern {
    override val tree = not(childSlot)

    override fun makeInstance(fill: SlotFill, destinationLabel: BlockLabel, jumpIf: Boolean) =
        instructions(fill) {
            test(reg(childLabel), reg(childLabel))
            if (jumpIf) jz(destinationLabel)
            else jnz(destinationLabel)
        }
}

object NegatedEqualsConditionPattern : BinaryConditionPattern() {
    override val tree = not(lhsSlot eq rhsSlot)

    override fun makeInstance(fill: SlotFill, destinationLabel: BlockLabel, jumpIf: Boolean) =
        if (jumpIf) notEqualsInstance(fill, destinationLabel)
        else equalsInstance(fill, destinationLabel)
}

object NegatedNotEqualsConditionPattern : BinaryConditionPattern() {
    override val tree = not(lhsSlot neq rhsSlot)

    override fun makeInstance(fill: SlotFill, destinationLabel: BlockLabel, jumpIf: Boolean) =
        if (jumpIf) equalsInstance(fill, destinationLabel)
        else notEqualsInstance(fill, destinationLabel)
}

object NegatedLessConditionPattern : BinaryConditionPattern() {
    override val tree = not(lhsSlot lt rhsSlot)

    override fun makeInstance(fill: SlotFill, destinationLabel: BlockLabel, jumpIf: Boolean) =
        if (jumpIf) greaterEqualInstance(fill, destinationLabel)
        else lessInstance(fill, destinationLabel)
}

object NegatedLessEqualConditionPattern : BinaryConditionPattern() {
    override val tree = not(lhsSlot leq rhsSlot)

    override fun makeInstance(fill: SlotFill, destinationLabel: BlockLabel, jumpIf: Boolean) =
        if (jumpIf) greaterInstance(fill, destinationLabel)
        else lessEqualInstance(fill, destinationLabel)
}

object NegatedGreaterConditionPattern : BinaryConditionPattern() {
    override val tree = not(lhsSlot gt rhsSlot)

    override fun makeInstance(fill: SlotFill, destinationLabel: BlockLabel, jumpIf: Boolean) =
        if (jumpIf) lessEqualInstance(fill, destinationLabel)
        else greaterInstance(fill, destinationLabel)
}

object NegatedGreaterEqualConditionPattern : BinaryConditionPattern() {
    override val tree = not(lhsSlot geq rhsSlot)

    override fun makeInstance(fill: SlotFill, destinationLabel: BlockLabel, jumpIf: Boolean) =
        if (jumpIf) lessInstance(fill, destinationLabel)
        else greaterEqualInstance(fill, destinationLabel)
}

object NegatedLogicalNotConditionPattern : ConditionPattern, UnaryOpPattern() {
    override val tree = not(not(childSlot))

    override fun makeInstance(fill: SlotFill, destinationLabel: BlockLabel, jumpIf: Boolean) =
        instructions(fill) {
            test(reg(childLabel), reg(childLabel))
            if (jumpIf) jnz(destinationLabel)
            else jz(destinationLabel)
        }
}
