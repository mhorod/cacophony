package cacophony.codegen.patterns.cacophonyPatterns

import cacophony.codegen.instructions.Instruction
import cacophony.codegen.patterns.SlotFill
import cacophony.codegen.patterns.ValuePattern
import cacophony.controlflow.*

class ValuePatterns {
    abstract class BinaryOpPattern {
        val lhsLabel = ValueLabel()
        val rhsLabel = ValueLabel()
    }

    abstract class UnaryOpPattern {
        val childLabel = ValueLabel()
    }

    // TODO: reduce boilerplate (typed DSL?)

    // for now, we can always dump a constant into a register and call it a value
    object ConstantPattern : ValuePattern {
        val label = ConstantLabel()
        val predicate: (Int) -> Boolean = { _ -> true }

        override val tree = CFGNode.ConstantSlot(label, predicate)

        override fun makeInstance(
            fill: SlotFill,
            destination: Register,
        ): List<Instruction> {
            TODO("Not yet implemented")
        }
    }

    object AdditionPattern : ValuePattern, BinaryOpPattern() {
        override val tree =
            CFGNode.Addition(
                CFGNode.ValueSlot(lhsLabel),
                CFGNode.ValueSlot(
                    rhsLabel,
                ),
            )

        override fun makeInstance(
            fill: SlotFill,
            destination: Register,
        ): List<Instruction> {
            TODO("Not yet implemented")
        }
    }

    object SubtractionPattern : ValuePattern, BinaryOpPattern() {
        override val tree =
            CFGNode.Subtraction(
                CFGNode.ValueSlot(lhsLabel),
                CFGNode.ValueSlot(
                    rhsLabel,
                ),
            )

        override fun makeInstance(
            fill: SlotFill,
            destination: Register,
        ): List<Instruction> {
            TODO("Not yet implemented")
        }
    }

    object MultiplicationPattern : ValuePattern, BinaryOpPattern() {
        override val tree =
            CFGNode.Multiplication(
                CFGNode.ValueSlot(lhsLabel),
                CFGNode.ValueSlot(
                    rhsLabel,
                ),
            )

        override fun makeInstance(
            fill: SlotFill,
            destination: Register,
        ): List<Instruction> {
            TODO("Not yet implemented")
        }
    }

    object DivisionPattern : ValuePattern, BinaryOpPattern() {
        override val tree =
            CFGNode.Division(
                CFGNode.ValueSlot(lhsLabel),
                CFGNode.ValueSlot(
                    rhsLabel,
                ),
            )

        override fun makeInstance(
            fill: SlotFill,
            destination: Register,
        ): List<Instruction> {
            TODO("Not yet implemented")
        }
    }

    object ModuloPattern : ValuePattern, BinaryOpPattern() {
        override val tree =
            CFGNode.Modulo(
                CFGNode.ValueSlot(lhsLabel),
                CFGNode.ValueSlot(
                    rhsLabel,
                ),
            )

        override fun makeInstance(
            fill: SlotFill,
            destination: Register,
        ): List<Instruction> {
            TODO("Not yet implemented")
        }
    }

    object EqualsPattern : ValuePattern, BinaryOpPattern() {
        override val tree =
            CFGNode.Equals(
                CFGNode.ValueSlot(lhsLabel),
                CFGNode.ValueSlot(
                    rhsLabel,
                ),
            )

        override fun makeInstance(
            fill: SlotFill,
            destination: Register,
        ): List<Instruction> {
            TODO("Not yet implemented")
        }
    }

    object NotEqualsPattern : ValuePattern, BinaryOpPattern() {
        override val tree =
            CFGNode.NotEquals(
                CFGNode.ValueSlot(lhsLabel),
                CFGNode.ValueSlot(
                    rhsLabel,
                ),
            )

        override fun makeInstance(
            fill: SlotFill,
            destination: Register,
        ): List<Instruction> {
            TODO("Not yet implemented")
        }
    }

    object LessPattern : ValuePattern, BinaryOpPattern() {
        override val tree =
            CFGNode.Less(
                CFGNode.ValueSlot(lhsLabel),
                CFGNode.ValueSlot(
                    rhsLabel,
                ),
            )

        override fun makeInstance(
            fill: SlotFill,
            destination: Register,
        ): List<Instruction> {
            TODO("Not yet implemented")
        }
    }

    object GreaterPattern : ValuePattern, BinaryOpPattern() {
        override val tree =
            CFGNode.Greater(
                CFGNode.ValueSlot(lhsLabel),
                CFGNode.ValueSlot(
                    rhsLabel,
                ),
            )

        override fun makeInstance(
            fill: SlotFill,
            destination: Register,
        ): List<Instruction> {
            TODO("Not yet implemented")
        }
    }

    object LessEqualPattern : ValuePattern, BinaryOpPattern() {
        override val tree =
            CFGNode.LessEqual(
                CFGNode.ValueSlot(lhsLabel),
                CFGNode.ValueSlot(
                    rhsLabel,
                ),
            )

        override fun makeInstance(
            fill: SlotFill,
            destination: Register,
        ): List<Instruction> {
            TODO("Not yet implemented")
        }
    }

    object GreaterEqualPattern : ValuePattern, BinaryOpPattern() {
        override val tree =
            CFGNode.GreaterEqual(
                CFGNode.ValueSlot(lhsLabel),
                CFGNode.ValueSlot(
                    rhsLabel,
                ),
            )

        override fun makeInstance(
            fill: SlotFill,
            destination: Register,
        ): List<Instruction> {
            TODO("Not yet implemented")
        }
    }

    object MinusPattern : ValuePattern, UnaryOpPattern() {
        override val tree = CFGNode.Minus(CFGNode.ValueSlot(childLabel))

        override fun makeInstance(
            fill: SlotFill,
            destination: Register,
        ): List<Instruction> {
            TODO("Not yet implemented")
        }
    }

    object LogicalNotPattern : ValuePattern, UnaryOpPattern() {
        override val tree = CFGNode.LogicalNot(CFGNode.ValueSlot(childLabel))

        override fun makeInstance(
            fill: SlotFill,
            destination: Register,
        ): List<Instruction> {
            TODO("Not yet implemented")
        }
    }

    /*********************************
     * CHAPTER 3. CONDITION PATTERNS *
     *********************************/
}
