package cacophony

import cacophony.controlflow.generation.CFGGenerationTest.Companion.pipeline
import cacophony.utils.StringInput
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

// TODO: Once the debugging dust settles, these should be converted to proper tests in their appropriate domains
class DebugRegressionTest {
    @Test
    fun `return with function call generates asm`() {
        pipeline.generateAsm(
            StringInput(
                """
                let f = [x: Int] -> Int => x;
                return f[1];
                """.trimIndent(),
            ),
        )

        pipeline.generateAsm(
            StringInput(
                """
                let f = [a: Int] -> Int => if a == 0 then 0 else a + f[a - 1];
                return f[5]
                """.trimIndent(),
            ),
        )
    }

    @Test
    fun `function call used as while condition generates asm`() {
        pipeline.generateAsm(
            StringInput(
                """                
                let keep_going = [] -> Bool => true;
                let say_hello = [] -> Unit => ();
                let x = true;
                while keep_going[] do
                say_hello[];
                """.trimIndent(),
            ),
        )
    }

    @Test
    fun `function call used as while condition generates asm if while contains break`() {
        pipeline.generateAsm(
            StringInput(
                """                
                let f = [] -> Bool => false;
                while f[] do
                    break;
                """.trimIndent(),
            ),
        )
    }

    @Test
    fun `function call used as if condition generates asm`() {
        pipeline.generateAsm(
            StringInput(
                """                
                let f = [] -> Bool => true;
                if f[] then 0 else 0
                """.trimIndent(),
            ),
        )
    }

    @Test
    fun `simple function call does not cause spills`() {
        val ast =
            pipeline.generateAST(
                StringInput(
                    """                
                    let f = [] -> Bool => true;
                    f[]
                    """.trimIndent(),
                ),
            )

        val liveness = pipeline.analyzeLiveness(ast)
        val allocation = pipeline.allocateRegisters(liveness)

        allocation.values.forEach {
            assertThat(it.spills).isEmpty()
        }
    }
}
