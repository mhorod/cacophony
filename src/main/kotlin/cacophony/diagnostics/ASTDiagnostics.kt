package cacophony.diagnostics

class ASTDiagnostics {
    data class ValueOutOfRange(
        val value: String,
    ) : DiagnosticMessage {
        override fun getMessage() = "Value $value is out of range"
    }

    data object NonFunctionalForeign : DiagnosticMessage {
        override fun getMessage() = "foreign variables are not allowed"
    }
}
