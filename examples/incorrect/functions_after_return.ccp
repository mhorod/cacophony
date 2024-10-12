let f = [] -> Int => (
    return 5;
    return true; # invalid - even though this will never be reached, it must type-check correctly
    return (); # however, `true` and `()` are not of type `Int`
);