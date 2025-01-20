package cacophony.pipeline

import cacophony.codegen.linearization.BasicBlock
import cacophony.codegen.registers.RegisterAllocation
import cacophony.codegen.registers.RegistersInteraction
import cacophony.controlflow.CFGFragment
import cacophony.controlflow.Register
import cacophony.grammars.AnalyzedGrammar
import cacophony.grammars.ParseTree
import cacophony.parser.CacophonyGrammarSymbol
import cacophony.semantic.analysis.*
import cacophony.semantic.names.*
import cacophony.semantic.syntaxtree.AST
import cacophony.semantic.syntaxtree.Definition
import cacophony.semantic.types.TypeCheckingResult
import cacophony.token.Token
import cacophony.token.TokenCategorySpecific
import java.nio.file.Path

interface Logger {
    fun logSuccessfulLexing(tokens: List<Token<TokenCategorySpecific>>)

    fun logFailedLexing()

    fun logSuccessfulGrammarAnalysis(analyzedGrammar: AnalyzedGrammar<Int, CacophonyGrammarSymbol>)

    fun logFailedGrammarAnalysis()

    fun logSuccessfulParsing(parseTree: ParseTree<CacophonyGrammarSymbol>)

    fun logFailedParsing()

    fun logSuccessfulAstGeneration(ast: AST)

    fun logFailedAstGeneration()

    fun logSuccessfulNameResolution(result: NameResolutionResult)

    fun logFailedNameResolution()

    fun logSuccessfulOverloadResolution(result: ResolvedVariables)

    fun logFailedOverloadResolution()

    fun logSuccessfulTypeChecking(result: TypeCheckingResult)

    fun logFailedTypeChecking()

    fun logFailedEscapeAnalysis()

    fun logSuccessfulEscapeAnalysis(result: EscapeAnalysisResult, variableMap: VariablesMap)

    fun logSuccessfulCallGraphGeneration(callGraph: CallGraph)

    fun logFailedCallGraphGeneration()

    fun logSuccessfulFunctionAnalysis(result: FunctionAnalysisResult)

    fun logSuccessfulRegisterAllocation(allocatedRegisters: Map<Definition.FunctionDefinition, RegisterAllocation>)

    fun logSuccessfulControlFlowGraphGeneration(cfg: Map<Definition.FunctionDefinition, CFGFragment>)

    fun logSuccessfulInstructionCovering(covering: Map<Definition.FunctionDefinition, List<BasicBlock>>)

    fun logSuccessfulRegistersInteractionGeneration(registersInteractions: Map<Definition.FunctionDefinition, RegistersInteraction>)

    fun logSpillHandlingAttempt(spareRegisters: Set<Register.FixedRegister>)

    fun logSuccessfulSpillHandling(covering: Map<Definition.FunctionDefinition, List<BasicBlock>>)

    fun logSuccessfulAsmGeneration(functions: Map<Definition.FunctionDefinition, String>)

    fun logSuccessfulAssembling(dest: Path)

    fun logFailedAssembling(status: Int)

    fun logSuccessfulLinking(dest: Path)

    fun logFailedLinking(status: Int)

    fun logSuccessfulVariableCreation(variableMap: VariablesMap)

    fun logFailedVariableCreation()

    fun logSuccessfulPreambleGeneration(preamble: String)
}
