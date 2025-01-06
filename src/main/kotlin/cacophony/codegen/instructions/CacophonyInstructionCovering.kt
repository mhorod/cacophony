package cacophony.codegen.instructions

import cacophony.codegen.BlockLabel
import cacophony.codegen.instructions.matching.InstructionMatcher
import cacophony.codegen.instructions.matching.Match
import cacophony.codegen.patterns.ValuePattern
import cacophony.controlflow.CFGNode
import cacophony.controlflow.Register
import cacophony.controlflow.generation.CFGReferenceAnalysis
import cacophony.controlflow.generation.analyzeCFGReferences
import cacophony.grammars.produces

class CacophonyInstructionCovering(private val instructionMatcher: InstructionMatcher) : InstructionCovering {
    private fun coverGivenMatch(match: Match, referenceAnalysis: CFGReferenceAnalysis): List<Instruction> {
        val tempRegisters = match.toFill.mapValues { Register.VirtualRegister(referenceAnalysis[it.value]!!) }

        val instructions =
            match.toFill
                .map { (label, node) ->
                    coverWithInstructionsForValue(node, referenceAnalysis, tempRegisters[label]!!)
                }.flatten()
                .plus(match.instructionMaker(tempRegisters))

        return instructions
    }

    // Pair<Int, Int> does not implement Comparable
    private fun Pair<Int, Int>.toLong(): Long = first * (1L shl 32) + second

    private fun coverGivenMatches(node: CFGNode, matches: Set<Match>, referenceAnalysis: CFGReferenceAnalysis): List<Instruction> {
        val bestMatch =
            matches.maxByOrNull { match ->
                Pair(match.size, match.pattern.priority()).toLong()
            } ?: error("No match found for $node, ${node.javaClass}")
        return coverGivenMatch(bestMatch, referenceAnalysis)
    }

    private fun coverWithInstructionsForValue(node: CFGNode, referenceAnalysis: CFGReferenceAnalysis, register: Register.VirtualRegister): List<Instruction> {
        val matches = instructionMatcher.findMatchesForValue(node, register)
        return coverGivenMatches(node, matches, referenceAnalysis)
    }

    override fun coverWithInstructions(node: CFGNode): List<Instruction> {
        val referenceAnalysis = analyzeCFGReferences(node)
        val holdsReference = referenceAnalysis[node]!!
        val matches =
            instructionMatcher
                .findMatchesForSideEffects(node)
                .union(instructionMatcher.findMatchesForValue(node, Register.VirtualRegister(holdsReference)))
        return coverGivenMatches(node, matches, referenceAnalysis)
    }

    override fun coverWithInstructionsWithoutTemporaryRegisters(node: CFGNode): List<Instruction> {
        val referenceAnalysis = analyzeCFGReferences(node)
        val matches = instructionMatcher.findMatchesWithoutTemporaryRegisters(node)
        val bestMatch =
            matches.maxByOrNull { match -> match.size } ?: error("No match without temporary registers found for $node, ${node.javaClass}")
        return coverGivenMatch(bestMatch, referenceAnalysis)
    }

    override fun coverWithInstructionsAndJump(node: CFGNode, label: BlockLabel, jumpIf: Boolean): List<Instruction> {
        val referenceAnalysis = analyzeCFGReferences(node)
        val matches = instructionMatcher.findMatchesForCondition(node, label, jumpIf)
        return coverGivenMatches(node, matches, referenceAnalysis)
    }
}
