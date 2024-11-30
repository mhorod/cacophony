package cacophony.semantic.names

import cacophony.diagnostics.Diagnostics
import cacophony.diagnostics.ORDiagnostics
import cacophony.semantic.syntaxtree.Block
import cacophony.semantic.syntaxtree.Definition
import cacophony.semantic.syntaxtree.FunctionCall
import cacophony.semantic.syntaxtree.Literal
import cacophony.semantic.syntaxtree.Statement
import cacophony.semantic.syntaxtree.VariableUse
import cacophony.utils.Location
import io.mockk.*
import io.mockk.impl.annotations.MockK
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class OverloadResolverTest {
    @MockK
    lateinit var diagnostics: Diagnostics

    @BeforeEach
    fun setUpMocks() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        every { diagnostics.report(any(), any<Pair<Location, Location>>()) } just runs
    }

    // Tests contain only a fragment of a proper ast.
    // Locations are arbitrary.

    @Test
    fun `single overload`() {
        // f[]
        val loc = Pair(Location(0), Location(0))
        val func = VariableUse(loc, "f")
        val funcCall = FunctionCall(loc, func, listOf())
        val ast = Block(loc, listOf(funcCall))

        val overloadSet = mockk<OverloadSet>()
        val def = mockk<Definition.FunctionDeclaration>()
        every { overloadSet[0] } returns def
        val nr: NameResolutionResult = mapOf(func to ResolvedName.Function(overloadSet))

        val resolvedVariables = resolveOverloads(ast, diagnostics, nr)

        assertThat(resolvedVariables).containsExactlyInAnyOrderEntriesOf(
            mapOf(func to def),
        )
    }

    @Test
    fun `multiple overloads`() {
        // f[0]
        val loc = Pair(Location(0), Location(0))
        val func = VariableUse(loc, "f")
        val arg = Literal.IntLiteral(loc, 0)
        val funcCall = FunctionCall(loc, func, listOf(arg))
        val ast = Block(loc, listOf(funcCall))

        val overloadSet = mockk<OverloadSet>()
        val def0 = mockk<Definition.FunctionDeclaration>()
        val def1 = mockk<Definition.FunctionDeclaration>()
        val def2 = mockk<Definition.FunctionDeclaration>()
        every { overloadSet[0] } returns def0
        every { overloadSet[1] } returns def1
        every { overloadSet[2] } returns def2
        val nr: NameResolutionResult = mapOf(func to ResolvedName.Function(overloadSet))

        val resolvedVariables = resolveOverloads(ast, diagnostics, nr)

        assertThat(resolvedVariables).containsExactlyInAnyOrderEntriesOf(
            mapOf(func to def1),
        )
    }

    @Test
    fun `no suitable overload`() {
        // f[0]
        val loc = Pair(Location(0), Location(0))
        val func = VariableUse(loc, "f")
        val arg = Literal.IntLiteral(loc, 0)
        val locFuncCall = Pair(Location(1), Location(2))
        val funcCall = FunctionCall(locFuncCall, func, listOf(arg))
        val ast = Block(loc, listOf(funcCall))

        val overloadSet = mockk<OverloadSet>()
        val def0 = mockk<Definition.FunctionDeclaration>()
        val def2 = mockk<Definition.FunctionDeclaration>()
        every { overloadSet[0] } returns def0
        every { overloadSet[1] } returns null
        every { overloadSet[2] } returns def2
        val nr: NameResolutionResult = mapOf(func to ResolvedName.Function(overloadSet))

        resolveOverloads(ast, diagnostics, nr)
        verify(exactly = 1) { diagnostics.report(ORDiagnostics.IdentifierNotFound("f"), any<Pair<Location, Location>>()) }
    }

    @Test
    fun `simple variable use in block`() {
        // (a)
        val loc = Pair(Location(0), Location(0))
        val variable = VariableUse(loc, "a")
        val block = Block(loc, listOf(variable))
        val ast = Block(loc, listOf(block))

        val def = mockk<Definition.VariableDeclaration>()
        val nr: NameResolutionResult = mapOf(variable to ResolvedName.Variable(def))

        val resolvedVariables = resolveOverloads(ast, diagnostics, nr)

        assertThat(resolvedVariables).containsExactlyInAnyOrderEntriesOf(
            mapOf(variable to def),
        )
    }

    @Test
    fun `simple argument use in block`() {
        // (a)
        val loc = Pair(Location(0), Location(0))
        val variable = VariableUse(loc, "a")
        val block = Block(loc, listOf(variable))
        val ast = Block(loc, listOf(block))

        val def = mockk<Definition.FunctionArgument>()
        val nr: NameResolutionResult = mapOf(variable to ResolvedName.Argument(def))

        val resolvedVariables = resolveOverloads(ast, diagnostics, nr)

        assertThat(resolvedVariables).containsExactlyInAnyOrderEntriesOf(
            mapOf(variable to def),
        )
    }

    @Test
    fun `throws when function name is not a simple expression`() {
        // (f)[0]
        val loc = Pair(Location(0), Location(0))
        val func = VariableUse(loc, "f")
        val arg = Literal.IntLiteral(loc, 0)
        val locFuncBlock = Pair(Location(1), Location(2))
        val block = Block(locFuncBlock, listOf(func))
        val funcCall = FunctionCall(loc, block, listOf(arg))
        val ast = Block(loc, listOf(funcCall))

        val overloadSet = mockk<OverloadSet>()
        val def = mockk<Definition.FunctionDeclaration>()
        every { overloadSet[1] } returns def
        val nr: NameResolutionResult = mapOf(func to ResolvedName.Function(overloadSet))

        resolveOverloads(ast, diagnostics, nr)
        verify(exactly = 1) { diagnostics.report(ORDiagnostics.FunctionIsNotVariableUse, any<Pair<Location, Location>>()) }
    }

    @Test
    fun `throws when function name is a variable`() {
        // f[0]
        val loc = Pair(Location(0), Location(0))
        val locFunc = Pair(Location(1), Location(2))
        val func = VariableUse(locFunc, "f")
        val arg = Literal.IntLiteral(loc, 0)
        val funcCall = FunctionCall(loc, func, listOf(arg))
        val ast = Block(loc, listOf(funcCall))

        val def = mockk<Definition.VariableDeclaration>()
        val nr: NameResolutionResult = mapOf(func to ResolvedName.Variable(def))

        resolveOverloads(ast, diagnostics, nr)
        verify(exactly = 1) { diagnostics.report(ORDiagnostics.UsingVariableAsFunction("f"), any<Pair<Location, Location>>()) }
    }

    @Test
    fun `throws when function name is a function argument`() {
        // f[0]
        val loc = Pair(Location(0), Location(0))
        val locFunc = Pair(Location(1), Location(2))
        val func = VariableUse(locFunc, "f")
        val arg = Literal.IntLiteral(loc, 0)
        val funcCall = FunctionCall(loc, func, listOf(arg))
        val ast = Block(loc, listOf(funcCall))

        val def = mockk<Definition.FunctionArgument>()
        val nr: NameResolutionResult = mapOf(func to ResolvedName.Argument(def))

        resolveOverloads(ast, diagnostics, nr)
        verify(exactly = 1) { diagnostics.report(ORDiagnostics.UsingArgumentAsFunction("f"), any<Pair<Location, Location>>()) }
    }

    @Test
    fun `recurses into if statement`() {
        // if true then a
        val loc = Pair(Location(0), Location(0))
        val variable = VariableUse(loc, "a")
        val trueLiteral = Literal.BoolLiteral(loc, true)
        val ifThen = Statement.IfElseStatement(loc, trueLiteral, variable, null)
        val ast = Block(loc, listOf(ifThen))

        val def = mockk<Definition.VariableDeclaration>()
        val nr: NameResolutionResult = mapOf(variable to ResolvedName.Variable(def))

        val resolvedVariables = resolveOverloads(ast, diagnostics, nr)

        assertThat(resolvedVariables).containsExactlyInAnyOrderEntriesOf(
            mapOf(variable to def),
        )
    }
}
