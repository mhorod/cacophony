package cacophony.codegen.registers

import cacophony.codegen.instructions.CopyInstruction
import cacophony.codegen.instructions.Instruction
import cacophony.codegen.linearization.BasicBlock
import cacophony.codegen.linearization.LoweredCFGFragment
import cacophony.controlflow.HardwareRegister
import cacophony.controlflow.Register
import cacophony.graphs.getSymmetricClosure

typealias RegisterRelations = Map<Register, Set<Register>>

/**
 * @param allRegisters All registers used by instructions in LoweredCFGFragment
 * @param interference Symmetric relation of interfering registers i.e.
 *   such that cannot be allocated to the same hardware register
 * @param copying Relation (non necessarily symmetric) consisting of pairs (B, A) such that
 *   A is a copy of B and allocating it to the same register as B will reduce redundant
 *   instructions at later stage of compilation
 */
data class RegistersInteraction(
    val allRegisters: Set<Register>,
    val interference: RegisterRelations,
    val copying: RegisterRelations,
)

/**
 * @throws IllegalArgumentException if
 * - Provided loweredCfgFragment contains an empty block (without any instructions).
 */
fun analyzeRegistersInteraction(
    loweredCfgFragment: LoweredCFGFragment,
    preservedRegisters: List<HardwareRegister>
): RegistersInteraction =
    RegistersInteractionAnalysis(loweredCfgFragment, preservedRegisters).analyze()

private class RegistersInteractionAnalysis(
    private val loweredCfgFragment: LoweredCFGFragment,
    private val preservedRegisters: List<HardwareRegister>,
) {
    private var allInstructions: Set<InstructionRef> = emptySet()
    private var allRegisters: Set<Register> = emptySet()
    private var nextInstructions: Map<InstructionRef, Set<InstructionRef>> = emptyMap()
    private var liveIn: Map<InstructionRef, MutableSet<Register>> = emptyMap()
    private var liveOut: Map<InstructionRef, MutableSet<Register>> = emptyMap()

    private var copying: MutableMap<Register, Set<Register>> = mutableMapOf()
    private var interference: MutableMap<Register, MutableSet<Register>> = mutableMapOf()

    private class InstructionRef(val ins: Instruction) {
        val registersRead: Set<Register>
            get() = ins.registersRead
        val registersWritten: Set<Register>
            get() = ins.registersWritten

        override fun equals(other: Any?): Boolean = other is InstructionRef && ins === other.ins

        override fun hashCode(): Int = System.identityHashCode(ins)
    }

    private fun getFirstInstructions(blocks: Set<BasicBlock>): List<InstructionRef> =
        blocks
            .mapNotNull {
                it.instructions().firstOrNull()
            }.map { InstructionRef(it) }

    private fun preprocessInstructions() {
        nextInstructions =
            loweredCfgFragment
                .flatMap { block ->
                    block
                        .instructions()
                        .map { InstructionRef(it) }
                        .zipWithNext { a, b -> a to listOf(b) } +
                        listOf(
                            InstructionRef(block.instructions().last()) to getFirstInstructions(block.successors()),
                        )
                }.associate { it.first to it.second.toSet() }

        allInstructions = nextInstructions.keys
        allRegisters = allInstructions.flatMap { it.registersRead union it.registersWritten }.toSet() union preservedRegisters.map { Register.FixedRegister(it) }
    }

    private fun calculateLiveness() {
        liveOut = allInstructions.associateWith { it.registersWritten.toMutableSet() }
        liveIn = allInstructions.associateWith { it.registersRead.toMutableSet() }

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
    }

    private fun calculateCopying() {
        // A is a copy of B iff:
        // - there is exactly one instruction I that defines A, i.e. A \in I.registersWritten(), and
        // - I is CopyInstruction with I.copyInto() === A and I.copyFrom() === B, and
        // - there is no instruction J \neq I, such that:
        //     * B \in J.registersWritten(), and
        //     * A \in liveOut[J]
        val registersToDefinitions =
            allRegisters.associateWith { reg ->
                allInstructions.filter { it.registersWritten.contains(reg) }
            }

        copying =
            allInstructions
                .asSequence()
                .map { it.ins }
                .filterIsInstance<CopyInstruction>()
                .map { it.copyInto() to it.copyFrom() } // all pairs A = B
                .filter { (a, _) -> registersToDefinitions[a]!!.size == 1 } // A is defined only once as copy of B
                .filter { (a, b) ->
                    registersToDefinitions[b]!!.none {
                        // B is never defined in instruction
                        //   after which A must live
                        liveOut[it]!!.contains(a)
                    }
                }.groupBy { it.second }
                .mapValues { (_, v) -> v.map { it.first }.toSet() }
                .toMutableMap()

        allRegisters.forEach {
            if (!copying.containsKey(it)) {
                copying[it] = setOf()
            }
        }
    }

    private fun calculateInterference() {
        // Registers A, B interfere iff:
        // - A and B are not in copying relation, and
        // - there is an instruction I that satisfies:
        //    * A \in liveOut[I], and
        //    * B \in I.registersWritten()
        //    (or symmetrically - if we swap A and B)
        interference =
            allInstructions
                .flatMap { ins ->
                    liveOut[ins]!!
                        .flatMap { a ->
                            // cartesian product of liveOut[ins] and ins.registersWritten()
                            ins.registersWritten.map { b -> a to b }
                        }.filter { (a, b) ->
                            // filter out loops
                            a != b
                        }.filter { (a, b) ->
                            // filter out pairs that are in copying relation
                            !copying[a]!!.contains(b) && !copying[b]!!.contains(a)
                        }
                }.groupBy { it.first }
                .mapValues { (_, v) -> v.map { it.second }.toMutableSet() }
                .toMutableMap()

        allRegisters.forEach {
            if (!interference.containsKey(it)) {
                interference[it] = mutableSetOf()
            }
        }

        allRegisters.filter { it is Register.VirtualRegister && it.holdsReference }.forEach {reg ->
            preservedRegisters.forEach { preservedReg ->
                interference[reg]!!.add(Register.FixedRegister(preservedReg))
                interference[Register.FixedRegister(preservedReg)]!!.add(reg)
            }
        }

        interference = getSymmetricClosure(interference.toMap()).mapValues { it.value.toMutableSet() }.toMutableMap()
    }

    fun analyze(): RegistersInteraction {
        require(loweredCfgFragment.all { it.instructions().isNotEmpty() }) { "Found empty basic block" }

        preprocessInstructions()
        calculateLiveness()
        calculateCopying()
        calculateInterference()

        return RegistersInteraction(allRegisters, interference, copying)
    }
}
