package cacophony.controlflow

import cacophony.semantic.AnalyzedFunction
import cacophony.semantic.syntaxtree.Definition
import cacophony.semantic.syntaxtree.Empty
import cacophony.semantic.syntaxtree.Type
import cacophony.utils.Location
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class FunctionHandlerImplTest {
    val mockRange = Location(0) to Location(0)

    val mockFunctionDeclaration =
        Definition.FunctionDeclaration(
            mockRange,
            "f",
            null,
            listOf(),
            Type.Basic(mockRange, "Int"),
            Empty(mockRange),
        )

    val mockAnalyzedFunction =
        AnalyzedFunction(
            null,
            setOf(),
            mutableSetOf(),
            0,
            setOf(),
        )

    @Test
    fun `returns correct function declaration`() {
        // given
        val functionHandler = FunctionHandlerImpl(mockFunctionDeclaration, mockAnalyzedFunction, listOf())

        // when
        val declaration = functionHandler.getFunctionDeclaration()

        // then
        assertThat(declaration).isEqualTo(mockFunctionDeclaration)
    }

    @Nested
    inner class GenerateVariableAccess {
        @Test
        fun `generates variable access to its own source variable allocated in a register`() {
            // let f = [] -> Int => (
            //     let x: Int = 10; #allocated in virtual register
            //     x #variable access
            // )
            TODO("Not yet implemented")
        }

        @Test
        fun `generates variable access to its own source variable allocated on stack`() {
            // let f = [] -> Int => (
            //     let x: Int = 10; #allocated on stack
            //     x #variable access
            // )
            TODO("Not yet implemented")
        }

        @Test
        fun `generates variable access to source variable of ancestor function`() {
            // let h = [] -> Int => (
            //     let x: Int = 10; #allocated on stack
            //     let g = [] -> Int => (
            //         let f = [] -> Int => (
            //             x #variable access
            //         );
            //     );
            // )
            TODO("Not yet implemented")
        }

        @Test
        fun `generates variable access to its own static link`() {
            // let g = [] -> Int => (
            //     let f = [] -> Int => 42 #request to frame pointer of g
            // )
            TODO("Not yet implemented")
        }

        @Test
        fun `generates variable access to static link of its ancestor`() {
            // let h = [] -> Int => (
            //     let g = [] -> Int => (
            //         let f = [] -> Int => 42 #request to frame pointer of h
            //                                 #should recursively request frame pointer of g
            //     );
            // )
            TODO("Not yet implemented")
        }

        @Test
        fun `throws if requested access to variable that is not accessible`() {
            // let f = [] -> Int => 42
            // let x: Int = 10 #request to access to this variable in f
            TODO("Not yet implemented")
        }
    }
}
