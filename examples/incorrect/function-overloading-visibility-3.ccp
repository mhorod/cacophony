# shadowed overload is not visible
(
    let f = [] -> Bool -> true;
    let f = [x: Int] => Int => x;
    (
        let f = [] -> Int => 1;
        let check = [x: Int] -> Int => x;
        # ok
        check[f[] + f[1]];
        let f = [x: Bool] => Int => 1;
        # bad
        check[f[] + f[1]];
    )
)