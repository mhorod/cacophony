package cacophony.diagnostics

sealed interface DiagnosticMessage {
    fun getMessage(): String
}
