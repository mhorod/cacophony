package cacophony.examples

class AssertNoErrors : DiagnosticsAssertion {
    override fun check(diagnostics: TestDiagnostics) {
        for (error in diagnostics.errors()) {
            println(error.message)
        }
        assert(diagnostics.errors().isEmpty())
    }
}

class AssertHasErrors : DiagnosticsAssertion {
    override fun check(diagnostics: TestDiagnostics) {
        assert(diagnostics.errors().isNotEmpty())
    }
}

fun assertionFromDescription(description: IncorrectExampleDescription): DiagnosticsAssertion =
    if (description.errors.lexerErrors) {
        AssertHasErrors()
    } else {
        AssertNoErrors()
    }
