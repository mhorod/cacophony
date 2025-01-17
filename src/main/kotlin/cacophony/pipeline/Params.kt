package cacophony.pipeline

import cacophony.codegen.instructions.CacophonyInstructionCovering
import cacophony.codegen.instructions.matching.CacophonyInstructionMatcher
import cacophony.controlflow.HardwareRegister
import cacophony.controlflow.Register
import cacophony.lexer.CacophonyLexer
import cacophony.parser.CacophonyParser
import java.nio.file.Paths

class Params {
    companion object {
        val lexer = CacophonyLexer()
        val parser = CacophonyParser()
        val backupRegs =
            setOf(
                Register.FixedRegister(HardwareRegister.R10),
                Register.FixedRegister(HardwareRegister.R11),
            )
        val instructionCovering = CacophonyInstructionCovering(CacophonyInstructionMatcher())
        val allGPRs = HardwareRegister.entries.toSet()
        val externalLibs = listOf(Paths.get("lib_cacophony/libcacophony.cpp"), Paths.get("lib_cacophony/gc/gc.cpp"))
        val outputParentDir = Paths.get("output")
    }
}
