package cacophony.examples

import cacophony.diagnostics.CacophonyDiagnostics
import cacophony.pipeline.CacophonyPipeline
import cacophony.pipeline.Params
import cacophony.utils.FileInput
import java.nio.file.Path
import kotlin.io.path.listDirectoryEntries

fun createBinary(path: Path, filename: String): Path {
    val programPath = path.resolve(filename)
    val asmFile = kotlin.io.path.createTempFile().apply { toFile().deleteOnExit() }
    val objFile = kotlin.io.path.createTempFile().apply { toFile().deleteOnExit() }
    val binFile = kotlin.io.path.createTempFile().apply { toFile().deleteOnExit() }
    val additionalObjects = path.listDirectoryEntries("*.cpp").toList()

    val input = FileInput(programPath.toString())
    val diagnostics = CacophonyDiagnostics(input)
    val pipeline = CacophonyPipeline(diagnostics, null)

    pipeline.compileAndLink(input, (additionalObjects + Params.externalLibs), asmFile, objFile, binFile)

    return binFile
}
