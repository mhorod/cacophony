package cacophony.pipeline

import cacophony.codegen.functionBodyLabel
import cacophony.codegen.instructions.CacophonyInstructionCovering
import cacophony.codegen.instructions.generateAsm
import cacophony.codegen.instructions.matching.CacophonyInstructionMatcher
import cacophony.codegen.linearization.LoweredCFGFragment
import cacophony.codegen.linearization.linearize
import cacophony.codegen.registers.Liveness
import cacophony.codegen.registers.RegisterAllocation
import cacophony.controlflow.HardwareRegister
import cacophony.controlflow.SystemVAMD64CallConvention
import cacophony.controlflow.generateFunctionHandlers
import cacophony.controlflow.generation.ProgramCFG
import cacophony.controlflow.generation.generateCFG
import cacophony.diagnostics.Diagnostics
import cacophony.grammars.ParseTree
import cacophony.lexer.CacophonyLexer
import cacophony.parser.CacophonyGrammarSymbol
import cacophony.parser.CacophonyParser
import cacophony.semantic.*
import cacophony.semantic.syntaxtree.AST
import cacophony.semantic.syntaxtree.Definition
import cacophony.token.Token
import cacophony.token.TokenCategorySpecific
import cacophony.utils.CompileException
import cacophony.utils.Input
import cacophony.utils.withExtension
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption

