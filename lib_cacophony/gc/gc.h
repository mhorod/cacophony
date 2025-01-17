#ifndef GC_LIB_H
#define GC_LIB_H

#include "gcimpl.h"

#include <string.h>

asm(
".text                                      \n"
".p2align 4                                 \n"
".globl clean_refs                          \n"
".type  clean_refs, @function               \n"


"clean_refs:                                \n"

"push %rax                                  \n" // # preserve registers used as local variables
"push %rdi                                  \n" //

"mov 8(%rbp), %rdi                          \n" // outline := <outline address from stack>
"mov (%rdi), %rdi                           \n" // struct_size := outline.size

"mov $1, %rax                               \n" // i := 1

".loop_begin:                               \n" // while(true) {

"cmp %rdi, %rax                             \n" //     if(i >= struct_size)
"jae .loop_end                              \n" //         break

"neg %rax                                   \n" //     # rax is negated because the offsets from rbp are negative
"movq $0, 0(%rbp, %rax, 8)                  \n" //     stack[i] = 0
"neg %rax                                   \n" //     # rax is negated back

"inc %rax                                   \n" //     i++

"jmp .loop_begin                            \n" // }

".loop_end:                                 \n" //

"pop %rdi                                   \n" // # restore registers used as local variables as if they never changed
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
#define GC_WAIT 100
#endif

/* ########### Cacophony Built-ins ########### */
extern "C" {
    ll ** alloc_struct(ll * outline, ll * rbp) {
        static int invocation_nr = 0;
        if(++invocation_nr == GC_WAIT) {
            invocation_nr = 0;
            runGc(rbp);
        }
        long long ** object_ptr = allocMemory(outline);
        memset(object_ptr, 0, 8 * **(object_ptr - 1));
        return object_ptr;
    }
}

#endif /* GC_LIB_H */