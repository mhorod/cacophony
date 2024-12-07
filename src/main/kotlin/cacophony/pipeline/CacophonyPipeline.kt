package cacophony.pipeline

import cacophony.codegen.functionBodyLabel
import cacophony.codegen.instructions.CacophonyInstructionCovering
import cacophony.codegen.instructions.generateAsm
import cacophony.codegen.instructions.matching.CacophonyInstructionMatcher
import cacophony.codegen.linearization.LoweredCFGFragment
import cacophony.codegen.linearization.linearize
import cacophony.codegen.registers.Liveness
import cacophony.codegen.registers.RegisterAllocation
import cacophony.codegen.registers.adjustLoweredCFGToHandleSpills
import cacophony.controlflow.HardwareRegister
import cacophony.controlflow.Register
import cacophony.controlflow.functions.FunctionHandler
import cacophony.controlflow.functions.SystemVAMD64CallConvention
import cacophony.controlflow.functions.generateFunctionHandlers
import cacophony.controlflow.generation.ProgramCFG
import cacophony.controlflow.generation.generateCFG
import cacophony.diagnostics.Diagnostics
import cacophony.grammars.ParseTree
import cacophony.lexer.CacophonyLexer
import cacophony.parser.CacophonyGrammarSymbol
import cacophony.parser.CacophonyParser
import cacophony.semantic.analysis.*
import cacophony.semantic.names.*
import cacophony.semantic.syntaxtree.AST
import cacophony.semantic.syntaxtree.Definition.FunctionDeclaration
import cacophony.semantic.syntaxtree.generateAST
import cacophony.semantic.types.TypeCheckingResult
import cacophony.semantic.types.checkTypes
import cacophony.token.Token
import cacophony.token.TokenCategorySpecific
import cacophony.utils.CompileException
import cacophony.utils.Input
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.writeLines

data class AstAnalysisResult(
    val resolvedVariables: ResolvedVariables,
    val types: TypeCheckingResult,
    val analyzedExpressions: UseTypeAnalysisResult,
    val functionHandlers: Map<FunctionDeclaration, FunctionHandler>,
)

