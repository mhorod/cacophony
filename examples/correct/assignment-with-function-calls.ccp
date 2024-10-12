let f = [x: Int] -> Int => x;

let x: Int = 42;
x = x + 1;

let y: Int = f[x];
y = x + f[y];

x = -y;

x += f[x];
