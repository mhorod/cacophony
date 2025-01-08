package cacophony.codegen

import cacophony.codegen.instructions.InstructionCovering
import cacophony.codegen.linearization.LoweredCFGFragment
import cacophony.codegen.linearization.linearize
import cacophony.codegen.registers.RegisterAllocation
import cacophony.codegen.registers.RegistersInteraction
import cacophony.codegen.registers.adjustLoweredCFGToHandleSpills
import cacophony.codegen.registers.analyzeRegistersInteraction
import cacophony.controlflow.HardwareRegister
import cacophony.controlflow.Register
import cacophony.controlflow.functions.FunctionHandler
import cacophony.controlflow.functions.SystemVAMD64CallConvention
import cacophony.controlflow.generation.ProgramCFG
import cacophony.graphs.FirstFitGraphColoring
import cacophony.semantic.syntaxtree.Definition.FunctionDefinition

/*
 * returns spill-free covering and register allocation
 * - linearize [cfg]
 * - attempt register allocation
 * - if spills occur, handle them
 */
fun safeLinearize(
    cfg: ProgramCFG,
    functionHandlers: Map<FunctionDefinition, FunctionHandler>,
    instructionCovering: InstructionCovering,
    allowedRegisters: Set<HardwareRegister>,
    backupRegs: Set<Register.FixedRegister>,
): Pair<
    Map<FunctionDefinition, LoweredCFGFragment>,
    Map<FunctionDefinition, RegisterAllocation>,
    > {
    val cover = cfg.mapValues { (_, cfg) -> linearize(cfg, instructionCovering) }
    val registersInteractions = analyzeRegistersInteraction(cover)
    val registerAllocation = allocateRegisters(registersInteractions, allowedRegisters)
    if (registerAllocation.values.all { it.spills.isEmpty() }) {
        return cover to registerAllocation
    }

    return handleSpills(
        functionHandlers,
        cover,
        registersInteractions,
        allowedRegisters,
        backupRegs,
        instructionCovering,
    )
}

fun analyzeRegistersInteraction(covering: Map<FunctionDefinition, LoweredCFGFragment>): Map<FunctionDefinition, RegistersInteraction> {
    val registersInteraction =
        covering.mapValues { (_, loweredCFG) ->
            analyzeRegistersInteraction(
                loweredCFG,
                SystemVAMD64CallConvention.preservedRegisters(),
            )
        }
    return registersInteraction
}

fun allocateRegisters(
    registersInteractions: Map<FunctionDefinition, RegistersInteraction>,
    allowedRegisters: Set<HardwareRegister>,
): Map<FunctionDefinition, RegisterAllocation> {
    val allocatedRegisters =
        registersInteractions.mapValues { (_, registersInteraction) ->
            cacophony.codegen.registers.allocateRegisters(registersInteraction, allowedRegisters)
        }
    return allocatedRegisters
}

private fun handleSpills(
    functionHandlers: Map<FunctionDefinition, FunctionHandler>,
    covering: Map<FunctionDefinition, LoweredCFGFragment>,
    registersInteractions: Map<FunctionDefinition, RegistersInteraction>,
    allowedRegisters: Set<HardwareRegister>,
    backupRegs: Set<Register.FixedRegister>,
    instructionCovering: InstructionCovering,
): Pair<
    Map<FunctionDefinition, LoweredCFGFragment>,
    Map<FunctionDefinition, RegisterAllocation>,
    > {
    val newRegisterAllocation =
        allocateRegisters(registersInteractions, allowedRegisters.minus(backupRegs.map { it.hardwareRegister }.toSet()))
            .mapValues { (_, value) ->
                RegisterAllocation(
                    value.successful.plus(backupRegs.associateWith { it.hardwareRegister }),
                    value.spills,
                )
            }

    if (newRegisterAllocation.values.all { it.spills.isEmpty() }) {
        return covering to newRegisterAllocation
    }

    val newCovering =
        covering
            .map { (functionDeclaration, loweredCfg) ->
                functionDeclaration to
                    adjustLoweredCFGToHandleSpills(
                        instructionCovering,
                        functionHandlers[functionDeclaration]!!,
                        loweredCfg,
                        registersInteractions[functionDeclaration]!!,
                        newRegisterAllocation[functionDeclaration]!!,
                        backupRegs,
                        FirstFitGraphColoring(),
                    )
            }.toMap()

    return newCovering to newRegisterAllocation
}