class CacophonyPipeline(
    val diagnostics: Diagnostics,
    private val logger: CacophonyLogger? = null,
    private val lexer: CacophonyLexer = cachedLexer,
    private val parser: CacophonyParser = cachedParser,
    private val instructionMatcher: CacophonyInstructionMatcher = cachedInstructionMatcher,
) {
    companion object {
        private val cachedLexer = CacophonyLexer()
        private val cachedParser = CacophonyParser()
        private val cachedInstructionMatcher = CacophonyInstructionMatcher()
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

    fun lex(input: Input): List<Token<TokenCategorySpecific>> {
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

    fun parse(input: Input): ParseTree<CacophonyGrammarSymbol> {
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
                assertEmptyDiagnosticsAfter { cacophony.semantic.generateAST(parseTree, diagnostics) }
            } catch (e: CompileException) {
                logger?.logFailedAstGeneration()
                throw e
            }
        logger?.logSuccessfulAstGeneration(ast)
        return ast
    }

    fun resolveNames(ast: AST): NameResolutionResult {
        val result =
            try {
                assertEmptyDiagnosticsAfter { cacophony.semantic.resolveNames(ast, diagnostics) }
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

    fun resolveOverloads(ast: AST, nr: NameResolutionResult): ResolvedVariables {
        val result =
            try {
                assertEmptyDiagnosticsAfter { cacophony.semantic.resolveOverloads(ast, diagnostics, nr) }
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
                assertEmptyDiagnosticsAfter { cacophony.semantic.checkTypes(ast, diagnostics, resolvedVariables) }
            } catch (e: CompileException) {
                logger?.logFailedTypeChecking()
                throw e
            }
        logger?.logSuccessfulTypeChecking(types)
        return types
    }

    fun generateCallGraph(ast: AST, resolvedVariables: ResolvedVariables): CallGraph {
        val callGraph =
            try {
                assertEmptyDiagnosticsAfter { cacophony.semantic.generateCallGraph(ast, diagnostics, resolvedVariables) }
            } catch (e: CompileException) {
                logger?.logFailedCallGraphGeneration()
                throw e
            }
        logger?.logSuccessfulCallGraphGeneration(callGraph)
        return callGraph
    }

    fun analyzeFunctions(input: Input): FunctionAnalysisResult = analyzeFunctions(generateAST(input))

    fun analyzeFunctions(ast: AST): FunctionAnalysisResult {
        val resolvedFunctions = resolveOverloads(ast)
        return analyzeFunctions(ast, resolvedFunctions)
    }

    fun analyzeFunctions(ast: AST, resolvedVariables: ResolvedVariables): FunctionAnalysisResult {
        val callGraph = generateCallGraph(ast, resolvedVariables)
        return analyzeFunctions(ast, resolvedVariables, callGraph)
    }

    fun analyzeFunctions(ast: AST, resolvedVariables: ResolvedVariables, callGraph: CallGraph): FunctionAnalysisResult {
        val result =
            try {
                assertEmptyDiagnosticsAfter { cacophony.semantic.analyzeFunctions(ast, resolvedVariables, callGraph) }
            } catch (e: CompileException) {
                logger?.logFailedFunctionAnalysis()
                throw e
            }
        logger?.logSuccessfulFunctionAnalysis(result)
        return result
    }

    fun generateControlFlowGraph(input: Input): ProgramCFG = generateControlFlowGraph(generateAST(input))

    fun generateControlFlowGraph(ast: AST): ProgramCFG {
        val resolvedVariables = resolveOverloads(ast)
        val callGraph = generateCallGraph(ast, resolvedVariables)
        val analyzedFunctions = analyzeFunctions(ast, resolvedVariables, callGraph)
        val analyzedExpressions = analyzeVarUseTypes(ast, resolvedVariables, analyzedFunctions)
        val functionHandlers = generateFunctionHandlers(analyzedFunctions, SystemVAMD64CallConvention)
        val cfg = generateCFG(resolvedVariables, analyzedExpressions, functionHandlers)
        logger?.logSuccessfulControlFlowGraphGeneration(cfg)
        return cfg
    }

    fun coverWithInstructions(ast: AST): Map<Definition.FunctionDeclaration, LoweredCFGFragment> =
        generateControlFlowGraph(ast).mapValues { (_, cfg) -> linearize(cfg, CacophonyInstructionCovering(instructionMatcher)) }

    fun coverWithInstructions(cfg: ProgramCFG): Map<Definition.FunctionDeclaration, LoweredCFGFragment> {
        val covering = cfg.mapValues { (_, cfg) -> linearize(cfg, CacophonyInstructionCovering(instructionMatcher)) }
        logger?.logSuccessfulInstructionCovering(covering)
        return covering
    }

    fun analyzeLiveness(ast: AST): Map<Definition.FunctionDeclaration, Liveness> =
        coverWithInstructions(ast).mapValues { (_, loweredCFG) -> cacophony.codegen.registers.analyzeLiveness(loweredCFG) }

    fun analyzeLiveness(covering: Map<Definition.FunctionDeclaration, LoweredCFGFragment>): Map<Definition.FunctionDeclaration, Liveness> {
        val liveness = covering.mapValues { (_, loweredCFG) -> cacophony.codegen.registers.analyzeLiveness(loweredCFG) }
        logger?.logSuccessfulLivenessGeneration(liveness)
        return liveness
    }

    fun allocateRegisters(
        ast: AST,
        allowedRegisters: Set<HardwareRegister> = allGPRs,
    ): Map<Definition.FunctionDeclaration, RegisterAllocation> =
        analyzeLiveness(ast).mapValues { (_, liveness) -> cacophony.codegen.registers.allocateRegisters(liveness, allowedRegisters) }

    fun allocateRegisters(
        liveness: Map<Definition.FunctionDeclaration, Liveness>,
    ): Map<Definition.FunctionDeclaration, RegisterAllocation> {
        val allocatedRegisters =
            liveness.mapValues { (_, liveness) ->
                cacophony.codegen.registers.allocateRegisters(liveness, allGPRs)
            }
        logger?.logSuccessfulRegisterAllocation(allocatedRegisters)
        return allocatedRegisters
    }

    fun generateAsm(ast: AST): Map<Definition.FunctionDeclaration, String> {
        val cfg = generateControlFlowGraph(ast)
        val covering = coverWithInstructions(cfg)
        val liveness = analyzeLiveness(covering)
        val registerAllocation = allocateRegisters(liveness)

        return covering.mapValues { (function, loweredCFG) ->
            run {
                val ra = registerAllocation[function] ?: error("No register allocation for function $function")
                cacophony.codegen.instructions.generateAsm(functionBodyLabel(function), loweredCFG, ra)
            }
        }
    }

    fun generateAsm(input: Input): Map<Definition.FunctionDeclaration, String> {
        val asm = generateAsm(generateAST(input))
        asm.forEach { (function, asm) -> println("$function generates asm:\n$asm") }
        return asm
    }

    fun generateAsm(input: Input, dest: Path) {
        val defs = generateAsm(input)
        Files.newBufferedWriter(dest, StandardOpenOption.CREATE).use { writer ->
            defs.values.forEach {
                writer.write(it)
                writer.newLine()
            }
        }
    }

    fun compile(src: Path, dest: Path) {
        System.out.println("Compiling generated assembly")
        val nasm = ProcessBuilder("nasm", "-f", "elf64", "-o", dest.toString(), src.toString()).inheritIO().start()
        nasm.waitFor().takeIf { it != 0 }?.let { status ->
            System.err.println("nasm failed with exit code: $status")
            throw CompileException("Unable to assemble generated code")
        } ?: System.out.println("Successfully saved compiled object in $dest")
    }

    fun link(src: Path, dest: Path) {
        System.out.println("Linking compiled ELF object")
        val gcc = ProcessBuilder("gcc", "-no-pie", "-o", dest.toString(), src.toString()).inheritIO().start()
        gcc.waitFor().takeIf { it != 0 }?.let { status ->
            System.err.println("gcc failed with exit code: $status")
            throw CompileException("Unable to link compiled code")
        } ?: System.out.println("Successfully saved linked executable in $dest")
    }

    fun compile(input: Input, src: Path) {
        val asmFile = withExtension(src, ".asm")
        val objFile = withExtension(src, ".o")
        val binFile = withExtension(src, "")

        generateAsm(input, asmFile)
        compile(asmFile, objFile)
        link(objFile, binFile)
    }
}
