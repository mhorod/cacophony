# Cacophony Compiler

An imperative language made as team project for Compilers course.

## Language Specification

### Extension

Files of cacophony language should have `.cac` extension.

### Comments

There is only one type of comments - inline comments, and there are no multiline comments.
A comment starts with a hash sign `#` and ranges to the end of the line. Everything after the `#` is  ignored by the compiler

### Expressions

There is no distinction between statements and expressions - everything has a value (hence everything is an expression), although sometimes the returned value is `()` which has type `Unit`.

Possible expressions in our language are:

- variable definition
- assignment to a variable
- function definition
- `if` conditional
- `while` loop
- `return` from a function
- arithmetic operations
- logical operations

### Declaring types

Types of names are declared as `<name> : <Type>`.

For readability types are denoted in `Uppercase` while variables and functions are denoted in `lowercase`.

### Variable definition

New variable is introduced as `let <name> : <Type> = <expression>`.

The type is optional as we don't allow uninitialized variables.

In the future we might allow to declare variable without value (in such case the type is required) but with dataflow check to spot use before initialization.

### Function definition

Functions are introduced using lambda-like syntax: `let <name> = [<args>] -> <Type> => <expression>`.

For now this is only part of `let` syntax, the `[<args>] -> <Type> => <expression>` part is not an expression itself, although it might be when we decide to implement anonymous functions.

Functions can be recursive - they can refer to themselves and other visible functions.

Function definitions can be nested - we can define and use new function in the body of another.

The names of the arguments are not visible outside of the function.

### Function overloading and shadowing

When creating a new variable with `let` it shadows all variables and functions with the same name.

However introducing a new function creates a new overload which does not shadow other functions.

### Returning value from a function

Value from a function is returned with `return` keyword, currently with mandatory value.

The `return <expr>` as expression itself has type `Void` which indicates that no calculations happen after return.

`Void` is a subtype of any other type which allows constructions like `let x = if y then return z else w`

### If conditional

The if conditional can take two forms:

- `if <expression> then <expression>`
- `if <expression> then <expression> else <expression>`

In first case, the type is `Unit` as no value is returned if expression evaluates to false.
In the second case the type of `if` is the type of true branch and false branch which have to match.

### While loop

There is only one type of loop - the `while` loop which takes form: `while <expression> do <expression>`

The loop itself takes no value.

It is possible to exit early from a loop by using `break` keyword

### Blocks

To allow for sequential execution/evaluation of expressions they can be organized into blocks of form `<expression>; <expression>; ... <expression>;`

Such block on itself is not treated as an expression - for example
in expressions like:

- `while x do y; z;`
- `if x then y; z;`

`z` does not belong to body of the `while` or `if` and is always executed

To use block as a sub-expression it has to be wrapped in parentheses:

- `while x do (y; z;)`
- `if x then (y; z;)`

The value of a block is the value of the expression after the last semicolon.
In particular:

- `(x; y)` has value of `y`
- `(x;y;)` has Unit value since the last expression is empty
- `(;)`, `(x;)`, `(;;;)`, `(x;;;)` - all are valid blocks with Unit value

### Operators

Currently we don't define what operators we use, it's left for a future decision.
Probably we should cover the basic ones:

- `+` (addition)
- `-` (subtraction)
- `*` (multiplication)
- `/` (integer division)
- `&&` (boolean and)
- `||` (boolean or)
- `<`, `>`, `<=`, `>=`, `==` (comparison operators)
