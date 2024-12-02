package cacophony

import cacophony.controlflow.generation.CFGGenerationTest.Companion.pipeline
import cacophony.utils.StringInput
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
    fun `return used in if condition generates asm`() {
        pipeline.generateAsm(
            StringInput(
                """                
                if return () then 2 else 3
                """.trimIndent(),
            ),
        )
    }

    @Test
    fun `return used in while condition generates asm`() {
        pipeline.generateAsm(
            StringInput(
                """                
                while return () do ();
                """.trimIndent(),
            ),
        )
    }

    @Test
    fun `break used in if condition generates asm`() {
        pipeline.generateAsm(
            StringInput(
                """                
                let c = 1;
                while c == 1 do (
                    while true && break do ();
                );
                """.trimIndent(),
            ),
        )
    }

    @Test
    fun `break used in while condition generates asm`() {
        pipeline.generateAsm(
            StringInput(
                """                
                let c = 1;
                while c == 1 do (
                    if c == 2 || break then 3 else 4
                );
                """.trimIndent(),
            ),
        )
    }
}
