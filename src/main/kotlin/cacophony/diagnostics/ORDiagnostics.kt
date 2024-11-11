package cacophony.diagnostics

class ORDiagnostics {
    data object UnexpectedFunctionCall : DiagnosticMessage {
        override fun getMessage() = "Unexpected function call"
    }

    data object FunctionIsNotVariableUse : DiagnosticMessage {
        override fun getMessage() = "Function has to be a simple expression"
    }

    data class UsingVariableAsFunction(
        val identifier: String,
    ) : DiagnosticMessage {
        override fun getMessage() = "Cannot use variable as a function: $identifier"
    }

    data class UsingArgumentAsFunction(
        val identifier: String,
    ) : DiagnosticMessage {
        override fun getMessage() = "Cannot use function argument as a function: $identifier"
    }

    data class IdentifierNotFound(
        val identifier: String,
    ) : DiagnosticMessage {
        override fun getMessage() = "Cannot find an appropriate function declaration: $identifier"
    }
}
