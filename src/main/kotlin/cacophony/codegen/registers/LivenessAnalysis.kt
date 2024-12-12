package cacophony.codegen.registers

import cacophony.codegen.instructions.CopyInstruction
import cacophony.codegen.instructions.Instruction
import cacophony.codegen.linearization.BasicBlock
import cacophony.codegen.linearization.LoweredCFGFragment
import cacophony.controlflow.HardwareRegister
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

private class InstructionRef(val ins: Instruction) {
    val registersRead: Set<Register>
        get() = ins.registersRead
    val registersWritten: Set<Register>
        get() = ins.registersWritten

    override fun equals(other: Any?): Boolean = other is InstructionRef && ins === other.ins

    override fun hashCode(): Int = System.identityHashCode(ins)
}

private fun getFirstInstructions(blocks: Set<BasicBlock>): List<InstructionRef> =
    blocks.mapNotNull {
        it.instructions().firstOrNull()
    }.map { InstructionRef(it) }

fun analyzeLiveness(cfgFragment: LoweredCFGFragment): Liveness {
    if (cfgFragment.any { it.instructions().isEmpty() }) {
        throw LivenessAnalysisErrorException("Found empty basic block")
    }

    val nextInstructions: Map<InstructionRef, Set<InstructionRef>> =
        cfgFragment
            .flatMap { block ->
                block
                    .instructions().map { InstructionRef(it) }
                    .zipWithNext { a, b -> a to listOf(b) } +
                    listOf(
                        InstructionRef(block.instructions().last()) to getFirstInstructions(block.successors()),
                    )
            }.associate { it.first to it.second.toSet() }

    val allInstructions = nextInstructions.keys

    fun MutableSet<Register>.hack(): MutableSet<Register> {
        add(Register.FixedRegister(HardwareRegister.RSP))
        add(Register.FixedRegister(HardwareRegister.RBP))
        return this
    }

    val liveOut: Map<InstructionRef, MutableSet<Register>> = allInstructions.associateWith { it.registersWritten.toMutableSet().hack() }
    val liveIn: Map<InstructionRef, MutableSet<Register>> = allInstructions.associateWith { it.registersRead.toMutableSet().hack() }

    var fixedPointObtained = false

    while (!fixedPointObtained) {
        fixedPointObtained = true

        allInstructions.forEach { instruction ->
            nextInstructions[instruction]!!
                .flatMap {
                    liveIn[it]!!.toList()
                }
                .forEach {
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
                .filter { instruction ->
                    instruction.ins !is CopyInstruction ||
                        !(instruction.registersRead + instruction.registersWritten).contains(reg)
                }
                .flatMap { instruction ->
                    listOf(
                        if (liveIn[instruction]!!.contains(reg)) liveIn[instruction]!!.toList() else listOf(),
                        if (liveOut[instruction]!!.contains(reg)) liveOut[instruction]!!.toList() else listOf(),
                    ).flatten()
                }.toSet()
                .minus(reg)
        }

    allInstructions.forEach { instruction ->
        println("\n\nInstruction ${instruction.ins}")
        println("LiveIn: ${liveIn[instruction]!!}")
        println("LiveOut: ${liveOut[instruction]!!}")
        println("Successors: ${nextInstructions[instruction]!!.map { it.ins }}")
    }

    /*val copying: RegisterRelations =
        allRegisters.associateWith { reg ->
            allInstructions
                .asSequence()
                .filter { it.ins is CopyInstruction }
                .filter { it.registersRead.contains(reg) }
                .flatMap { it.registersWritten }
                .toSet()
                .minus(reg)
                .minus(interference[reg]!!)
                .toSet()
        }*/ // TODO: FIX, THIS IS NOT WORKING
    val copying = allRegisters.associateWith { setOf<Register>() }

    return Liveness(allRegisters, interference, copying)
}
