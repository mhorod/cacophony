package cacophony.pipeline

import cacophony.codegen.functionBodyLabel
import cacophony.codegen.instructions.CacophonyInstructionCovering
import cacophony.codegen.instructions.generateAsm
import cacophony.codegen.instructions.matching.CacophonyInstructionMatcher
import cacophony.codegen.linearization.LoweredCFGFragment
import cacophony.codegen.linearization.linearize
import cacophony.codegen.registers.*
import cacophony.controlflow.HardwareRegister
import cacophony.controlflow.Register
import cacophony.controlflow.functions.FunctionHandler
import cacophony.controlflow.functions.SystemVAMD64CallConvention
import cacophony.controlflow.functions.generateFunctionHandlers
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

    private fun findForeignFunctions(nr: NameResolutionResult): Set<Definition.ForeignFunctionDeclaration> =
        nr.values
            .filterIsInstance<ResolvedName.Function>()
            .flatMap {
                it.def.toMap().values
            }.filterIsInstance<Definition.ForeignFunctionDeclaration>()
            .toSet()

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
        val resolvedNames = resolveNames(ast)
        val resolvedVariables = resolveOverloads(ast, resolvedNames)
        val types = checkTypes(ast, resolvedVariables)
        val callGraph = generateCallGraph(ast, resolvedVariables)
        val analyzedFunctions = analyzeFunctions(ast, resolvedVariables, callGraph)
        val analyzedExpressions = analyzeVarUseTypes(ast, resolvedVariables, analyzedFunctions)
        val functionHandlers = generateFunctionHandlers(analyzedFunctions, SystemVAMD64CallConvention)
        val foreignFunctions = findForeignFunctions(resolvedNames)
        return AstAnalysisResult(resolvedVariables, types, analyzedExpressions, functionHandlers, foreignFunctions)
    }

    fun generateControlFlowGraph(input: Input): ProgramCFG = generateControlFlowGraph(generateAST(input))

    fun generateControlFlowGraph(ast: AST): ProgramCFG = generateControlFlowGraph(analyzeAst(ast))

    fun generateControlFlowGraph(analyzedAst: AstAnalysisResult): ProgramCFG {
        val cfg = generateCFG(analyzedAst.resolvedVariables, analyzedAst.analyzedExpressions, analyzedAst.functionHandlers)
        logger?.logSuccessfulControlFlowGraphGeneration(cfg)
        return cfg
    }

    private fun coverWithInstructions(ast: AST): Map<FunctionDefinition, LoweredCFGFragment> =
        generateControlFlowGraph(ast).mapValues { (_, cfg) -> linearize(cfg, instructionCovering) }

    private fun coverWithInstructions(cfg: ProgramCFG): Map<FunctionDefinition, LoweredCFGFragment> {
        val covering = cfg.mapValues { (_, cfg) -> linearize(cfg, instructionCovering) }
        logger?.logSuccessfulInstructionCovering(covering)
        return covering
    }

    fun analyzeRegistersInteraction(ast: AST): Map<FunctionDefinition, RegistersInteraction> =
        coverWithInstructions(ast).mapValues { (_, loweredCFG) -> analyzeRegistersInteraction(loweredCFG) }

    private fun analyzeRegistersInteraction(
        covering: Map<FunctionDefinition, LoweredCFGFragment>,
    ): Map<FunctionDefinition, RegistersInteraction> {
        val registersInteraction = covering.mapValues { (_, loweredCFG) -> analyzeRegistersInteraction(loweredCFG) }
        logger?.logSuccessfulRegistersInteractionGeneration(registersInteraction)
        return registersInteraction
    }

    fun allocateRegisters(ast: AST, allowedRegisters: Set<HardwareRegister> = allGPRs): Map<FunctionDefinition, RegisterAllocation> =
        analyzeRegistersInteraction(ast).mapValues { (_, registersInteraction) ->
            allocateRegisters(registersInteraction, allowedRegisters)
        }

    fun allocateRegisters(
        registersInteractions: Map<FunctionDefinition, RegistersInteraction>,
        allowedRegisters: Set<HardwareRegister> = allGPRs,
    ): Map<FunctionDefinition, RegisterAllocation> {
        val allocatedRegisters =
            registersInteractions.mapValues { (_, registersInteraction) ->
                allocateRegisters(registersInteraction, allowedRegisters)
            }
        logger?.logSuccessfulRegisterAllocation(allocatedRegisters)
        return allocatedRegisters
    }

    private fun handleSpills(
        functionHandlers: Map<FunctionDefinition, FunctionHandler>,
        covering: Map<FunctionDefinition, LoweredCFGFragment>,
        registersInteractions: Map<FunctionDefinition, RegistersInteraction>,
        registerAllocation: Map<FunctionDefinition, RegisterAllocation>,
    ): Pair<
        Map<FunctionDefinition, LoweredCFGFragment>,
        Map<FunctionDefinition, RegisterAllocation>,
        > {
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

    private fun generateAsmPreamble(foreignFunctions: Set<Definition.ForeignFunctionDeclaration>): String =
        (
            listOf("SECTION .data") +
                foreignFunctions.map {
                    "extern ${it.identifier}"
                } + listOf("global main", "SECTION .text")
        ).joinToString("\n")

    private fun generateAsmImpl(ast: AST): Pair<String, Map<FunctionDefinition, String>> {
        val analyzedAst = analyzeAst(ast)
        val cfg = generateControlFlowGraph(analyzedAst)
        val covering = coverWithInstructions(cfg)
        val registersInteractions = analyzeRegistersInteraction(covering)
        val registerAllocation = allocateRegisters(registersInteractions)

        val (coveringWithSpillsHandled, registerAllocationWithSpillsHandled) =
            handleSpills(
                analyzedAst.functionHandlers,
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
        asm.forEach { (function, asm) -> println("$function generates asm:\n$asm") }
        return Pair(generateAsmPreamble(analyzedAst.foreignFunctions), asm)
    }

    private fun generateAsm(ast: AST): String {
        val (preamble, functions) = generateAsmImpl(ast)
        functions.forEach { (function, asm) -> println("$function generates asm:\n$asm") }
        return (listOf(preamble) + functions.values).joinToString("\n")
    }

    fun generateAsm(input: Input): String = generateAsm(generateAST(input))

    private fun compile(src: Path, dest: Path) {
        val options = listOf("nasm", "-f", "elf64", "-o", dest.toString(), src.toString())
        val nasm = ProcessBuilder(options).inheritIO().start()
        nasm.waitFor().takeIf { it != 0 }?.let { status ->
            logger?.logFailedAssembling(status)
            throw RuntimeException("Unable to assemble generated code")
        } ?: logger?.logSuccessfulAssembling(dest)
    }

    fun link(sources: List<Path>, dest: Path) {
        val options = listOf("gcc", "-no-pie", "-z", "noexecstack", "-o", dest.toString()) + sources.map { it.toString() }
        val gcc = ProcessBuilder(options).inheritIO().start()
        gcc.waitFor().takeIf { it != 0 }?.let { status ->
            logger?.logFailedLinking(status)
            throw RuntimeException("Unable to link compiled code")
        } ?: logger?.logSuccessfulLinking(dest)
    }

    fun compile(
        input: Input,
        additionalObjectFiles: List<Path>,
        asmFile: Path,
        objFile: Path,
        binFile: Path,
    ) {
        asmFile.writeText(generateAsm(input))
        compile(asmFile, objFile)
        link(listOf(objFile, Paths.get("libcacophony.c")) + additionalObjectFiles, binFile)
    }

    fun compile(input: Input, src: Path) {
        compile(
            input,
            emptyList(),
            Paths.get("${src.fileName}.asm"),
            Paths.get("${src.fileName}.o"),
            Paths.get("${src.fileName}.bin"),
        )
    }
}
