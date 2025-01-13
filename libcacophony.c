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

asm(
".text                                      \n"
".p2align 4                                 \n"
".globl clean_refs                          \n"
".type  clean_refs, @function               \n"


"clean_refs:                                \n"

"push %rax                                  \n" // # preserve registers used as local variables
"push %rsi                                  \n" //
"push %rdx                                  \n" //
"push %rcx                                  \n" //

"mov $1, %rax                               \n" // i := 1

".loop_begin:                               \n" // while(true) {

"cmp (%rdi), %rax                           \n" //     if(i >= outline.struct_size)
"jae .loop_end                              \n" //         break

"mov %rax, %rsi                             \n" //     tmp0 = i
"shr $6, %rsi                               \n" //     tmp0 = i / 64
"mov 8(%rdi, %rsi, 8), %rsi                 \n" //     tmp0 = outline.blocks[i / 64]

"mov %rax, %rcx                             \n" //     tmp1 = i
"and $63, %rcx                              \n" //     tmp1 = i % 64
"mov $1, %rdx                               \n" //     tmp2 = 1
"shl %cl, %rdx                              \n" //     tmp2 = 1 << (i % 64)

"test %rdx, %rsi                            \n" //     if(outline.blocks[i / 64] & (1 << (i % 64)))
"jz .if_end                                 \n" //     {

"neg %rax                                   \n" //         # rax is negated because the offsets from rbp are negative
"movq $0, 0(%rbp, %rax, 8)                  \n" //         stack[i] = 0
"neg %rax                                   \n" //         # rax is negated back

".if_end:                                   \n" //     }

"inc %rax                                   \n" //     i++

"jmp .loop_begin                            \n" // }

".loop_end:                                 \n" //

"pop %rcx                                   \n" // # restore registers used as local variables as if they never changed
"pop %rdx                                   \n" //
"pop %rsi                                   \n" //
"pop %rax                                   \n" //

"ret                                        \n" // return
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

/* ########### Garbage Collection and Allocation ########### */

/*
 * The memory associated with an allocated object looks as follows:
 *
 *      ----- increasing addresses at object site ----->
 *      [8-byte pointer to outline] [8 bytes of object data] [8 bytes of object data] ...
 *                |                 ^
 *                |                 |
 *                |                 the 8 byte pointer returned from `alloc_object` points here
 *                |
 *                V
 *                [S: 8-byte size of object data] [A: 0th 8-byte block of outline] [B: 1st 8-byte block of outline] ...
 *                ----- increasing addresses at outline site ----->
 *
 * In the outline:
 * - the size of object data is understood as the number of 8-byte blocks (i.e. Int or reference has size 1)
 * - A, B, ... are ceil(size / 64) blocks of outline after the size block S - they contain info if a field in the struct
 *   is a pointer
 * Therefore, to check if the i-th field in flattened structure is a pointer, check:
 *   block number i / 64 on bit position i % 64.
 */

#ifndef GC_WAIT
#define GC_WAIT 10
#endif

static void run_gc(long long *) {}

static long long ** alloc_object(long long * outline_ptr) {
    long long size = 8 * (1 + *outline_ptr);
    long long ** allocated_memory_ptr = malloc(size);
    *allocated_memory_ptr = outline_ptr;
    return allocated_memory_ptr + 1;
}

static void clean_references(long long ** object_ptr) {
    long long * outline_ptr = *(object_ptr - 1);
    long long struct_size = *outline_ptr;
    long long ** fields = object_ptr;
    long long * blocks = outline_ptr + 1;
    for(long long i = 0; i < struct_size; i++) {
        long long field_block = i / 64;
        long long field_bit = 1LL << (i % 64);
        if(blocks[field_block] & field_bit)
            fields[i] = 0;
    }
}

/* ########### Cacophony Built-ins ########### */

long long ** alloc_struct(long long * outline, long long * rbp) {
    static int invocation_nr = 0;
    if(++invocation_nr == GC_WAIT) {
        invocation_nr = 0;
        run_gc(rbp);
    }
    long long ** object_ptr = alloc_object(outline);
    clean_references(object_ptr);
    return object_ptr;
}
