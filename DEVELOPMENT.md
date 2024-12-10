# Design of structs

Example 'literal' of a struct (the explicit type is optional):

    let s : {a : Int, b : Bool, c : {x : Int, y : Int}} = {a : Int = 2, b = false && true, c : {x : Int, y : Int} = {x = 3, y = (let z = 3+4; z)}};

The type of struct is determined by the names and types of its fields (in the unordered manner). Field names have to be unique. Scopes of init expressions are independent.

Type is a subtype of Type B iff for every field of A with type T_A and name N, there is a field of B with the same name N and type T_B such that T_A is a subtype of T_B.

For example, the type `{x: Int, y : {a : Int}}` is a subtype of `{x : Int, y: {a : Int, b : Bool}, z : Bool}`. There is no relation between types `Int` and `{a: Int}`.

Richer type hierarchy implies that we need to implement some logic for the hierarchy of function types.

## Implementation details

Disclaimer: The usage of the String type in the following code snippets is up for discussion and change to the more appropriate type.

Consider the following snippet:

    let s = {x = 5, y = {a = 3, b = true}};
    let f = [] -> Int => s.x + s.y.a;

In theory, we do not need to keep the whole `s` in memory. We could place `s.x` and `s.y.a` on the stack for the static link to work inside `f`, but `x.y.b` could be placed in a virtual register.

To achieve this effect, we propose the following interface for variables:

```kotlin
sealed class VariableInfo
class PrimitiveVariable : VariableInfo()
class StructVariable(val fields: Map<String, Info>) : VariableInfo()

sealed class Variable(open val info: VariableInfo)

class SourceVariable(val definition: Definition, override val info : VariableInfo) : Variable(info)

sealed class AuxVariable(info: VariableInfo) : Variable(info)
class SpillVariable(override val info: PrimitiveVariable) : AuxVariable(info)
class StaticLinkVariable(override val info: PrimitiveVariable) : AuxVariable(info)

class ReturnVariable(override val info: VariableInfo) : Variable(info)
```

The previous snippet should introduce the following `VariableInfo`:

    sx = PrimitiveVariable()
    sya = PrimitiveVariable()
    syb = PrimitiveVariable()
    sy = StructVariable({"a" : sya, "b" : syb})
    s = StructVariable({"x" : sx, "y" : sy})

### AST

Introducing the following node type should be enough (we assume that its semantics are clear).

```kotlin
class SubfieldAccess(val expr: Expression, val field: String)
```

TypeChecker should check if every subfield access is valid.

### Variable Analysis / Name Resolution

New logic to determine all source variables and their usage is needed. FunctionHandler should receive analyzed `SourceVariable` corresponding to every subfield, and not the `Definition` like now.

In particular, the previous example should introduce 5 instances of `SourceVariable` and (after analysis) pipe them to the FunctionHandler.

### Function Handler

The constructor of Function Handler should receive instances of `SourceVariable` and allocate them (on the stack or in the virtual registers) based on information received from Analysis.

The declaration of `generateVariableAccess()` is changed to

```kotlin
fun generateVariableAccess(variable: PrimitiveVariable): CFGNode
```

Its implementation should remain similar.

Additionally, as the value returned from a function can now be of the structural type, the `getReturnRegister() : VirtualRegister` method should change its declaration to something like `getResultVariable() : ResultVariable` and the corresponding ReturnVariable (and allocation of its primitive fields) could be created in the constructor.

### CFG Generation

We introduce the following type for describing how to access the primitive fields/subfields of the given struct.


```kotlin
sealed class Layout

class SimpleLayout(val access: CFGNode) : Layout()
class StructLayout(val fields: Map<String, Layout>) : Layout()
```

The type of the `access` field of the `SubCFG` class is changed to `Layout` instead of `CFGNode`.

As every Expression can have a structural type, the declaration of the `visit` method (and its specializations) inside `CFGGenerator` is changed to

```kotlin
fun visit(expr: Expression, ...) : Layout
```

Most of the resulting changes should be quite automatic (for nodes that work only on the primitive types, changes are trivial).

E.g., in AssignmentHandler, the following fragment

```kotlin
val variableAccess = cfgGenerator.getCurrentFunctionHandler().generateVariableAccess(Variable.SourceVariable(variable))
val valueCFG = cfgGenerator.visit(value, EvalMode.Value, context)
val variableWrite = CFGNode.Assignment(variableAccess, valueCFG.access)
```

Could be replaced with a series of assignments between primitive subfields of `lhs` and `Layout` returned by `visit` (by the way, `Variable.SourceVariable(variable)` should be replaced with `functionHandler.getVariableFromDefinition(variable)` as creating new instances of `SourceVariable` was hacky before, and now should be just impossible given that they hold more information).

### Function Call and Return

Method `generateCall` (or whatever its name is now) should flatten every structural argument and put all primitive subfields into registers/on the stack as specified in the call convention.

For functions returning structural types, we propose the following call convention (this is a visualization of a state of the stack at the moment of the function call):

    field3
    field2
    field1
    arg8
    arg7
    ret address <- rsp

The idea is that the epilogue will move the returned value's fields into corresponding memory cells prepared by `generateCall`. After ret and `rsp` adjustment, the fields should be immediately moved into their destination (usually the `Layout` of a temporary struct consisting of a bunch of virtual registers).

For the primitive types, this part is just `mov vir, rax` after code generated by `generateCall`.

It is worth noting that the `Layout` of the returned value on the stack is different before the call/after ret and inside the function; at atm we handle this by implementing similar (but not quite) logic in two places (`generateCall` and epilogue). We can keep this that way—there is definitely a question of if trying to unify both implementations is in fact cleaner/easier to adjust later.
