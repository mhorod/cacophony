package cacophony.controlflow

sealed class Register {
    class VirtualRegister : Register()

    data class FixedRegister(
        val hardwareRegister: HardwareRegister,
    ) : Register()
}
