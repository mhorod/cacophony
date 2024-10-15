# using not visible function
let is_odd = [x: Int] -> Bool => (
        if x == 0 then
            false
        else
            is_even[x - 1]
    );

let is_even = [x: Int] -> Bool => (
    if x == 0 then
        true
    else
        is_odd[x - 1];
);

is_even[10];