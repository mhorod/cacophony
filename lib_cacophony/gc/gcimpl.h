#ifndef GC_IMPL_H
#define GC_IMPL_H

#include <vector>
#include <map>
#include <cstdlib>
#include <cstdio>
#include <iostream>
#include <string>

typedef long long ll;

const bool LOG_GC = true;

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

static void runGc(ll *) {}

static ll ** allocMemory(ll* outline) {
    ll size = 8 * (1 + *outline);
    if(LOG_GC) std::cerr << "allocating " << size << " bytes of memory" << std::endl;
    ll **ptr = (ll**)malloc(size);
    *ptr = outline;
    return ptr + 1;
}

#endif /* GC_IMPL_H */