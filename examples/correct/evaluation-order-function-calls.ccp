let f = [x: Int, z: Int] -> Int => x * z + 1;

let x: Int = 3;
let y: Int = 2;
y = f[x = f[x = f[y, x], y = f[x, x]], x * y]
x = f[x = f[x = f[y, x], y = f[x, x]], x * y]