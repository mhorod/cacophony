# shadowing variable does not retrieve old overloads
let f = [] -> Bool => true;
let f : Int = 7;
let f = [x: Int] -> Bool => true;
# ok
f[1];
# bad
f[];