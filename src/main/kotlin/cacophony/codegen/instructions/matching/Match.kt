package cacophony.codegen.instructions.matching

import cacophony.codegen.instructions.InstructionMaker
import cacophony.codegen.patterns.Pattern
import cacophony.controlflow.FillingGuide

/**
 * @param instructionMaker Function which expects some value slots to be filled and returns a sequence of instructions
 * @param toFill Value slots remaining to be filled
 * @param size Cost to be compared against other matches
 * @param pattern Matched pattern
 */
class Match(val instructionMaker: InstructionMaker, val toFill: FillingGuide, val size: Int, val pattern: Pattern)
