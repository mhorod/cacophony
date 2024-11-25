package cacophony.codegen.instructions.cacophonyInstructions

import cacophony.codegen.instructions.MemoryAddress

fun MemoryAddress.toAsm(): String {
    val builder = StringBuilder()
    builder.append("[").append(base)
    if (index != null && scale != null) {
        builder.append("+$scale*$index")
    }
    if (displacement != null) {
        builder.append("+$displacement")
    }
    builder.append("]")
    return builder.toString()
}
