package cacophony.diagnostics

class ASTDiagnostics {
    data class ValueOutOfRange(
        val value: String,
    ) : DiagnosticMessage {
        override fun getMessage() = "Value $value is out of range"
    }

    data class DuplicateField(
        val field: String,
    ) : DiagnosticMessage {
        override fun getMessage() = "Field $field already defined"
    }
}
