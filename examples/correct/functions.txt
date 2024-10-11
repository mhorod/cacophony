let simple_check = [x: Int] -> Bool => x == 42; # the body of a function can be a simple expression
simple_check[43]; # functions can be called - returns `false`

let complex_check = [x: Int] -> Bool => (
    let temporary = simple[x];
    temporary || x == 43
); # the body of a function can also be a block
complex_check[43]; # returns true

let global = false;
let confusing = [x: Int] -> Bool =>
    global = true; # careful! this is syntactically correct but the indentation is confusing
    false; # this expression does not belong to the `confusing` function
confusing[5]; # sets `global` to `true` and returns it

let unit: Unit = let f = [x: Int] -> Int => x + 1; # function definition returns `Unit`

# functions can have arity other than 1
let no_params = [] -> Int => 5;
no_params[]; # returns `5`
let many_params = [x: Int, y: Bool] -> Bool => x == 7 && y;
many_params[8, false]; # returns `false`
