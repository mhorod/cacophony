package cacophony.codegen.patterns

import cacophony.codegen.instructions.CacophonyInstructionCovering
import cacophony.codegen.instructions.cacophonyInstructions.PushReg
import cacophony.codegen.instructions.matching.InstructionMatcherImpl
import cacophony.codegen.linearization.linearize
import cacophony.codegen.patterns.cacophonyPatterns.conditionPatterns
import cacophony.codegen.patterns.cacophonyPatterns.sideEffectPatterns
import cacophony.codegen.patterns.cacophonyPatterns.valuePatterns
import cacophony.controlflow.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

class PatternsTest {
    private val instructionCovering = CacophonyInstructionCovering(InstructionMatcherImpl(valuePatterns, sideEffectPatterns, conditionPatterns))

    // TODO patterns priority
    @Disabled
    @Test
    fun `push rsp generates single instruction`() {
        val label = CFGLabel()
        val register = Register.FixedRegister(HardwareRegister.RSP)
        val fragment = CFGFragment(
            mapOf(
                label to CFGVertex.Final(CFGNode.Push(CFGNode.RegisterUse(register))),
            ),
            label
        )
        val loweredFragment = linearize(fragment, instructionCovering)
        assertThat(loweredFragment).hasSize(1)

        val instructions = loweredFragment.first().instructions()
        assertThat(instructions).hasSize(1).first().isEqualTo(PushReg(register))
    }
}