package cacophony.semantic.analysis

import cacophony.block
import cacophony.controlflow.Variable
import cacophony.intArg
import cacophony.intFunctionDefinition
import cacophony.intType
import cacophony.lit
import cacophony.lvalueFieldRef
import cacophony.rvalueFieldRef
import cacophony.struct
import cacophony.structType
import cacophony.testPipeline
import cacophony.typedArg
import cacophony.variableDeclaration
import cacophony.variableUse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.Map.entry

class VariablesMapCreationTest {
    @Test
    fun `variable is created for let definitions`() {
        // given
        val xDef = variableDeclaration("x", lit(1))
        val xUse = variableUse("x")
        val ast = block(xDef, xUse)

        // when
        val variables = testPipeline().createVariables(ast)

        // then
        assertThat(variables.definitions).containsKeys(xDef)
        assertThat(variables.lvalues).contains(entry(xUse, variables.definitions[xDef]))
    }

    @Test
    fun `primitive variable is created for function argument`() {
        // given
        val xArg = intArg("x")
        val xUse = variableUse("x")
        val fDef = intFunctionDefinition("f", listOf(xArg), xUse)

        // when
        val variables = testPipeline().createVariables(fDef)

        // then
        assertThat(variables.definitions).containsKeys(xArg)
        assertThat(variables.lvalues).contains(entry(xUse, variables.definitions[xArg]))
    }

    @Test
    fun `struct variable is created for struct function argument`() {
        // given
        val xArg = typedArg("x", structType("a" to intType()))
        val xUse = variableUse("x")
        val aUse = lvalueFieldRef(xUse, "a")
        val fDef = intFunctionDefinition("f", listOf(xArg), aUse)

        // when
        val variables = testPipeline().createVariables(fDef)

        // then
        assertThat(variables.definitions).containsKeys(xArg)
        assertThat(variables.lvalues).contains(entry(xUse, variables.definitions[xArg]))
        assertThat(variables.lvalues).contains(entry(aUse, field(variables.definitions[xArg], "a")))
    }

    @Test
    fun `struct variable is created for simple struct`() {
        // given
        val xDef = variableDeclaration("x", struct("a" to lit(1)))
        val xUse1 = variableUse("x")
        val xUse2 = variableUse("x")
        val aUse = lvalueFieldRef(xUse2, "a")
        val ast = block(xDef, xUse1, aUse)

        // when
        val variables = testPipeline().createVariables(ast)

        // then
        assertThat(variables.definitions).containsKeys(xDef)
        assertThat(variables.lvalues).contains(entry(xUse1, variables.definitions[xDef]))
        assertThat(variables.lvalues).contains(entry(xUse2, variables.definitions[xDef]))
        assertThat(variables.lvalues).contains(entry(aUse, field(variables.definitions[xDef], "a")))
    }

    @Test
    fun `struct variable is created for nested struct`() {
        // given
        val xDef = variableDeclaration("x", struct("a" to struct("b" to lit(1))))
        val xUse1 = variableUse("x")
        val xUse2 = variableUse("x")
        val aUse = lvalueFieldRef(xUse2, "a")
        val bUse = lvalueFieldRef(aUse, "b")
        val ast = block(xDef, xUse1, bUse)

        // when
        val variables = testPipeline().createVariables(ast)

        // then
        assertThat(variables.definitions).containsKeys(xDef)
        assertThat(variables.lvalues).contains(entry(xUse1, variables.definitions[xDef]))
        assertThat(variables.lvalues).contains(entry(xUse2, variables.definitions[xDef]))
        assertThat(variables.lvalues).contains(entry(aUse, field(variables.definitions[xDef], "a")))
        assertThat(variables.lvalues).contains(entry(bUse, field(field(variables.definitions[xDef], "a"), "b")))
    }

    @Test
    fun `variable is not created for rvalue struct field access`() {
        // given
        val access = rvalueFieldRef(struct("a" to lit(1)), "a")

        // when
        val variables = testPipeline().createVariables(access)

        // then
        assertThat(variables.definitions).isEmpty()
        assertThat(variables.lvalues).isEmpty()
    }

    private fun field(variable: Variable?, vararg names: String): Variable {
        var current = variable!!
        for (name in names) {
            current = (current as Variable.StructVariable).fields[name]!!
        }
        return current
    }
}
