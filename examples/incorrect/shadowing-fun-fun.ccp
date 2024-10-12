# shadowed function should not be visible
let x : Int = 7;
let f : [x: Int] -> Int => x;
let f : [x: Bool] -> Bool => x;
# f is [Bool] -> Bool
check[x];
