from random import randint

decls = 'foreign write_int: [Int] -> Unit;\n'
calls = ''

for n in range(1, 11):
    arg_list = ', '.join(f'x{i}: Int' for i in range(n))
    arg_sum = ' + '.join(f'{i+1} * x{i}' for i in range(n))
    decls += f'let f = [{arg_list}] -> Int => {arg_sum};\n'

    args = [randint(-100, 100) for _ in range(n)]
    args[0] += 100 - sum(args[i] * (i+1) for i in range(n))
    args = ', '.join(str(x) for x in args)
    calls += f'write_int[f[{args}]];\n'

with open('input.txt', 'w') as f:
    pass

with open('output.txt', 'w') as f:
    for _ in range(n):
        print(100, file=f)

with open('program.cac', 'w') as f:
    print(decls + calls, file=f)
