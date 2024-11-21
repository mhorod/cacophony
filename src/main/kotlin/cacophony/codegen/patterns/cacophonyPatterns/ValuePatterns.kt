package cacophony.codegen.patterns.cacophonyPatterns

import cacophony.codegen.instructions.Instruction
import cacophony.codegen.patterns.SlotFill
import cacophony.codegen.patterns.ValuePattern
import cacophony.controlflow.*

class ValuePatterns {
    abstract class BinaryOpPattern {
        val lhsLabel = ValueLabel()
        val rhsLabel = ValueLabel()
        val lhsSlot = CFGNode.ValueSlot(lhsLabel)
        val rhsSlot = CFGNode.ValueSlot(rhsLabel)
    }

    abstract class UnaryOpPattern {
        val childLabel = ValueLabel()
    }

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

    object EqualsPattern : ValuePattern, BinaryOpPattern() {
        override val tree = lhsSlot eq rhsSlot

        override fun makeInstance(fill: SlotFill, destination: Register): List<Instruction> {
            TODO("Not yet implemented")
        }
    }

    object NotEqualsPattern : ValuePattern, BinaryOpPattern() {
        override val tree = lhsSlot neq rhsSlot

        override fun makeInstance(fill: SlotFill, destination: Register): List<Instruction> {
            TODO("Not yet implemented")
        }
    }

    object LessPattern : ValuePattern, BinaryOpPattern() {
        override val tree = lhsSlot lt rhsSlot

        override fun makeInstance(fill: SlotFill, destination: Register): List<Instruction> {
            TODO("Not yet implemented")
        }
    }

    object GreaterPattern : ValuePattern, BinaryOpPattern() {
        override val tree = lhsSlot gt rhsSlot

        override fun makeInstance(fill: SlotFill, destination: Register): List<Instruction> {
            TODO("Not yet implemented")
        }
    }

    object LessEqualPattern : ValuePattern, BinaryOpPattern() {
        override val tree = lhsSlot leq rhsSlot

        override fun makeInstance(fill: SlotFill, destination: Register): List<Instruction> {
            TODO("Not yet implemented")
        }
    }

    object GreaterEqualPattern : ValuePattern, BinaryOpPattern() {
        override val tree = lhsSlot geq rhsSlot

        override fun makeInstance(fill: SlotFill, destination: Register): List<Instruction> {
            TODO("Not yet implemented")
        }
    }

    object MinusPattern : ValuePattern, UnaryOpPattern() {
        override val tree = CFGNode.Minus(CFGNode.ValueSlot(childLabel))

        override fun makeInstance(fill: SlotFill, destination: Register): List<Instruction> {
            TODO("Not yet implemented")
        }
    }

    object LogicalNotPattern : ValuePattern, UnaryOpPattern() {
        override val tree = CFGNode.LogicalNot(CFGNode.ValueSlot(childLabel))

        override fun makeInstance(fill: SlotFill, destination: Register): List<Instruction> {
            TODO("Not yet implemented")
        }
    }
}
