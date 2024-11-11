package cacophony.semantic

import cacophony.diagnostics.Diagnostics
import cacophony.diagnostics.NRDiagnostics
import cacophony.semantic.ResolvedName.Argument
import cacophony.semantic.ResolvedName.Function
import cacophony.semantic.ResolvedName.Variable
import cacophony.semantic.syntaxtree.*
import cacophony.semantic.syntaxtree.Definition.FunctionArgument
import cacophony.semantic.syntaxtree.Definition.FunctionDeclaration
import cacophony.semantic.syntaxtree.Definition.VariableDeclaration
import cacophony.semantic.syntaxtree.Literal.BoolLiteral
import cacophony.semantic.syntaxtree.Literal.IntLiteral
import cacophony.semantic.syntaxtree.Statement.IfElseStatement
import cacophony.semantic.syntaxtree.Statement.ReturnStatement
import cacophony.semantic.syntaxtree.Statement.WhileStatement
import cacophony.semantic.syntaxtree.Type.Basic
import cacophony.semantic.syntaxtree.Type.Functional
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
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class NameResolverTest {
    val mockRange = Location(0) to Location(0)
    private val mockValue = Empty(mockRange)

    private fun variableDeclaration(name: String): VariableDeclaration = VariableDeclaration(mockRange, name, null, mockValue)

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
                val aDef = variableDeclaration("a")
                val bDef = variableDeclaration("b")
                val aUse = VariableUse(mockRange, "a")
                val bUse = VariableUse(mockRange, "b")
                val ast =
                    Block(
                        mockRange,
                        listOf(aDef, bDef, aUse, bUse),
                    )

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
                val aUse = VariableUse(mockRange, "a")
                val ast =
                    Block(
                        mockRange,
                        listOf(aDef, VariableDeclaration(mockRange, "b", null, aUse)),
                    )

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
                val xDef = FunctionArgument(mockRange, "x", Basic(mockRange, "Bool"))
                val yDef = FunctionArgument(mockRange, "y", Basic(mockRange, "Int"))
                val xUse = VariableUse(mockRange, "x")
                val yUse = VariableUse(mockRange, "y")
                val ast =
                    Block(
                        mockRange,
                        listOf(
                            FunctionDeclaration(
                                mockRange,
                                "f",
                                null,
                                listOf(xDef, yDef),
                                Basic(mockRange, "Int"),
                                Block(
                                    mockRange,
                                    listOf(
                                        xUse,
                                        yUse,
                                    ),
                                ),
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
                    FunctionDeclaration(
                        mockRange,
                        "f",
                        null,
                        listOf(),
                        Basic(mockRange, "Bool"),
                        BoolLiteral(mockRange, false),
                    )
                val gDef =
                    FunctionDeclaration(
                        mockRange,
                        "g",
                        null,
                        listOf(),
                        Basic(mockRange, "Int"),
                        IntLiteral(mockRange, 0),
                    )
                val fUse = VariableUse(mockRange, "f")
                val gUse = VariableUse(mockRange, "g")
                val ast =
                    Block(
                        mockRange,
                        listOf(fDef, gDef, gUse, fUse),
                    )

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
                val fUse = VariableUse(mockRange, "f")
                val fDef =
                    FunctionDeclaration(
                        mockRange,
                        "f",
                        null,
                        listOf(),
                        Basic(mockRange, "Bool"),
                        BoolLiteral(mockRange, true),
                    )
                val ast =
                    Block(
                        mockRange,
                        listOf(
                            fDef,
                            FunctionCall(mockRange, fUse, listOf()),
                        ),
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
                val fUse = VariableUse(mockRange, "f")
                val fDef =
                    FunctionDeclaration(
                        mockRange,
                        "f",
                        null,
                        listOf(),
                        Basic(mockRange, "Bool"),
                        FunctionCall(
                            mockRange,
                            fUse,
                            listOf(),
                        ),
                    )
                val ast = Block(mockRange, listOf(fDef))

                // when
                val resolvedNames = resolveNames(ast, diagnostics)

                // then
                assertThatResolvedNames(resolvedNames)
                    .hasOverloadSet(fUse to mapOf(0 to fDef))
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
                val aUse = VariableUse(mockRange, "a")
                val ast = Block(mockRange, listOf(aDef1, aDef2, aUse))

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
                val aUse1 = VariableUse(mockRange, "a")
                val aUse2 = VariableUse(mockRange, "a")
                val ast =
                    Block(
                        mockRange,
                        listOf(
                            aDef1,
                            Block(mockRange, listOf(aDef2, aUse1)),
                            aUse2,
                        ),
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
                val fUse = VariableUse(mockRange, "f")
                val fDef1 =
                    FunctionDeclaration(
                        mockRange,
                        "f",
                        null,
                        listOf(),
                        Basic(mockRange, "Bool"),
                        BoolLiteral(mockRange, false),
                    )
                val fDef2 =
                    FunctionDeclaration(
                        mockRange,
                        "f",
                        null,
                        listOf(),
                        Basic(mockRange, "Bool"),
                        BoolLiteral(mockRange, true),
                    )
                val ast = Block(mockRange, listOf(fDef1, fDef2, fUse))

                // when
                val resolvedNames = resolveNames(ast, diagnostics)

                // then
                assertThatResolvedNames(resolvedNames)
                    .hasOverloadSet(fUse to mapOf(0 to fDef2))
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
                val fUse1 = VariableUse(mockRange, "f")
                val fUse2 = VariableUse(mockRange, "f")
                val fDef1 =
                    FunctionDeclaration(
                        mockRange,
                        "f",
                        null,
                        listOf(),
                        Basic(mockRange, "Bool"),
                        BoolLiteral(mockRange, false),
                    )
                val fDef2 =
                    FunctionDeclaration(
                        mockRange,
                        "f",
                        null,
                        listOf(),
                        Basic(mockRange, "Int"),
                        IntLiteral(mockRange, 0),
                    )
                val ast =
                    Block(
                        mockRange,
                        listOf(
                            fDef1,
                            Block(mockRange, listOf(fDef2, fUse1)),
                            fUse2,
                        ),
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
                val fUse1 = VariableUse(mockRange, "f")
                val fUse2 = VariableUse(mockRange, "f")
                val fDef2 =
                    FunctionDeclaration(
                        mockRange,
                        "f",
                        null,
                        listOf(),
                        Basic(mockRange, "Int"),
                        IntLiteral(mockRange, 0),
                    )
                val fDef1 =
                    FunctionDeclaration(
                        mockRange,
                        "f",
                        null,
                        listOf(),
                        Basic(mockRange, "Bool"),
                        Block(mockRange, listOf(fDef2, fUse1)),
                    )
                val ast =
                    Block(
                        mockRange,
                        listOf(fDef1, fUse2),
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
            fun `arguments shadow arguments`() {
                // let f = [x: Int] -> Int => (
                //      let g = [x: Bool] -> Bool => x;
                //      x
                // )

                // given
                val xUse1 = VariableUse(mockRange, "x")
                val xUse2 = VariableUse(mockRange, "x")
                val xDef1 = FunctionArgument(mockRange, "x", Basic(mockRange, "Int"))
                val xDef2 = FunctionArgument(mockRange, "x", Basic(mockRange, "Bool"))
                val gDef =
                    FunctionDeclaration(
                        mockRange,
                        "g",
                        null,
                        listOf(xDef2),
                        Basic(mockRange, "Bool"),
                        xUse1,
                    )
                val fDef =
                    FunctionDeclaration(
                        mockRange,
                        "f",
                        null,
                        listOf(xDef1),
                        Basic(mockRange, "Int"),
                        Block(mockRange, listOf(gDef, xUse2)),
                    )
                val ast =
                    Block(
                        mockRange,
                        listOf(fDef),
                    )

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
                val xUse1 = VariableUse(mockRange, "x")
                val xUse2 = VariableUse(mockRange, "x")
                val xDef1 = FunctionArgument(mockRange, "x", Basic(mockRange, "Int"))
                val xDef2 = variableDeclaration("x")
                val fDef =
                    FunctionDeclaration(
                        mockRange,
                        "f",
                        null,
                        listOf(xDef1),
                        Basic(mockRange, "Bool"),
                        Block(
                            mockRange,
                            listOf(
                                Block(mockRange, listOf(xDef2, xUse1)),
                                xUse2,
                            ),
                        ),
                    )
                val ast =
                    Block(
                        mockRange,
                        listOf(fDef),
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
                val xUse1 = VariableUse(mockRange, "x")
                val xUse2 = VariableUse(mockRange, "x")
                val xDef1 = variableDeclaration("x")
                val xDef2 = FunctionArgument(mockRange, "x", Basic(mockRange, "Int"))
                val fDef =
                    FunctionDeclaration(
                        mockRange,
                        "f",
                        null,
                        listOf(xDef2),
                        Basic(mockRange, "Int"),
                        xUse1,
                    )
                val ast =
                    Block(
                        mockRange,
                        listOf(xDef1, fDef, xUse2),
                    )

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
                val fUse1 = VariableUse(mockRange, "f")
                val fUse2 = VariableUse(mockRange, "f")
                val fDef1 =
                    FunctionDeclaration(
                        mockRange,
                        "f",
                        null,
                        listOf(),
                        Basic(mockRange, "Bool"),
                        BoolLiteral(mockRange, true),
                    )
                val fDef2 = variableDeclaration("f")
                val ast =
                    Block(
                        mockRange,
                        listOf(
                            fDef1,
                            Block(mockRange, listOf(fDef2, fUse1)),
                            fUse2,
                        ),
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
                val fUse1 = VariableUse(mockRange, "f")
                val fUse2 = VariableUse(mockRange, "f")
                val fDef2 = variableDeclaration("f")
                val fDef1 =
                    FunctionDeclaration(
                        mockRange,
                        "f",
                        null,
                        listOf(),
                        Basic(mockRange, "Bool"),
                        Block(mockRange, listOf(fDef2, fUse1)),
                    )
                val ast =
                    Block(
                        mockRange,
                        listOf(fDef1, fUse2),
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
            fun `functions shadow variables`() {
                // let a = 0;
                // (
                //      let a = [] -> Bool => true;
                //      a
                // );
                // a

                // given
                val aUse1 = VariableUse(mockRange, "a")
                val aUse2 = VariableUse(mockRange, "a")
                val aDef1 = variableDeclaration("a")
                val aDef2 =
                    FunctionDeclaration(
                        mockRange,
                        "a",
                        null,
                        listOf(),
                        Basic(mockRange, "Bool"),
                        BoolLiteral(mockRange, true),
                    )
                val ast =
                    Block(
                        mockRange,
                        listOf(
                            aDef1,
                            Block(mockRange, listOf(aDef2, aUse1)),
                            aUse2,
                        ),
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
                val fUse1 = VariableUse(mockRange, "f")
                val fUse2 = VariableUse(mockRange, "f")
                val fDef1 =
                    FunctionDeclaration(
                        mockRange,
                        "f",
                        null,
                        listOf(),
                        Basic(mockRange, "Bool"),
                        BoolLiteral(mockRange, true),
                    )
                val fDef2 = FunctionArgument(mockRange, "f", Basic(mockRange, "Int"))
                val ast =
                    Block(
                        mockRange,
                        listOf(
                            fDef1,
                            FunctionDeclaration(
                                mockRange,
                                "g",
                                null,
                                listOf(fDef2),
                                Basic(mockRange, "Int"),
                                fUse1,
                            ),
                            fUse2,
                        ),
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
                val fUse1 = VariableUse(mockRange, "f")
                val fUse2 = VariableUse(mockRange, "f")
                val fDef2 = FunctionArgument(mockRange, "f", Basic(mockRange, "Bool"))
                val fDef1 =
                    FunctionDeclaration(
                        mockRange,
                        "f",
                        null,
                        listOf(fDef2),
                        Basic(mockRange, "Bool"),
                        fUse1,
                    )
                val ast =
                    Block(
                        mockRange,
                        listOf(fDef1, fUse2),
                    )

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
                val xUse1 = VariableUse(mockRange, "x")
                val xUse2 = VariableUse(mockRange, "x")
                val xDef1 = FunctionArgument(mockRange, "x", Basic(mockRange, "Int"))
                val xDef2 =
                    FunctionDeclaration(
                        mockRange,
                        "x",
                        null,
                        listOf(),
                        Basic(mockRange, "Bool"),
                        BoolLiteral(mockRange, true),
                    )
                val ast =
                    Block(
                        mockRange,
                        listOf(
                            FunctionDeclaration(
                                mockRange,
                                "f",
                                null,
                                listOf(xDef1),
                                Basic(mockRange, "Int"),
                                Block(
                                    mockRange,
                                    listOf(
                                        Block(mockRange, listOf(xDef2, xUse1)),
                                        xUse2,
                                    ),
                                ),
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
            val fUse = VariableUse(mockRange, "f")
            val fDef1 =
                FunctionDeclaration(
                    mockRange,
                    "f",
                    null,
                    listOf(),
                    Basic(mockRange, "Bool"),
                    BoolLiteral(mockRange, true),
                )
            val fDef2 =
                FunctionDeclaration(
                    mockRange,
                    "f",
                    null,
                    listOf(FunctionArgument(mockRange, "x", Basic(mockRange, "Bool"))),
                    Basic(mockRange, "Bool"),
                    BoolLiteral(mockRange, false),
                )
            val ast = Block(mockRange, listOf(fDef1, fDef2, fUse))

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
            val fUse1 = VariableUse(mockRange, "f")
            val fUse2 = VariableUse(mockRange, "f")
            val fDef1 =
                FunctionDeclaration(
                    mockRange,
                    "f",
                    null,
                    listOf(),
                    Basic(mockRange, "Bool"),
                    BoolLiteral(mockRange, true),
                )
            val fDef2 =
                FunctionDeclaration(
                    mockRange,
                    "f",
                    null,
                    listOf(FunctionArgument(mockRange, "x", Basic(mockRange, "Bool"))),
                    Basic(mockRange, "Bool"),
                    BoolLiteral(mockRange, false),
                )
            val fDef3 =
                FunctionDeclaration(
                    mockRange,
                    "f",
                    null,
                    listOf(),
                    Basic(mockRange, "Int"),
                    IntLiteral(mockRange, 1),
                )
            val ast =
                Block(
                    mockRange,
                    listOf(
                        fDef1,
                        fDef2,
                        Block(
                            mockRange,
                            listOf(
                                fDef3,
                                fUse1,
                            ),
                        ),
                        fUse2,
                    ),
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
                Block(
                    mockRange,
                    listOf(
                        variableDeclaration("a"),
                        VariableUse(invalidUseRange, "b"),
                    ),
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
                Block(
                    mockRange,
                    listOf(
                        VariableUse(invalidUseRange, "a"),
                        variableDeclaration("a"),
                    ),
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
                Block(
                    mockRange,
                    listOf(
                        Block(mockRange, listOf(variableDeclaration("a"))),
                        VariableUse(invalidUseRange, "a"),
                    ),
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
                Block(
                    mockRange,
                    listOf(
                        FunctionDeclaration(
                            mockRange,
                            "f",
                            null,
                            listOf(
                                FunctionArgument(mockRange, "x", Basic(mockRange, "Int")),
                                FunctionArgument(mockRange, "y", Basic(mockRange, "Int")),
                            ),
                            Basic(mockRange, "Bool"),
                            BoolLiteral(mockRange, true),
                        ),
                        FunctionCall(
                            mockRange,
                            VariableUse(mockRange, "f"),
                            listOf(
                                variableDeclaration("x"),
                                VariableUse(invalidUseRange1, "x"),
                            ),
                        ),
                        VariableUse(invalidUseRange2, "x"),
                    ),
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
                Block(
                    mockRange,
                    listOf(
                        IfElseStatement(
                            mockRange,
                            variableDeclaration("x"),
                            VariableUse(invalidUseRange1, "x"),
                            VariableUse(invalidUseRange2, "x"),
                        ),
                        VariableUse(invalidUseRange3, "x"),
                    ),
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
                Block(
                    mockRange,
                    listOf(
                        IfElseStatement(
                            mockRange,
                            BoolLiteral(mockRange, true),
                            variableDeclaration("x"),
                            VariableUse(invalidUseRange1, "x"),
                        ),
                        VariableUse(invalidUseRange2, "x"),
                    ),
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
                Block(
                    mockRange,
                    listOf(
                        IfElseStatement(
                            mockRange,
                            BoolLiteral(mockRange, true),
                            BoolLiteral(mockRange, true),
                            variableDeclaration("x"),
                        ),
                        VariableUse(invalidUseRange1, "x"),
                    ),
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
                Block(
                    mockRange,
                    listOf(
                        WhileStatement(
                            mockRange,
                            variableDeclaration("x"),
                            VariableUse(invalidUseRange1, "x"),
                        ),
                        VariableUse(invalidUseRange2, "x"),
                    ),
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
                Block(
                    mockRange,
                    listOf(
                        WhileStatement(
                            mockRange,
                            BoolLiteral(mockRange, true),
                            variableDeclaration("x"),
                        ),
                        VariableUse(invalidUseRange1, "x"),
                    ),
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
                Block(
                    mockRange,
                    listOf(
                        ReturnStatement(mockRange, variableDeclaration("x")),
                        VariableUse(invalidUseRange1, "x"),
                    ),
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
                Block(
                    mockRange,
                    listOf(
                        OperatorUnary.Negation(mockRange, variableDeclaration("x")),
                        VariableUse(invalidUseRange1, "x"),
                    ),
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
                Block(
                    mockRange,
                    listOf(
                        OperatorBinary.Addition(
                            mockRange,
                            variableDeclaration("x"),
                            VariableUse(invalidUseRange1, "x"),
                        ),
                        VariableUse(invalidUseRange2, "x"),
                    ),
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
                Block(
                    mockRange,
                    listOf(
                        OperatorBinary.Addition(
                            mockRange,
                            IntLiteral(mockRange, 5),
                            variableDeclaration("x"),
                        ),
                        VariableUse(invalidUseRange1, "x"),
                    ),
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
                Block(
                    mockRange,
                    listOf(
                        FunctionDeclaration(
                            mockRange,
                            "f",
                            null,
                            listOf(
                                FunctionArgument(
                                    functionalArgumentRange,
                                    "x",
                                    Functional(mockRange, listOf(), Basic(mockRange, "Int")),
                                ),
                            ),
                            Basic(mockRange, "Int"),
                            IntLiteral(mockRange, 0),
                        ),
                    ),
                )

            // when & then
            resolveNames(ast, diagnostics)
            verify(exactly = 1) { diagnostics.report(NRDiagnostics.IllegalFunctionalArgument("x"), functionalArgumentRange) }
            confirmVerified(diagnostics)
        }
    }
}
