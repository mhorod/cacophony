package cacophony.diagnostics

class LexerDiagnostics {
    data object NoValidToken : DiagnosticMessage {
        override fun getMessage() = "Lexer error: no valid token found"
    }
}
