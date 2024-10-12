let early_return = [] -> Bool => (
    while true do
        return true; # an early return from a function is legal
    false
);
early_return[]; # `true`

let with_semicolon = [] -> Int => (return 5;); # the trailing `return` can have a trailing semicolon...
let without_semicolon = [] -> Int => (return 6); # ...or not
let without_block = [] -> Int => return 7; # a `return` is a single expression

let voids_in_action = [] -> Bool => (
    # all these definitions type-check because the type of a `return` expression is `Void` which is a subtype of any type
    let b: Bool = return true;
    let i: Int = return true;
    let u: Unit = return true;
    let v: Void = return true;
    false
);

let nested_return = [] -> Bool => return return return true; # only the rightmost `return` will actually... well... return