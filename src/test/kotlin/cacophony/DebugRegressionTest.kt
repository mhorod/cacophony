package cacophony

import cacophony.codegen.allocateRegisters
import cacophony.codegen.analyzeRegistersInteraction
import cacophony.codegen.linearization.linearize
import cacophony.controlflow.functions.SimpleCallGenerator
import cacophony.pipeline.Params
import cacophony.semantic.rtti.createObjectOutlines
import cacophony.semantic.rtti.generateClosureOutlines
import cacophony.utils.StringInput
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

// TODO: Once the debugging dust settles, these should be converted to proper tests in their appropriate domains
class DebugRegressionTest {
    @Test
    fun `return with function call generates asm`() {
        testPipeline().generateAsm(
            StringInput(
                """
                let f = [x: Int] -> Int => x;
                return f[1];
                """.trimIndent(),
            ),
        )

        testPipeline().generateAsm(
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
        testPipeline().generateAsm(
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
        testPipeline().generateAsm(
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
        testPipeline().generateAsm(
            StringInput(
                """                
                let f = [] -> Bool => true;
                if f[] then 0 else 0
                """.trimIndent(),
            ),
        )
    }

    @Test
    fun `missing assign from memory pattern`() {
        testPipeline().generateAsm(
            StringInput(
                """
                let g = [] -> Bool => (
                    return g[]
                );
                """.trimIndent(),
            ),
        )
    }

    @Test
    fun `variable as condition`() {
        testPipeline().generateAsm(
            StringInput(
                """
                let x: Bool = true;
                if x then ();
                """.trimIndent(),
            ),
        )
    }

    @Test
    fun `double assignment`() {
        testPipeline().generateAsm(
            StringInput(
                """
                let x: Bool = true;
                let y: Bool = true;
                x = y = false;
                """.trimIndent(),
            ),
        )
    }

    @Test
    fun `return used in if condition generates asm`() {
        testPipeline().generateAsm(
            StringInput(
                """                
                if return 42 then 2 else 3
                """.trimIndent(),
            ),
        )
    }

    @Test
    fun `return used in while condition generates asm`() {
        testPipeline().generateAsm(
            StringInput(
                """                
                while return 42 do ();
                """.trimIndent(),
            ),
        )
    }

    @Test
    fun `break used in if condition generates asm`() {
        testPipeline().generateAsm(
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
        testPipeline().generateAsm(
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

    @Test
    fun `simple function call does not cause spills`() {
        val ast =
            testPipeline().generateAst(
                StringInput(
                    """                
                    let f = [] -> Bool => true;
                    f[]
                    """.trimIndent(),
                ),
            )

        val pipeline = testPipeline()
        val semantics = pipeline.analyzeAst(ast)
        val objectOutlines =
            createObjectOutlines(
                pipeline.getUsedTypes(semantics.types),
            )
        val cfg =
            pipeline.generateControlFlowGraph(
                pipeline.analyzeAst(ast),
                SimpleCallGenerator(),
                objectOutlines.locations,
                generateClosureOutlines(semantics.lambdaHandlers),
            )
        val covering = cfg.mapValues { (_, cfg) -> linearize(cfg, Params.instructionCovering) }
        val registersInteractions = analyzeRegistersInteraction(covering)
        val allocation = allocateRegisters(registersInteractions, Params.allGPRs)

        allocation.values.forEach {
            assertThat(it.spills).isEmpty()
        }
    }

    @Test
    fun `simple function with spills`() {
        testPipeline().generateAsm(
            StringInput(
                """
                let f = [x: Int] -> Int => (
                    let a = 1;
                    let b = 2;
                    let c = 3;
                    let d = 4;
                    let e = 5;
                    let f = 6;
                    let g = 7;
                    let h = 8;
                    let i = 9;
                    let j = 10;
                    let k = 11;
                    let l = 12;
                    let m = 13;
                    let n = 14;
                    let o = 15;
                    let p = 16;
                    let r = 17;
                    let s = 18;
                    let t = 19;
                    return a+b+c+d+e+f+g+h+i+j+k+l+m+n+o+p+r+s+t;
                );
                return f[1];
                """.trimIndent(),
            ),
        )
    }
}
