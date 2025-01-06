package cacophony.controlflow

sealed class Register {
    class VirtualRegister(
        val holdsReference: Boolean = false,
    ) : Register() {
        private val myNumber = number++

        override fun toString(): String = "reg$myNumber"

        private companion object {
            private var number: Int = 0
        }
    }

    data class FixedRegister(
        val hardwareRegister: HardwareRegister,
    ) : Register() {
        override fun toString(): String = hardwareRegister.name
    }
}
