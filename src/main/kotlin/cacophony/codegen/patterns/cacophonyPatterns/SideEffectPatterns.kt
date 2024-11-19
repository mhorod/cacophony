package cacophony.codegen.patterns.cacophonyPatterns

import cacophony.codegen.instructions.Instruction
import cacophony.codegen.patterns.SideEffectPattern
import cacophony.codegen.patterns.SlotFill
import cacophony.controlflow.CFGNode
import cacophony.controlflow.RegisterLabel
import cacophony.controlflow.ValueLabel

class SideEffectPatterns {
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
}
