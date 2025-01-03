package cacophony.pipeline

import cacophony.codegen.linearization.BasicBlock
import cacophony.codegen.registers.RegisterAllocation
import cacophony.codegen.registers.RegistersInteraction
import cacophony.controlflow.CFGFragment
import cacophony.controlflow.Register
import cacophony.controlflow.Variable
import cacophony.controlflow.programCfgToGraphviz
import cacophony.grammars.AnalyzedGrammar
import cacophony.grammars.ParseTree
import cacophony.parser.CacophonyGrammarSymbol
import cacophony.semantic.analysis.CallGraph
import cacophony.semantic.analysis.FunctionAnalysisResult
import cacophony.semantic.analysis.VariableUseType
import cacophony.semantic.analysis.VariablesMap
import cacophony.semantic.names.NameResolutionResult
import cacophony.semantic.names.ResolvedName
import cacophony.semantic.names.ResolvedVariables
import cacophony.semantic.syntaxtree.AST
import cacophony.semantic.syntaxtree.Definition
import cacophony.semantic.types.TypeCheckingResult
import cacophony.token.Token
import cacophony.token.TokenCategorySpecific
import cacophony.utils.TreePrinter
import java.nio.file.Path

class CacophonyLogger : Logger<Int, TokenCategorySpecific, CacophonyGrammarSymbol> {
    override fun logSuccessfulGrammarAnalysis(analyzedGrammar: AnalyzedGrammar<Int, CacophonyGrammarSymbol>) =
        println("Grammar analysis successful :D")

    override fun logFailedGrammarAnalysis() = println("Grammar analysis failed :(")

    override fun logSuccessfulLexing(tokens: List<Token<TokenCategorySpecific>>) {
        println("Lexing successful :D")
        println("Found tokens:")
        tokens.forEach { println("  $it") }
    }

    override fun logFailedLexing() = println("Lexing failed :(")

    override fun logSuccessfulParsing(parseTree: ParseTree<CacophonyGrammarSymbol>) {
        println("Parsing successful :D")
        println("Parse tree:")
        println(TreePrinter(StringBuilder()).printTree(parseTree))
    }

    override fun logFailedParsing() = println("Parsing failed :(")

    override fun logSuccessfulAstGeneration(ast: AST) {
        println("AST generation successful :D")
        println("AST:")
        println(TreePrinter(StringBuilder()).printTree(ast))
    }

    override fun logFailedAstGeneration() = println("AST generation failed :(")

    override fun logSuccessfulNameResolution(result: NameResolutionResult) {
        println("Name resolution successful :D")
        println("Resolved names:")
        result.forEach { println("  ${it.key.identifier} -> ${resolvedNameToString(it.value)}") }
    }

    private fun resolvedNameToString(resolvedName: ResolvedName) =
        when (resolvedName) {
            is ResolvedName.Variable -> "Variable ${resolvedName.def}"
            is ResolvedName.Argument -> "Argument ${resolvedName.def}"
            is ResolvedName.Function -> "Function ${resolvedName.def.toMap()}"
        }

    override fun logFailedNameResolution() = println("Name resolution failed :(")

    override fun logSuccessfulOverloadResolution(result: ResolvedVariables) {
        println("Overload resolution successful :D")
        println("Resolved variables:")
        result.forEach { println("  ${it.key.identifier} -> ${it.value}") }
    }

    override fun logFailedOverloadResolution() = println("Overload resolution failed :(")

    override fun logSuccessfulTypeChecking(result: TypeCheckingResult) {
        println("Type checking successful :D")
        println("Types:")
        result.expressionTypes.forEach { println("  ${it.key} : ${it.value}") }
    }

    override fun logFailedTypeChecking() = println("Type checking failed :(")

    override fun logSuccessfulCallGraphGeneration(callGraph: CallGraph) {
        println("Call graph generation successful :D")
        println("Calls:")
        callGraph.forEach { (caller, callees) ->
            println("  $caller (${caller.identifier}/${caller.arguments.size}) calls:")
            callees.forEach { callee ->
                println("    $callee (${callee.identifier}/${callee.arguments.size})")
            }
        }
    }

    override fun logFailedCallGraphGeneration() = println("Call graph generation failed :(")

