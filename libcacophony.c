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

"xor %rax, %rax                             \n" // i := 0

".loop_begin:                               \n" // while(true) {

"cmp (%rdi), %rax                           \n" //     if(i == outline.struct_size)
"je .loop_end                               \n" //         break

"mov %rax, %rsi                             \n" //     tmp0 = i
"shr $6, %rsi                               \n" //     tmp0 = i / 64
"mov 8(%rdi, %rsi, 8), %rsi                 \n" //     tmp0 = outline.blocks[i / 64]

"mov %rax, %rcx                             \n" //     tmp1 = i
"and $63, %rcx                              \n" //     tmp1 = i % 64
"mov $1, %rdx                               \n" //     tmp2 = 1
"shl %cl, %rdx                              \n" //     tmp2 = 1 << (i % 64)

"test %rdx, %rsi                            \n" //     if(outline.blocks[i / 64] & (1 << (i % 64)))
"jz .if_end                                 \n" //     {
"neg %rax                                   \n" //
"movq $0, 0(%rbp, %rax, 8)                  \n" //         stack[i] = 0
"neg %rax                                   \n" //
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

#ifndef GC_WAIT
#define GC_WAIT 10
#endif

#ifndef OUTLINE_PTR_OFFSET_FROM_OBJECT
#define OUTLINE_PTR_OFFSET_FROM_OBJECT (-1)
#endif

#ifndef DATA_OFFSET_FROM_OBJECT
#define DATA_OFFSET_FROM_OBJECT 0
#endif

#ifndef SIZE_OFFSET_FROM_OUTLINE
#define SIZE_OFFSET_FROM_OUTLINE 0
#endif

#ifndef BLOCK_OFFSET_FROM_OUTLINE
#define BLOCK_OFFSET_FROM_OUTLINE 1
#endif

#ifndef BLOCK_SIZE_IN_BITS
#define BLOCK_SIZE_IN_BITS 64
#endif

#ifndef NULL_REFERENCE
#define NULL_REFERENCE 0
#endif

static void run_gc(long long *) {}

static long long ** alloc_object(long long * outline_ptr) {
    long long size = 8 * (1 + *outline_ptr);
    long long ** allocated_memory_ptr = malloc(size);
    *allocated_memory_ptr = outline_ptr;
    return allocated_memory_ptr - OUTLINE_PTR_OFFSET_FROM_OBJECT;
}

static void clean_references(long long ** object_ptr) {
    long long * outline_ptr = *(object_ptr + OUTLINE_PTR_OFFSET_FROM_OBJECT);
    long long struct_size = *(outline_ptr + SIZE_OFFSET_FROM_OUTLINE);
    long long ** fields = object_ptr + DATA_OFFSET_FROM_OBJECT;
    long long * blocks = outline_ptr + BLOCK_OFFSET_FROM_OUTLINE;
    for(long long i = 0; i < struct_size; i++) {
        long long field_block = i / BLOCK_SIZE_IN_BITS;
        long long field_bit = 1LL << (i % BLOCK_SIZE_IN_BITS);
        if(blocks[field_block] & field_bit)
            fields[i] = NULL_REFERENCE;
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
