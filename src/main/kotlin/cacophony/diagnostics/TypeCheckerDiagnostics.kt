package cacophony.diagnostics

class TypeCheckerDiagnostics {
    data class TypeMismatch(
        val expected: String,
        val found: String,
    ) : DiagnosticMessage {
        override fun getMessage() = "Type mismatch: expected $expected, found $found"
    }

    data object UnknownType : DiagnosticMessage {
        override fun getMessage() = "Unknown type"
    }

    data object ExpectedFunction : DiagnosticMessage {
        override fun getMessage() = "Expected function"
    }

    data object ExpectedLValueReference : DiagnosticMessage {
        override fun getMessage() = "Expected lvalue reference"
    }

    data class UnsupportedOperation(
        val type: String,
        val operation: String,
    ) : DiagnosticMessage {
        override fun getMessage() = "Type $type does not support operation $operation"
    }

    data class NoCommonType(
        val type1: String,
        val type2: String,
    ) : DiagnosticMessage {
        override fun getMessage() = "Could not find common type for $type1 and $type2"
    }

    data object MisplacedReturn : DiagnosticMessage {
        override fun getMessage() = "Return outside function body"
    }
}
