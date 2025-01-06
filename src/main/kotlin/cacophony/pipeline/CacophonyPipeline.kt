package cacophony.pipeline

import cacophony.codegen.functionBodyLabel
import cacophony.codegen.instructions.CacophonyInstructionCovering
import cacophony.codegen.instructions.generateAsm
import cacophony.codegen.instructions.generateAsmPreamble
import cacophony.codegen.instructions.matching.CacophonyInstructionMatcher
import cacophony.codegen.linearization.LoweredCFGFragment
import cacophony.codegen.linearization.linearize
import cacophony.codegen.registers.*
import cacophony.controlflow.HardwareRegister
import cacophony.controlflow.Register
import cacophony.controlflow.functions.*
import cacophony.controlflow.generation.ProgramCFG
import cacophony.controlflow.generation.generateCFG
import cacophony.diagnostics.Diagnostics
import cacophony.grammars.ParseTree
import cacophony.graphs.FirstFitGraphColoring
import cacophony.lexer.CacophonyLexer
import cacophony.parser.CacophonyGrammarSymbol
import cacophony.parser.CacophonyParser
import cacophony.semantic.analysis.*
import cacophony.semantic.names.*
import cacophony.semantic.syntaxtree.AST
import cacophony.semantic.syntaxtree.Definition
import cacophony.semantic.syntaxtree.Definition.FunctionDefinition
import cacophony.semantic.syntaxtree.generateAST
import cacophony.semantic.types.TypeCheckingResult
import cacophony.semantic.types.checkTypes
import cacophony.token.Token
import cacophony.token.TokenCategorySpecific
import cacophony.utils.CompileException
import cacophony.utils.Input
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.writeText

