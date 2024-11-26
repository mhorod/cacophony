package cacophony.codegen.instructions.cacophonyInstructions

import cacophony.codegen.instructions.MemoryAddress
import cacophony.controlflow.HardwareRegisterMapping

fun MemoryAddress.toAsm(hardwareRegisterMapping: HardwareRegisterMapping): String {
    val builder = StringBuilder()
    builder.append("[").append(hardwareRegisterMapping[base])
    if (index != null && scale != null) {
        builder.append("+$scale*${hardwareRegisterMapping[index]}")
    }
    if (displacement != null) {
        builder.append("+$displacement")
    }
    builder.append("]")
    return builder.toString()
}
