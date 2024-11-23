package cacophony.codegen.patterns.cacophonyPatterns

import cacophony.codegen.instructions.Instruction
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

    override fun makeInstance(fill: SlotFill, destination: Register): List<Instruction> {
        TODO("Not yet implemented")
    }
}

object AdditionPattern : ValuePattern, BinaryOpPattern() {
    override val tree = lhsSlot add rhsSlot

    override fun makeInstance(fill: SlotFill, destination: Register): List<Instruction> {
        TODO("Not yet implemented")
    }
}

object SubtractionPattern : ValuePattern, BinaryOpPattern() {
    override val tree = lhsSlot sub rhsSlot

    override fun makeInstance(fill: SlotFill, destination: Register): List<Instruction> {
        TODO("Not yet implemented")
    }
}

object MultiplicationPattern : ValuePattern, BinaryOpPattern() {
    override val tree = lhsSlot mul rhsSlot

    override fun makeInstance(fill: SlotFill, destination: Register): List<Instruction> {
        TODO("Not yet implemented")
    }
}

object DivisionPattern : ValuePattern, BinaryOpPattern() {
    override val tree = lhsSlot div rhsSlot

    override fun makeInstance(fill: SlotFill, destination: Register): List<Instruction> {
        TODO("Not yet implemented")
    }
}

object ModuloPattern : ValuePattern, BinaryOpPattern() {
    override val tree = lhsSlot mod rhsSlot

    override fun makeInstance(fill: SlotFill, destination: Register): List<Instruction> {
        TODO("Not yet implemented")
    }
}

object MinusPattern : ValuePattern, UnaryOpPattern() {
    override val tree = minus(childSlot)

    override fun makeInstance(fill: SlotFill, destination: Register): List<Instruction> {
        TODO("Not yet implemented")
    }
}

object EqualsValuePattern : ValuePattern, BinaryOpPattern() {
    override val tree = lhsSlot eq rhsSlot

    override fun makeInstance(fill: SlotFill, destination: Register): List<Instruction> {
        TODO("Not yet implemented")
    }
}

object NotEqualsValuePattern : ValuePattern, BinaryOpPattern() {
    override val tree = lhsSlot neq rhsSlot

    override fun makeInstance(fill: SlotFill, destination: Register): List<Instruction> {
        TODO("Not yet implemented")
    }
}

object LessValuePattern : ValuePattern, BinaryOpPattern() {
    override val tree = lhsSlot lt rhsSlot

    override fun makeInstance(fill: SlotFill, destination: Register): List<Instruction> {
        TODO("Not yet implemented")
    }
}

object LessEqualValuePattern : ValuePattern, BinaryOpPattern() {
    override val tree = lhsSlot leq rhsSlot

    override fun makeInstance(fill: SlotFill, destination: Register): List<Instruction> {
        TODO("Not yet implemented")
    }
}

object GreaterValuePattern : ValuePattern, BinaryOpPattern() {
    override val tree = lhsSlot gt rhsSlot

    override fun makeInstance(fill: SlotFill, destination: Register): List<Instruction> {
        TODO("Not yet implemented")
    }
}

object GreaterEqualValuePattern : ValuePattern, BinaryOpPattern() {
    override val tree = lhsSlot geq rhsSlot

    override fun makeInstance(fill: SlotFill, destination: Register): List<Instruction> {
        TODO("Not yet implemented")
    }
}

object LogicalNotValuePattern : ValuePattern, UnaryOpPattern() {
    override val tree = not(childSlot)

    override fun makeInstance(fill: SlotFill, destination: Register): List<Instruction> {
        TODO("Not yet implemented")
    }
}
