#include <cstdio>
#include <cstdlib>
#include <cassert>

/* ########### Standard Foreign Functions ########### */
extern "C" {
    void write_int(long long x) {
        printf("%lld\n", x);
    }

    void write_char(char ch) {
        printf("%c", ch);
    }

    long long read_int() {
        long long x, unused = scanf("%lld", &x);
        fprintf(stderr, "read %lld\n", x);
        return x;
    }

    asm(
    ".text                                      \n"
    ".p2align 4                                 \n"
    ".globl	check_rsp                           \n"
    ".type	check_rsp, @function                \n"
    "check_rsp:                                 \n"
    "mov %rsp, %rdi                             \n"
    "jmp check_rsp_impl                         \n"
    );

    void check_rsp_impl(long long rsp) {
        if (rsp % 16 != 8)
            _Exit(50);
    }

    void * alloc(long long count) {
        return malloc(count);
    }

    long long get_mem(long long * ptr) {
        return *ptr;
    }

    void put_mem(long long * ptr, long long val) {
        *ptr = val;
    }
}
