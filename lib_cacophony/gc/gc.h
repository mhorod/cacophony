#include<vector>
#include<map>

typedef long long ll;

struct memoryPage {
    int size;
    int occupied;
    ll *ptr;
};

class memoryManager {
    public:

    ll * allocateMemory(int size);
    std::map<ll*, ll*> cleanup(std::vector<ll*> &alive_objects);
};

static std::vector<ll> offsetsFromOutline(ll *outline);

static std::vector<ll*> getAliveReferences();

static void remapReferences(std::map<ll*, ll*> mapping);

#ifndef GC_WAIT
#define GC_WAIT 10
#endif

static void runGc(ll *) {}

static ll ** allocMemory(ll* outline) {
    ll size = 8 * (1 + *outline);
    ll **ptr = malloc(size);
    *ptr = outline;
    return ptr + 1;
}

/* ########### Cacophony Built-ins ########### */

ll ** alloc_struct(ll * outline, ll * rbp) {
    static int invocation_nr = 0;
    if(++invocation_nr == GC_WAIT) {
        invocation_nr = 0;
        run_gc(rbp);
    }
    return alloc_memory(outline);
}

void initialize_gc(ll *stack_bottom);