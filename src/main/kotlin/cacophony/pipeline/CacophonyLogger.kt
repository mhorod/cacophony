package cacophony.pipeline

import cacophony.codegen.linearization.BasicBlock
import cacophony.codegen.registers.RegisterAllocation
import cacophony.codegen.registers.RegistersInteraction
import cacophony.controlflow.CFGFragment
import cacophony.controlflow.Register
import cacophony.controlflow.Variable
import cacophony.controlflow.print.programCfgToGraphviz
import cacophony.grammars.AnalyzedGrammar
import cacophony.grammars.ParseTree
import cacophony.parser.CacophonyGrammarSymbol
import cacophony.semantic.analysis.*
import cacophony.semantic.names.NameResolutionResult
import cacophony.semantic.names.ResolvedName
import cacophony.semantic.names.ResolvedVariables
import cacophony.semantic.syntaxtree.AST
import cacophony.semantic.syntaxtree.LambdaExpression
import cacophony.semantic.types.TypeCheckingResult
import cacophony.token.Token
import cacophony.token.TokenCategorySpecific
import cacophony.utils.TreePrinter
import java.io.File
import java.nio.file.Path

class CacophonyLogger(
    private val logParsing: Boolean,
    private val logAST: Boolean,
    private val logNameRes: Boolean,
    private val logOverloads: Boolean,
    private val logTypes: Boolean,
    private val logEscapeAnalysis: Boolean,
    private val logVariables: Boolean,
    private val logCallGraph: Boolean,
    private val logFunctions: Boolean,
    private val logClosures: Boolean,
    private val logCFG: Boolean,
    private val logCover: Boolean,
    private val logRegs: Boolean,
    private val logAsm: Boolean,
    private val logDirectory: Path?,
) : Logger {
    private fun logMaybeSave(header: String, content: String?) {
        val escape = "\u001B"
        val bold = "$escape[1m"
        val endBold = "$escape[22m"
        println("\n${bold}$header:$endBold")
        println(content)
        if (logDirectory != null && content != null) {
            val filename = header.lowercase().replace(" ", "_") + ".log"
            File(logDirectory.toString(), filename).printWriter().use { out ->
                out.println(content)
            }
        }
    }

    private fun printError(message: String) {
        val escape = "\u001B"
        val boldRed = "$escape[1;31m"
        val endBoldRed = "$escape[0m"
        println("\n${boldRed}$message$endBoldRed")
    }

    override fun logSuccessfulGrammarAnalysis(analyzedGrammar: AnalyzedGrammar<Int, CacophonyGrammarSymbol>) =
        println("Grammar analysis successful :D")

    override fun logFailedGrammarAnalysis() = printError("Grammar analysis failed :(")

    override fun logSuccessfulLexing(tokens: List<Token<TokenCategorySpecific>>) {
        if (logParsing) {
            logMaybeSave("Found tokens", tokens.joinToString("\n") { it.toString() })
        }
    }

    override fun logFailedLexing() = printError("Lexing failed :(")

    override fun logSuccessfulParsing(parseTree: ParseTree<CacophonyGrammarSymbol>) {
        if (logParsing) {
            logMaybeSave("Parse tree", TreePrinter(StringBuilder()).printTree(parseTree))
        }
    }

    override fun logFailedParsing() = printError("Parsing failed :(")

    override fun logSuccessfulAstGeneration(ast: AST) {
        if (logAST) {
            logMaybeSave("AST", TreePrinter(StringBuilder()).printTree(ast))
        }
    }

    override fun logFailedAstGeneration() = printError("AST generation failed :(")

    override fun logSuccessfulNameResolution(result: NameResolutionResult) {
        if (logNameRes) {
            logMaybeSave(
                "Resolved names",
                result.entries.joinToString("\n") { "${it.key.identifier} -> ${resolvedNameToString(it.value)}" },
            )
        }
    }

    private fun resolvedNameToString(resolvedName: ResolvedName) =
        when (resolvedName) {
            is ResolvedName.Variable -> "Variable ${resolvedName.def}"
            is ResolvedName.Argument -> "Argument ${resolvedName.def}"
            is ResolvedName.Function -> "Function ${resolvedName.def.toMap()}"
        }

    override fun logFailedNameResolution() = printError("Name resolution failed :(")

    override fun logSuccessfulOverloadResolution(result: ResolvedVariables) {
        if (logOverloads) {
            logMaybeSave("Resolved variables", result.entries.joinToString("\n") { "${it.key.identifier} -> ${it.value}" })
        }
    }

    override fun logFailedOverloadResolution() = printError("Overload resolution failed :(")

    override fun logSuccessfulTypeChecking(result: TypeCheckingResult) {
        if (logTypes) {
            logMaybeSave("Types", result.expressionTypes.entries.joinToString("\n") { "${it.key} : ${it.value}" })
        }
    }

    override fun logFailedTypeChecking() = printError("Type checking failed :(")

    override fun logSuccessfulEscapeAnalysis(result: EscapeAnalysisResult, variableMap: VariablesMap) {
        if (logEscapeAnalysis) {
            val variableToDefinition = variableMap.definitions.entries.associate { (k, v) -> v to k }
            logMaybeSave(
                "Escaping variables",
                result
                    .joinToString("\n") { "$it (${variableToDefinition.getOrDefault(it, "no declaration")})" },
            )
        }
    }

    override fun logFailedEscapeAnalysis() = printError("Escape analysis failed :(")

    override fun logSuccessfulVariableCreation(variableMap: VariablesMap) {
        if (logVariables) {
            logMaybeSave(
                "Definition variables",
                variableMap.definitions.entries.joinToString("\n") { "${it.key} -> ${variableToString(it.value)}" },
            )
            logMaybeSave(
                "Assignables variables",
                variableMap.definitions.entries.joinToString("\n") {
                    "${it.key} -> ${variableToString(it.value)}"
                },
            )
        }
    }

    override fun logFailedVariableCreation() {
        printError("Variable creation failed :(")
    }

    override fun logSuccessfulCallGraphGeneration(callGraph: CallGraph) {
        if (logCallGraph) {
            val content = StringBuilder()
            callGraph.forEach { (caller, callees) ->
                content.appendLine("$caller (${caller.getLabel()}/${caller.arguments.size}) calls:")
                callees.forEach { callee ->
                    content.appendLine("  $callee (${callee.getLabel()}/${callee.arguments.size})")
                }
            }
            logMaybeSave("Calls", content.lines().joinToString("\n"))
        }
    }

    override fun logFailedCallGraphGeneration() = printError("Call graph generation failed :(")

    override fun logSuccessfulFunctionAnalysis(result: FunctionAnalysisResult) {
        if (logFunctions) {
            val content = StringBuilder()
            result.forEach { (function, analysis) ->
                content.appendLine(
                    "$function (${function.getLabel()}/${function.arguments.size}) at static depth ${analysis.staticDepth}:",
                )
                content.appendLine("    Parent link: ${analysis.parentLink}")
                content.appendLine("    Used variables: ${analysis.variables.size}")
                analysis.variables.forEach { variable ->
                    val usage =
                        when (variable.useType) {
                            VariableUseType.UNUSED -> "--"
                            VariableUseType.READ -> "r-"
                            VariableUseType.WRITE -> "-w"
                            VariableUseType.READ_WRITE -> "rw"
                        }
                    val from =
                        "${variable.definedIn} (${variable.definedIn.getLabel()}/${variable.definedIn.arguments.size})"
                    content.appendLine("      [$usage] $variable ($variable) from $from")
                }
                content.appendLine("    Variables used in nested functions: ${analysis.variablesUsedInNestedFunctions.size}")
                analysis.variablesUsedInNestedFunctions.forEach { content.appendLine("      $it") }
            }

            logMaybeSave("Function analysis", content.lines().joinToString("\n"))
        }
    }

    override fun logSuccessfulClosureAnalysis(result: ClosureAnalysisResult) {
        if (logClosures) {
            val content = StringBuilder()
            result.forEach { (lambda, closure) ->
                content.appendLine("Lambda $lambda has closure variables:")
                closure.forEach {
                    content.appendLine("  $it")
                }
            }
            logMaybeSave("Closure analysis", content.lines().joinToString("\n"))
        }
    }

    override fun logSuccessfulControlFlowGraphGeneration(cfg: Map<LambdaExpression, CFGFragment>) {
        if (logCFG) {
            logMaybeSave("CFG", programCfgToGraphviz(cfg))
        }
    }

    private fun logCovering(covering: Map<LambdaExpression, List<BasicBlock>>): String {
        val content = StringBuilder()
        covering.forEach { (function, blocks) ->
            content.appendLine("  $function (${function.getLabel()}/${function.arguments.size})")
            blocks.forEach { block ->
                content.appendLine("   ${block.label()}")
                block.instructions().forEach { instruction ->
                    content.appendLine("     $instruction")
                }
            }
        }
        return content.lines().joinToString("\n")
    }

    override fun logSuccessfulInstructionCovering(covering: Map<LambdaExpression, List<BasicBlock>>) {
        if (logCover) {
            logMaybeSave("Instruction covering", logCovering(covering))
        }
    }

    override fun logSuccessfulRegistersInteractionGeneration(registersInteractions: Map<LambdaExpression, RegistersInteraction>) {
        if (logRegs) {
            val content = StringBuilder()
            registersInteractions.forEach { (function, registersInteraction) ->
                content.appendLine("Function $function registers interaction: ")
                content.appendLine("      Interference:")
                registersInteraction.interference.forEach { (k, v) ->
                    if (v.isNotEmpty())
                        content.appendLine("        ${shortRegisterName(k)} -> ${v.map { shortRegisterName(it) }}")
                }
                content.appendLine("      Copying:")
                registersInteraction.copying.forEach { (k, v) ->
                    if (v.isNotEmpty())
                        content.appendLine("        ${shortRegisterName(k)} -> ${v.map { shortRegisterName(it) }}")
                }
            }
            logMaybeSave("Registers interactions", content.lines().joinToString("\n"))
        }
    }

    override fun logSuccessfulRegisterAllocation(allocatedRegisters: Map<LambdaExpression, RegisterAllocation>) {
        if (logRegs) {
            val content = StringBuilder()
            allocatedRegisters.forEach { (function, allocation) ->
                content.appendLine("$function (${function.getLabel()}/${function.arguments.size})")
                content.appendLine("Successful registers:")
                allocation.successful.toSortedMap(compareBy { it.toString() }).forEach { (variable, register) ->
                    content.appendLine("  $variable -> $register")
                }
                content.appendLine("Spilled registers:")
                allocation.spills.forEach { spill ->
                    content.appendLine("  $spill")
                }
            }
            logMaybeSave("Registers allocation", content.lines().joinToString("\n"))
        }
    }

    override fun logSpillHandlingAttempt(spareRegisters: Set<Register.FixedRegister>) {
        if (logRegs) {
            println(
                "Some virtual registers have spilled. Attempting to handle spills using spare registers: [" +
                    spareRegisters.joinToString(", ") { shortRegisterName(it) } + "]",
            )
        }
    }

    override fun logSuccessfulSpillHandling(covering: Map<LambdaExpression, List<BasicBlock>>) {
        if (logRegs) {
            println("Spill handling successful :D")
            logCovering(covering)
        }
    }

    override fun logSuccessfulAsmGeneration(functions: Map<LambdaExpression, String>) {
        if (logAsm) {
            logMaybeSave("Generated ASM", functions.entries.joinToString("\n") { "${it.key} generates asm:\n${it.value}" })
        }
    }

    override fun logSuccessfulPreambleGeneration(preamble: String) {
        if (logAsm) {
            logMaybeSave("Generated ASM preamble", preamble)
        }
    }

    override fun logSuccessfulAssembling(dest: Path) {
        println("Successfully saved compiled object in $dest")
    }

    override fun logFailedAssembling(status: Int) {
        printError("nasm failed with exit code: $status")
    }

    override fun logSuccessfulLinking(dest: Path) {
        println("Successfully saved linked executable in $dest")
    }

    override fun logFailedLinking(status: Int) {
        printError("gcc failed with exit code: $status")
    }

    private fun variableToString(variable: Variable): String =
        when (variable) {
            is Variable.PrimitiveVariable -> "$variable".split("$").last()
            is Variable.StructVariable ->
                "$variable".split("$").last() + " { " +
                    variable.fields.entries.joinToString(", ") { (k, v) -> "$k -> ${variableToString(v)}" } + " }"

            is Variable.Heap -> variable.toString()
        }

    private fun shortRegisterName(register: Register?) =
        when (register) {
            null -> "null"
            is Register.FixedRegister -> register.hardwareRegister.name
            is Register.VirtualRegister -> "$register".split("$").last()
        }
}
