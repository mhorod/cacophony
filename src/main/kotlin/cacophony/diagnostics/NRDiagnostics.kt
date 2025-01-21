package cacophony.diagnostics

class NRDiagnostics {
    data class UnidentifiedIdentifier(
        val identifier: String,
    ) : DiagnosticMessage {
        override fun getMessage() = "Unidentified identifier \"$identifier\""
    }

    data class DuplicatedFunctionArgument(
        val identifier: String,
    ) : DiagnosticMessage {
        override fun getMessage() = "Duplicated function argument \"$identifier\""
    }
}
