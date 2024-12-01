package cacophony.controlflow

sealed class VariableAllocation {
    class InRegister(
        val register: Register,
    ) : VariableAllocation()

    class OnStack(
        val offset: Int,
    ) : VariableAllocation()
}
