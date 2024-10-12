# basic overloading
(
    let f = [] -> Bool => true;
    let f = [x : Int] -> Int => x;
    f[];
    f[1];
)

# many functions
(
    let f = [] -> Bool => true;
    let f = [x : Int] -> Int => x;
    let f = [x : Int, y: Int] -> Int => x + y;
    let f = [x : Int, y: Int, z: Int] -> Int => x + y + z;
    f[] + f[1] + f[1,1] + f[1,1,1];
)

# different scopes
(
    let f = [] -> Bool => true;
    (
        let f = [x : Int] -> Int => x;
        (
            let f = [x : Int, y: Int] -> Int => x + y;
            (
                let f = [x : Int, y: Int, z: Int] -> Int => x + y + z;
                f[] + f[1] + f[1,1] + f[1,1,1];
            )
        )
    )
)

# shadowing/overloading in same scope
(
    let f = [] -> Bool => true;
    let f = [x: Int] => Int => x;
    let f = [] -> Int => 1;
    let check = [x: Int] -> Int => x;
    check[f[] + f[1]];
)

# shadowing/overloading in different scope
(
    let f = [] -> Bool -> true;
    let f = [x: Int] => Int => x;
    (
        let f = [] -> Int => 1;
        let check = [x: Int] -> Int => x;
        check[f[] + f[1]];
        let f = [x: Bool] => Int => 1;
        check[f[] + f[true]];
    )
    let check = [x: Bool] -> Bool => x;
    check[f[] && (f[1] == 1)];
)

# overload inside function definition - basic
(
    let f = [] -> Bool => true;
    let g = [] -> Bool => (
        let f = [x: Int] -> Bool => false;
        if f[] then f[1] else true
    )
)

# overload inside function definition - recursion
(
    let f = [] -> Bool => (
        let f = [x: Int] -> Bool => true;
        if f[x] then true else f[]
    )
)

# shadowing stopping recursion
(
    let f = [x: Bool] -> Int (
        let f = [x: Int] -> Int => x;
        f[1];
    )
)