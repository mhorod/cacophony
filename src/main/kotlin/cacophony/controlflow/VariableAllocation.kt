package cacophony.controlflow

sealed class VariableAllocation {
    class InRegister(
        val register: Register,
    ) : VariableAllocation()

    class OnStack(
        val offset: Int,
    ) : VariableAllocation()

    // The variable itself is allocated on heap, while the pointer to it is on stack
    class ViaPointer(val pointer: VariableAllocation) : VariableAllocation()
}
