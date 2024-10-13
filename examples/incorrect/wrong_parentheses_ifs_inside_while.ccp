# ifs inside a while
# MISSING PAREN IN MAIN WHILE
let b: Int = 54;
let ans: Int = 0;
let d: Int = 1;
while (d < (b + 1)) do
    # test whether b == x*d
    let x: Int = 0;
    while (x < (b + 1)) do
        if (x * d) == b then
            ans = ans + 1
        else (if (x * d) > b then break);
    d = d + 1;

