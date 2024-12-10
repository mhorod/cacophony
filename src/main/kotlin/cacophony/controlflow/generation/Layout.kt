package cacophony.controlflow.generation

import cacophony.controlflow.CFGNode

sealed class Layout

class SimpleLayout(val access: CFGNode) : Layout()

class StructLayout(val fields: Map<String, Layout>) : Layout()
