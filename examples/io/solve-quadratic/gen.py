from random import randint

tc = []

for a in range(-1,3):
    if a == 0:
        continue
    for y in range(3):
        for x in range(y+1):
            tc.append((a, x, y))

for _ in range(18):
    a = 0
    while a == 0:
        a = randint(-50, 50)
    x = randint(-50, 50)
    y = randint(-50, 50)
    if x > y:
        x, y = y, x
    tc.append((a, x, y))

with open('input.txt', 'w') as f:
    print(len(tc), file=f)
    for a, x, y in tc:
        print(a, -a*(x+y), a*x*y, file=f)

with open('output.txt', 'w') as f:
    for a, x, y in tc:
        print(x, file=f)
        print(y, file=f)
