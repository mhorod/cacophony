package cacophony.codegen.registers

import cacophony.codegen.BlockLabel
import cacophony.codegen.instructions.CopyInstruction
import cacophony.codegen.instructions.Instruction
import cacophony.codegen.instructions.InstructionCovering
import cacophony.codegen.linearization.BasicBlock
import cacophony.codegen.linearization.LoweredCFGFragment
import cacophony.controlflow.CFGNode
import cacophony.controlflow.Register
import cacophony.controlflow.Register.FixedRegister
import cacophony.controlflow.Register.VirtualRegister
import cacophony.controlflow.Variable
import cacophony.controlflow.functions.FunctionHandler
import cacophony.graphs.GraphColoring

class SpillHandlingException(reason: String) : Exception(reason)

/**
 * @throws SpillHandlingException if spill handling fails, i.e.
 * - One of spare registers is used in register allocation (in the map image)
 * - A fixed register has spilled in register allocation
 * - Not enough spare registers provided
 * - Failed to color spills for optimization
 * @return List of BasicBlocks with adjusted and new instructions.
 */
fun adjustLoweredCFGToHandleSpills(
    instructionCovering: InstructionCovering,
    functionHandler: FunctionHandler,
    loweredCfg: LoweredCFGFragment,
    registersInteraction: RegistersInteraction,
    registerAllocation: RegisterAllocation,
    spareRegisters: Set<FixedRegister>,
    graphColoring: GraphColoring<VirtualRegister, Int>,
): LoweredCFGFragment {
    if (registerAllocation.successful
            .filter { it.key is VirtualRegister }
            .values
            .intersect(spareRegisters.map { it.hardwareRegister }.toSet())
            .isNotEmpty()
    ) {
        throw SpillHandlingException("Spare register is used in virtual register allocation.")
    }

    if (registerAllocation.spills.isEmpty()) {
        return loweredCfg
    }

    val spills: Set<VirtualRegister> =
        registerAllocation.spills
            .map {
                when (it) {
                    is FixedRegister -> throw SpillHandlingException("Fixed register $it has spilled.")
                    is VirtualRegister -> it
                }
            }.toSet()

    val spillsColoring = colorSpills(spills, registersInteraction, graphColoring)
    val spillsFrameAllocation = allocateFrameMemoryForSpills(functionHandler, spillsColoring)

    fun loadSpillIntoReg(spill: VirtualRegister, reg: Register): List<Instruction> =
        instructionCovering.coverWithInstructionsWithoutTemporaryRegisters(
            CFGNode.Assignment(
                CFGNode.RegisterUse(reg, spill.holdsReference),
                spillsFrameAllocation[spill]!!,
            ),
        )

    fun saveRegIntoSpill(reg: Register, spill: VirtualRegister): List<Instruction> =
        instructionCovering.coverWithInstructionsWithoutTemporaryRegisters(
            CFGNode.Assignment(
                spillsFrameAllocation[spill]!!,
                CFGNode.RegisterUse(reg, spill.holdsReference),
            ),
        )

    fun isRedundantCopy(instruction: Instruction): Boolean =
        instruction is CopyInstruction &&
            spills.contains(instruction.copyInto()) &&
            spills.contains(instruction.copyFrom()) &&
            spillsColoring[instruction.copyInto()] === spillsColoring[instruction.copyFrom()]

    return loweredCfg.map { block ->
        val newInstructions =
            block.instructions().flatMap { instruction ->
                val readSpills =
                    instruction.registersRead.filterIsInstance<VirtualRegister>().filter { spills.contains(it) }
                val writtenSpills =
                    instruction.registersWritten.filterIsInstance<VirtualRegister>().filter { spills.contains(it) }
                val usedSpills = (readSpills + writtenSpills).toSet()

                if (usedSpills.isEmpty()) {
                    listOf(instruction)
                } else if (isRedundantCopy(instruction)) {
                    listOf()
                } else {
                    val availableSpareRegisters =
                        spareRegisters
                            .minus(instruction.registersWritten)
                            .minus(instruction.registersRead)

                    if (usedSpills.size > availableSpareRegisters.size) {
                        throw SpillHandlingException(
                            "Not enough spare registers: Detected instruction $instruction using ${usedSpills.size} spilled registers, " +
                                "but only have ${availableSpareRegisters.size} available spare registers to use",
                        )
                    }

                    val registersSubstitution: Map<Register, Register> = usedSpills.zip(availableSpareRegisters).toMap()

                    val instructionPrologue = readSpills.flatMap { loadSpillIntoReg(it, registersSubstitution[it]!!) }
                    val instructionEpilogue =
                        writtenSpills.flatMap { saveRegIntoSpill(registersSubstitution[it]!!, it) }

                    instructionPrologue + listOf(instruction.substituteRegisters(registersSubstitution)) + instructionEpilogue
                }
            }

        object : BasicBlock {
            override fun label(): BlockLabel = block.label()

            override fun instructions(): List<Instruction> = newInstructions

            override fun successors(): Set<BasicBlock> = block.successors()

            override fun predecessors(): Set<BasicBlock> = block.predecessors()
        }
    }
}

private fun colorSpills(
    spills: Set<VirtualRegister>,
    registersInteraction: RegistersInteraction,
    graphColoring: GraphColoring<VirtualRegister, Int>,
): Map<VirtualRegister, Int> {
    val spillsInterference =
        spills.associateWith { v ->
            registersInteraction.interference
                .getOrDefault(v, setOf())
                .filterIsInstance<VirtualRegister>()
                .filter { u -> spills.contains(u) }
                .toSet()
        }
    val spillsCopying =
        spills.associateWith { v ->
            registersInteraction.copying
                .getOrDefault(v, setOf())
                .filterIsInstance<VirtualRegister>()
                .filter { u -> spills.contains(u) }
                .toSet()
        }

    val spillsColoring =
        graphColoring.doColor(
            spillsInterference,
            spillsCopying,
            mapOf(),
            spills.indices.toSet(),
        )

    if (!spillsColoring.keys.containsAll(spills)) {
        throw SpillHandlingException("Coloring spills for memory optimization failed.")
    }
    
    if (mutableMapOf<Int, MutableSet<VirtualRegister>>().apply {
            spillsColoring.forEach { (reg, color) ->
                getOrPut(color) { mutableSetOf() }.add(reg)
            }
        }.any {
            it.value.any { reg -> reg.holdsReference } &&
                it.value.any { reg -> !reg.holdsReference }
        }
    ) {
        throw SpillHandlingException("Same color assigned to register with and without reference")
    }

    return spillsColoring
}

private fun allocateFrameMemoryForSpills(
    functionHandler: FunctionHandler,
    spillsColoring: Map<VirtualRegister, Int>,
): Map<VirtualRegister, CFGNode.LValue> {
    val colorToFrameMemory =
        mutableMapOf<Int, CFGNode.LValue>().apply {
            spillsColoring.forEach { (reg, color) ->
                if (get(color) == null) {
                    put(color, functionHandler.allocateFrameVariable(Variable.PrimitiveVariable(reg.holdsReference)))
                }
            }
        }
    return spillsColoring.mapValues { (_, c) -> colorToFrameMemory[c]!! }.toMap()
}
