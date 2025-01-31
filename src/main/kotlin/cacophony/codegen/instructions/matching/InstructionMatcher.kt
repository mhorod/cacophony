package cacophony.codegen.instructions.matching

import cacophony.codegen.BlockLabel
import cacophony.codegen.instructions.InstructionMaker
import cacophony.codegen.patterns.*
import cacophony.controlflow.*

interface InstructionMatcher {
    fun findMatchesForValue(node: CFGNode, destinationRegister: Register): Set<Match>

    fun findMatchesForSideEffects(node: CFGNode): Set<Match>

    fun findMatchesWithoutTemporaryRegisters(node: CFGNode): Set<Match>

    fun findMatchesForCondition(node: CFGNode, destinationLabel: BlockLabel, jumpIf: Boolean = true): Set<Match>
}

fun slotFillFromMetadata(valueMapping: ValueSlotMapping, metadata: InstructionMatcherImpl.MatchMetadata): SlotFill =
    SlotFill(
        valueMapping,
        metadata.registerFill,
        metadata.constantFill,
        metadata.nodeFill,
    )

class InstructionMatcherImpl(
    private val valuePatterns: List<ValuePattern>,
    private val sideEffectPatterns: List<SideEffectPattern>,
    private val conditionPatterns: List<ConditionPattern>,
    private val noTemporaryRegistersPatterns: List<NoTemporaryRegistersPattern>,
) : InstructionMatcher {
    override fun findMatchesForValue(node: CFGNode, destinationRegister: Register): Set<Match> =
        findMatchesWithInstructionMaker(
            node,
            valuePatterns,
        ) { metadata: MatchMetadata, pattern: ValuePattern ->
            { mapping: ValueSlotMapping ->
                pattern.makeInstance(
                    slotFillFromMetadata(mapping, metadata),
                    destinationRegister,
                )
            }
        }

    override fun findMatchesForSideEffects(node: CFGNode): Set<Match> =
        findMatchesWithInstructionMaker(
            node,
            sideEffectPatterns,
        ) { metadata: MatchMetadata, pattern: SideEffectPattern ->
            { mapping: ValueSlotMapping ->
                pattern.makeInstance(
                    slotFillFromMetadata(mapping, metadata),
                )
            }
        }

    override fun findMatchesWithoutTemporaryRegisters(node: CFGNode): Set<Match> =
        findMatchesWithInstructionMaker(
            node,
            noTemporaryRegistersPatterns,
        ) { metadata: MatchMetadata, pattern: SideEffectPattern ->
            { mapping: ValueSlotMapping ->
                pattern.makeInstance(
                    slotFillFromMetadata(mapping, metadata),
                )
            }
        }

    override fun findMatchesForCondition(node: CFGNode, destinationLabel: BlockLabel, jumpIf: Boolean): Set<Match> =
        findMatchesWithInstructionMaker(
            node,
            conditionPatterns,
        ) { metadata: MatchMetadata, pattern: ConditionPattern ->
            { mapping: ValueSlotMapping ->
                pattern.makeInstance(
                    slotFillFromMetadata(mapping, metadata),
                    destinationLabel,
                    jumpIf,
                )
            }
        }

    data class MatchMetadata(
        val registerFill: MutableMap<RegisterLabel, Register> = mutableMapOf(),
        val constantFill: MutableMap<ConstantLabel, CFGNode.Constant> = mutableMapOf(),
        val toFill: MutableMap<ValueLabel, CFGNode> = mutableMapOf(),
        val nodeFill: MutableMap<NodeLabel, CFGNode> = mutableMapOf(),
        var size: Int = 0,
    )

    // Internal function to save some boilerplate from three exported functions.
    // As only thing that is different is instruction maker, it accepts createInstructionMaker to simplify.
    private fun <PatternType : Pattern> findMatchesWithInstructionMaker(
        node: CFGNode,
        patterns: List<PatternType>,
        createInstructionMaker: (MatchMetadata, PatternType) -> InstructionMaker,
    ): Set<Match> {
        val result: MutableSet<Match> = mutableSetOf()
        for (pattern in patterns) {
            val metadata = MatchMetadata()
            if (tryMatch(node, pattern.tree, metadata)) {
                result.add(
                    Match(
                        createInstructionMaker(metadata, pattern),
                        metadata.toFill,
                        metadata.size,
                        pattern,
                    ),
                )
            }
        }
        return result
    }

    // Tries to match pattern starting at node, updates matchMetadata with fill info and returns true on success.
    private fun tryMatch(node: CFGNode, pattern: CFGNode, matchMetadata: MatchMetadata): Boolean {
        matchMetadata.size += 1
        if (pattern is CFGNode.Slot) {
            when (pattern) {
                is CFGNode.RegisterSlot -> {
                    if (node !is CFGNode.RegisterUse) return false
                    matchMetadata.registerFill[pattern.label] = node.register
                }
                is CFGNode.ConstantSlot -> {
                    if (node !is CFGNode.Constant || !pattern.predicate(node.value)) return false
                    matchMetadata.constantFill[pattern.label] = node
                }
                is CFGNode.ValueSlot -> {
                    if (node !is CFGNode.Value) return false
                    matchMetadata.toFill[pattern.label] = node
                }
                is CFGNode.NodeSlot<*> -> {
                    if (!pattern.clazz.isInstance(node)) return false
                    matchMetadata.nodeFill[pattern.label] = node
                }
            }
        } else {
            if (pattern::class != node::class) return false
            for ((nestedNode, nestedPattern) in node.children().zip(pattern.children())) {
                if (!tryMatch(nestedNode, nestedPattern, matchMetadata)) return false
            }
        }
        return true
    }
}
