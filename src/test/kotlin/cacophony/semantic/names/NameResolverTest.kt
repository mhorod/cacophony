package cacophony.semantic.names

import cacophony.*
import cacophony.diagnostics.Diagnostics
import cacophony.diagnostics.NRDiagnostics
import cacophony.semantic.names.ResolvedName.Argument
import cacophony.semantic.names.ResolvedName.Function
import cacophony.semantic.names.ResolvedName.Variable
import cacophony.semantic.syntaxtree.*
import cacophony.semantic.syntaxtree.Definition.FunctionArgument
import cacophony.semantic.syntaxtree.Definition.FunctionDeclaration
import cacophony.semantic.syntaxtree.Definition.VariableDeclaration
import cacophony.utils.Location
import io.mockk.Called
import io.mockk.MockKAnnotations
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.just
import io.mockk.runs
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class NameResolverTest {
    private val mockValue = Empty(mockRange())

    private fun variableDeclaration(name: String): VariableDeclaration = variableDeclaration(name, mockValue)

    interface ResolvedNamesAssert {
        fun hasVariable(binding: Pair<VariableUse, VariableDeclaration>): ResolvedNamesAssert

        fun hasArgument(binding: Pair<VariableUse, FunctionArgument>): ResolvedNamesAssert

        fun hasOverloadSet(binding: Pair<VariableUse, Map<Int, FunctionDeclaration>>): ResolvedNamesAssert

        fun andNothingElse()
    }

    fun assertThatResolvedNames(resolvedNames: NameResolutionResult): ResolvedNamesAssert {
        val checked = mutableListOf<VariableUse>()

        return object : ResolvedNamesAssert {
            override fun hasVariable(binding: Pair<VariableUse, VariableDeclaration>): ResolvedNamesAssert {
                val resolvedName = resolvedNames[binding.first]
                assert(resolvedName !== null)
                assert(resolvedName is Variable)
                assertThat((resolvedName as Variable).def).isEqualTo(binding.second)
                checked.add(binding.first)
                return this
            }

            override fun hasArgument(binding: Pair<VariableUse, FunctionArgument>): ResolvedNamesAssert {
                val resolvedName = resolvedNames[binding.first]
                assert(resolvedName !== null)
                assert(resolvedName is Argument)
                assertThat((resolvedName as Argument).def).isEqualTo(binding.second)
                checked.add(binding.first)
                return this
            }

            override fun hasOverloadSet(binding: Pair<VariableUse, Map<Int, FunctionDeclaration>>): ResolvedNamesAssert {
                val overloadSet = resolvedNames[binding.first]
                assert(overloadSet !== null)
                assert(overloadSet is Function)
                assertThat((overloadSet as Function).def.toMap()).containsExactlyInAnyOrderEntriesOf(binding.second)
                checked.add(binding.first)
                return this
            }

            override fun andNothingElse() {
                assertThat(resolvedNames.keys).containsExactlyInAnyOrderElementsOf(checked)
            }
        }
    }

    @MockK
    lateinit var diagnostics: Diagnostics

    @BeforeEach
    fun setUpMocks() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        every { diagnostics.report(any(), any<Location>()) } just runs
        every { diagnostics.fatal() } returns Throwable()
    }

    @Nested
    inner class CorrectCases {
        @Nested
        inner class Simple {
            @Test
            fun `variables in single block`() {
                // let a = false;
                // let b = true;
                // a;
                // b

                // given
                val aDef = variableDeclaration("a", lit(false))
                val bDef = variableDeclaration("b", lit(true))
                val aUse = variableUse("a")
                val bUse = variableUse("b")
                val ast = block(aDef, bDef, aUse, bUse)

                // when
                val resolvedNames = resolveNames(ast, diagnostics)

                // then
                assertThatResolvedNames(resolvedNames)
                    .hasVariable(aUse to aDef)
                    .hasVariable(bUse to bDef)
                    .andNothingElse()
                verify { diagnostics wasNot Called }
            }

            @Test
            fun `variable is visible in variable declaration value`() {
                // let a = 5
                // let b = a

                // given
                val aDef = variableDeclaration("a")
                val aUse = variableUse("a")
                val ast =
                    block(aDef, variableDeclaration("b", aUse))

                // when
                val resolvedNames = resolveNames(ast, diagnostics)

                // then
                assertThatResolvedNames(resolvedNames)
                    .hasVariable(aUse to aDef)
                    .andNothingElse()
                verify { diagnostics wasNot Called }
            }

            @Test
            fun `arguments in single block`() {
                // let f = [x: Bool, y: Int] -> Int => (
                //     x;
                //     y
                // )

                // given
                val xDef = arg("x")
                val yDef = arg("y")
                val xUse = variableUse("x")
                val yUse = variableUse("y")
                val ast =
                    block(
                        unitFunctionDefinition(
                            "f",
                            listOf(xDef, yDef),
                            block(
                                xUse,
                                yUse,
                            ),
                        ),
                    )

                // when
                val resolvedNames = resolveNames(ast, diagnostics)

                // then
                assertThatResolvedNames(resolvedNames)
                    .hasArgument(xUse to xDef)
                    .hasArgument(yUse to yDef)
                    .andNothingElse()
                verify { diagnostics wasNot Called }
            }

            @Test
            fun `functions in single block`() {
                // let f = [] -> Bool => false;
                // let g = [] -> Int => 0;
                // g;
                // f

                // given
                val fDef =
                    unitFunctionDefinition(
                        "f",
                        listOf(),
                        lit(false),
                    )
                val gDef =
                    unitFunctionDefinition(
                        "g",
                        listOf(),
                        lit(0),
                    )
                val fUse = variableUse("f")
                val gUse = variableUse("g")
                val ast =
                    block(fDef, gDef, gUse, fUse)

                // when
                val resolvedNames = resolveNames(ast, diagnostics)

                // then
                assertThatResolvedNames(resolvedNames)
                    .hasOverloadSet(fUse to mapOf(0 to fDef))
                    .hasOverloadSet(gUse to mapOf(0 to gDef))
                    .andNothingElse()
                verify { diagnostics wasNot Called }
            }

            @Test
            fun `function identifier is visible when calling it`() {
                // let f = [] -> Bool => true
                // f[]

                // given
                val fUse = variableUse("f")
                val fDef =
                    unitFunctionDefinition(
                        "f",
                        listOf(),
                        lit(true),
                    )
                val ast =
                    block(
                        fDef,
                        call(fUse),
                    )

                // when
                val resolvedNames = resolveNames(ast, diagnostics)

                // then
                assertThatResolvedNames(resolvedNames)
                    .hasOverloadSet(fUse to mapOf(0 to fDef))
                    .andNothingElse()
                verify { diagnostics wasNot Called }
            }

            @Test
            fun recursion() {
                // let f = [] -> Bool => f[]

                // given
                val fUse = variableUse("f")
                val fDef =
                    unitFunctionDefinition(
                        "f",
                        listOf(),
                        call(
                            fUse,
                        ),
                    )
                val ast = block(fDef)

                // when
                val resolvedNames = resolveNames(ast, diagnostics)

                // then
                assertThatResolvedNames(resolvedNames)
                    .hasOverloadSet(fUse to mapOf(0 to fDef))
                    .andNothingElse()
                verify { diagnostics wasNot Called }
            }

            @Test
            fun `foreign function in single block`() {
                // foreign f = [] -> Bool;
                // f

                // given
                val fDef =
                    foreignFunctionDeclaration(
                        "f",
                        listOf(),
                        Type.Basic(mockRange(), "Bool"),
                    )
                val fUse = variableUse("f")
                val ast =
                    block(fDef, fUse)

                // when
                val resolvedNames = resolveNames(ast, diagnostics)

                // then
                assertThatResolvedNames(resolvedNames)
                    .hasOverloadSet(fUse to mapOf(0 to fDef))
                    .andNothingElse()
                verify { diagnostics wasNot Called }
            }

            @Test
            fun `foreign function is visible when calling it`() {
                // foreign f = [] -> Bool;
                // f[]

                // given
                val fDef =
                    foreignFunctionDeclaration(
                        "f",
                        listOf(),
                        Type.Basic(mockRange(), "Bool"),
                    )
                val fUse = variableUse("f")
                val ast =
                    block(
                        fDef,
                        call(fUse),
                    )

                // when
                val resolvedNames = resolveNames(ast, diagnostics)

                // then
                assertThatResolvedNames(resolvedNames)
                    .hasOverloadSet(fUse to mapOf(0 to fDef))
                    .andNothingElse()
                verify { diagnostics wasNot Called }
            }

            @Test
            fun `foreign function is visible when calling it with arguments`() {
                // foreign f = [Int] -> Bool;
                // f[5]

                // given
                val fDef =
                    foreignFunctionDeclaration(
                        "f",
                        listOf(Type.Basic(mockRange(), "Int")),
                        Type.Basic(mockRange(), "Bool"),
                    )
                val fUse = variableUse("f")
                val ast =
                    block(
                        fDef,
                        call(fUse, arg("5")),
                    )

                // when
                val resolvedNames = resolveNames(ast, diagnostics)

                // then
                assertThatResolvedNames(resolvedNames)
                    .hasOverloadSet(fUse to mapOf(1 to fDef))
                    .andNothingElse()
                verify { diagnostics wasNot Called }
            }
        }

        @Nested
        inner class Shadowing {
            @Test
            fun `variables shadow variables in single block`() {
                // let a = 0;
                // let a = 1;
                // a

                // given
                val aDef1 = variableDeclaration("a")
                val aDef2 = variableDeclaration("a")
                val aUse = variableUse("a")
                val ast = block(aDef1, aDef2, aUse)

                // when
                val resolvedNames = resolveNames(ast, diagnostics)

                // then
                assertThatResolvedNames(resolvedNames)
                    .hasVariable(aUse to aDef2)
                    .andNothingElse()
                verify { diagnostics wasNot Called }
            }

            @Test
            fun `variables shadow variables in nested blocks`() {
                // let a = true;
                // (
                //     let a = 0;
                //     a
                // );
                // a

                // given
                val aDef1 = variableDeclaration("a")
                val aDef2 = variableDeclaration("a")
                val aUse1 = variableUse("a")
                val aUse2 = variableUse("a")
                val ast =
                    block(
                        aDef1,
                        block(aDef2, aUse1),
                        aUse2,
                    )

                // when
                val resolvedNames = resolveNames(ast, diagnostics)

                // then
                assertThatResolvedNames(resolvedNames)
                    .hasVariable(aUse1 to aDef2)
                    .hasVariable(aUse2 to aDef1)
                    .andNothingElse()
                verify { diagnostics wasNot Called }
            }

            @Test
            fun `functions shadow functions in single scope`() {
                // let f = [] -> Bool => false;
                // let f = [] -> Bool => true;
                // f

                // given
                val fUse = variableUse("f")
                val fDef1 =
                    unitFunctionDefinition(
                        "f",
                        listOf(),
                        lit(false),
                    )
                val fDef2 =
                    unitFunctionDefinition(
                        "f",
                        listOf(),
                        lit(true),
                    )
                val ast = block(fDef1, fDef2, fUse)

                // when
                val resolvedNames = resolveNames(ast, diagnostics)

                // then
                assertThatResolvedNames(resolvedNames)
                    .hasOverloadSet(fUse to mapOf(0 to fDef2))
                    .andNothingElse()
                verify { diagnostics wasNot Called }
            }

            @Test
            fun `foreign functions shadow functions in single scope`() {
                // let f = [] -> Bool => false;
                // foreign f = [] -> Bool;
                // f

                // given
                val fUse = variableUse("f")
                val fDef1 =
                    functionDefinition(
                        "f",
                        listOf(),
                        lit(false),
                    )
                val fDef2 =
                    foreignFunctionDeclaration(
                        "f",
                        listOf(),
                        Type.Basic(mockRange(), "Bool"),
                    )
                val ast = block(fDef1, fDef2, fUse)

                // when
                val resolvedNames = resolveNames(ast, diagnostics)

                // then
                assertThatResolvedNames(resolvedNames)
                    .hasOverloadSet(fUse to mapOf(0 to fDef2))
                    .andNothingElse()
                verify { diagnostics wasNot Called }
            }

            @Test
            fun `functions shadow foreign functions in single scope`() {
                // foreign f = [] -> Bool;
                // let f = [] -> Bool => false;
                // f

                // given
                val fUse = variableUse("f")
                val fDef1 =
                    functionDefinition(
                        "f",
                        listOf(),
                        lit(false),
                    )
                val fDef2 =
                    foreignFunctionDeclaration(
                        "f",
                        listOf(),
                        Type.Basic(mockRange(), "Bool"),
                    )
                val ast = block(fDef2, fDef1, fUse)

                // when
                val resolvedNames = resolveNames(ast, diagnostics)

                // then
                assertThatResolvedNames(resolvedNames)
                    .hasOverloadSet(fUse to mapOf(0 to fDef1))
                    .andNothingElse()
                verify { diagnostics wasNot Called }
            }

            @Test
            fun `functions shadow functions in nested blocks`() {
                // let f = [] -> Bool => false;
                // (
                //     let f = [] -> Int => 0;
                //     f
                // );
                // f

                // given
                val fUse1 = variableUse("f")
                val fUse2 = variableUse("f")
                val fDef1 =
                    unitFunctionDefinition(
                        "f",
                        listOf(),
                        lit(false),
                    )
                val fDef2 =
                    unitFunctionDefinition(
                        "f",
                        listOf(),
                        lit(0),
                    )
                val ast =
                    block(
                        fDef1,
                        block(fDef2, fUse1),
                        fUse2,
                    )

                // when
                val resolvedNames = resolveNames(ast, diagnostics)

                // then
                assertThatResolvedNames(resolvedNames)
                    .hasOverloadSet(fUse1 to mapOf(0 to fDef2))
                    .hasOverloadSet(fUse2 to mapOf(0 to fDef1))
                    .andNothingElse()
                verify { diagnostics wasNot Called }
            }

            @Test
            fun `function shadows function in its body`() {
                // let f = [] -> Bool => (
                //     let f = [] -> Int => 0;
                //     f
                // );
                // f

                // given
                val fUse1 = variableUse("f")
                val fUse2 = variableUse("f")
                val fDef2 =
                    unitFunctionDefinition(
                        "f",
                        listOf(),
                        lit(0),
                    )
                val fDef1 =
                    unitFunctionDefinition(
                        "f",
                        listOf(),
                        block(fDef2, fUse1),
                    )
                val ast =
                    block(fDef1, fUse2)

                // when
                val resolvedNames = resolveNames(ast, diagnostics)

                // then
                assertThatResolvedNames(resolvedNames)
                    .hasOverloadSet(fUse1 to mapOf(0 to fDef2))
                    .hasOverloadSet(fUse2 to mapOf(0 to fDef1))
                    .andNothingElse()
                verify { diagnostics wasNot Called }
            }

            @Test
            fun `arguments shadow arguments`() {
                // let f = [x: Int] -> Int => (
                //      let g = [x: Bool] -> Bool => x;
                //      x
                // )

                // given
                val xUse1 = variableUse("x")
                val xUse2 = variableUse("x")
                val xDef1 = arg("x")
                val xDef2 = arg("x")
                val gDef =
                    unitFunctionDefinition(
                        "g",
                        listOf(xDef2),
                        xUse1,
                    )
                val fDef =
                    unitFunctionDefinition(
                        "f",
                        listOf(xDef1),
                        block(gDef, xUse2),
                    )
                val ast =
                    block(fDef)

                // when
                val resolvedNames = resolveNames(ast, diagnostics)

                // then
                assertThatResolvedNames(resolvedNames)
                    .hasArgument(xUse1 to xDef2)
                    .hasArgument(xUse2 to xDef1)
                    .andNothingElse()
                verify { diagnostics wasNot Called }
            }

            @Test
            fun `variables shadow arguments`() {
                // let f = [x: Int] -> Bool => (
                //      (
                //          let x = true;
                //          x
                //      );
                //      x
                // )

                // given
                val xUse1 = variableUse("x")
                val xUse2 = variableUse("x")
                val xDef1 = arg("x")
                val xDef2 = variableDeclaration("x")
                val fDef =
                    unitFunctionDefinition(
                        "f",
                        listOf(xDef1),
                        block(
                            block(xDef2, xUse1),
                            xUse2,
                        ),
                    )
                val ast =
                    block(
                        fDef,
                    )

                // when
                val resolvedNames = resolveNames(ast, diagnostics)

                // then
                assertThatResolvedNames(resolvedNames)
                    .hasVariable(xUse1 to xDef2)
                    .hasArgument(xUse2 to xDef1)
                    .andNothingElse()
                verify { diagnostics wasNot Called }
            }

            @Test
            fun `arguments shadow variables`() {
                // let x = true;
                // let f = [x: Int] -> Int => x;
                // x

                // given
                val xUse1 = variableUse("x")
                val xUse2 = variableUse("x")
                val xDef1 = variableDeclaration("x")
                val xDef2 = arg("x")
                val fDef =
                    unitFunctionDefinition(
                        "f",
                        listOf(xDef2),
                        xUse1,
                    )
                val ast =
                    block(xDef1, fDef, xUse2)

                // when
                val resolvedNames = resolveNames(ast, diagnostics)

                // then
                assertThatResolvedNames(resolvedNames)
                    .hasArgument(xUse1 to xDef2)
                    .hasVariable(xUse2 to xDef1)
                    .andNothingElse()
                verify { diagnostics wasNot Called }
            }

            @Test
            fun `variables shadow functions`() {
                // let f = [] -> Bool => true
                // (
                //      let f = true;
                //      f
                // );
                // f

                // given
                val fUse1 = variableUse("f")
                val fUse2 = variableUse("f")
                val fDef1 =
                    unitFunctionDefinition(
                        "f",
                        listOf(),
                        lit(true),
                    )
                val fDef2 = variableDeclaration("f")
                val ast =
                    block(
                        fDef1,
                        block(fDef2, fUse1),
                        fUse2,
                    )

                // when
                val resolvedNames = resolveNames(ast, diagnostics)

                // then
                assertThatResolvedNames(resolvedNames)
                    .hasVariable(fUse1 to fDef2)
                    .hasOverloadSet(fUse2 to mapOf(0 to fDef1))
                    .andNothingElse()
                verify { diagnostics wasNot Called }
            }

            @Test
            fun `variable shadows function in its body`() {
                // let f = [] -> Bool => (
                //      let f = true;
                //      f
                // );
                // f

                // given
                val fUse1 = variableUse("f")
                val fUse2 = variableUse("f")
                val fDef2 = variableDeclaration("f")
                val fDef1 =
                    unitFunctionDefinition(
                        "f",
                        listOf(),
                        block(fDef2, fUse1),
                    )
                val ast =
                    block(fDef1, fUse2)

                // when
                val resolvedNames = resolveNames(ast, diagnostics)

                // then
                assertThatResolvedNames(resolvedNames)
                    .hasVariable(fUse1 to fDef2)
                    .hasOverloadSet(fUse2 to mapOf(0 to fDef1))
                    .andNothingElse()
                verify { diagnostics wasNot Called }
            }

            @Test
            fun `functions shadow variables`() {
                // let a = 0;
                // (
                //      let a = [] -> Bool => true;
                //      a
                // );
                // a

                // given
                val aUse1 = variableUse("a")
                val aUse2 = variableUse("a")
                val aDef1 = variableDeclaration("a")
                val aDef2 =
                    unitFunctionDefinition(
                        "a",
                        listOf(),
                        lit(true),
                    )
                val ast =
                    block(
                        aDef1,
                        block(aDef2, aUse1),
                        aUse2,
                    )

                // when
                val resolvedNames = resolveNames(ast, diagnostics)

                // then
                assertThatResolvedNames(resolvedNames)
                    .hasOverloadSet(aUse1 to mapOf(0 to aDef2))
                    .hasVariable(aUse2 to aDef1)
                    .andNothingElse()
                verify { diagnostics wasNot Called }
            }

            @Test
            fun `arguments shadow functions`() {
                // let f = [] -> Bool => true;
                // let g = [f: Int] -> Int => f;
                // f

                // given
                val fUse1 = variableUse("f")
                val fUse2 = variableUse("f")
                val fDef1 =
                    unitFunctionDefinition(
                        "f",
                        listOf(),
                        lit(true),
                    )
                val fDef2 = arg("f")
                val ast =
                    block(
                        fDef1,
                        unitFunctionDefinition(
                            "g",
                            listOf(fDef2),
                            fUse1,
                        ),
                        fUse2,
                    )

                // when
                val resolvedNames = resolveNames(ast, diagnostics)

                // then
                assertThatResolvedNames(resolvedNames)
                    .hasArgument(fUse1 to fDef2)
                    .hasOverloadSet(fUse2 to mapOf(0 to fDef1))
                    .andNothingElse()
                verify { diagnostics wasNot Called }
            }

            @Test
            fun `arguments shadow function in its body`() {
                // let f = [f: Bool] -> Bool => f
                // f

                // given
                val fUse1 = variableUse("f")
                val fUse2 = variableUse("f")
                val fDef2 = arg("f")
                val fDef1 =
                    unitFunctionDefinition(
                        "f",
                        listOf(fDef2),
                        fUse1,
                    )
                val ast =
                    block(fDef1, fUse2)

                // when
                val resolvedNames = resolveNames(ast, diagnostics)

                // then
                assertThatResolvedNames(resolvedNames)
                    .hasArgument(fUse1 to fDef2)
                    .hasOverloadSet(fUse2 to mapOf(1 to fDef1))
                    .andNothingElse()
                verify { diagnostics wasNot Called }
            }

            @Test
            fun `functions shadow arguments`() {
                // let f = [x: Int] -> Int => (
                //      (
                //          let x = [] -> Bool => true;
                //          x
                //      );
                //      x
                // )

                // given
                val xUse1 = variableUse("x")
                val xUse2 = variableUse("x")
                val xDef1 = arg("x")
                val xDef2 =
                    unitFunctionDefinition(
                        "x",
                        listOf(),
                        lit(true),
                    )
                val ast =
                    block(
                        unitFunctionDefinition(
                            "f",
                            listOf(xDef1),
                            block(
                                block(xDef2, xUse1),
                                xUse2,
                            ),
                        ),
                    )

                // when
                val resolvedNames = resolveNames(ast, diagnostics)

                // then
                assertThatResolvedNames(resolvedNames)
                    .hasOverloadSet(xUse1 to mapOf(0 to xDef2))
                    .hasArgument(xUse2 to xDef1)
                    .andNothingElse()
                verify { diagnostics wasNot Called }
            }
        }
    }

    @Nested
    inner class Overloading {
        @Test
        fun `function overloads other functions with the same name and different arity`() {
            // let f = [] -> Bool => true;
            // let f = [x: Bool] -> Bool => false;
            // f

            // given
            val fUse = variableUse("f")
            val fDef1 =
                unitFunctionDefinition(
                    "f",
                    listOf(),
                    lit(true),
                )
            val fDef2 =
                unitFunctionDefinition(
                    "f",
                    listOf(arg("x")),
                    lit(false),
                )
            val ast = block(fDef1, fDef2, fUse)

            // when
            val resolvedNames = resolveNames(ast, diagnostics)

            // then
            assertThatResolvedNames(resolvedNames)
                .hasOverloadSet(fUse to mapOf(0 to fDef1, 1 to fDef2))
                .andNothingElse()
            verify { diagnostics wasNot Called }
        }

        @Test
        fun `foreign function overloads other functions with the same name and different arity`() {
            // foreign f = [] -> Bool => true;
            // let f = [x: Bool] -> Bool => false;
            // f

            // given
            val fUse = variableUse("f")
            val fDef1 =
                foreignFunctionDeclaration(
                    "f",
                    listOf(),
                    Type.Basic(mockRange(), "Bool"),
                )
            val fDef2 =
                functionDefinition(
                    "f",
                    listOf(arg("x")),
                    lit(false),
                )
            val ast = block(fDef1, fDef2, fUse)

            // when
            val resolvedNames = resolveNames(ast, diagnostics)

            // then
            assertThatResolvedNames(resolvedNames)
                .hasOverloadSet(fUse to mapOf(0 to fDef1, 1 to fDef2))
                .andNothingElse()
            verify { diagnostics wasNot Called }
        }

        @Test
        fun `function shadows other functions only with the same name and arity`() {
            // let f = [] -> Bool => true;
            // let f = [x: Bool] -> Bool => false;
            // (
            //      let f = [] -> Int => 1;
            //      f
            // );
            // f

            // given
            val fUse1 = variableUse("f")
            val fUse2 = variableUse("f")
            val fDef1 =
                unitFunctionDefinition(
                    "f",
                    listOf(),
                    lit(true),
                )
            val fDef2 =
                unitFunctionDefinition(
                    "f",
                    listOf(arg("x")),
                    lit(false),
                )
            val fDef3 =
                unitFunctionDefinition(
                    "f",
                    listOf(),
                    lit(1),
                )
            val ast =
                block(
                    fDef1,
                    fDef2,
                    block(
                        fDef3,
                        fUse1,
                    ),
                    fUse2,
                )

            // when
            val resolvedNames = resolveNames(ast, diagnostics)

            // then
            assertThatResolvedNames(resolvedNames)
                .hasOverloadSet(fUse1 to mapOf(0 to fDef3, 1 to fDef2))
                .hasOverloadSet(fUse2 to mapOf(0 to fDef1, 1 to fDef2))
                .andNothingElse()
            verify { diagnostics wasNot Called }
        }
    }

    @Nested
    inner class IncorrectCases {
        @Test
        fun `error when identifier is not defined`() {
            // let a = 0;
            // b

            // given
            val invalidUseRange = Location(10) to Location(10)
            val ast =
                block(
                    variableDeclaration("a"),
                    VariableUse(invalidUseRange, "b"),
                )

            // when
            val resolvedNames = resolveNames(ast, diagnostics)

            // then
            assertThatResolvedNames(resolvedNames)
                .andNothingElse()
            verify(exactly = 1) { diagnostics.report(NRDiagnostics.UnidentifiedIdentifier("b"), invalidUseRange) }
            confirmVerified(diagnostics)
        }

        @Test
        fun `error when identifier is defined after use`() {
            // a;
            // let a = 0

            // given
            val invalidUseRange = Location(1) to Location(5)
            val ast =
                block(
                    VariableUse(invalidUseRange, "a"),
                    variableDeclaration("a"),
                )

            // when
            val resolvedNames = resolveNames(ast, diagnostics)

            // then
            assertThatResolvedNames(resolvedNames)
                .andNothingElse()
            verify(exactly = 1) { diagnostics.report(NRDiagnostics.UnidentifiedIdentifier("a"), invalidUseRange) }
            confirmVerified(diagnostics)
        }

        @Test
        fun `declaration is not visible after block is closed`() {
            // (let a = true);
            // a

            // given
            val invalidUseRange = Location(2) to Location(5)
            val ast =
                block(
                    block(variableDeclaration("a")),
                    VariableUse(invalidUseRange, "a"),
                )

            // when
            val resolvedNames = resolveNames(ast, diagnostics)

            // then
            assertThatResolvedNames(resolvedNames)
                .andNothingElse()
            verify(exactly = 1) { diagnostics.report(NRDiagnostics.UnidentifiedIdentifier("a"), invalidUseRange) }
            confirmVerified(diagnostics)
        }

        @Test
        fun `declaration in function call argument expression is not visible after it is closed`() {
            // let f = [x: Int, y: Int] -> Bool => true
            // f[let x = 5, x];
            // x

            // given
            val invalidUseRange1 = Location(2) to Location(5)
            val invalidUseRange2 = Location(6) to Location(13)
            val ast =
                block(
                    unitFunctionDefinition(
                        "f",
                        listOf(
                            arg("x"),
                            arg("y"),
                        ),
                        lit(true),
                    ),
                    call(
                        "f",
                        variableDeclaration("x"),
                        VariableUse(invalidUseRange1, "x"),
                    ),
                    VariableUse(invalidUseRange2, "x"),
                )

            // when
            resolveNames(ast, diagnostics)

            // then
            verify(exactly = 1) { diagnostics.report(NRDiagnostics.UnidentifiedIdentifier("x"), invalidUseRange1) }
            verify(exactly = 1) { diagnostics.report(NRDiagnostics.UnidentifiedIdentifier("x"), invalidUseRange2) }
            confirmVerified(diagnostics)
        }

        @Test
        fun `declaration is not visible after if statement test expression`() {
            // if let x = 5 then x else x;
            // x

            // given
            val invalidUseRange1 = Location(2) to Location(5)
            val invalidUseRange2 = Location(6) to Location(13)
            val invalidUseRange3 = Location(16) to Location(31)
            val ast =
                block(
                    ifThenElse(
                        variableDeclaration("x"),
                        VariableUse(invalidUseRange1, "x"),
                        VariableUse(invalidUseRange2, "x"),
                    ),
                    VariableUse(invalidUseRange3, "x"),
                )

            // when
            val resolvedNames = resolveNames(ast, diagnostics)

            // then
            assertThatResolvedNames(resolvedNames)
                .andNothingElse()
            verify(exactly = 1) { diagnostics.report(NRDiagnostics.UnidentifiedIdentifier("x"), invalidUseRange1) }
            verify(exactly = 1) { diagnostics.report(NRDiagnostics.UnidentifiedIdentifier("x"), invalidUseRange2) }
            verify(exactly = 1) { diagnostics.report(NRDiagnostics.UnidentifiedIdentifier("x"), invalidUseRange3) }
            confirmVerified(diagnostics)
        }

        @Test
        fun `declaration is not visible after if statement do expression`() {
            // if true then let x = 5 else x;
            // x

            // given
            val invalidUseRange1 = Location(2) to Location(5)
            val invalidUseRange2 = Location(6) to Location(13)
            val ast =
                block(
                    ifThenElse(
                        lit(true),
                        variableDeclaration("x"),
                        VariableUse(invalidUseRange1, "x"),
                    ),
                    VariableUse(invalidUseRange2, "x"),
                )

            // when
            val resolvedNames = resolveNames(ast, diagnostics)

            // then
            assertThatResolvedNames(resolvedNames)
                .andNothingElse()
            verify(exactly = 1) { diagnostics.report(NRDiagnostics.UnidentifiedIdentifier("x"), invalidUseRange1) }
            verify(exactly = 1) { diagnostics.report(NRDiagnostics.UnidentifiedIdentifier("x"), invalidUseRange2) }
            confirmVerified(diagnostics)
        }

        @Test
        fun `declaration is not visible after if statement else expression`() {
            // if true then let true else let x = 5;
            // x

            // given
            val invalidUseRange1 = Location(2) to Location(5)
            val ast =
                block(
                    ifThenElse(
                        lit(true),
                        lit(true),
                        variableDeclaration("x"),
                    ),
                    VariableUse(invalidUseRange1, "x"),
                )

            // when
            val resolvedNames = resolveNames(ast, diagnostics)

            // then
            assertThatResolvedNames(resolvedNames)
                .andNothingElse()
            verify(exactly = 1) { diagnostics.report(NRDiagnostics.UnidentifiedIdentifier("x"), invalidUseRange1) }
            confirmVerified(diagnostics)
        }

        @Test
        fun `declaration is not visible after while test expression`() {
            // while let x = 5 do x;
            // x

            // given
            val invalidUseRange1 = Location(2) to Location(5)
            val invalidUseRange2 = Location(6) to Location(13)
            val ast =
                block(
                    whileLoop(
                        variableDeclaration("x"),
                        VariableUse(invalidUseRange1, "x"),
                    ),
                    VariableUse(invalidUseRange2, "x"),
                )

            // when
            val resolvedNames = resolveNames(ast, diagnostics)

            // then
            assertThatResolvedNames(resolvedNames)
                .andNothingElse()
            verify(exactly = 1) { diagnostics.report(NRDiagnostics.UnidentifiedIdentifier("x"), invalidUseRange1) }
            verify(exactly = 1) { diagnostics.report(NRDiagnostics.UnidentifiedIdentifier("x"), invalidUseRange2) }
            confirmVerified(diagnostics)
        }

        @Test
        fun `declaration is not visible after while body`() {
            // while true do let x = 5;
            // x

            // given
            val invalidUseRange1 = Location(2) to Location(5)
            val ast =
                block(
                    whileLoop(
                        lit(true),
                        variableDeclaration("x"),
                    ),
                    VariableUse(invalidUseRange1, "x"),
                )

            // when
            val resolvedNames = resolveNames(ast, diagnostics)

            // then
            assertThatResolvedNames(resolvedNames)
                .andNothingElse()
            verify(exactly = 1) { diagnostics.report(NRDiagnostics.UnidentifiedIdentifier("x"), invalidUseRange1) }
            confirmVerified(diagnostics)
        }

        @Test
        fun `declaration is not visible after return statement expression`() {
            // return let x = 5
            // x

            // given
            val invalidUseRange1 = Location(2) to Location(5)
            val ast =
                block(
                    returnStatement(variableDeclaration("x")),
                    VariableUse(invalidUseRange1, "x"),
                )

            // when
            val resolvedNames = resolveNames(ast, diagnostics)

            // then
            assertThatResolvedNames(resolvedNames)
                .andNothingElse()
            verify(exactly = 1) { diagnostics.report(NRDiagnostics.UnidentifiedIdentifier("x"), invalidUseRange1) }
            confirmVerified(diagnostics)
        }

        @Test
        fun `declaration is not visible after unary operator expression`() {
            // ~let x = 5;
            // x

            // given
            val invalidUseRange1 = Location(2) to Location(5)
            val ast =
                block(
                    lnot(variableDeclaration("x")),
                    VariableUse(invalidUseRange1, "x"),
                )

            // when
            val resolvedNames = resolveNames(ast, diagnostics)

            // then
            assertThatResolvedNames(resolvedNames)
                .andNothingElse()
            verify(exactly = 1) { diagnostics.report(NRDiagnostics.UnidentifiedIdentifier("x"), invalidUseRange1) }
            confirmVerified(diagnostics)
        }

        @Test
        fun `declaration is not visible after binary operator lhs expression`() {
            // let x=5+x; <- this is illegal either way, but always nice to check
            // x

            // given
            val invalidUseRange1 = Location(2) to Location(5)
            val invalidUseRange2 = Location(7) to Location(15)
            val ast =
                block(
                    variableDeclaration("x") add
                        VariableUse(invalidUseRange1, "x"),
                    VariableUse(invalidUseRange2, "x"),
                )

            // when
            val resolvedNames = resolveNames(ast, diagnostics)

            // then
            assertThatResolvedNames(resolvedNames)
                .andNothingElse()
            verify(exactly = 1) { diagnostics.report(NRDiagnostics.UnidentifiedIdentifier("x"), invalidUseRange1) }
            verify(exactly = 1) { diagnostics.report(NRDiagnostics.UnidentifiedIdentifier("x"), invalidUseRange2) }
            confirmVerified(diagnostics)
        }

        @Test
        fun `declaration is not visible after binary operator rhs expression`() {
            // 5+let x = 5; <- this is illegal either way, but always nice to check
            // x

            // given
            val invalidUseRange1 = Location(2) to Location(5)
            val ast =
                block(
                    lit(5) add
                        variableDeclaration("x"),
                    VariableUse(invalidUseRange1, "x"),
                )

            // when
            val resolvedNames = resolveNames(ast, diagnostics)

            // then
            assertThatResolvedNames(resolvedNames)
                .andNothingElse()
            verify(exactly = 1) { diagnostics.report(NRDiagnostics.UnidentifiedIdentifier("x"), invalidUseRange1) }
            confirmVerified(diagnostics)
        }

        @Test
        fun `error when finds argument of functional type`() {
            // let f = [x: () -> Int] -> Int => 0

            // given
            val functionalArgumentRange = Location(9) to Location(20)
            val ast =
                block(
                    unitFunctionDefinition(
                        "f",
                        listOf(
                            FunctionArgument(
                                functionalArgumentRange,
                                "x",
                                Type.Functional(mockRange(), listOf(), Type.Basic(mockRange(), "Int")),
                            ),
                        ),
                        lit(0),
                    ),
                )

            // when & then
            resolveNames(ast, diagnostics)
            verify(exactly = 1) { diagnostics.report(NRDiagnostics.IllegalFunctionalArgument("x"), functionalArgumentRange) }
            confirmVerified(diagnostics)
        }

        @Test
        fun `error when finds duplicated argument names`() {
            // let f = [x: Int, x: Bool] -> Int => 0

            // given
            val firstArgumentRange = Location(1) to Location(1)
            val secondArgumentRange = Location(2) to Location(2)
            val ast =
                block(
                    intFunctionDefinition(
                        "f",
                        listOf(
                            FunctionArgument(
                                firstArgumentRange,
                                "x",
                                Type.Basic(mockRange(), "Int"),
                            ),
                            FunctionArgument(
                                secondArgumentRange,
                                "x",
                                Type.Basic(mockRange(), "Int"),
                            ),
                        ),
                        lit(0),
                    ),
                )

            // when & then
            assertThatThrownBy { resolveNames(ast, diagnostics) }.isInstanceOf(Throwable::class.java)
            verify(exactly = 1) {
                diagnostics.report(NRDiagnostics.DuplicatedFunctionArgument("x"), firstArgumentRange)
                diagnostics.report(NRDiagnostics.DuplicatedFunctionArgument("x"), secondArgumentRange)
                diagnostics.fatal()
            }
            confirmVerified(diagnostics)
        }
    }
}
