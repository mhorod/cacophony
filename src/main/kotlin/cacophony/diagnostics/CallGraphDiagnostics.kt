package cacophony.diagnostics

class CallGraphDiagnostics {
    data class CallingNonFunction(
        val identifier: String,
    ) : DiagnosticMessage {
        override fun getMessage() = "Cannot call non-function $identifier"
    }

    data class CallingNonExistentIdentifier(
        val identifier: String,
    ) : DiagnosticMessage {
        override fun getMessage() = "Identifier $identifier does not exist"
    }
}
