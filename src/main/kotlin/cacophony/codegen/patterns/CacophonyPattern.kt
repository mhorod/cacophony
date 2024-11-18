package cacophony.codegen.patterns

import cacophony.codegen.instructions.Instruction
import cacophony.controlflow.*

class CacophonyPattern {
    /*****************************
     * CHAPTER 1. VALUE PATTERNS *
     *****************************/

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
        val predicate: (Int) -> Boolean = { _ -> false }

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

    /***********************************
     * CHAPTER 2. SIDE EFFECT PATTERNS *
     ***********************************/

    abstract class RegisterAssignmentTemplate {
        val lhsRegisterLabel = RegisterLabel()
        val rhsLabel = ValueLabel()
    }

    abstract class MemoryAssignmentTemplate {
        val lhsLabel = ValueLabel()
        val rhsLabel = ValueLabel()
    }

    object AdditionAssignmentRegisterPattern : SideEffectPattern, RegisterAssignmentTemplate() {
        override val tree =
            CFGNode.AdditionAssignment(
                CFGNode.RegisterSlot(lhsRegisterLabel),
                CFGNode.ValueSlot(rhsLabel),
            )

        override fun makeInstance(fill: SlotFill): List<Instruction> {
            TODO("Not yet implemented")
        }
    }

    object AdditionAssignmentMemoryPattern : SideEffectPattern, MemoryAssignmentTemplate() {
        override val tree =
            CFGNode.AdditionAssignment(
                CFGNode.MemoryAccess(CFGNode.ValueSlot(lhsLabel)),
                CFGNode.ValueSlot(
                    rhsLabel,
                ),
            )

        override fun makeInstance(fill: SlotFill): List<Instruction> {
            TODO("Not yet implemented")
        }
    }

    object SubtractionAssignmentRegisterPattern : SideEffectPattern, RegisterAssignmentTemplate() {
        override val tree =
            CFGNode.SubtractionAssignment(
                CFGNode.RegisterSlot(lhsRegisterLabel),
                CFGNode.ValueSlot(rhsLabel),
            )

        override fun makeInstance(fill: SlotFill): List<Instruction> {
            TODO("Not yet implemented")
        }
    }

    object SubtractionAssignmentMemoryPattern : SideEffectPattern, MemoryAssignmentTemplate() {
        override val tree =
            CFGNode.SubtractionAssignment(
                CFGNode.MemoryAccess(CFGNode.ValueSlot(lhsLabel)),
                CFGNode.ValueSlot(
                    rhsLabel,
                ),
            )

        override fun makeInstance(fill: SlotFill): List<Instruction> {
            TODO("Not yet implemented")
        }
    }

    object MultiplicationAssignmentRegisterPattern : SideEffectPattern, RegisterAssignmentTemplate() {
        override val tree =
            CFGNode.MultiplicationAssignment(
                CFGNode.RegisterSlot(lhsRegisterLabel),
                CFGNode.ValueSlot(rhsLabel),
            )

        override fun makeInstance(fill: SlotFill): List<Instruction> {
            TODO("Not yet implemented")
        }
    }

    object MultiplicationAssignmentMemoryPattern : SideEffectPattern, MemoryAssignmentTemplate() {
        override val tree =
            CFGNode.MultiplicationAssignment(
                CFGNode.MemoryAccess(CFGNode.ValueSlot(lhsLabel)),
                CFGNode.ValueSlot(
                    rhsLabel,
                ),
            )

        override fun makeInstance(fill: SlotFill): List<Instruction> {
            TODO("Not yet implemented")
        }
    }

    object DivisionAssignmentRegisterPattern : SideEffectPattern, RegisterAssignmentTemplate() {
        override val tree =
            CFGNode.DivisionAssignment(
                CFGNode.RegisterSlot(lhsRegisterLabel),
                CFGNode.ValueSlot(rhsLabel),
            )

        override fun makeInstance(fill: SlotFill): List<Instruction> {
            TODO("Not yet implemented")
        }
    }

    object DivisionAssignmentMemoryPattern : SideEffectPattern, MemoryAssignmentTemplate() {
        override val tree =
            CFGNode.DivisionAssignment(
                CFGNode.MemoryAccess(CFGNode.ValueSlot(lhsLabel)),
                CFGNode.ValueSlot(
                    rhsLabel,
                ),
            )

        override fun makeInstance(fill: SlotFill): List<Instruction> {
            TODO("Not yet implemented")
        }
    }

    object ModuloAssignmentMemoryPattern : SideEffectPattern, MemoryAssignmentTemplate() {
        override val tree =
            CFGNode.ModuloAssignment(
                CFGNode.MemoryAccess(CFGNode.ValueSlot(lhsLabel)),
                CFGNode.ValueSlot(
                    rhsLabel,
                ),
            )

        override fun makeInstance(fill: SlotFill): List<Instruction> {
            TODO("Not yet implemented")
        }
    }

    object ModuloAssignmentRegisterPattern : SideEffectPattern, RegisterAssignmentTemplate() {
        override val tree =
            CFGNode.ModuloAssignment(
                CFGNode.RegisterSlot(lhsRegisterLabel),
                CFGNode.ValueSlot(rhsLabel),
            )

        override fun makeInstance(fill: SlotFill): List<Instruction> {
            TODO("Not yet implemented")
        }
    }

    object ReturnPattern : SideEffectPattern {
        override val tree = CFGNode.Return

        override fun makeInstance(fill: SlotFill): List<Instruction> {
            TODO("Not yet implemented")
        }
    }

    object PushPattern : SideEffectPattern {
        val childLabel = ValueLabel()

        override val tree = CFGNode.Push(CFGNode.ValueSlot(childLabel))

        override fun makeInstance(fill: SlotFill): List<Instruction> {
            TODO("Not yet implemented")
        }
    }

    object PopPattern : SideEffectPattern {
        val regLabel = RegisterLabel()

        override val tree = CFGNode.Pop(CFGNode.RegisterSlot(regLabel))
//        override val tree = CFGNode.Pop()

        override fun makeInstance(fill: SlotFill): List<Instruction> {
            TODO("Not yet implemented")
        }
    }

    object RegisterAssignmentPattern : SideEffectPattern, RegisterAssignmentTemplate() {
        override val tree =
            CFGNode.Assignment(
                CFGNode.RegisterSlot(lhsRegisterLabel),
                CFGNode.ValueSlot(rhsLabel),
            )

        override fun makeInstance(fill: SlotFill): List<Instruction> {
            TODO("Not yet implemented")
        }
    }

    object MemoryAssignmentPattern : SideEffectPattern, MemoryAssignmentTemplate() {
        override val tree = CFGNode.Assignment(CFGNode.MemoryAccess(CFGNode.ValueSlot(lhsLabel)), CFGNode.ValueSlot(rhsLabel))

        override fun makeInstance(fill: SlotFill): List<Instruction> {
            TODO("Not yet implemented")
        }
    }

    /*********************************
     * CHAPTER 3. CONDITION PATTERNS *
     *********************************/
}
