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
    // return statement
    // assignment operators = += *= -= /= %= - they should be right - associative, which is reverse to other operators - how will we handle this?
    // logical operators || &&
    // equality operators == !=
    // comparison operators < > <= >=
    // arithmetic operators - +
    // arithmetic operators * / %
    // unary operators - !
    // type declarations :
    // function call or definition
    // statements - ifThen, ifThenElse, WhileDo
    // nested expressions in parentheses ( ) - important syntax, creates new scope and resets priority (grammar should go back to starting state?)
    // atom expressions - variables, types, keywords
}
