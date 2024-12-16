package cacophony.codegen.instructions.cacophonyInstructions

import cacophony.codegen.instructions.MemoryAddress
import cacophony.controlflow.HardwareRegisterMapping
import cacophony.controlflow.Register

fun MemoryAddress.toAsm(hardwareRegisterMapping: HardwareRegisterMapping): String {
    val baseHardwareRegister = hardwareRegisterMapping[base]
    require(baseHardwareRegister != null) { "No hardware register mapping found for $base" }

    val builder = StringBuilder()
    builder.append("[").append(baseHardwareRegister)
    if (index != null && scale != null) {
        val indexHardwareRegister = hardwareRegisterMapping[index]
        require(indexHardwareRegister != null) { "No hardware register mapping found for $index" }
        builder.append("+$scale*$indexHardwareRegister")
    }
    if (displacement != null && displacement.value != 0) {
        val sign = if (displacement.value < 0) "" else "+"
        builder.append("$sign${displacement.value}")
    }
    builder.append("]")
    return builder.toString()
}

fun MemoryAddress.substituteRegisters(registersSubstitution: Map<Register, Register>): MemoryAddress =
    MemoryAddress(
        base.substitute(registersSubstitution),
        index?.substitute(registersSubstitution),
        this.scale,
        this.displacement,
    )

fun Register.substitute(registersSubstitution: Map<Register, Register>): Register = registersSubstitution.getOrDefault(this, this)
