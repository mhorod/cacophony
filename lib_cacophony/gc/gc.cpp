#include "gc.h"
#include "gcimpl.h"

#include <cassert>

void objectTraversal::traverseObjects(ll *object, bool is_stack_frame) {
    if(LOG_GC) {
        std::cerr << "traversing " << object << ", is stack " << is_stack_frame << std::endl;
    }
    if(!is_stack_frame) markVisited(object);

    ll *outline = object - 1;
    std::vector<ll> offsets = offsetsFromOutline(outline);
    if(object == stack_bottom) {
        // we are at the last stack frame, so we don't want to go lower
        assert(offsets.size() > 0 && offsets[0] == 0);
        offsets.erase(offsets.begin());
    }
    if(LOG_GC) {
        std::cerr << "reference offsets:";
        for(int offset: offsets) std::cerr << " " << offset;
        std::cerr << std::endl;
    }

    for(int offset: offsets) {
        ll **reference = reinterpret_cast<ll**>(object + offset);
        if(*reference == nullptr) continue;
        if(reference_mapping.count(*reference)) {
            if(LOG_GC) {
                std::cerr << "mapping " << *reference << " to " << reference_mapping[*reference] << std::endl;
            }
            *reference = reference_mapping[*reference];
        }
        ll *referenced_object = *reference;

        bool next_is_stack_frame = is_stack_frame && offset == 0;
        if(next_is_stack_frame || !isVisited(referenced_object)) {
            traverseObjects(referenced_object, next_is_stack_frame);
        }
    }
}

void objectTraversal::remapReferences(ll *rbp, std::unordered_map<ll*, ll*> mapping) {
    clear();
    reference_mapping = mapping;
    traverseObjects(rbp, true);
}

std::vector<ll*> objectTraversal::getAliveReferences(ll *rbp) {
    clear();
    traverseObjects(rbp, true);
    std::vector<ll*> alive_objects = std::vector<ll*>(visited_objects.begin(), visited_objects.end());
    if(LOG_GC) {
        std::cerr << "alive objects:";
        for(ll* object: alive_objects) std::cerr << " " << object;
        std::cerr << std::endl;
    }
    return alive_objects;
}

static std::vector<ll> offsetsFromOutline(ll *outline) {
    const int MASK_SIZE = sizeof(ll) * 8;
    ll size = *outline;
    ll num_of_chunks = (size + MASK_SIZE - 1) / MASK_SIZE;
    std::vector<ll> offsets;

    for(int chunk_ind = 0; chunk_ind < num_of_chunks; chunk_ind++) {
        ull chunk = *(outline + chunk_ind + 1);
        for(int pos = 0; pos < MASK_SIZE; pos++) {
            if(chunk & (1ULL << pos)) {
                offsets.push_back(chunk_ind * MASK_SIZE + pos);
            }
        }
    }

    return offsets;
}

static ll getObjectSize(ll *outline) {
    return 8 * (1 + *outline);
}

static void runGc(ll *rbp) {
    if(LOG_GC) std::cerr << "running gc" << std::endl;
    objectTraversal object_traversal;
    std::vector<ll*> alive_objects = object_traversal.getAliveReferences(rbp);
    std::unordered_map<ll*, ll*> reference_mapping = memory_manager.cleanup(alive_objects); 
    object_traversal.remapReferences(rbp, reference_mapping);
}

static ll** allocMemory(ll *outline) {
    ll size = getObjectSize(outline);
    ll **ptr = reinterpret_cast<ll**>(memory_manager.allocateMemory(size));
    if(LOG_GC) {
        std::cerr << "allocating " << size << " bytes at " << ptr << std::endl;
    }
    *ptr = outline;
    return ptr + 1;
}