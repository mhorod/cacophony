package cacophony.codegen.patterns.cacophonyPatterns

import cacophony.codegen.instructions.Instruction
import cacophony.codegen.patterns.NoTemporaryRegistersPattern
import cacophony.codegen.patterns.SideEffectPattern
import cacophony.codegen.patterns.SlotFill
import cacophony.controlflow.*

val sideEffectPatterns =
    listOf(
        NoOpPattern,
        CommentPattern,
        // +=
        AdditionAssignmentMemoryPattern,
        ConstantAdditionAssignmentRegisterPattern,
        AdditionAssignmentRegisterPattern,
        // -=
        SubtractionAssignmentRegisterPattern,
        SubtractionAssignmentMemoryPattern,
        ConstantSubtractionAssignmentRegisterPattern,
        MultiplicationAssignmentRegisterPattern,
        MultiplicationAssignmentMemoryPattern,
        DivisionAssignmentRegisterPattern,
        DivisionAssignmentMemoryPattern,
        ModuloAssignmentRegisterPattern,
        ModuloAssignmentMemoryPattern,
        CallPattern,
        ReturnPattern,
        PushPattern,
        PushRegPattern,
        PopPattern,
        // assignments
        RegisterAssignmentPattern,
        RegisterToMemoryWithAddedDisplacementAssignmentPattern,
        RegisterToMemoryWithSubtractedDisplacementAssignmentPattern,
        RegisterToMemoryAssignmentPattern,
        MemoryAssignmentPattern,
        MemoryToRegisterAssignmentPattern,
        MemoryByRegisterWithSubtractionToRegisterAssignment,
        RegisterToMemoryByRegisterWithSubtractionAssignment,
    )

val noTemporaryRegistersPatterns = sideEffectPatterns.filterIsInstance<NoTemporaryRegistersPattern>()

object NoOpPattern : SideEffectPattern {
    override val tree = CFGNode.NoOp

    override fun makeInstance(fill: SlotFill) = emptyList<Instruction>()
}

object CommentPattern : SideEffectPattern {
    override val tree = CFGNode.NodeSlot(NodeLabel(), CFGNode.Comment::class)

    override fun makeInstance(fill: SlotFill): List<Instruction> =
        instructions(fill) {
            comment(node(tree).comment)
        }
}

object AdditionAssignmentRegisterPattern : NoTemporaryRegistersPattern, RegisterAssignmentTemplate() {
    override val tree = lhsSlot addeq rhsSlot

    override fun makeInstance(fill: SlotFill) =
        instructions(fill) {
            add(reg(lhsRegisterLabel), reg(rhsLabel))
        }
}

object AdditionAssignmentMemoryPattern : SideEffectPattern, MemoryAssignmentTemplate() {
    override val tree = lhsSlot addeq rhsSlot

    override fun makeInstance(fill: SlotFill) =
        instructions(fill) {
            val temporaryRegister = Register.VirtualRegister()
            mov(temporaryRegister, mem(reg(lhsLabel)))
            add(temporaryRegister, reg(rhsLabel))
            mov(mem(reg(lhsLabel)), temporaryRegister)
        }
}

object ConstantAdditionAssignmentRegisterPattern : NoTemporaryRegistersPattern {
    private val lhsLabel = RegisterLabel()
    private val rhsLabel = ConstantLabel()
    private val lhsSlot = CFGNode.RegisterSlot(lhsLabel)
    private val rhsSlot = CFGNode.ConstantSlot(rhsLabel) { true }
    override val tree = lhsSlot addeq rhsSlot

    override fun makeInstance(fill: SlotFill) =
        instructions(fill) {
            add(reg(lhsLabel), const(rhsLabel))
        }

    override fun priority(): Int = 10
}

object SubtractionAssignmentRegisterPattern : NoTemporaryRegistersPattern, RegisterAssignmentTemplate() {
    override val tree = lhsSlot subeq rhsSlot

    override fun makeInstance(fill: SlotFill) =
        instructions(fill) {
            sub(reg(lhsRegisterLabel), reg(rhsLabel))
        }
}

object SubtractionAssignmentMemoryPattern : SideEffectPattern, MemoryAssignmentTemplate() {
    override val tree = lhsSlot subeq rhsSlot

    override fun makeInstance(fill: SlotFill) =
        instructions(fill) {
            val temporaryRegister = Register.VirtualRegister()
            mov(temporaryRegister, mem(reg(lhsLabel)))
            sub(temporaryRegister, reg(rhsLabel))
            mov(mem(reg(lhsLabel)), temporaryRegister)
        }
}

