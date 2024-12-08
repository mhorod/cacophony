package cacophony.semantic.types

import cacophony.diagnostics.Diagnostics
import cacophony.diagnostics.TypeCheckerDiagnostics
import cacophony.utils.Location

// class responsible for the interaction with Diagnostics
internal class ErrorHandler(
    val diagnostics: Diagnostics,
) {
    fun typeMismatchError(expected: TypeExpr, found: TypeExpr, range: Pair<Location, Location>) {
        diagnostics.report(TypeCheckerDiagnostics.TypeMismatch(expected.toString(), found.toString()), range)
    }

    fun unknownType(range: Pair<Location, Location>) {
        diagnostics.report(TypeCheckerDiagnostics.UnknownType, range)
    }

    fun expectedFunction(range: Pair<Location, Location>) {
        diagnostics.report(TypeCheckerDiagnostics.ExpectedFunction, range)
    }

    fun expectedLvalue(range: Pair<Location, Location>) {
        diagnostics.report(TypeCheckerDiagnostics.ExpectedLValueReference, range)
    }

    fun operationNotSupportedOn(operation: String, type: TypeExpr, range: Pair<Location, Location>) {
        diagnostics.report(TypeCheckerDiagnostics.UnsupportedOperation(type.toString(), operation), range)
    }

    fun noCommonType(type1: TypeExpr, type2: TypeExpr, range: Pair<Location, Location>) {
        diagnostics.report(TypeCheckerDiagnostics.NoCommonType(type1.toString(), type2.toString()), range)
    }

    fun returnOutsideFunction(range: Pair<Location, Location>) {
        diagnostics.report(TypeCheckerDiagnostics.MisplacedReturn, range)
    }

    fun breakOutsideWhile(range: Pair<Location, Location>) {
        diagnostics.report(TypeCheckerDiagnostics.BreakOutsideWhile, range)
    }
}
