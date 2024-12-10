package cacophony

import cacophony.diagnostics.CacophonyDiagnostics
import cacophony.pipeline.CacophonyPipeline
import cacophony.utils.Input
import io.mockk.every
import io.mockk.mockk

fun testPipeline(): CacophonyPipeline {
    val input = mockk<Input>()
    every { input.locationRangeToString(any(), any()) } answers { (from, to) -> "$from-$to" }
    val diagnostics = CacophonyDiagnostics(input)
    return CacophonyPipeline(diagnostics)
}
