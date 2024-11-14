package cacophony.codegen.patterns

import cacophony.codegen.instructions.Instruction
import cacophony.controlflow.CFGNode.*
import cacophony.controlflow.Register

class CacophonyPatterns {
    object AdditionPattern : ValuePattern(Addition(ValueSlot(), ValueSlot())) {
        override fun makeInstance(fill: SlotFill, destination: Register): List<Instruction> {
            return listOf()
        }
    }
}
