package cacophony.codegen.patterns

import cacophony.codegen.instructions.Instruction
import cacophony.controlflow.CFGNode
import cacophony.controlflow.Register
import cacophony.controlflow.RegisterLabel

class CacophonyPattern {
    abstract class BinaryOpPattern {
        val lhsRegisterLabel = RegisterLabel()
        val rhsRegisterLabel = RegisterLabel()
    }

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
}
