package cacophony.semantic.syntaxtree

public class Hierarchy { // dont know how to implement it yet
    // defines priority on grammar productions, which is reverse to the priority of operators
    // for example, grammar on only addition, multiplication and brackets should look like this:
    // S -> S+S | A
    // A -> A+A | B
    // B -> B*B | C
    // C -> id | (S)

    // Priority:
    // semicolons ; dividing expressions
    // TODO: statements - ifThen, ifThenElse, WhileDo
    // return statement
    // declarations (typed and untyped)
    // assignment operators = += *= -= /= %= - they should be right - associative, which is reverse to other operators - how will we handle this?
    // TODO: logical operators || &&
    // TODO:equality operators == !=
    // TODO:comparison operators < > <= >=
    // TODO:arithmetic operators - +
    // TODO:arithmetic operators * / %

    // unary operators - !
    // TODO: function call
    // nested expressions in parentheses ( ) - important syntax, creates new scope and resets priority (grammar should go back to starting state?)
    // atom expressions - variables, types, keywords
}
