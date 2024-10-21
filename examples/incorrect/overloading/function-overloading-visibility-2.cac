# variable shadows all overloads (at least for now)
let f = [] -> Bool => true;
let f = [x: Int] -> Bool => true;
let f : Int = 7;
# ok
f + f;
# bad
f[];
