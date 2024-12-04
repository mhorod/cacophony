package cacophony.semantic.names

import cacophony.*
import cacophony.diagnostics.Diagnostics
import cacophony.diagnostics.ORDiagnostics
import cacophony.semantic.syntaxtree.Definition
import cacophony.semantic.syntaxtree.FunctionCall
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

    @Test
    fun `single overload`() {
        // f[]
        val func = variableUse("f")
        val funcCall = call(func)
        val ast = block(funcCall)

        val overloadSet = mockk<OverloadSet>()
        val def = mockk<Definition.FunctionDefinition>()
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
        val func = variableUse("f")
        val arg = lit(0)
        val funcCall = call(func, arg)
        val ast = block(funcCall)

        val overloadSet = mockk<OverloadSet>()
        val def0 = mockk<Definition.FunctionDefinition>()
        val def1 = mockk<Definition.FunctionDefinition>()
        val def2 = mockk<Definition.FunctionDefinition>()
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
        val func = variableUse("f")
        val arg = lit(0)
        val funcCall = call(func, arg)
        val ast = block(funcCall)

        val overloadSet = mockk<OverloadSet>()
        val def0 = mockk<Definition.FunctionDefinition>()
        val def2 = mockk<Definition.FunctionDefinition>()
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
        val variable = variableUse("a")
        val block = block(variable)
        val ast = block(block)

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
        val variable = variableUse("a")
        val block = block(variable)
        val ast = block(block)

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
        val func = variableUse("f")
        val arg = lit(0)
        val block = block(func)
        val funcCall = FunctionCall(mockRange(), block, listOf(arg))
        val ast = block(funcCall)

        val overloadSet = mockk<OverloadSet>()
        val def = mockk<Definition.FunctionDefinition>()
        every { overloadSet[1] } returns def
        val nr: NameResolutionResult = mapOf(func to ResolvedName.Function(overloadSet))

        resolveOverloads(ast, diagnostics, nr)
        verify(exactly = 1) { diagnostics.report(ORDiagnostics.FunctionIsNotVariableUse, any<Pair<Location, Location>>()) }
    }

    @Test
    fun `throws when function name is a variable`() {
        // f[0]
        val func = variableUse("f")
        val arg = lit(0)
        val funcCall = call(func, arg)
        val ast = block(funcCall)

        val def = mockk<Definition.VariableDeclaration>()
        val nr: NameResolutionResult = mapOf(func to ResolvedName.Variable(def))

        resolveOverloads(ast, diagnostics, nr)
        verify(exactly = 1) { diagnostics.report(ORDiagnostics.UsingVariableAsFunction("f"), any<Pair<Location, Location>>()) }
    }

    @Test
    fun `throws when function name is a function argument`() {
        // f[0]
        val func = variableUse("f")
        val arg = lit(0)
        val funcCall = call(func, arg)
        val ast = block(funcCall)

        val def = mockk<Definition.FunctionArgument>()
        val nr: NameResolutionResult = mapOf(func to ResolvedName.Argument(def))

        resolveOverloads(ast, diagnostics, nr)
        verify(exactly = 1) { diagnostics.report(ORDiagnostics.UsingArgumentAsFunction("f"), any<Pair<Location, Location>>()) }
    }

    @Test
    fun `recurses into if statement`() {
        // if true then a
        val variable = variableUse("a")
        val trueLiteral = lit(true)
        val ifThen = ifThenElse(trueLiteral, variable, empty())
        val ast = block(ifThen)

        val def = mockk<Definition.VariableDeclaration>()
        val nr: NameResolutionResult = mapOf(variable to ResolvedName.Variable(def))

        val resolvedVariables = resolveOverloads(ast, diagnostics, nr)

        assertThat(resolvedVariables).containsExactlyInAnyOrderEntriesOf(
            mapOf(variable to def),
        )
    }
}
