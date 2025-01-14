#ifndef GC_LIB_H
#define GC_LIB_H

#include "gcimpl.h"

asm(
".text                                      \n"
".p2align 4                                 \n"
".globl clean_refs                          \n"
".type  clean_refs, @function               \n"


"clean_refs:                                \n"

"push %rax                                  \n" // # preserve registers used as local variables
"push %rdi                                  \n" //
"push %rsi                                  \n" //
"push %rdx                                  \n" //
"push %rcx                                  \n" //

"mov 8(%rbp), %rdi                          \n" // outline := <outline address from stack>

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
"pop %rdi                                   \n" //
"pop %rax                                   \n" //

"ret                                        \n" // return
);

/* ########### Randomness for test purposes ########### */
static unsigned long long rand_seed = 17;
const unsigned long long rand_multiplier = 16807;
const unsigned long long rand_mod = 2147483647;
static unsigned long long rand_() {
    return rand_seed = ((rand_seed * rand_multiplier) % rand_mod);
}

extern "C" {
    // returns integer from [l, r]
    long long randint(long long l, long long r) {
        unsigned long long a = rand_();
        return (a % (r - l + 1) + l);
    }

    void cassert(long long b) {
        if (b == 0) exit(1);
    }
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
#define GC_WAIT 1 // TODO: adjust after testing is finished
#endif

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
extern "C" {
    ll ** alloc_struct(ll * outline, ll * rbp) {
        static int invocation_nr = 0;
        if(++invocation_nr == GC_WAIT) {
            invocation_nr = 0;
            runGc(rbp);
        }
        long long ** object_ptr = allocMemory(outline);
        clean_references(object_ptr);
        return object_ptr;
    }
}

#endif /* GC_LIB_H */