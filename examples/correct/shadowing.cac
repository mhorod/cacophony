# basic, same scope
(
    let f : Int = 7;
    let f : Bool = true;
    let check = [x: Bool] -> Bool => x;
    check[f];
);

# basic, same scope, same type
(
    let f : Int = 7;
    let f : Int = 4;
    let check = [x: Bool] -> Bool => x;
    check[f == 4];
);

# basic, different scope
(
    let f : Int = 7;
    (
        let f : Bool = true;
        let check = [x: Bool] -> Bool => x;
        check[f];
    );
    let check = [x: Int] -> Int => x;
    check[f];
);

# shadowing as parameter
(
    let x : Bool = true;
    let f = [x: Int] -> Int => x;
    f[1];
);

# shadowing in strange scope
(
    let x : Int = 7;
    let f = [a: Int] -> Int => a;
    f[(let x : Bool = true; let g = [a: Bool] -> Int => 1; g[x])];
);

# shadowing of parameter
(
    let g = [x: Bool] -> Int => 1;
    let f = [x: Int] -> Int => (
        let x : Bool = true;
        g[x]
    );
);

# variable shadows function
(
    let f = [] -> Int => 1;
    let f : Int = 1;
    f + f;
);

# function shadows variable
(
    let f : Int = 1;
    let f = [] -> Int => 1;
    f[] + f[];
);

# function shadows function
(
    let x : Int = 1;
    let f = [a: Bool] -> Bool => a;
    let f = [a: Int] -> Int => a;
    f[x];
);

# parameter shadows function name
(
    let f = [f: Int] -> Int => f + 1;
);
