package cacophony.codegen.patterns

import cacophony.codegen.instructions.Instruction
import cacophony.controlflow.CFGNode
import cacophony.controlflow.Register
import cacophony.controlflow.RegisterLabel
import cacophony.semantic.syntaxtree.Definition

class CacophonyPattern {
    abstract class BinaryOpPattern {
        val lhsRegisterLabel = RegisterLabel()
        val rhsRegisterLabel = RegisterLabel()
    }

    abstract class UnaryOpPattern {
        val registerLabel = RegisterLabel()
    }

    /*****************************
     * CHAPTER 1. VALUE PATTERNS *
     *****************************/

    object AdditionPattern : ValuePattern, BinaryOpPattern() {
        override val tree =
            CFGNode.Addition(
                CFGNode.RegisterSlot(lhsRegisterLabel),
                CFGNode.RegisterSlot(
                    rhsRegisterLabel,
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
                CFGNode.RegisterSlot(lhsRegisterLabel),
                CFGNode.RegisterSlot(
                    rhsRegisterLabel,
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
                CFGNode.RegisterSlot(lhsRegisterLabel),
                CFGNode.RegisterSlot(
                    rhsRegisterLabel,
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
                CFGNode.RegisterSlot(lhsRegisterLabel),
                CFGNode.RegisterSlot(
                    rhsRegisterLabel,
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
                CFGNode.RegisterSlot(lhsRegisterLabel),
                CFGNode.RegisterSlot(
                    rhsRegisterLabel,
                ),
            )

        override fun makeInstance(
            fill: SlotFill,
            destination: Register,
        ): List<Instruction> {
            TODO("Not yet implemented")
        }
    }

    object Equals : ValuePattern, BinaryOpPattern() {
        override val tree =
            CFGNode.Equals(
                CFGNode.RegisterSlot(lhsRegisterLabel),
                CFGNode.RegisterSlot(
                    rhsRegisterLabel,
                ),
            )

        override fun makeInstance(
            fill: SlotFill,
            destination: Register,
        ): List<Instruction> {
            TODO("Not yet implemented")
        }
    }

    object NotEquals : ValuePattern, BinaryOpPattern() {
        override val tree =
            CFGNode.NotEquals(
                CFGNode.RegisterSlot(lhsRegisterLabel),
                CFGNode.RegisterSlot(
                    rhsRegisterLabel,
                ),
            )

        override fun makeInstance(
            fill: SlotFill,
            destination: Register,
        ): List<Instruction> {
            TODO("Not yet implemented")
        }
    }

    object Less : ValuePattern, BinaryOpPattern() {
        override val tree =
            CFGNode.Less(
                CFGNode.RegisterSlot(lhsRegisterLabel),
                CFGNode.RegisterSlot(
                    rhsRegisterLabel,
                ),
            )

        override fun makeInstance(
            fill: SlotFill,
            destination: Register,
        ): List<Instruction> {
            TODO("Not yet implemented")
        }
    }

    object Greater : ValuePattern, BinaryOpPattern() {
        override val tree =
            CFGNode.Greater(
                CFGNode.RegisterSlot(lhsRegisterLabel),
                CFGNode.RegisterSlot(
                    rhsRegisterLabel,
                ),
            )

        override fun makeInstance(
            fill: SlotFill,
            destination: Register,
        ): List<Instruction> {
            TODO("Not yet implemented")
        }
    }

    object LessEqual : ValuePattern, BinaryOpPattern() {
        override val tree =
            CFGNode.LessEqual(
                CFGNode.RegisterSlot(lhsRegisterLabel),
                CFGNode.RegisterSlot(
                    rhsRegisterLabel,
                ),
            )

        override fun makeInstance(
            fill: SlotFill,
            destination: Register,
        ): List<Instruction> {
            TODO("Not yet implemented")
        }
    }

    object GreaterEqual : ValuePattern, BinaryOpPattern() {
        override val tree =
            CFGNode.GreaterEqual(
                CFGNode.RegisterSlot(lhsRegisterLabel),
                CFGNode.RegisterSlot(
                    rhsRegisterLabel,
                ),
            )

        override fun makeInstance(
            fill: SlotFill,
            destination: Register,
        ): List<Instruction> {
            TODO("Not yet implemented")
        }
    }

    object MinusPattern: ValuePattern, UnaryOpPattern() {
        override val tree = CFGNode.Minus(CFGNode.RegisterSlot(registerLabel))
        override fun makeInstance(
            fill: SlotFill,
            destination: Register,
        ): List<Instruction> {
            TODO("Not yet implemented")
        }
    }

    object LogicalNotPattern: ValuePattern, UnaryOpPattern() {
        override val tree = CFGNode.LogicalNot(CFGNode.RegisterSlot(registerLabel))
        override fun makeInstance(
            fill: SlotFill,
            destination: Register,
        ): List<Instruction> {
            TODO("Not yet implemented")
        }
    }

    /**********************************
     * CHAPTER 2. SIDEEFFECT PATTERNS *
     **********************************/

    object AdditionAssignmentPattern: SideEffectPattern, BinaryOpPattern() {
        override val tree =
            CFGNode.AdditionAssignment(
                CFGNode.RegisterSlot(lhsRegisterLabel),
                CFGNode.RegisterSlot(
                    rhsRegisterLabel,
                ),
            )

        override fun makeInstance(
            fill: SlotFill,
        ): List<Instruction> {
            TODO("Not yet implemented")
        }
    }

    object SubtractionAssignmentPattern: SideEffectPattern, BinaryOpPattern() {
        override val tree =
            CFGNode.SubtractionAssignment(
                CFGNode.RegisterSlot(lhsRegisterLabel),
                CFGNode.RegisterSlot(
                    rhsRegisterLabel,
                ),
            )

        override fun makeInstance(
            fill: SlotFill,
        ): List<Instruction> {
            TODO("Not yet implemented")
        }
    }

    object MultiplicationAssignmentPattern: SideEffectPattern, BinaryOpPattern() {
        override val tree =
            CFGNode.MultiplicationAssignment(
                CFGNode.RegisterSlot(lhsRegisterLabel),
                CFGNode.RegisterSlot(
                    rhsRegisterLabel,
                ),
            )

        override fun makeInstance(
            fill: SlotFill,
        ): List<Instruction> {
            TODO("Not yet implemented")
        }
    }

    object DivisionAssignmentPattern: SideEffectPattern, BinaryOpPattern() {
        override val tree =
            CFGNode.DivisionAssignment(
                CFGNode.RegisterSlot(lhsRegisterLabel),
                CFGNode.RegisterSlot(
                    rhsRegisterLabel,
                ),
            )

        override fun makeInstance(
            fill: SlotFill,
        ): List<Instruction> {
            TODO("Not yet implemented")
        }
    }

    object ModuloAssignmentPattern: SideEffectPattern, BinaryOpPattern() {
        override val tree =
            CFGNode.ModuloAssignment(
                CFGNode.RegisterSlot(lhsRegisterLabel),
                CFGNode.RegisterSlot(
                    rhsRegisterLabel,
                ),
            )

        override fun makeInstance(
            fill: SlotFill,
        ): List<Instruction> {
            TODO("Not yet implemented")
        }
    }

    object Return: SideEffectPattern {
        override val tree = CFGNode.Return

        override fun makeInstance(fill: SlotFill): List<Instruction> {
            TODO("Not yet implemented")
        }
    }

    // TODO
//    object Call: SideEffectPattern {
//        override val tree = CFGNode.Return
//
//        val functionDeclaration: Definition.FunctionDeclaration =
//        override fun makeInstance(fill: SlotFill): List<Instruction> {
//            TODO("Not yet implemented")
//        }
//    }

    object Push: SideEffectPattern {
        override val tree = CFGNode.Push(CFGNode.RegisterSlot(registerLabel))

        override fun makeInstance(fill: SlotFill): List<Instruction> {
            TODO("Not yet implemented")
        }
    }
}
