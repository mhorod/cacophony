# shadowed variable should not be visible
let f : [x: Int] -> Int => x;
let f : Int = 7;
let check = [a: Int] -> Int => a;
# f is Int
check[f[1]];
