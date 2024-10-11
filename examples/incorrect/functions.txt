let f1 = [x: Int] => Bool -> x == 42; # the arrows should be the other way around

let f2 = x: Int -> Bool => x == 7; # parameter list not enclosed in `[]`

let f3 = [x: Int y: Bool] -> Bool => x == 8 && y; # parameters not separated by `,`

let f4 = [x: Int] => x + 1; # missing return type

let f5 = [x] -> Int => 2 * x; # missing parameter type

let g = [x: Int, y: Bool] -> Bool => x < 5 && y; # correct definition
# wrong calls:
g 4 false;
g 4, false;
g[4 false];
