package cacophony.controlflow.generation

/**
 * Context for converting Expression into CFG
 *
 * @property currentLoopExit Exit point of current while loop, used in break jump
 */
internal class Context(val currentLoopExit: GeneralCFGVertex.UnconditionalVertex?)
