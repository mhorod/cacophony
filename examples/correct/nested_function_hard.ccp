let f = [a: Int] -> Int => (
  let g = [
    a: Int,
    b: Bool,
    c: Int
  ] -> Bool => (
    let ret = [d: Int] -> Int => d;
    ret[a];
    ret[c];
    b
  );
  g[a, true, a];
  a
);
f[0]
