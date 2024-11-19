package cacophony.controlflow

typealias HardwareRegisterMapping = Map<Register, HardwareRegister>

typealias ValueSlotMapping = Map<ValueLabel, Register>

typealias FillingGuide = Map<ValueLabel, CFGNode>
