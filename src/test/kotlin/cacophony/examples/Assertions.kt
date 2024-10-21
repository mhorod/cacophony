package cacophony.examples

class AssertNoErrors : DiagnosticsAssertion {
    override fun check(diagnostics: TestDiagnostics) {
        assert(diagnostics.errors().isEmpty())
    }
}

class AssertHasErrors : DiagnosticsAssertion {
    override fun check(diagnostics: TestDiagnostics) {
        assert(diagnostics.errors().isNotEmpty())
    }
}

fun assertionFromDescription(description: IncorrectExampleDescription): DiagnosticsAssertion {
    return if (description.errors.lexerErrors) {
        AssertHasErrors()
    } else {
        AssertNoErrors()
    }
}
