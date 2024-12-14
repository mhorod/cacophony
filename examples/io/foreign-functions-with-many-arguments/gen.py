from random import randint

c_decls = ''
decls = 'foreign write_int: [Int] -> Unit;\n'
calls = ''

for n in range(1, 11):
    c_arg_list = ', '.join(f'int x{i}' for i in range(n))
    c_arg_sum = ' + '.join(f'{i+1} * x{i}' for i in range(n))
    c_decls += f'int f{n}({c_arg_list}) {{ return {c_arg_sum}; }};\n'

    arg_list = ', '.join(f'Int' for i in range(n))
    decls += f'foreign f{n}: [{arg_list}] -> Int;\n'

    args = [randint(-100, 100) for _ in range(n)]
    args[0] += 100 - sum(args[i] * (i+1) for i in range(n))
    args = ', '.join(str(x) for x in args)
    calls += f'write_int[f{n}[{args}]];\n'

with open('foreign-functions.c', 'w') as f:
    print(c_decls, file=f)

with open('input.txt', 'w') as f:
    pass

with open('output.txt', 'w') as f:
    for _ in range(n):
        print(100, file=f)

with open('program.cac', 'w') as f:
    print(decls + calls, file=f)
