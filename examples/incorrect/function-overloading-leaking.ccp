# leaking overload from inside scope
let f = [] -> Bool => true;
(
    let f = [Int: x] -> Int => x;
    # ok
    f[];
    # ok
    f[1];
)
# ok
f[];
# bad
f[2];