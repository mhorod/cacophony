package cacophony.codegen.patterns.cacophonyPatterns

import cacophony.codegen.instructions.Instruction
import cacophony.codegen.patterns.SideEffectPattern
import cacophony.codegen.patterns.SlotFill
import cacophony.controlflow.*

class SideEffectPatterns {
    abstract class RegisterAssignmentTemplate {
        val lhsRegisterLabel = RegisterLabel()
        val rhsLabel = ValueLabel()
        val lhsSlot = CFGNode.RegisterSlot(lhsRegisterLabel)
        val rhsSlot = CFGNode.ValueSlot(rhsLabel)
    }

    abstract class MemoryAssignmentTemplate {
        val lhsLabel = ValueLabel()
        val rhsLabel = ValueLabel()
        val lhsSlot = CFGNode.MemoryAccess(CFGNode.ValueSlot(lhsLabel))
        val rhsSlot = CFGNode.ValueSlot(rhsLabel)
    }

    object AdditionAssignmentRegisterPattern : SideEffectPattern, RegisterAssignmentTemplate() {
        override val tree = lhsSlot addeq rhsSlot

        override fun makeInstance(fill: SlotFill): List<Instruction> {
            TODO("Not yet implemented")
        }
    }

    object AdditionAssignmentMemoryPattern : SideEffectPattern, MemoryAssignmentTemplate() {
        override val tree = lhsSlot addeq rhsSlot

        override fun makeInstance(fill: SlotFill): List<Instruction> {
            TODO("Not yet implemented")
        }
    }

    object SubtractionAssignmentRegisterPattern : SideEffectPattern, RegisterAssignmentTemplate() {
        override val tree = lhsSlot subeq rhsSlot

        override fun makeInstance(fill: SlotFill): List<Instruction> {
            TODO("Not yet implemented")
        }
    }

    object SubtractionAssignmentMemoryPattern : SideEffectPattern, MemoryAssignmentTemplate() {
        override val tree = lhsSlot subeq rhsSlot

        override fun makeInstance(fill: SlotFill): List<Instruction> {
            TODO("Not yet implemented")
        }
    }

    object MultiplicationAssignmentRegisterPattern : SideEffectPattern, RegisterAssignmentTemplate() {
        override val tree = lhsSlot subeq rhsSlot

        override fun makeInstance(fill: SlotFill): List<Instruction> {
            TODO("Not yet implemented")
        }
    }

    object MultiplicationAssignmentMemoryPattern : SideEffectPattern, MemoryAssignmentTemplate() {
        override val tree = lhsSlot muleq rhsSlot

        override fun makeInstance(fill: SlotFill): List<Instruction> {
            TODO("Not yet implemented")
        }
    }

    object DivisionAssignmentRegisterPattern : SideEffectPattern, RegisterAssignmentTemplate() {
        override val tree = lhsSlot diveq rhsSlot

        override fun makeInstance(fill: SlotFill): List<Instruction> {
            TODO("Not yet implemented")
        }
    }

    object DivisionAssignmentMemoryPattern : SideEffectPattern, MemoryAssignmentTemplate() {
        override val tree = lhsSlot diveq rhsSlot

        override fun makeInstance(fill: SlotFill): List<Instruction> {
            TODO("Not yet implemented")
        }
    }

    object ModuloAssignmentRegisterPattern : SideEffectPattern, RegisterAssignmentTemplate() {
        override val tree = lhsSlot modeq rhsSlot

        override fun makeInstance(fill: SlotFill): List<Instruction> {
            TODO("Not yet implemented")
        }
    }

    object ModuloAssignmentMemoryPattern : SideEffectPattern, MemoryAssignmentTemplate() {
        override val tree = lhsSlot modeq rhsSlot

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
        override val tree = lhsSlot assign rhsSlot

        override fun makeInstance(fill: SlotFill): List<Instruction> {
            TODO("Not yet implemented")
        }
    }

    object MemoryAssignmentPattern : SideEffectPattern, MemoryAssignmentTemplate() {
        override val tree = lhsSlot assign rhsSlot

        override fun makeInstance(fill: SlotFill): List<Instruction> {
            TODO("Not yet implemented")
        }
    }
}
