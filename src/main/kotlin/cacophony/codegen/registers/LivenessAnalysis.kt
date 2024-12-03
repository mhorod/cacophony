package cacophony.codegen.registers

import cacophony.codegen.instructions.CopyInstruction
import cacophony.codegen.instructions.Instruction
import cacophony.codegen.linearization.BasicBlock
import cacophony.codegen.linearization.LoweredCFGFragment
import cacophony.controlflow.Register
import cacophony.utils.CompileException

typealias RegisterRelations = Map<Register, Set<Register>>

data class Liveness(
    val allRegisters: Set<Register>,
    val interference: RegisterRelations,
    val copying: RegisterRelations,
)

class LivenessAnalysisErrorException(
    reason: String,
) : CompileException(reason)

private fun getFirstInstructions(blocks: Set<BasicBlock>): List<Instruction> = blocks.mapNotNull { it.instructions().firstOrNull() }

fun analyzeLiveness(cfgFragment: LoweredCFGFragment): Liveness {
    if (cfgFragment.any { it.instructions().isEmpty() }) {
        throw LivenessAnalysisErrorException("Found empty basic block")
    }

    val nextInstructions: Map<Instruction, Set<Instruction>> =
        cfgFragment
            .flatMap { block ->
                block
                    .instructions()
                    .zipWithNext { a, b -> a to listOf(b) } +
                    listOf(
                        block.instructions().last() to getFirstInstructions(block.successors()),
                    )
            }.associate { it.first to it.second.toSet() }

    val allInstructions = nextInstructions.keys

    val liveOut: Map<Instruction, MutableSet<Register>> = allInstructions.associateWith { it.registersWritten.toMutableSet() }
    val liveIn: Map<Instruction, MutableSet<Register>> = allInstructions.associateWith { it.registersRead.toMutableSet() }

    var fixedPointObtained = false

    while (!fixedPointObtained) {
        fixedPointObtained = true

        allInstructions.forEach { instruction ->
            nextInstructions[instruction]!!
                .flatMap {
                    liveIn[it]!!.toList()
                }.forEach {
                    if (!liveOut[instruction]!!.contains(it)) {
                        fixedPointObtained = false
                        liveOut[instruction]!!.add(it)
                    }
                }

            liveOut[instruction]!!.forEach {
                if (!liveIn[instruction]!!.contains(it) && !instruction.registersWritten.contains(it)) {
                    fixedPointObtained = false
                    liveIn[instruction]!!.add(it)
                }
            }
        }
    }

    val allRegisters = allInstructions.flatMap { it.registersRead.plus(it.registersWritten) }.toSet()

    val interference: RegisterRelations =
        allRegisters.associateWith { reg ->
            allInstructions
                // TODO: co na Teutatesa? czemu mov jest specjalny???
                .flatMap { instruction ->
                    listOf(
                        if (liveIn[instruction]!!.contains(reg)) liveIn[instruction]!!.toList() else listOf(),
                        if (liveOut[instruction]!!.contains(reg)) liveOut[instruction]!!.toList() else listOf(),
                    ).flatten()
                }.toSet()
                .minus(reg)
        }

    val copying: RegisterRelations =
        allRegisters.associateWith { reg ->
            allInstructions
                .asSequence()
                .filterIsInstance<CopyInstruction>()
                .filter { it.registersRead.contains(reg) }
                .flatMap { it.registersWritten }
                .toSet()
                .minus(reg)
                .minus(interference[reg]!!)
                .toSet()
        }

    return Liveness(allRegisters, interference, copying)
}
