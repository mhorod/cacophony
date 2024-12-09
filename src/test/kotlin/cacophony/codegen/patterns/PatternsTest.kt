package cacophony.codegen.patterns

import cacophony.codegen.instructions.CacophonyInstructionCovering
import cacophony.codegen.instructions.cacophonyInstructions.AddRegImm
import cacophony.codegen.instructions.cacophonyInstructions.PushReg
import cacophony.codegen.instructions.cacophonyInstructions.SubRegImm
import cacophony.codegen.instructions.matching.InstructionMatcherImpl
import cacophony.codegen.linearization.linearize
import cacophony.codegen.patterns.cacophonyPatterns.conditionPatterns
import cacophony.codegen.patterns.cacophonyPatterns.sideEffectPatterns
import cacophony.codegen.patterns.cacophonyPatterns.valuePatterns
import cacophony.controlflow.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class PatternsTest {
    private val instructionCovering =
        CacophonyInstructionCovering(InstructionMatcherImpl(valuePatterns, sideEffectPatterns, conditionPatterns))

    @Test
    fun `push rsp generates a single instruction`() {
        val label = CFGLabel()
        val register = Register.FixedRegister(HardwareRegister.RSP)
        val fragment =
            CFGFragment(
                mapOf(
                    label to CFGVertex.Final(CFGNode.Push(CFGNode.RegisterUse(register))),
                ),
                label,
            )
        val loweredFragment = linearize(fragment, instructionCovering)
        assertThat(loweredFragment).hasSize(1)

        val instructions = loweredFragment.first().instructions()
        assertThat(instructions).last().isEqualTo(PushReg(register))
    }

    @Test
    fun `sub rsp 8 generates a single instruction`() {
        val label = CFGLabel()
        val register = Register.FixedRegister(HardwareRegister.RSP)
        val constant = CFGNode.ConstantKnown(8)
        val fragment =
            CFGFragment(
                mapOf(
                    label to CFGVertex.Final(CFGNode.SubtractionAssignment(CFGNode.RegisterUse(register), constant)),
                ),
                label,
            )
        val loweredFragment = linearize(fragment, instructionCovering)
        assertThat(loweredFragment).hasSize(1)

        val instructions = loweredFragment.first().instructions()
        assertThat(instructions).last().isEqualTo(SubRegImm(register, constant))
    }

    @Test
    fun `add rsp 8 generates a single instruction`() {
        val label = CFGLabel()
        val register = Register.FixedRegister(HardwareRegister.RSP)
        val constant = CFGNode.ConstantKnown(8)
        val fragment =
            CFGFragment(
                mapOf(
                    label to CFGVertex.Final(CFGNode.AdditionAssignment(CFGNode.RegisterUse(register), constant)),
                ),
                label,
            )
        val loweredFragment = linearize(fragment, instructionCovering)
        assertThat(loweredFragment).hasSize(1)

        val instructions = loweredFragment.first().instructions()
        assertThat(instructions).last().isEqualTo(AddRegImm(register, constant))
    }
}
