package cacophony.pipeline

import cacophony.codegen.linearization.BasicBlock
import cacophony.codegen.registers.Liveness
import cacophony.codegen.registers.RegisterAllocation
import cacophony.controlflow.CFGFragment
import cacophony.controlflow.Register
import cacophony.grammars.AnalyzedGrammar
import cacophony.grammars.ParseTree
import cacophony.semantic.analysis.*
import cacophony.semantic.names.*
import cacophony.semantic.syntaxtree.AST
import cacophony.semantic.syntaxtree.Definition
import cacophony.semantic.types.TypeCheckingResult
import cacophony.token.Token
import java.nio.file.Path

interface Logger<StateT, TokenT : Enum<TokenT>, GrammarSymbol : Enum<GrammarSymbol>> {
    fun logSuccessfulLexing(tokens: List<Token<TokenT>>)

    fun logFailedLexing()

    fun logSuccessfulGrammarAnalysis(analyzedGrammar: AnalyzedGrammar<StateT, GrammarSymbol>)

    fun logFailedGrammarAnalysis()

    fun logSuccessfulParsing(parseTree: ParseTree<GrammarSymbol>)

    fun logFailedParsing()

    fun logSuccessfulAstGeneration(ast: AST)

    fun logFailedAstGeneration()

    fun logSuccessfulNameResolution(result: NameResolutionResult)

    fun logFailedNameResolution()

    fun logSuccessfulOverloadResolution(result: ResolvedVariables)

    fun logFailedOverloadResolution()

    fun logSuccessfulTypeChecking(result: TypeCheckingResult)

    fun logFailedTypeChecking()

    fun logSuccessfulCallGraphGeneration(callGraph: CallGraph)

    fun logFailedCallGraphGeneration()

    fun logSuccessfulFunctionAnalysis(result: FunctionAnalysisResult)

    fun logFailedFunctionAnalysis()

    fun logSuccessfulRegisterAllocation(allocatedRegisters: Map<Definition.FunctionDefinition, RegisterAllocation>)

    fun logSuccessfulControlFlowGraphGeneration(cfg: Map<Definition.FunctionDefinition, CFGFragment>)

    fun logSuccessfulInstructionCovering(covering: Map<Definition.FunctionDefinition, List<BasicBlock>>)

    fun logSuccessfulLivenessGeneration(liveness: Map<Definition.FunctionDefinition, Liveness>)

    fun logSpillHandlingAttempt(spareRegisters: Set<Register.FixedRegister>)

    fun logSuccessfulSpillHandling(covering: Map<Definition.FunctionDefinition, List<BasicBlock>>)

    fun logSuccessfulAssembling(dest: Path)

    fun logFailedAssembling(status: Int)

    fun logSuccessfulLinking(dest: Path)

    fun logFailedLinking(status: Int)
}
