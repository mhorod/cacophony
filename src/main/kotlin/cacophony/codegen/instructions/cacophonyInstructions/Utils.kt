package cacophony.codegen.instructions.cacophonyInstructions

import cacophony.codegen.instructions.MemoryAddress
import cacophony.controlflow.HardwareRegisterMapping

fun MemoryAddress.toAsm(hardwareRegisterMapping: HardwareRegisterMapping): String {
    val builder = StringBuilder()
    builder.append("[").append(hardwareRegisterMapping[base])
    if (index != null && scale != null) {
        builder.append("+$scale*${hardwareRegisterMapping[index]}")
    }
    if (displacement != null && displacement != 0) {
        val sign = if (displacement < 0) "" else "+"
        builder.append("$sign$displacement")
    }
    builder.append("]")
    return builder.toString()
}