data class AstAnalysisResult(
    val resolvedVariables: ResolvedVariables,
    val types: TypeCheckingResult,
    val variablesMap: VariablesMap,
    val analyzedExpressions: UseTypeAnalysisResult,
    val functionHandlers: Map<FunctionDefinition, FunctionHandler>,
    val foreignFunctions: Set<Definition.ForeignFunctionDeclaration>,
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

    // SHARED
    private fun lex(input: Input): List<Token<TokenCategorySpecific>> {
        println("Inside lex")
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

    // SHARED
    private fun parse(input: Input): ParseTree<CacophonyGrammarSymbol> {
        println("Inside parse")
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

    // SHARED
    fun generateAst(input: Input): AST {
        println("Inside generateAST")
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

    // SHARED
    fun resolveNames(ast: AST): NameResolutionResult {
        println("Inside resolveNames")
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

    // NOT IN TESTS
    private fun findForeignFunctions(nr: NameResolutionResult): Set<Definition.ForeignFunctionDeclaration> {
        println("find Foreign Functions")
        return nr.values
            .filterIsInstance<ResolvedName.Function>()
            .flatMap {
                it.def.toMap().values
            }.filterIsInstance<Definition.ForeignFunctionDeclaration>()
            .toSet()
    }

    // SHARED
    fun resolveOverloads(ast: AST, nr: NameResolutionResult): ResolvedVariables {
        println("resolve Overloads 2")
        val result =
            try {
                assertEmptyDiagnosticsAfter { resolveOverloads(ast, nr, diagnostics) }
            } catch (e: CompileException) {
                logger?.logFailedOverloadResolution()
                throw e
            }
        logger?.logSuccessfulOverloadResolution(result)
        return result
    }

    fun checkTypes(ast: AST, resolvedVariables: ResolvedVariables): TypeCheckingResult {
        println("check types")
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

    // SHARED
    fun createVariables(ast: AST, resolvedVariables: ResolvedVariables, types: TypeCheckingResult): VariablesMap {
        println("create variables")
        val variableMap = createVariablesMap(ast, resolvedVariables, types)
        logger?.logSuccessfulVariableCreation(variableMap)
        return variableMap
    }

    // SHARED
    fun generateCallGraph(ast: AST, resolvedVariables: ResolvedVariables): CallGraph {
        println("generate callGraph")
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

    // SHARED
    fun analyzeFunctions(
        ast: AST,
        variablesMap: VariablesMap,
        resolvedVariables: ResolvedVariables,
        callGraph: CallGraph,
    ): FunctionAnalysisResult {
        println("analyze Functions 4")
        val result =
            try {
                assertEmptyDiagnosticsAfter {
                    analyzeFunctions(
                        ast,
                        resolvedVariables,
                        callGraph,
                        variablesMap,
                    )
                }
            } catch (e: CompileException) {
                logger?.logFailedFunctionAnalysis()
                throw e
            }
        logger?.logSuccessfulFunctionAnalysis(result)
        return result
    }

    // NOT IN TESTS
    fun analyzeAst(ast: AST): AstAnalysisResult {
        println("analyze AST")
        val resolvedNames = resolveNames(ast)
        val resolvedVariables = resolveOverloads(ast, resolvedNames)
        val types = checkTypes(ast, resolvedVariables)
        val variablesMap = createVariables(ast, resolvedVariables, types)
        val callGraph = generateCallGraph(ast, resolvedVariables)
        val analyzedFunctions = analyzeFunctions(ast, variablesMap, resolvedVariables, callGraph)
        val analyzedExpressions = analyzeVarUseTypes(ast, resolvedVariables, analyzedFunctions, variablesMap)
        val functionHandlers = generateFunctionHandlers(analyzedFunctions, SystemVAMD64CallConvention, variablesMap)
        val foreignFunctions = findForeignFunctions(resolvedNames)
        return AstAnalysisResult(resolvedVariables, types, variablesMap, analyzedExpressions, functionHandlers, foreignFunctions)
    }

    // NOT IN TESTS
    fun generateControlFlowGraph(analyzedAst: AstAnalysisResult, callGenerator: CallGenerator): ProgramCFG {
        println("gen CFG 2")
        val cfg =
            generateCFG(
                analyzedAst.resolvedVariables,
                analyzedAst.analyzedExpressions,
                analyzedAst.functionHandlers,
                analyzedAst.variablesMap,
                analyzedAst.types,
                callGenerator,
                mapOf(), // TODO(Rafał): populate this
            )
        logger?.logSuccessfulControlFlowGraphGeneration(cfg)
        return cfg
    }

    // NOT IN TESTS
    fun coverWithInstructions(cfg: ProgramCFG): Map<FunctionDefinition, LoweredCFGFragment> {
        println("cover 2")
        val covering = cfg.mapValues { (_, cfg) -> linearize(cfg, instructionCovering) }
        logger?.logSuccessfulInstructionCovering(covering)
        return covering
    }

    // NOT IN TESTS
    fun analyzeRegistersInteraction(covering: Map<FunctionDefinition, LoweredCFGFragment>): Map<FunctionDefinition, RegistersInteraction> {
        println("reg interaction 2")
        val registersInteraction = covering.mapValues { (_, loweredCFG) -> analyzeRegistersInteraction(loweredCFG) }
        logger?.logSuccessfulRegistersInteractionGeneration(registersInteraction)
        return registersInteraction
    }

    // NOT IN TESTS
    fun allocateRegisters(
        registersInteractions: Map<FunctionDefinition, RegistersInteraction>,
        allowedRegisters: Set<HardwareRegister> = allGPRs,
    ): Map<FunctionDefinition, RegisterAllocation> {
        println("allocate regs 2")
        val allocatedRegisters =
            registersInteractions.mapValues { (_, registersInteraction) ->
                allocateRegisters(registersInteraction, allowedRegisters)
            }
        logger?.logSuccessfulRegisterAllocation(allocatedRegisters)
        return allocatedRegisters
    }

    // NOT IN TESTS
    private fun handleSpills(
        functionHandlers: Map<FunctionDefinition, FunctionHandler>,
        covering: Map<FunctionDefinition, LoweredCFGFragment>,
        registersInteractions: Map<FunctionDefinition, RegistersInteraction>,
        registerAllocation: Map<FunctionDefinition, RegisterAllocation>,
    ): Pair<
        Map<FunctionDefinition, LoweredCFGFragment>,
        Map<FunctionDefinition, RegisterAllocation>,
    > {
        println("Handle spills 1")
        if (registerAllocation.values.all { it.spills.isEmpty() }) {
            return covering to registerAllocation
        }

        val spareRegisters =
            setOf(
                Register.FixedRegister(HardwareRegister.R10),
                Register.FixedRegister(HardwareRegister.R11),
            )

        logger?.logSpillHandlingAttempt(spareRegisters)

        val newRegisterAllocation =
            allocateRegisters(registersInteractions, allGPRs.minus(spareRegisters.map { it.hardwareRegister }.toSet()))
                .mapValues { (_, value) ->
                    RegisterAllocation(
                        value.successful.plus(spareRegisters.associateWith { it.hardwareRegister }),
                        value.spills,
                    )
                }

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
                            registersInteractions[functionDeclaration]!!,
                            newRegisterAllocation[functionDeclaration]!!,
                            spareRegisters,
                            FirstFitGraphColoring(),
                        )
                }.toMap()

        return newCovering to newRegisterAllocation
    }

    // NOT IN TESTS
    fun generateAsm(input: Input): String {
        println("Generate ASM 1")
        val (preamble, functions) = process(input)
        logger?.logSuccessfulPreambleGeneration(preamble)
        logger?.logSuccessfulAsmGeneration(functions)
        return (listOf(preamble) + functions.values).joinToString("\n")
    }

    // SHARED WITH IO TESTS
    private fun assemble(src: Path, dest: Path) {
        println("assemble")
        val options = listOf("nasm", "-f", "elf64", "-o", dest.toString(), src.toString())
        val nasm = ProcessBuilder(options).inheritIO().start()
        nasm.waitFor().takeIf { it != 0 }?.let { status ->
            logger?.logFailedAssembling(status)
            throw RuntimeException("Unable to assemble generated code")
        } ?: logger?.logSuccessfulAssembling(dest)
    }

    // SHARED WITH IO TESTS
    fun compileAndLink(
        input: Input,
        additionalObjectFiles: List<Path>,
        asmFile: Path,
        objFile: Path,
        binFile: Path,
    ) {
        println("compile and link")
        asmFile.writeText(generateAsm(input))
        assemble(asmFile, objFile)
        val sources = listOf(objFile) + additionalObjectFiles
        val options = listOf("gcc", "-no-pie", "-z", "noexecstack", "-o", binFile.toString()) + sources.map { it.toString() }
        val gcc = ProcessBuilder(options).inheritIO().start()
        gcc.waitFor().takeIf { it != 0 }?.let { status ->
            logger?.logFailedLinking(status)
            throw RuntimeException("Unable to link compiled code")
        } ?: logger?.logSuccessfulLinking(binFile)
    }

    // NOT IN TESTS
    fun compile(input: Input, outputDir: Path) {
        println("compile")
        compileAndLink(
            input,
            listOf(Paths.get("libcacophony.c")),
            Paths.get("${outputDir.fileName}", "${outputDir.fileName}.asm"),
            Paths.get("${outputDir.fileName}", "${outputDir.fileName}.o"),
            Paths.get("${outputDir.fileName}", "${outputDir.fileName}.bin"),
        )
    }

    fun process(input: Input): Pair<String, Map<FunctionDefinition, String>> {
        val ast = generateAst(input)
        val semantics = analyzeAst(ast)
        val cfg = generateControlFlowGraph(semantics, SimpleCallGenerator())
        val covering = coverWithInstructions(cfg)
        val registersInteractions = analyzeRegistersInteraction(covering)
        val registerAllocation = allocateRegisters(registersInteractions)
        val (coveringWithSpillsHandled, registerAllocationWithSpillsHandled) =
            handleSpills(
                semantics.functionHandlers,
                covering,
                registersInteractions,
                registerAllocation,
            )
        val asm =
            coveringWithSpillsHandled.mapValues { (function, loweredCFG) ->
                run {
                    val ra = registerAllocationWithSpillsHandled[function] ?: error("No register allocation for function $function")
                    generateAsm(functionBodyLabel(function), loweredCFG, ra)
                }
            }
        return Pair(generateAsmPreamble(semantics.foreignFunctions), asm)
    }
}
