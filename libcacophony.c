#include <stdio.h>
#include <stdlib.h>
#include <assert.h>

/* ########### Standard Foreign Functions ########### */

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

/* ########### Randomness for test purposes ########### */
static unsigned long long rand_seed = 17;
const unsigned long long rand_multiplier = 16807;
const unsigned long long rand_mod = 2147483647;
static unsigned long long rand_() {
    return rand_seed = ((rand_seed * rand_multiplier) % rand_mod);
}

// returns integer from [l, r]
long long randint(long long l, long long r) {
	unsigned long long a = rand_();
	return (a % (r - l + 1) + l);
}

void cassert(long long b) {
	if (b == 0) exit(1);
}

/* ########### Garbage Collection and Allocation ########### */

#ifndef GC_WAIT
#define GC_WAIT 10
#endif

static void run_gc(long long *) {}

static long long ** alloc_memory(long long * outline) {
    long long size = 8 * (1 + *outline);
    long long **ptr = malloc(size);
    *ptr = outline;
    return ptr + 1;
}

/* ########### Cacophony Built-ins ########### */

long long ** alloc_struct(long long * outline, long long * rbp) {
    static int invocation_nr = 0;
    if(++invocation_nr == GC_WAIT) {
        invocation_nr = 0;
        run_gc(rbp);
    }
    return alloc_memory(outline);
}
