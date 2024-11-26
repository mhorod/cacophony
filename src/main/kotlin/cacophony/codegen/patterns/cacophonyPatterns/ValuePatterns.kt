package cacophony.codegen.patterns.cacophonyPatterns

import cacophony.codegen.instructions.Instruction
import cacophony.codegen.instructions.RegisterByte
import cacophony.codegen.patterns.SlotFill
import cacophony.codegen.patterns.ValuePattern
import cacophony.controlflow.*

/**
 * A collection of patterns that can be used to generate value-producing instructions.
 *
 * LogicalAnd and LogicalOr are not included here because they are short-circuiting and cfg handles them differently.
 */
val valuePatterns =
    listOf(
        ConstantPattern,
        RegisterUsePattern,
        // arithmetic
        AdditionPattern,
        ConstantSubtractionPattern,
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
        // assignment
        AdditionAssignmentRegisterValuePattern,
        SubtractionAssignmentRegisterValuePattern,
        MultiplicationAssignmentRegisterValuePattern,
        DivisionAssignmentRegisterValuePattern,
        ModuloAssignmentRegisterValuePattern,
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

object RegisterUsePattern : ValuePattern {
    private val label = RegisterLabel()
    override val tree = CFGNode.RegisterSlot(label)

    override fun makeInstance(fill: SlotFill, destination: Register): List<Instruction> =
        instructions(fill) {
            mov(destination, reg(label))
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

object ConstantSubtractionPattern : ValuePattern {
    private val lhsLabel = ValueLabel()
    private val rhsLabel = ConstantLabel()
    private val lhsSlot = CFGNode.ValueSlot(lhsLabel)
    private val rhsSlot = CFGNode.ConstantSlot(rhsLabel, { true })
    override val tree = lhsSlot sub rhsSlot

    override fun makeInstance(fill: SlotFill, destination: Register) =
        instructions(fill) {
            mov(destination, reg(lhsLabel))
            sub(destination, const(rhsLabel))
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

abstract class BinaryLogicalOperatorValuePattern : BinaryOpPattern(), ValuePattern {
    protected fun makeInstance(fill: SlotFill, destination: Register, setcc: InstructionBuilder.(RegisterByte) -> Unit) =
        instructions(fill) {
            cmp(reg(lhsLabel), reg(rhsLabel))
            setcc(byte(destination))
            movzx(destination, byte(destination))
        }
}

object EqualsValuePattern : BinaryLogicalOperatorValuePattern() {
    override val tree = lhsSlot eq rhsSlot

    override fun makeInstance(fill: SlotFill, destination: Register) = makeInstance(fill, destination, InstructionBuilder::sete)
}

object NotEqualsValuePattern : BinaryLogicalOperatorValuePattern() {
    override val tree = lhsSlot neq rhsSlot

    override fun makeInstance(fill: SlotFill, destination: Register) = makeInstance(fill, destination, InstructionBuilder::setne)
}

object LessValuePattern : BinaryLogicalOperatorValuePattern() {
    override val tree = lhsSlot lt rhsSlot

    override fun makeInstance(fill: SlotFill, destination: Register) = makeInstance(fill, destination, InstructionBuilder::setl)
}

object LessEqualValuePattern : BinaryLogicalOperatorValuePattern() {
    override val tree = lhsSlot leq rhsSlot

    override fun makeInstance(fill: SlotFill, destination: Register) = makeInstance(fill, destination, InstructionBuilder::setle)
}

object GreaterValuePattern : BinaryLogicalOperatorValuePattern() {
    override val tree = lhsSlot gt rhsSlot

    override fun makeInstance(fill: SlotFill, destination: Register) = makeInstance(fill, destination, InstructionBuilder::setg)
}

object GreaterEqualValuePattern : BinaryLogicalOperatorValuePattern() {
    override val tree = lhsSlot geq rhsSlot

    override fun makeInstance(fill: SlotFill, destination: Register) = makeInstance(fill, destination, InstructionBuilder::setge)
}

object LogicalNotValuePattern : ValuePattern, UnaryOpPattern() {
    override val tree = not(childSlot)

    override fun makeInstance(fill: SlotFill, destination: Register) =
        instructions(fill) {
            mov(destination, reg(childLabel))
            xor(destination, 1)
        }
}

object AdditionAssignmentRegisterValuePattern : ValuePattern, RegisterAssignmentTemplate() {
    override val tree = lhsSlot addeq rhsSlot

    override fun makeInstance(fill: SlotFill, destination: Register) =
        instructions(fill) {
            add(reg(lhsRegisterLabel), reg(rhsLabel))
            mov(destination, reg(lhsRegisterLabel))
        }
}

object SubtractionAssignmentRegisterValuePattern : ValuePattern, RegisterAssignmentTemplate() {
    override val tree = lhsSlot subeq rhsSlot

    override fun makeInstance(fill: SlotFill, destination: Register) =
        instructions(fill) {
            sub(reg(lhsRegisterLabel), reg(rhsLabel))
            mov(destination, reg(lhsRegisterLabel))
        }
}

object MultiplicationAssignmentRegisterValuePattern : ValuePattern, RegisterAssignmentTemplate() {
    override val tree = lhsSlot muleq rhsSlot

    override fun makeInstance(fill: SlotFill, destination: Register) =
        instructions(fill) {
            imul(reg(lhsRegisterLabel), reg(rhsLabel))
            mov(destination, reg(lhsRegisterLabel))
        }
}

object DivisionAssignmentRegisterValuePattern : ValuePattern, RegisterAssignmentTemplate() {
    override val tree = lhsSlot diveq rhsSlot

    override fun makeInstance(fill: SlotFill, destination: Register) =
        instructions(fill) {
            mov(rax, reg(lhsRegisterLabel))
            cqo()
            idiv(reg(rhsLabel))
            mov(destination, rax)
        }
}

object ModuloAssignmentRegisterValuePattern : ValuePattern, RegisterAssignmentTemplate() {
    override val tree = lhsSlot modeq rhsSlot

    override fun makeInstance(fill: SlotFill, destination: Register) =
        instructions(fill) {
            mov(rax, reg(lhsRegisterLabel))
            cqo()
            idiv(reg(rhsLabel))
            mov(destination, rdx)
        }
}
