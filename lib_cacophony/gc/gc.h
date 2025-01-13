#ifndef GC_LIB_H
#define GC_LIB_H

#include "gcimpl.h"

#ifndef GC_WAIT
#define GC_WAIT 1
#endif

/* ########### Cacophony Built-ins ########### */
extern "C" {
    ll ** alloc_struct(ll * outline, ll * rbp) {
        static int invocation_nr = 0;
        if(++invocation_nr == GC_WAIT) {
            invocation_nr = 0;
            runGc(rbp);
        }
        return allocMemory(outline);
    }

    void initialize_gc(ll *stack_bottom);
}

#endif /* GC_LIB_H */