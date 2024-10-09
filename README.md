# [name TBD] lang Compiler

An imperative language made as team project for Compilers course.

## Language Specification

### Expressions
There is no distinction between statements and expressions - everything has a value (hence everything is an expression), although sometimes the returned value is `()` or `Unit` or `Void` however we call it.

Possible expressions in our language are:
- variable definition
- assignment to a variable
- function definition
- `if` conditional
- `while` loop
- `return` from a function
- arithmetic operations
- logical operations
- block (list) of consecutive expressions
- comments

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

Functions can be recursive - they can refer to themselves as well as to other functions visible in the same scope.

Function definitions can be nested - we can define and use new function in the body of another.

The names of the arguments are not visible outside of the function.

### Function overloading

We allow overloading - there can be two functions with the same name **defined in the same block** that differ in the argument lists.

### Shadowing

Apart from function overloading there cannot be two variables or functions with the same name defined in the same block.

However, it is possible to shadow already existing variable or function in a nested block.

### Returning value from a function

Value from a function is returned with `return` keyword, currently with mandatory value. 

The `return <expr>` as expression itself has no value (type `Unit`) but the type of expression has to match with return type of the corresponding function

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

To allow for sequential execution/evaluation of expressions they can be organized into blocks of form `(<expression>; <expression>; ...)`

The value (and hence the type) of such block is the value of last expression in the block.


### Operators

Currently we don't define what operators we use, it's left for a future decision.
Probably we should cover the basic ones:
- `+` (addition)
- `-` (subtraction)
- `*` (multiplication)
- `/` (integer division)
- `&&` (boolean and)
- `||` (boolean or)