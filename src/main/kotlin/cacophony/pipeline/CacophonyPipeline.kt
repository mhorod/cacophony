package cacophony.pipeline

import cacophony.codegen.functionBodyLabel
import cacophony.codegen.instructions.CacophonyInstructionCovering
import cacophony.codegen.instructions.generateAsm
import cacophony.codegen.instructions.generateAsmPreamble
import cacophony.codegen.instructions.matching.CacophonyInstructionMatcher
import cacophony.codegen.linearization.LoweredCFGFragment
import cacophony.codegen.registers.*
import cacophony.codegen.safeLinearize
import cacophony.controlflow.HardwareRegister
import cacophony.controlflow.Register
import cacophony.controlflow.functions.*
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
        private val cachedBackupRegs =
            setOf(
                Register.FixedRegister(HardwareRegister.R10),
                Register.FixedRegister(HardwareRegister.R11),
            )
        val cachedInstructionCovering = CacophonyInstructionCovering(CacophonyInstructionMatcher())
        val allGPRs = HardwareRegister.entries.toSet()
    }

    private fun <T> assertEmptyDiagnosticsAfter(action: () -> T): T {
        val x = action()
        if (diagnostics.getErrors().isNotEmpty()) {
            throw diagnostics.fatal()
        }
        return x
    }

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

    fun generateAst(input: Input): AST {
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

    fun resolveNames(ast: AST): NameResolutionResult {
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

    fun resolveOverloads(ast: AST, nr: NameResolutionResult): ResolvedVariables {
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
        val types =
            try {
                assertEmptyDiagnosticsAfter { checkTypes(ast, resolvedVariables, diagnostics) }
            } catch (e: CompileException) {
                logger?.logFailedTypeChecking()
                throw e
            }
        logger?.logSuccessfulTypeChecking(types)
        return types
    }

    fun createVariables(ast: AST, resolvedVariables: ResolvedVariables, types: TypeCheckingResult): VariablesMap {
        val variableMap =
            try {
                createVariablesMap(ast, resolvedVariables, types)
            } catch (e: CompileException) {
                logger?.logFailedVariableCreation()
                throw e
            }
        logger?.logSuccessfulVariableCreation(variableMap)
        return variableMap
    }

    fun generateCallGraph(ast: AST, resolvedVariables: ResolvedVariables): CallGraph {
        val callGraph =
            try {
                assertEmptyDiagnosticsAfter { generateCallGraph(ast, resolvedVariables, diagnostics) }
            } catch (e: CompileException) {
                logger?.logFailedCallGraphGeneration()
                throw e
            }
        logger?.logSuccessfulCallGraphGeneration(callGraph)
        return callGraph
    }

    fun analyzeFunctions(
        ast: AST,
        variablesMap: VariablesMap,
        resolvedVariables: ResolvedVariables,
        callGraph: CallGraph,
    ): FunctionAnalysisResult {
        val result =
            try {
                assertEmptyDiagnosticsAfter {
                    analyzeFunctions(
                        ast,
                        resolvedVariables,
                        callGraph,
                        variablesMap,
                        // TODO: no diagnostics passed?
                    )
                }
            } catch (e: CompileException) {
                logger?.logFailedFunctionAnalysis()
                throw e
            }
        logger?.logSuccessfulFunctionAnalysis(result)
        return result
    }

    private fun filterForeignFunctions(nr: NameResolutionResult): Set<Definition.ForeignFunctionDeclaration> =
        nr.values
            .filterIsInstance<ResolvedName.Function>()
            .flatMap {
                it.def.toMap().values
            }.filterIsInstance<Definition.ForeignFunctionDeclaration>()
            .toSet()

    fun analyzeAst(ast: AST): AstAnalysisResult {
        val resolvedNames = resolveNames(ast)
        val resolvedVariables = resolveOverloads(ast, resolvedNames)
        val types = checkTypes(ast, resolvedVariables)
        val variablesMap = createVariables(ast, resolvedVariables, types)
        val callGraph = generateCallGraph(ast, resolvedVariables)
        val analyzedFunctions = analyzeFunctions(ast, variablesMap, resolvedVariables, callGraph)
        val analyzedExpressions = analyzeVarUseTypes(ast, resolvedVariables, analyzedFunctions, variablesMap)
        val functionHandlers = generateFunctionHandlers(analyzedFunctions, SystemVAMD64CallConvention, variablesMap)
        val foreignFunctions = filterForeignFunctions(resolvedNames)
        return AstAnalysisResult(resolvedVariables, types, variablesMap, analyzedExpressions, functionHandlers, foreignFunctions)
    }

    fun generateControlFlowGraph(analyzedAst: AstAnalysisResult, callGenerator: CallGenerator): ProgramCFG {
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

    fun linearize(
        cfg: ProgramCFG,
        functionHandlers: Map<FunctionDefinition, FunctionHandler>,
    ): Pair<Map<FunctionDefinition, LoweredCFGFragment>, Map<FunctionDefinition, RegisterAllocation>> {
        val (covering, registerAllocation) = safeLinearize(cfg, functionHandlers, instructionCovering, allGPRs, cachedBackupRegs)
        logger?.logSuccessfulInstructionCovering(covering)
        logger?.logSuccessfulRegisterAllocation(registerAllocation)
        return Pair(covering, registerAllocation)
    }

    fun process(input: Input): Pair<String, Map<FunctionDefinition, String>> {
        val ast = generateAst(input)
        val semantics = analyzeAst(ast)
        val cfg = generateControlFlowGraph(semantics, SimpleCallGenerator())
        val (covering, registerAllocation) = linearize(cfg, semantics.functionHandlers)
        val asm =
            covering.mapValues { (function, loweredCFG) ->
                run {
                    val ra = registerAllocation[function] ?: error("No register allocation for function $function")
                    generateAsm(functionBodyLabel(function), loweredCFG, ra)
                }
            }
        return Pair(generateAsmPreamble(semantics.foreignFunctions), asm)
    }

    private fun assemble(src: Path, dest: Path) {
        val options = listOf("nasm", "-f", "elf64", "-o", dest.toString(), src.toString())
        val nasm = ProcessBuilder(options).inheritIO().start()
        nasm.waitFor().takeIf { it != 0 }?.let { status ->
            logger?.logFailedAssembling(status)
            throw RuntimeException("Unable to assemble generated code")
        } ?: logger?.logSuccessfulAssembling(dest)
    }

    fun generateAsm(input: Input): String {
        val (preamble, functions) = process(input)
        logger?.logSuccessfulPreambleGeneration(preamble)
        logger?.logSuccessfulAsmGeneration(functions)
        return (listOf(preamble) + functions.values).joinToString("\n")
    }

    fun compileAndLink(
        input: Input,
        additionalObjectFiles: List<Path>,
        asmFile: Path,
        objFile: Path,
        binFile: Path,
    ) {
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

    fun compile(input: Input, outputDir: Path) {
        compileAndLink(
            input,
            listOf(Paths.get("libcacophony.c")),
            Paths.get("${outputDir.fileName}", "${outputDir.fileName}.asm"),
            Paths.get("${outputDir.fileName}", "${outputDir.fileName}.o"),
            Paths.get("${outputDir.fileName}", "${outputDir.fileName}.bin"),
        )
    }
}
