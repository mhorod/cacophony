package cacophony.codegen.patterns.cacophonyPatterns

import cacophony.codegen.instructions.Instruction
import cacophony.codegen.instructions.RegisterByte
import cacophony.codegen.patterns.SlotFill
import cacophony.codegen.patterns.ValuePattern
import cacophony.codegen.patterns.cacophonyPatterns.LessEqualValuePattern.lhsLabel
import cacophony.codegen.patterns.cacophonyPatterns.LessEqualValuePattern.rhsLabel
import cacophony.controlflow.*

/**
 * A collection of patterns that can be used to generate value-producing instructions.
 *
 * LogicalAnd and LogicalOr are not included here because they are short-circuiting and cfg handles them differently.
 */
val valuePatterns =
    listOf(
        ConstantPattern,
        // arithmetic
        AdditionPattern,
        SubtractionPattern,
        MultiplicationPattern,
        DivisionPattern,
        ModuloPattern,
        MinusPattern,
        // logical
        EqualsValuePattern,
        NotEqualsValuePattern,
        LessValuePattern,
        LessEqualValuePattern,
        GreaterValuePattern,
        GreaterEqualValuePattern,
        LogicalNotValuePattern,
    )

// for now, we can always dump a constant into a register and call it a value
object ConstantPattern : ValuePattern {
    val label = ConstantLabel()
    val predicate: (Int) -> Boolean = { _ -> true }

    override val tree = CFGNode.ConstantSlot(label, predicate)

    override fun makeInstance(fill: SlotFill, destination: Register): List<Instruction> =
        instructions(fill) {
            mov(destination, const(label))
        }
}

object AdditionPattern : ValuePattern, BinaryOpPattern() {
    override val tree = lhsSlot add rhsSlot

    override fun makeInstance(fill: SlotFill, destination: Register) =
        instructions(fill) {
            mov(destination, reg(lhsLabel))
            add(destination, reg(rhsLabel))
        }
}

object SubtractionPattern : ValuePattern, BinaryOpPattern() {
    override val tree = lhsSlot sub rhsSlot

    override fun makeInstance(fill: SlotFill, destination: Register) =
        instructions(fill) {
            mov(destination, reg(lhsLabel))
            sub(destination, reg(rhsLabel))
        }
}

object MultiplicationPattern : ValuePattern, BinaryOpPattern() {
    override val tree = lhsSlot mul rhsSlot

    override fun makeInstance(fill: SlotFill, destination: Register) =
        instructions(fill) {
            mov(destination, reg(lhsLabel))
            imul(destination, reg(rhsLabel))
        }
}

object DivisionPattern : ValuePattern, BinaryOpPattern() {
    override val tree = lhsSlot div rhsSlot

    override fun makeInstance(fill: SlotFill, destination: Register) =
        instructions(fill) {
            mov(rax, reg(lhsLabel))
            cqo()
            idiv(reg(rhsLabel))
            mov(destination, rax)
        }
}

object ModuloPattern : ValuePattern, BinaryOpPattern() {
    override val tree = lhsSlot mod rhsSlot

    override fun makeInstance(fill: SlotFill, destination: Register) =
        instructions(fill) {
            mov(rax, reg(lhsLabel))
            cqo()
            idiv(reg(rhsLabel))
            mov(destination, rdx)
        }
}

object MinusPattern : ValuePattern, UnaryOpPattern() {
    override val tree = minus(childSlot)

    override fun makeInstance(fill: SlotFill, destination: Register) =
        instructions(fill) {
            xor(destination, destination)
            sub(destination, reg(childLabel))
        }
}

object EqualsValuePattern : ValuePattern, BinaryOpPattern() {
    override val tree = lhsSlot eq rhsSlot

    override fun makeInstance(fill: SlotFill, destination: Register) =
        makeBinaryLogicalOperatorInstance(
            fill,
            destination,
            InstructionBuilder::sete,
        )
}

object NotEqualsValuePattern : ValuePattern, BinaryOpPattern() {
    override val tree = lhsSlot neq rhsSlot

    override fun makeInstance(fill: SlotFill, destination: Register) =
        makeBinaryLogicalOperatorInstance(
            fill,
            destination,
            InstructionBuilder::setne,
        )
}

object LessValuePattern : ValuePattern, BinaryOpPattern() {
    override val tree = lhsSlot lt rhsSlot

    override fun makeInstance(fill: SlotFill, destination: Register) =
        makeBinaryLogicalOperatorInstance(
            fill,
            destination,
            InstructionBuilder::setl,
        )
}

object LessEqualValuePattern : ValuePattern, BinaryOpPattern() {
    override val tree = lhsSlot leq rhsSlot

    override fun makeInstance(fill: SlotFill, destination: Register) =
        makeBinaryLogicalOperatorInstance(
            fill,
            destination,
            InstructionBuilder::setle,
        )
}

object GreaterValuePattern : ValuePattern, BinaryOpPattern() {
    override val tree = lhsSlot gt rhsSlot

    override fun makeInstance(fill: SlotFill, destination: Register) =
        makeBinaryLogicalOperatorInstance(
            fill,
            destination,
            InstructionBuilder::setg,
        )
}

object GreaterEqualValuePattern : ValuePattern, BinaryOpPattern() {
    override val tree = lhsSlot geq rhsSlot

    override fun makeInstance(fill: SlotFill, destination: Register) =
        makeBinaryLogicalOperatorInstance(
            fill,
            destination,
            InstructionBuilder::setge,
        )
}

object LogicalNotValuePattern : ValuePattern, UnaryOpPattern() {
    override val tree = not(childSlot)

    override fun makeInstance(fill: SlotFill, destination: Register) =
        instructions(fill) {
            mov(destination, reg(childLabel))
            xor(destination, 1)
        }
}

private fun makeBinaryLogicalOperatorInstance(fill: SlotFill, destination: Register, setcc: InstructionBuilder.(RegisterByte) -> Unit) =
    instructions(fill) {
        cmp(reg(lhsLabel), reg(rhsLabel))
        setcc(byte(destination))
        movzx(destination, byte(destination))
    }
