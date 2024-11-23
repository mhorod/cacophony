package cacophony.codegen.instructions

import cacophony.codegen.BlockLabel
import cacophony.codegen.instructions.matching.InstructionMatcher
import cacophony.codegen.instructions.matching.Match
import cacophony.controlflow.CFGNode
import cacophony.controlflow.Register

class CacophonyInstructionCovering(private val instructionMatcher: InstructionMatcher) : InstructionCovering {
    private fun coverGivenMatch(match: Match): List<Instruction> {
        val tempRegisters = match.toFill.keys.associateWith { Register.VirtualRegister() }

        val instructions = mutableListOf<Instruction>()
        match.toFill.forEach { (label, node) ->
            instructions += coverWithInstructionsForValue(node, tempRegisters[label]!!)
        }
        instructions += match.instructionMaker(tempRegisters)

        return instructions
    }

    private fun coverWithInstructionsForValue(node: CFGNode, register: Register): List<Instruction> {
        val matches = instructionMatcher.findMatchesForValue(node, register)
        val bestMatch = matches.maxByOrNull { match -> match.size }!!
        return coverGivenMatch(bestMatch)
    }

    override fun coverWithInstructions(node: CFGNode): List<Instruction> {
        if (node is CFGNode.Value) {
            return coverWithInstructionsForValue(node, Register.VirtualRegister())
        }
        val matches = instructionMatcher.findMatchesForSideEffects(node)
        val bestMatch = matches.maxByOrNull { match -> match.size }!!
        return coverGivenMatch(bestMatch)
    }

    override fun coverWithInstructionsAndJump(node: CFGNode, label: BlockLabel, jumpIf: Boolean): List<Instruction> {
        val matches = instructionMatcher.findMatchesForCondition(node, label, jumpIf)
        val bestMatch = matches.maxByOrNull { match -> match.size }!!
        return coverGivenMatch(bestMatch)
    }
}
