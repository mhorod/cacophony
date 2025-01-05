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

class CacophonyLogger(
    private val logParsing: Boolean,
    private val logAST: Boolean,
    // private val logAnalysis: Boolean,
    private val logNameRes: Boolean,
    private val logOverloads: Boolean,
    private val logTypes: Boolean,
    private val logVariables: Boolean,
    private val logCallGraph: Boolean,
    private val logFunctions: Boolean,
    private val logCFG: Boolean,
    private val logCover: Boolean,
    private val logRegs: Boolean,
    private val logAsm: Boolean,
) : Logger<Int, TokenCategorySpecific, CacophonyGrammarSymbol> {
    override fun logSuccessfulGrammarAnalysis(analyzedGrammar: AnalyzedGrammar<Int, CacophonyGrammarSymbol>) =
        println("Grammar analysis successful :D")

    override fun logFailedGrammarAnalysis() = println("Grammar analysis failed :(")

    override fun logSuccessfulLexing(tokens: List<Token<TokenCategorySpecific>>) {
        if (logParsing) {
            println("Lexing successful :D")
            println("Found tokens:")
            tokens.forEach { println("  $it") }
        }
    }

    override fun logFailedLexing() = println("Lexing failed :(")

    override fun logSuccessfulParsing(parseTree: ParseTree<CacophonyGrammarSymbol>) {
        if (logParsing) {
            println("Parsing successful :D")
            println("Parse tree:")
            println(TreePrinter(StringBuilder()).printTree(parseTree))
        }
    }

    override fun logFailedParsing() = println("Parsing failed :(")

    override fun logSuccessfulAstGeneration(ast: AST) {
        if (logAST) {
            println("AST generation successful :D")
            println("AST:")
            println(TreePrinter(StringBuilder()).printTree(ast))
        }
    }

    override fun logFailedAstGeneration() = println("AST generation failed :(")

    override fun logSuccessfulNameResolution(result: NameResolutionResult) {
        if (logNameRes) {
            println("Name resolution successful :D")
            println("Resolved names:")
            result.forEach { println("  ${it.key.identifier} -> ${resolvedNameToString(it.value)}") }
        }
    }

    private fun resolvedNameToString(resolvedName: ResolvedName) =
        when (resolvedName) {
            is ResolvedName.Variable -> "Variable ${resolvedName.def}"
            is ResolvedName.Argument -> "Argument ${resolvedName.def}"
            is ResolvedName.Function -> "Function ${resolvedName.def.toMap()}"
        }

    override fun logFailedNameResolution() = println("Name resolution failed :(")

    override fun logSuccessfulOverloadResolution(result: ResolvedVariables) {
        if (logOverloads) {
            println("Overload resolution successful :D")
            println("Resolved variables:")
            result.forEach { println("  ${it.key.identifier} -> ${it.value}") }
        }
    }

    override fun logFailedOverloadResolution() = println("Overload resolution failed :(")

    override fun logSuccessfulTypeChecking(result: TypeCheckingResult) {
        if (logTypes) {
            println("Type checking successful :D")
            println("Types:")
            result.expressionTypes.forEach { println("  ${it.key} : ${it.value}") }
        }
    }

    override fun logFailedTypeChecking() = println("Type checking failed :(")

    override fun logSuccessfulVariableCreation(variableMap: VariablesMap) {
        if (logVariables) {
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
    }

    override fun logSuccessfulCallGraphGeneration(callGraph: CallGraph) {
        if (logCallGraph) {
            println("Call graph generation successful :D")
            println("Calls:")
            callGraph.forEach { (caller, callees) ->
                println("  $caller (${caller.identifier}/${caller.arguments.size}) calls:")
                callees.forEach { callee ->
                    println("    $callee (${callee.identifier}/${callee.arguments.size})")
                }
            }
        }
    }

    override fun logFailedCallGraphGeneration() = println("Call graph generation failed :(")

    override fun logSuccessfulFunctionAnalysis(result: FunctionAnalysisResult) {
        if (logFunctions) {
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
    }

    override fun logFailedFunctionAnalysis() = println("Function analysis failed :(")

    override fun logSuccessfulControlFlowGraphGeneration(cfg: Map<Definition.FunctionDefinition, CFGFragment>) {
        if (logCFG) {
            println("Control flow graph generation successful :D")
            println(programCfgToGraphviz(cfg))
        }
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
        if (logCover) {
            println("Instruction covering successful :D")
            logCovering(covering)
        }
    }

    override fun logSuccessfulRegistersInteractionGeneration(
        registersInteractions: Map<Definition.FunctionDefinition, RegistersInteraction>,
    ) {
        if (logRegs) {
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
    }

    override fun logSuccessfulRegisterAllocation(allocatedRegisters: Map<Definition.FunctionDefinition, RegisterAllocation>) {
        if (logRegs) {
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
    }

    override fun logSpillHandlingAttempt(spareRegisters: Set<Register.FixedRegister>) {
        if (logRegs) {
            println(
                "Some virtual registers have spilled. Attempting to handle spills using spare registers: [" +
                    spareRegisters.joinToString(", ") { shortRegisterName(it) } + "]",
            )
        }
    }

    override fun logSuccessfulSpillHandling(covering: Map<Definition.FunctionDefinition, List<BasicBlock>>) {
        if (logRegs) {
            println("Spill handling successful :D")
            logCovering(covering)
        }
    }

    override fun logSuccessfulAsmGeneration(functions: Map<Definition.FunctionDefinition, String>) {
        if (logAsm) {
            functions.forEach { (function, asm) -> println("$function generates asm:\n$asm") }
        }
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
