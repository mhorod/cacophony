package cacophony.diagnostics

class ParserDiagnostics {
    data class UnexpectedToken(
        val tokenName: String,
        val symbolName: String,
    ) : DiagnosticMessage {
        override fun getMessage() = "Unexpected token $tokenName while parsing $symbolName"
    }

    data class UnableToContinueParsing(
        val symbolName: String,
    ) : DiagnosticMessage {
        override fun getMessage() = "Unable to continue parsing symbol $symbolName"
    }
}