object ConstantSubtractionAssignmentRegisterPattern : NoTemporaryRegistersPattern {
    private val lhsLabel = RegisterLabel()
    private val rhsLabel = ConstantLabel()
    private val lhsSlot = CFGNode.RegisterSlot(lhsLabel)
    private val rhsSlot = CFGNode.ConstantSlot(rhsLabel) { true }
    override val tree = lhsSlot subeq rhsSlot

    override fun makeInstance(fill: SlotFill) =
        instructions(fill) {
            sub(reg(lhsLabel), const(rhsLabel))
        }

    override fun priority(): Int = 10
}

object MultiplicationAssignmentRegisterPattern : NoTemporaryRegistersPattern, RegisterAssignmentTemplate() {
    override val tree = lhsSlot muleq rhsSlot

    override fun makeInstance(fill: SlotFill) =
        instructions(fill) {
            imul(reg(lhsRegisterLabel), reg(rhsLabel))
        }
}

object MultiplicationAssignmentMemoryPattern : SideEffectPattern, MemoryAssignmentTemplate() {
    override val tree = lhsSlot muleq rhsSlot

    override fun makeInstance(fill: SlotFill) =
        instructions(fill) {
            val temporaryRegister = Register.VirtualRegister()
            mov(temporaryRegister, mem(reg(lhsLabel)))
            imul(temporaryRegister, reg(rhsLabel))
            mov(mem(reg(lhsLabel)), temporaryRegister)
        }
}

object DivisionAssignmentRegisterPattern : NoTemporaryRegistersPattern, RegisterAssignmentTemplate() {
    override val tree = lhsSlot diveq rhsSlot

    override fun makeInstance(fill: SlotFill) =
        instructions(fill) {
            mov(rax, reg(lhsRegisterLabel))
            cqo()
            idiv(reg(rhsLabel))
            mov(reg(lhsRegisterLabel), rax)
        }
}

object DivisionAssignmentMemoryPattern : NoTemporaryRegistersPattern, MemoryAssignmentTemplate() {
    override val tree = lhsSlot diveq rhsSlot

    override fun makeInstance(fill: SlotFill) =
        instructions(fill) {
            mov(rax, mem(reg(lhsLabel)))
            cqo()
            idiv(reg(rhsLabel))
            mov(mem(reg(lhsLabel)), rax)
        }
}

object ModuloAssignmentRegisterPattern : NoTemporaryRegistersPattern, RegisterAssignmentTemplate() {
    override val tree = lhsSlot modeq rhsSlot

    override fun makeInstance(fill: SlotFill) =
        instructions(fill) {
            mov(rax, reg(lhsRegisterLabel))
            cqo()
            idiv(reg(rhsLabel))
            mov(reg(lhsRegisterLabel), rdx)
        }
}

object ModuloAssignmentMemoryPattern : NoTemporaryRegistersPattern, MemoryAssignmentTemplate() {
    override val tree = lhsSlot modeq rhsSlot

    override fun makeInstance(fill: SlotFill) =
        instructions(fill) {
            mov(rax, mem(reg(lhsLabel)))
            cqo()
            idiv(reg(rhsLabel))
            mov(mem(reg(lhsLabel)), rdx)
        }
}

object CallPattern : NoTemporaryRegistersPattern {
    private val functionLabel = FunctionLabel()

    override val tree = CFGNode.Call(CFGNode.FunctionSlot(functionLabel))

    override fun makeInstance(fill: SlotFill) =
        instructions(fill) {
            call(functionLabel)
        }
}

object ReturnPattern : NoTemporaryRegistersPattern {
    override val tree = CFGNode.Return

    override fun makeInstance(fill: SlotFill) =
        instructions(fill) {
            ret()
        }
}

object PushPattern : SideEffectPattern {
    val childLabel = ValueLabel()

    override val tree = CFGNode.Push(CFGNode.ValueSlot(childLabel))

    override fun makeInstance(fill: SlotFill) =
        instructions(fill) {
            push(reg(childLabel))
        }
}

object PushRegPattern : NoTemporaryRegistersPattern {
    private val childLabel = RegisterLabel()

    override val tree = CFGNode.Push(CFGNode.RegisterSlot(childLabel))

    override fun makeInstance(fill: SlotFill) =
        instructions(fill) {
            push(reg(childLabel))
        }

    override fun priority(): Int = 10
}

object PopPattern : NoTemporaryRegistersPattern {
    val regLabel = RegisterLabel()

    override val tree = CFGNode.Pop(CFGNode.RegisterSlot(regLabel))

    override fun makeInstance(fill: SlotFill) =
        instructions(fill) {
            pop(reg(regLabel))
        }
}

object RegisterAssignmentPattern : NoTemporaryRegistersPattern, RegisterAssignmentTemplate() {
    override val tree = lhsSlot assign rhsSlot

    override fun makeInstance(fill: SlotFill) =
        instructions(fill) {
            mov(reg(lhsRegisterLabel), reg(rhsLabel))
        }
}

