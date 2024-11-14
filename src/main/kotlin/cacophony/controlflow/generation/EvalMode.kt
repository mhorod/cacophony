package cacophony.controlflow.generation

/**
 * Mode of evaluation - how the expression is being used
 */
internal sealed interface EvalMode {
    /**
     * Indicates that the expression is used directly as a value
     */
    data object Value : EvalMode

    /**
     * Indicates that the expression is only computed for its side effects
     */
    data object SideEffect : EvalMode

    /**
     * Indicates that the (boolean) expression is used as a condition of if-then-else or while
     *
     * @property trueEntry Entry point to jump to when the expression is evaluated to true
     * @property falseEntry Entry point to jump to when the expression is evaluated to false
     * @property exit Joined exit point of both branches i.e. where to jump after executing either of them
     */
    data class Conditional(
        val trueEntry: GeneralCFGVertex,
        val falseEntry: GeneralCFGVertex,
        val exit: GeneralCFGVertex.UnconditionalVertex,
    ) : EvalMode
}
