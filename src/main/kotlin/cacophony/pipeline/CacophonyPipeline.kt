package cacophony.pipeline

import cacophony.codegen.functionBodyLabel
import cacophony.codegen.instructions.CacophonyInstructionCovering
import cacophony.codegen.instructions.generateAsm
import cacophony.codegen.instructions.generateAsmPreamble
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
import cacophony.semantic.rtti.*
import cacophony.semantic.syntaxtree.AST
import cacophony.semantic.syntaxtree.Definition
import cacophony.semantic.syntaxtree.LambdaExpression
import cacophony.semantic.syntaxtree.generateAST
import cacophony.semantic.types.ResolvedVariables
import cacophony.semantic.types.TypeCheckingResult
import cacophony.semantic.types.TypeExpr
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
    val callableHandlers: CallableHandlers,
    val foreignFunctions: Set<Definition.ForeignFunctionDeclaration>,
    val escapeAnalysisResult: EscapeAnalysisResult,
)

class CacophonyPipeline(
    val diagnostics: Diagnostics,
    private val logger: CacophonyLogger? = null,
    private val lexer: CacophonyLexer = Params.lexer,
    private val parser: CacophonyParser = Params.parser,
    private val backupRegs: Set<Register.FixedRegister> = Params.backupRegs,
    private val allowedRegisters: Set<HardwareRegister> = Params.allGPRs,
    private val instructionCovering: CacophonyInstructionCovering = Params.instructionCovering,
) {
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

    fun checkTypes(ast: AST, nr: NameResolutionResult): TypeCheckingResult {
        val types =
            try {
                assertEmptyDiagnosticsAfter {
                    cacophony.semantic.types.checkTypes(
                        ast,
                        nr,
                        diagnostics,
                    )
                }
            } catch (e: CompileException) {
                logger?.logFailedTypeChecking()
                throw e
            }
        logger?.logSuccessfulTypeChecking(types)
        return types
    }

    fun createVariables(ast: AST, types: TypeCheckingResult): VariablesMap {
        val variableMap =
            try {
                createVariablesMap(ast, types)
            } catch (e: CompileException) {
                logger?.logFailedVariableCreation()
                throw e
            }
        logger?.logSuccessfulVariableCreation(variableMap)
        return variableMap
    }

    fun analyzeFunctions(ast: AST, variablesMap: VariablesMap, resolvedVariables: ResolvedVariables): FunctionAnalysisResult {
        val result =
            analyzeFunctions(
                ast,
                resolvedVariables,
                variablesMap,
            )
        logger?.logSuccessfulFunctionAnalysis(result)
        return result
    }

    private fun filterForeignFunctions(nr: NameResolutionResult): Set<Definition.ForeignFunctionDeclaration> =
        nr.entityResolution
            .values
            .filterIsInstance<ResolvedEntity.WithOverloads>()
            .flatMap { it.overloads.values }
            .filterIsInstance<Definition.ForeignFunctionDeclaration>()
            .toSet()

    private fun getClosureAnalysis(ast: AST, variablesMap: VariablesMap, escapeAnalysis: EscapeAnalysisResult): ClosureAnalysisResult {
        val analyzedClosures = analyzeClosures(ast, variablesMap, escapeAnalysis)
        logger?.logSuccessfulClosureAnalysis(analyzedClosures)
        return analyzedClosures
    }

    private fun findEscapingVariables(
        ast: AST,
        resolvedVariables: ResolvedVariables,
        analyzedFunctions: FunctionAnalysisResult,
        variablesMap: VariablesMap,
        types: TypeCheckingResult,
    ): EscapeAnalysisResult {
        val escapeAnalysisResult =
            try {
                escapeAnalysis(ast, resolvedVariables, analyzedFunctions, variablesMap, types)
            } catch (e: Exception) {
                logger?.logFailedEscapeAnalysis()
                throw e
            }
        logger?.logSuccessfulEscapeAnalysis(escapeAnalysisResult, variablesMap)
        return escapeAnalysisResult
    }

    fun analyzeAst(ast: AST): AstAnalysisResult {
        val resolvedNames = resolveNames(ast)
        val types = checkTypes(ast, resolvedNames)
        val variablesMap = createVariables(ast, types)
        val analyzedFunctions = analyzeFunctions(ast, variablesMap, types.resolvedVariables)
        val escapeAnalysis = findEscapingVariables(ast, types.resolvedVariables, analyzedFunctions, variablesMap, types)
        val closureAnalysis = getClosureAnalysis(ast, variablesMap, escapeAnalysis)
        val handlers =
            generateCallableHandlers(
                analyzedFunctions,
                escapeAnalysis,
                SystemVAMD64CallConvention,
                variablesMap,
                closureAnalysis,
            )
        val foreignFunctions = filterForeignFunctions(resolvedNames)
        return AstAnalysisResult(
            types.resolvedVariables,
            types,
            variablesMap,
            handlers,
            foreignFunctions,
            escapeAnalysis,
        )
    }

    fun getUsedTypes(types: TypeCheckingResult): List<TypeExpr> = types.expressionTypes.values + types.definitionTypes.values

    fun generateControlFlowGraph(
        analyzedAst: AstAnalysisResult,
        callGenerator: CallGenerator,
        objectOutlineLocation: ObjectOutlineLocation,
        lambdaOutlineLocation: LambdaOutlineLocation,
    ): ProgramCFG {
        val cfg =
            generateCFG(
                analyzedAst.resolvedVariables,
                analyzedAst.callableHandlers,
                analyzedAst.variablesMap,
                analyzedAst.types,
                callGenerator,
                objectOutlineLocation,
                lambdaOutlineLocation,
            )
        logger?.logSuccessfulControlFlowGraphGeneration(cfg)
        return cfg
    }

    private fun linearize(
        cfg: ProgramCFG,
        callableHandlers: Map<LambdaExpression, CallableHandler>,
    ): Pair<Map<LambdaExpression, LoweredCFGFragment>, Map<LambdaExpression, RegisterAllocation>> {
        val (covering, registerAllocation) = safeLinearize(cfg, callableHandlers, instructionCovering, allowedRegisters, backupRegs)
        logger?.logSuccessfulInstructionCovering(covering)
        logger?.logSuccessfulRegisterAllocation(registerAllocation)
        return Pair(covering, registerAllocation)
    }

    fun process(input: Input): Pair<String, Map<LambdaExpression, String>> {
        val ast = generateAst(input)
        val semantics = analyzeAst(ast)
        val outlines =
            OutlineCollection(
                createObjectOutlines(getUsedTypes(semantics.types)),
                generateClosureOutlines(semantics.callableHandlers.closureHandlers),
                generateStackFrameOutlines(semantics.callableHandlers.staticFunctionHandlers.values),
            )
        val cfg =
            generateControlFlowGraph(
                semantics,
                SimpleCallGenerator(),
                outlines.objectOutlines.locations,
                outlines.closureOutlines,
            )
        val (covering, registerAllocation) = linearize(cfg, semantics.callableHandlers.getAll())
        val asm =
            covering.mapValues { (function, loweredCFG) ->
                run {
                    val ra = registerAllocation[function] ?: error("No register allocation for function $function")
                    generateAsm(functionBodyLabel(function), loweredCFG, ra)
                }
            }
        return Pair(generateAsmPreamble(semantics.foreignFunctions, outlines.toAsm()), asm)
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
        libraryFiles: List<Path>,
        asmFile: Path,
        objFile: Path,
        binFile: Path,
    ) {
        asmFile.writeText(generateAsm(input))
        assemble(asmFile, objFile)
        val sources = listOf(objFile) + libraryFiles
        val options = listOf("g++", "-no-pie", "-z", "noexecstack", "-o", binFile.toString()) + sources.map { it.toString() }
        val gcc = ProcessBuilder(options).inheritIO().start()
        gcc.waitFor().takeIf { it != 0 }?.let { status ->
            logger?.logFailedLinking(status)
            throw RuntimeException("Unable to link compiled code")
        } ?: logger?.logSuccessfulLinking(binFile)
    }

    fun compile(input: Input, outputName: String, outputDir: Path) {
        compileAndLink(
            input,
            Params.externalLibs,
            Paths.get("$outputDir", "$outputName.asm"),
            Paths.get("$outputDir", "$outputName.o"),
            Paths.get("$outputDir", "$outputName.bin"),
        )
    }
}
