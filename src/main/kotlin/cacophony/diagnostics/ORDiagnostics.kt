package cacophony.diagnostics

class ORDiagnostics {
    data object UnexpectedFunctionCall : DiagnosticMessage {
        override fun getMessage() = "Unexpected function call"
    }
}
