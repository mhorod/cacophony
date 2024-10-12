let is_even = [x: Int] -> Bool => (
    let is_odd = [x: Int] -> Bool => (
        if x == 0 then
            false
        else
            is_even[x - 1]
    );

    if x == 0 then
        true
    else
        is_odd[x - 1];
);
is_even[10];


let f = [x: Int] -> Int => (
    let g = [x: Int] -> Int => (
        if x < 2 then
            1
        else
            g[x - 1] + f[x - 2]
    );

    if x < 2 then 
        1
    else
        f[x - 1] + g[x - 2]
);
f[6];