# basic recursion
let g = [x: Int] -> Bool => (
    if x == 0 then
        true
    else
        g[x - 1]
);
g[5];

# recursion with some calculations
let factorial = [x: Int] -> Int => (
    if x == 0 then
        1
    else
        factorial[x - 1] * x
);
factorial[7];

# many recursive calls
let fibonacci = [x: Int] -> Int => (
    if x < 2 then
        1
    else
        fibonacci[x - 1] + fibonacci[x - 2]
);
fibonacci[7];

# two arguments nested recursion
let ackermann = [x: Int, y: Int] -> Int => (
    if x == 0 then
        y + 1
    else (
        if y == 1 then
            ackermann(x - 1, 1)
        else
            ackermann(x - 1, ackermann(x, y - 1))
    )
);
ackermann[3, 6];