class CacophonyPipeline(
    val diagnostics: Diagnostics,
    private val logger: CacophonyLogger? = null,
    private val lexer: CacophonyLexer = cachedLexer,
    private val parser: CacophonyParser = cachedParser,
    private val instructionCovering: CacophonyInstructionCovering = cachedInstructionCovering,
) {
    companion object {
        private val cachedLexer = CacophonyLexer()
        private val cachedParser = CacophonyParser()
        private val cachedInstructionCovering = CacophonyInstructionCovering(CacophonyInstructionMatcher())
        private val allGPRs = HardwareRegister.entries.toSet()
    }

    private fun <T> assertEmptyDiagnosticsAfter(action: () -> T): T {
        val x = action()
        if (diagnostics.getErrors().isNotEmpty()) {
            throw diagnostics.fatal()
        }
        return x
    }

    // run the full pipeline
    fun process(input: Input): FunctionAnalysisResult = analyzeFunctions(input)

    private fun lex(input: Input): List<Token<TokenCategorySpecific>> {
        val tokens =
            try {
                assertEmptyDiagnosticsAfter { lexer.process(input, diagnostics) }
            } catch (e: CompileException) {
                logger?.logFailedLexing()
                throw e
            }
        logger?.logSuccessfulLexing(tokens)
        return tokens
    }

    private fun parse(input: Input): ParseTree<CacophonyGrammarSymbol> {
        val terminals = lex(input).map { token -> ParseTree.Leaf(CacophonyGrammarSymbol.fromLexerToken(token)) }
        val parseTree =
            try {
                assertEmptyDiagnosticsAfter { parser.process(terminals, diagnostics) }
            } catch (e: CompileException) {
                logger?.logFailedParsing()
                throw e
            }
        logger?.logSuccessfulParsing(parseTree)
        return parseTree
    }

    fun generateAST(input: Input): AST {
        val parseTree = parse(input)
        val ast =
            try {
                assertEmptyDiagnosticsAfter { generateAST(parseTree, diagnostics) }
            } catch (e: CompileException) {
                logger?.logFailedAstGeneration()
                throw e
            }
        logger?.logSuccessfulAstGeneration(ast)
        return ast
    }

    private fun resolveNames(ast: AST): NameResolutionResult {
        val result =
            try {
                assertEmptyDiagnosticsAfter { resolveNames(ast, diagnostics) }
            } catch (e: CompileException) {
                logger?.logFailedNameResolution()
                throw e
            }
        logger?.logSuccessfulNameResolution(result)
        return result
    }

    fun resolveOverloads(ast: AST): ResolvedVariables {
        val nr = resolveNames(ast)
        return resolveOverloads(ast, nr)
    }

    private fun resolveOverloads(ast: AST, nr: NameResolutionResult): ResolvedVariables {
        val result =
            try {
                assertEmptyDiagnosticsAfter { resolveOverloads(ast, diagnostics, nr) }
            } catch (e: CompileException) {
                logger?.logFailedOverloadResolution()
                throw e
            }
        logger?.logSuccessfulOverloadResolution(result)
        return result
    }

    fun checkTypes(ast: AST, resolvedVariables: ResolvedVariables): TypeCheckingResult {
        val types =
            try {
                assertEmptyDiagnosticsAfter { checkTypes(ast, diagnostics, resolvedVariables) }
            } catch (e: CompileException) {
                logger?.logFailedTypeChecking()
                throw e
            }
        logger?.logSuccessfulTypeChecking(types)
        return types
    }

    private fun generateCallGraph(ast: AST, resolvedVariables: ResolvedVariables): CallGraph {
        val callGraph =
            try {
                assertEmptyDiagnosticsAfter { generateCallGraph(ast, diagnostics, resolvedVariables) }
            } catch (e: CompileException) {
                logger?.logFailedCallGraphGeneration()
                throw e
            }
        logger?.logSuccessfulCallGraphGeneration(callGraph)
        return callGraph
    }

    private fun analyzeFunctions(input: Input): FunctionAnalysisResult = analyzeFunctions(generateAST(input))

    private fun analyzeFunctions(ast: AST): FunctionAnalysisResult {
        val resolvedFunctions = resolveOverloads(ast)
        checkTypes(ast, resolvedFunctions)
        return analyzeFunctions(ast, resolvedFunctions)
    }

    fun analyzeFunctions(ast: AST, resolvedVariables: ResolvedVariables): FunctionAnalysisResult {
        val callGraph = generateCallGraph(ast, resolvedVariables)
        return analyzeFunctions(ast, resolvedVariables, callGraph)
    }

    private fun analyzeFunctions(ast: AST, resolvedVariables: ResolvedVariables, callGraph: CallGraph): FunctionAnalysisResult {
        val result =
            try {
                assertEmptyDiagnosticsAfter { cacophony.semantic.analysis.analyzeFunctions(ast, resolvedVariables, callGraph) }
            } catch (e: CompileException) {
                logger?.logFailedFunctionAnalysis()
                throw e
            }
        logger?.logSuccessfulFunctionAnalysis(result)
        return result
    }

    private fun analyzeAst(ast: AST): AstAnalysisResult {
        val resolvedVariables = resolveOverloads(ast)
        val types = checkTypes(ast, resolvedVariables)
        val callGraph = generateCallGraph(ast, resolvedVariables)
        val analyzedFunctions = analyzeFunctions(ast, resolvedVariables, callGraph)
        val analyzedExpressions = analyzeVarUseTypes(ast, resolvedVariables, analyzedFunctions)
        val functionHandlers = generateFunctionHandlers(analyzedFunctions, SystemVAMD64CallConvention)
        return AstAnalysisResult(resolvedVariables, types, analyzedExpressions, functionHandlers)
    }

    fun generateControlFlowGraph(input: Input): ProgramCFG = generateControlFlowGraph(generateAST(input))

    fun generateControlFlowGraph(ast: AST): ProgramCFG = generateControlFlowGraph(analyzeAst(ast))

    fun generateControlFlowGraph(analyzedAst: AstAnalysisResult): ProgramCFG {
        val cfg = generateCFG(analyzedAst.resolvedVariables, analyzedAst.analyzedExpressions, analyzedAst.functionHandlers)
        logger?.logSuccessfulControlFlowGraphGeneration(cfg)
        return cfg
    }

    private fun coverWithInstructions(ast: AST): Map<FunctionDeclaration, LoweredCFGFragment> =
        generateControlFlowGraph(ast).mapValues { (_, cfg) -> linearize(cfg, instructionCovering) }

    private fun coverWithInstructions(cfg: ProgramCFG): Map<FunctionDeclaration, LoweredCFGFragment> {
        val covering = cfg.mapValues { (_, cfg) -> linearize(cfg, instructionCovering) }
        logger?.logSuccessfulInstructionCovering(covering)
        return covering
    }

    fun analyzeLiveness(ast: AST): Map<FunctionDeclaration, Liveness> =
        coverWithInstructions(ast).mapValues { (_, loweredCFG) -> cacophony.codegen.registers.analyzeLiveness(loweredCFG) }

    private fun analyzeLiveness(covering: Map<FunctionDeclaration, LoweredCFGFragment>): Map<FunctionDeclaration, Liveness> {
        val liveness = covering.mapValues { (_, loweredCFG) -> cacophony.codegen.registers.analyzeLiveness(loweredCFG) }
        logger?.logSuccessfulLivenessGeneration(liveness)
        return liveness
    }

    fun allocateRegisters(ast: AST, allowedRegisters: Set<HardwareRegister> = allGPRs): Map<FunctionDeclaration, RegisterAllocation> =
        analyzeLiveness(ast).mapValues { (_, liveness) -> cacophony.codegen.registers.allocateRegisters(liveness, allowedRegisters) }

    fun allocateRegisters(
        liveness: Map<FunctionDeclaration, Liveness>,
        allowedRegisters: Set<HardwareRegister> = allGPRs,
    ): Map<FunctionDeclaration, RegisterAllocation> {
        val allocatedRegisters =
            liveness.mapValues { (_, liveness) ->
                cacophony.codegen.registers.allocateRegisters(liveness, allowedRegisters)
            }
        logger?.logSuccessfulRegisterAllocation(allocatedRegisters)
        return allocatedRegisters
    }

    private fun handleSpills(
        functionHandlers: Map<FunctionDeclaration, FunctionHandler>,
        covering: Map<FunctionDeclaration, LoweredCFGFragment>,
        liveness: Map<FunctionDeclaration, Liveness>,
        registerAllocation: Map<FunctionDeclaration, RegisterAllocation>,
    ): Pair<
        Map<FunctionDeclaration, LoweredCFGFragment>,
        Map<FunctionDeclaration, RegisterAllocation>,
        > {
        if (registerAllocation.values.all { it.spills.isEmpty() }) {
            return covering to registerAllocation
        }

        val spareRegisters = setOf(Register.FixedRegister(HardwareRegister.R8), Register.FixedRegister(HardwareRegister.R9))

        logger?.logSpillHandlingAttempt(spareRegisters)

        val newRegisterAllocation = allocateRegisters(liveness, allGPRs.minus(spareRegisters.map { it.hardwareRegister }.toSet()))

        if (newRegisterAllocation.values.all { it.spills.isEmpty() }) {
            return covering to newRegisterAllocation
        }

        val newCovering =
            covering
                .map { (functionDeclaration, loweredCfg) ->
                    functionDeclaration to
                        adjustLoweredCFGToHandleSpills(
                            instructionCovering,
                            functionHandlers[functionDeclaration]!!,
                            loweredCfg,
                            newRegisterAllocation[functionDeclaration]!!,
                            spareRegisters,
                        )
                }.toMap()

        return newCovering to newRegisterAllocation
    }

    private fun generateAsm(ast: AST): Map<FunctionDeclaration, String> {
        val analyzedAst = analyzeAst(ast)
        val cfg = generateControlFlowGraph(analyzedAst)
        val covering = coverWithInstructions(cfg)
        val liveness = analyzeLiveness(covering)
        val registerAllocation = allocateRegisters(liveness)

        val (coveringWithSpillsHandled, registerAllocationWithSpillsHandled) =
            handleSpills(
                analyzedAst.functionHandlers,
                covering,
                liveness,
                registerAllocation,
            )

        return coveringWithSpillsHandled.mapValues { (function, loweredCFG) ->
            run {
                val ra = registerAllocationWithSpillsHandled[function] ?: error("No register allocation for function $function")
                generateAsm(functionBodyLabel(function), loweredCFG, ra)
            }
        }
    }

    fun generateAsm(input: Input): Map<FunctionDeclaration, String> {
        val asm = generateAsm(generateAST(input))
        asm.forEach { (function, asm) -> println("$function generates asm:\n$asm") }
        return asm
    }

    private fun compile(src: Path, dest: Path) {
        val nasm = ProcessBuilder("nasm", "-f", "elf64", "-o", dest.toString(), src.toString()).inheritIO().start()
        nasm.waitFor().takeIf { it != 0 }?.let { status ->
            logger?.logFailedAssembling(status)
            throw RuntimeException("Unable to assemble generated code")
        } ?: logger?.logSuccessfulAssembling(dest)
    }

    fun link(src: Path, dest: Path) {
        val gcc = ProcessBuilder("gcc", "-no-pie", "-o", dest.toString(), src.toString()).inheritIO().start()
        gcc.waitFor().takeIf { it != 0 }?.let { status ->
            logger?.logFailedLinking(status)
            throw RuntimeException("Unable to link compiled code")
        } ?: logger?.logSuccessfulLinking(dest)
    }

    fun compile(input: Input, src: Path) {
        val asmFile = Paths.get("${src.fileName}.asm")
        val objFile = Paths.get("${src.fileName}.o")
        val binFile = Paths.get("${src.fileName}.bin")

        asmFile.writeLines(generateAsm(input).values)
        compile(asmFile, objFile)
        link(objFile, binFile)
    }
}