object MemoryAssignmentPattern : SideEffectPattern, MemoryAssignmentTemplate() {
    override val tree = lhsSlot assign rhsSlot

    override fun makeInstance(fill: SlotFill) =
        instructions(fill) {
            val temporaryRegister = Register.VirtualRegister()
            mov(temporaryRegister, reg(rhsLabel))
            mov(mem(reg(lhsLabel)), temporaryRegister)
        }
}

object RegisterToMemoryAssignmentPattern : SideEffectPattern {
    private val lhsLabel = ValueLabel()
    private val rhsLabel = RegisterLabel()
    private val lhsSlot = CFGNode.MemoryAccess(CFGNode.ValueSlot(lhsLabel))
    private val rhsSlot = CFGNode.RegisterSlot(rhsLabel)
    override val tree = lhsSlot assign rhsSlot

    override fun makeInstance(fill: SlotFill) =
        instructions(fill) {
            mov(mem(reg(lhsLabel)), reg(rhsLabel))
        }
}

object MemoryToRegisterAssignmentPattern : SideEffectPattern {
    private val lhsLabel = RegisterLabel()
    private val rhsLabel = ValueLabel()
    private val lhsSlot = CFGNode.RegisterSlot(lhsLabel)
    private val rhsSlot = CFGNode.MemoryAccess(CFGNode.ValueSlot(rhsLabel))

    override val tree = lhsSlot assign rhsSlot

    override fun makeInstance(fill: SlotFill): List<Instruction> =
        instructions(fill) {
            mov(reg(lhsLabel), mem(reg(rhsLabel)))
        }
}

object RegisterToMemoryWithAddedDisplacementAssignmentPattern : SideEffectPattern {
    private val lhsLabel = ValueLabel()
    private val displacementLabel = ConstantLabel()
    private val rhsLabel = RegisterLabel()

    private val lhsSlot =
        memoryAccess(
            CFGNode.ValueSlot(lhsLabel)
                add
                CFGNode.ConstantSlot(displacementLabel, { it in listOf(1, 2, 4, 8) }),
        )
    private val rhsSlot = CFGNode.RegisterSlot(rhsLabel)
    override val tree = lhsSlot assign rhsSlot

    override fun makeInstance(fill: SlotFill) =
        instructions(fill) {
            mov(memWithDisplacement(reg(lhsLabel), const(displacementLabel)), reg(rhsLabel))
        }
}

object RegisterToMemoryWithSubtractedDisplacementAssignmentPattern : SideEffectPattern {
    private val lhsLabel = ValueLabel()
    private val displacementLabel = ConstantLabel()
    private val rhsLabel = RegisterLabel()

    private val lhsSlot =
        memoryAccess(
            CFGNode.ValueSlot(lhsLabel)
                sub
                CFGNode.ConstantSlot(displacementLabel) { it in listOf(1, 2, 4, 8) },
        )
    private val rhsSlot = CFGNode.RegisterSlot(rhsLabel)
    override val tree = lhsSlot assign rhsSlot

    override fun makeInstance(fill: SlotFill) =
        instructions(fill) {
            mov(memWithDisplacement(reg(lhsLabel), -const(displacementLabel)), reg(rhsLabel))
        }
}

object MemoryByRegisterWithSubtractionToRegisterAssignment : NoTemporaryRegistersPattern {
    private val lhsRegister = RegisterLabel()
    private val displacementLabel = ConstantLabel()
    private val rhsRegister = RegisterLabel()

    private val lhsSlot = CFGNode.RegisterSlot(lhsRegister)
    private val rhsSlot =
        memoryAccess(
            CFGNode.RegisterSlot(rhsRegister)
                sub
                CFGNode.ConstantSlot(displacementLabel) { true },
        )
    override val tree = lhsSlot assign rhsSlot

    override fun makeInstance(fill: SlotFill) =
        instructions(fill) {
            mov(reg(lhsRegister), memWithDisplacement(reg(rhsRegister), -const(displacementLabel)))
        }
}

object RegisterToMemoryByRegisterWithSubtractionAssignment : NoTemporaryRegistersPattern {
    private val lhsRegister = RegisterLabel()
    private val displacementLabel = ConstantLabel()
    private val rhsRegister = RegisterLabel()

    private val lhsSlot =
        memoryAccess(
            CFGNode.RegisterSlot(lhsRegister)
                sub
                CFGNode.ConstantSlot(displacementLabel) { true },
        )
    private val rhsSlot = CFGNode.RegisterSlot(rhsRegister)

    override val tree = lhsSlot assign rhsSlot

    override fun makeInstance(fill: SlotFill) =
        instructions(fill) {
            mov(memWithDisplacement(reg(lhsRegister), -const(displacementLabel) ), reg(rhsRegister))
        }
}
