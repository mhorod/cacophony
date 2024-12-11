from random import randint

n = 50
q = 50

ops = []
ans = []

a = [0 for _ in range(n)]
for _ in range(q):
    if randint(0, 1) == 0:
        i = randint(0, n - 1)
        v = randint(-100, 100)
        a[i] = v
        ops.append((0, i, v))
    else:
        l = randint(0, n - 1)
        r = randint(l, n)
        ops.append((1, l, r))
        ans.append(sum(a[l:r]))

with open('input.txt', 'w') as f:
    print(n, q, file=f)
    for x in ops:
        print(*x, file=f)

with open('output.txt', 'w') as f:
    for x in ans:
        print(x, file=f)