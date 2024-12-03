package cacophony.codegen.instructions

import cacophony.codegen.BlockLabel
import cacophony.codegen.instructions.matching.InstructionMatcher
import cacophony.codegen.instructions.matching.Match
import cacophony.controlflow.CFGNode
import cacophony.controlflow.Register

class CacophonyInstructionCovering(private val instructionMatcher: InstructionMatcher) : InstructionCovering {
    private fun coverGivenMatch(match: Match): List<Instruction> {
        val tempRegisters = match.toFill.mapValues { Register.VirtualRegister() }

        val instructions =
            match.toFill
                .map { (label, node) ->
                    coverWithInstructionsForValue(node, tempRegisters[label]!!)
                }.flatten()
                .plus(match.instructionMaker(tempRegisters))

        return instructions
    }

    private fun coverWithInstructionsForValue(node: CFGNode, register: Register): List<Instruction> {
        val matches = instructionMatcher.findMatchesForValue(node, register)
        val bestMatch =
            matches.maxByOrNull { match -> match.size } ?: error("No match with instructions for value found for $node, ${node.javaClass}")
        return coverGivenMatch(bestMatch)
    }

    override fun coverWithInstructions(node: CFGNode): List<Instruction> {
        val matches =
            instructionMatcher
                .findMatchesForSideEffects(node)
                .union(instructionMatcher.findMatchesForValue(node, Register.VirtualRegister()))
        val bestMatch = matches.maxByOrNull { match -> match.size } ?: error("No match found for $node, ${node.javaClass}")
        return coverGivenMatch(bestMatch)
    }

    override fun coverWithInstructionsWithoutVirtualRegisters(node: CFGNode): List<Instruction> {
        val matches = instructionMatcher.findMatchesForSideEffects(node)
        val bestMatch =
            matches
                .filter { it.toFill.isEmpty() }
                .maxByOrNull { match -> match.size } ?: error("No match without virtual registers found for $node, ${node.javaClass}")
        return coverGivenMatch(bestMatch)
    }

    override fun coverWithInstructionsAndJump(node: CFGNode, label: BlockLabel, jumpIf: Boolean): List<Instruction> {
        val matches = instructionMatcher.findMatchesForCondition(node, label, jumpIf)
        val bestMatch = matches.maxByOrNull { match -> match.size } ?: error("No match found for $node, ${node.javaClass}")
        return coverGivenMatch(bestMatch)
    }
}
