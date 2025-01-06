package cacophony.controlflow.print

import cacophony.controlflow.CFGFragment
import cacophony.controlflow.CFGLabel
import cacophony.controlflow.CFGNode
import cacophony.controlflow.CFGVertex
import cacophony.controlflow.Register
import cacophony.controlflow.generation.ProgramCFG
import cacophony.semantic.syntaxtree.Definition

fun programCfgToBuilder(cfg: ProgramCFG): String {
    val builder = StringBuilder()
    builder.append("cfg {\n")
    cfg.entries.forEach { (def, fragment) ->
        builder.append(cfgFragmentToBuilder(def, fragment))
    }
    builder.append("}")
    return builder.toString()
}

fun cfgFragmentToBuilder(def: Definition.FunctionDefinition, fragment: CFGFragment): String {
    val builder = StringBuilder()
    builder.append("  fragment(${def.identifier}Def) {\n")

    val labels = mutableMapOf<CFGLabel, String>()
    fragment.vertices.keys.forEach { labels[it] = "v$it" }
    labels[fragment.initialLabel] = "entry"

    fragment.vertices.forEach { (label, vertex) ->
        run {
            builder.append("    \"${labels[label]}\" does ")
            when (vertex) {
                is CFGVertex.Conditional -> {
                    builder.append("conditional(\"${labels[vertex.trueDestination]}\", \"${labels[vertex.falseDestination]}\")")
                }

                is CFGVertex.Jump -> {
                    builder.append("jump(\"${labels[vertex.destination]}\")")
                }

                is CFGVertex.Final -> {
                    builder.append("final")
                }
            }

            builder.append(" {\n")
            builder.append("      ")
            builder.append(cfgNodeToBuilder(vertex.tree))
            builder.append("\n    }\n")
        }
    }

    builder.append("  }\n")
    return builder.toString()
}

fun cfgNodeToBuilder(tree: CFGNode): String =
    when (tree) {
        is CFGNode.Assignment ->
            cfgNodeToBuilder(tree.destination) + " assign " + cfgNodeToBuilder(tree.value)

        is CFGNode.Call -> "call(\"${tree.functionRef.function?.identifier}\")"
        is CFGNode.MemoryAccess -> "memoryAccess(${cfgNodeToBuilder(tree.destination)})"
        is CFGNode.RegisterSlot -> "registerSlot"
        is CFGNode.RegisterUse -> {
            val register =
                when (tree.register) {
                    is Register.FixedRegister -> tree.register.hardwareRegister.toString().lowercase()
                    is Register.VirtualRegister -> "virtualRegister(\"${tree.register}\")"
                }
            "registerUse($register)"
        }
        is CFGNode.Comment -> "CFGNode.Comment(\"${tree.comment}\")"
        is CFGNode.Constant -> "CFGNode.ConstantKnown(${tree.value})"
        is CFGNode.Function -> "function"
        is CFGNode.FunctionSlot -> "functionSlot"
        CFGNode.NoOp -> "CFGNode.NoOp"
        is CFGNode.Pop -> "popRegister(${tree.register})"
        is CFGNode.Return -> "returnNode(${cfgNodeToBuilder(tree.resultSize)})"
        is CFGNode.Push -> "CFGNode.Push(${cfgNodeToBuilder(tree.value)})"
        is CFGNode.ConstantSlot -> "constantSlot"
        is CFGNode.NodeSlot<*> -> "nodeSlot"
        is CFGNode.ValueSlot -> "valueSlot"
        is CFGNode.Addition -> "(" + cfgNodeToBuilder(tree.lhs) + " add " + cfgNodeToBuilder(tree.rhs) + ")"
        is CFGNode.AdditionAssignment -> "(" + cfgNodeToBuilder(tree.lhs) + " addeq " + cfgNodeToBuilder(tree.rhs) + ")"
        is CFGNode.DivisionAssignment -> "(" + cfgNodeToBuilder(tree.lhs) + " diveq " + cfgNodeToBuilder(tree.rhs) + ")"
        is CFGNode.ModuloAssignment -> "(" + cfgNodeToBuilder(tree.lhs) + " modeq " + cfgNodeToBuilder(tree.rhs) + ")"
        is CFGNode.MultiplicationAssignment -> "(" + cfgNodeToBuilder(tree.lhs) + " muleq " + cfgNodeToBuilder(tree.rhs) + ")"
        is CFGNode.SubtractionAssignment -> "(" + cfgNodeToBuilder(tree.lhs) + " subeq " + cfgNodeToBuilder(tree.rhs) + ")"
        is CFGNode.Division -> "(" + cfgNodeToBuilder(tree.lhs) + " div " + cfgNodeToBuilder(tree.rhs) + ")"
        is CFGNode.Minus -> "minus(${tree.value})"
        is CFGNode.Modulo -> "(" + cfgNodeToBuilder(tree.lhs) + " mod " + cfgNodeToBuilder(tree.rhs) + ")"
        is CFGNode.Multiplication -> "(" + cfgNodeToBuilder(tree.lhs) + " mul " + cfgNodeToBuilder(tree.rhs) + ")"
        is CFGNode.Subtraction -> "(" + cfgNodeToBuilder(tree.lhs) + " sub " + cfgNodeToBuilder(tree.rhs) + ")"
        is CFGNode.Equals -> "(" + cfgNodeToBuilder(tree.lhs) + " eq " + cfgNodeToBuilder(tree.rhs) + ")"
        is CFGNode.Greater -> "(" + cfgNodeToBuilder(tree.lhs) + " gt " + cfgNodeToBuilder(tree.rhs) + ")"
        is CFGNode.GreaterEqual -> "(" + cfgNodeToBuilder(tree.lhs) + " geq " + cfgNodeToBuilder(tree.rhs) + ")"
        is CFGNode.Less -> "(" + cfgNodeToBuilder(tree.lhs) + " lt " + cfgNodeToBuilder(tree.rhs) + ")"
        is CFGNode.LessEqual -> "(" + cfgNodeToBuilder(tree.lhs) + " leq " + cfgNodeToBuilder(tree.rhs) + ")"
        is CFGNode.LogicalNot -> "not(${tree.value})"
        is CFGNode.NotEquals -> "(" + cfgNodeToBuilder(tree.lhs) + " neq " + cfgNodeToBuilder(tree.rhs) + ")"
        is CFGNode.DataLabel -> "CFGNode.DataLabel(${tree.dataLabel})"
    }
