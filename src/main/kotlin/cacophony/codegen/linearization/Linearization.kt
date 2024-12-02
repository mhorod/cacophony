package cacophony.codegen.linearization

import cacophony.codegen.BlockLabel
import cacophony.codegen.instructions.Instruction
import cacophony.codegen.instructions.InstructionCovering
import cacophony.codegen.instructions.cacophonyInstructions.Jmp
import cacophony.codegen.instructions.cacophonyInstructions.LocalLabel
import cacophony.controlflow.CFGFragment
import cacophony.controlflow.CFGLabel
import cacophony.controlflow.CFGVertex
import kotlin.math.absoluteValue

typealias LoweredCFGFragment = List<BasicBlock>

private typealias PartialLinearization = Pair<MutableBasicBlock, Iterator<MutableBasicBlock>>

private class MutableBasicBlock(
    val label: BlockLabel,
    val instructions: MutableList<Instruction> = mutableListOf(LocalLabel(label)),
    val successors: MutableSet<MutableBasicBlock> = mutableSetOf(),
    val predecessors: MutableSet<MutableBasicBlock> = mutableSetOf(),
) : BasicBlock {
    override fun label() = label

    override fun instructions() = instructions

    override fun successors() = successors

    override fun predecessors() = predecessors
}

private fun <T> combine(vararg iters: Iterator<T>) = iterator { iters.forEach { yieldAll(it) } }

private class Linearizer(val fragment: CFGFragment, val covering: InstructionCovering) {
    val visited = mutableMapOf<CFGLabel, MutableBasicBlock>()
    var blocks = 0

    fun getLabel() = BlockLabel("bb${blocks++}_${fragment.hashCode().absoluteValue}")

    fun dfs(label: CFGLabel): PartialLinearization {
        val vertex = fragment.vertices[label]!!
        val block = MutableBasicBlock(getLabel())
        visited[label] = block

        fun handle(vertex: CFGVertex.Final) =
            Pair(
                block.also { it.instructions += covering.coverWithInstructions(vertex.tree) },
                iterator { yield(block) },
            )

        fun handle(vertex: CFGVertex.Jump): PartialLinearization {
            var result = iterator { yield(block) }

            (
                visited[vertex.destination]?.also { neighbor ->
                    block.instructions += covering.coverWithInstructions(vertex.tree)
                    block.instructions.add(Jmp(neighbor.label))
                } ?: dfs(vertex.destination).let { (neighbor, iter) ->
                    block.instructions += covering.coverWithInstructions(vertex.tree)
                    result = combine(result, iter)
                    neighbor
                }
            ).also { neighbor ->
                block.successors.add(neighbor)
                neighbor.predecessors.add(block)
            }

            return Pair(block, result)
        }

        fun handle(vertex: CFGVertex.Conditional): PartialLinearization {
            var result = iterator { yield(block) }

            var swapped = false
            var doubled = false

            val falseBlock =
                (
                    visited[vertex.falseDestination]?.also { swapped = true } ?: dfs(vertex.falseDestination).let { (neighbor, iter) ->
                        result = combine(result, iter)
                        neighbor
                    }
                ).also { neighbor ->
                    block.successors.add(neighbor)
                    neighbor.predecessors.add(block)
                }

            val trueBlock =
                (
                    visited[vertex.trueDestination]?.also {
                        doubled = swapped
                        swapped = false
                    } ?: dfs(vertex.trueDestination).let { (neighbor, iter) ->
                        result = combine(result, iter)
                        neighbor
                    }
                ).also { neighbor ->
                    block.successors.add(neighbor)
                    neighbor.predecessors.add(block)
                }

            if (swapped) {
                // false edge goes backwards, true edge forwards
                block.instructions += covering.coverWithInstructionsAndJump(vertex.tree, falseBlock.label, false)
            } else if (doubled) {
                // both edges go backwards, need 2 jumps
                block.instructions += covering.coverWithInstructionsAndJump(vertex.tree, trueBlock.label)
                block.instructions.add(Jmp(falseBlock.label))
            } else {
                // false edge goes forwards
                block.instructions += covering.coverWithInstructionsAndJump(vertex.tree, trueBlock.label)
            }

            return Pair(block, result)
        }

        return when (vertex) {
            is CFGVertex.Final -> handle(vertex)
            is CFGVertex.Jump -> handle(vertex)
            is CFGVertex.Conditional -> handle(vertex)
        }
    }
}

fun linearize(fragment: CFGFragment, covering: InstructionCovering): LoweredCFGFragment =
    mutableListOf<MutableBasicBlock>().apply {
        Linearizer(fragment, covering).dfs(fragment.initialLabel).second.forEach { this.add(it) }
    }