    override fun logSuccessfulFunctionAnalysis(result: FunctionAnalysisResult) {
        println("Function analysis successful :D")
        result.forEach { (function, analysis) ->
            println("  $function (${function.identifier}/${function.arguments.size}) at static depth ${analysis.staticDepth}:")
            println("    Parent link: ${analysis.parentLink}")
            println("    Used variables: ${analysis.variables.size}")
            analysis.variables.forEach { variable ->
                val usage =
                    when (variable.useType) {
                        VariableUseType.UNUSED -> "--"
                        VariableUseType.READ -> "r-"
                        VariableUseType.WRITE -> "-w"
                        VariableUseType.READ_WRITE -> "rw"
                    }
                val from =
                    "${variable.definedIn} (${variable.definedIn.identifier}/${variable.definedIn.arguments.size})"
                println("      [$usage] $variable ($variable) from $from")
            }
            println("    Variables used in nested functions: ${analysis.variablesUsedInNestedFunctions.size}")
            analysis.variablesUsedInNestedFunctions.forEach { println("      $it") }
        }
    }

    override fun logFailedFunctionAnalysis() = println("Function analysis failed :(")

    override fun logSuccessfulRegisterAllocation(allocatedRegisters: Map<Definition.FunctionDefinition, RegisterAllocation>) {
        println("Register allocation successful :D")
        allocatedRegisters.forEach { (function, allocation) ->
            println("  $function (${function.identifier}/${function.arguments.size})")
            println("Successful registers:")
            allocation.successful.toSortedMap(compareBy { it.toString() }).forEach { (variable, register) ->
                println("  $variable -> $register")
            }
            println("Spilled registers:")
            allocation.spills.forEach { spill ->
                println("  $spill")
            }
        }
    }

    override fun logSuccessfulControlFlowGraphGeneration(cfg: Map<Definition.FunctionDefinition, CFGFragment>) {
        println("Control flow graph generation successful :D")
        println(programCfgToGraphviz(cfg))
    }

    private fun logCovering(covering: Map<Definition.FunctionDefinition, List<BasicBlock>>) {
        covering.forEach { (function, blocks) ->
            println("  $function (${function.identifier}/${function.arguments.size})")
            blocks.forEach { block ->
                println("   ${block.label()}")
                block.instructions().forEach { instruction ->
                    println("     $instruction")
                }
            }
        }
    }

    override fun logSuccessfulInstructionCovering(covering: Map<Definition.FunctionDefinition, List<BasicBlock>>) {
        println("Instruction covering successful :D")
        logCovering(covering)
    }

    override fun logSuccessfulRegistersInteractionGeneration(
        registersInteractions: Map<Definition.FunctionDefinition, RegistersInteraction>,
    ) {
        println("Registers interaction generation successful :D")
        registersInteractions.forEach { (function, registersInteraction) ->
            println("  Function $function registers interaction: ")
            println("        Interference:")
            registersInteraction.interference.forEach { (k, v) ->
                if (v.isNotEmpty())
                    println("          ${shortRegisterName(k)} -> ${v.map { shortRegisterName(it) }}")
            }

            println("        Copying:")
            registersInteraction.copying.forEach { (k, v) ->
                if (v.isNotEmpty())
                    println("          ${shortRegisterName(k)} -> ${v.map { shortRegisterName(it) }}")
            }
        }
    }

    override fun logSpillHandlingAttempt(spareRegisters: Set<Register.FixedRegister>) {
        println(
            "Some virtual registers have spilled. Attempting to handle spills using spare registers: [" +
                spareRegisters.joinToString(", ") { shortRegisterName(it) } + "]",
        )
    }

    override fun logSuccessfulSpillHandling(covering: Map<Definition.FunctionDefinition, List<BasicBlock>>) {
        println("Spill handling successful :D")
        logCovering(covering)
    }

    override fun logSuccessfulAssembling(dest: Path) {
        println("Successfully saved compiled object in $dest")
    }

    override fun logFailedAssembling(status: Int) {
        println("nasm failed with exit code: $status")
    }

    override fun logSuccessfulLinking(dest: Path) {
        println("Successfully saved linked executable in $dest")
    }

    override fun logFailedLinking(status: Int) {
        println("gcc failed with exit code: $status")
    }

    override fun logSuccessfulVariableCreation(variableMap: VariablesMap) {
        println("Variable creation successful :D")
        println("  Definition variables:")
        variableMap.definitions.forEach { (definition, variable) ->
            println("    $definition -> ${variableToString(variable)}")
        }

        println("  Assignables' variables:")
        variableMap.lvalues.forEach { (assignable, variable) ->
            println("    $assignable -> ${variableToString(variable)}")
        }
    }

    private fun variableToString(variable: Variable): String =
        when (variable) {
            is Variable.PrimitiveVariable -> "$variable".split("$").last()
            is Variable.StructVariable ->
                "$variable".split("$").last() + " { " +
                    variable.fields.entries.joinToString(", ") { (k, v) -> "$k -> ${variableToString(v)}" } + " }"
        }

    private fun shortRegisterName(register: Register?) =
        when (register) {
            null -> "null"
            is Register.FixedRegister -> register.hardwareRegister.name
            is Register.VirtualRegister -> "$register".split("$").last()
        }
}
