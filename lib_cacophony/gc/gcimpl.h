#ifndef GC_IMPL_H
#define GC_IMPL_H

#include <vector>
#include <unordered_map>
#include <unordered_set>
#include <set>
#include <cstdlib>
#include <cstdio>
#include <iostream>

typedef long long ll;
typedef unsigned long long ull;

const bool LOG_GC = true;

struct memoryPage {
    int size;
    int occupied;
    ll *ptr;
};

class memoryManager {
  private:
    void createNewPage(int size);
    std::vector<memoryPage> allocated_pages;
    
  public:
    ll* allocateMemory(int size);
    std::unordered_map<ll*, ll*> cleanup(std::set<ll*> &alive_objects);
};

memoryManager memory_manager;

class objectTraversal {
  private:
    void traverseObjects(ll *object, bool is_stack_frame);

    bool remap_refs = false;
    std::unordered_map<ll*, ll*> reference_mapping;
    std::unordered_set<ll*> visited_objects;
    void markVisited(ll *object) { visited_objects.insert(object); }
    bool isVisited(ll *object) { return object == nullptr || visited_objects.find(object) != visited_objects.end();}
    void clear() { 
        remap_refs = false;
        reference_mapping.clear();
        visited_objects.clear(); 
    }

  public:
    void remapReferences(ll *rbp, std::unordered_map<ll*, ll*> mapping);
    std::set<ll*> getAliveReferences(ll *rbp);
};

static void runGc(ll *rbp);

static ll** allocMemory(ll *outline);

#endif /* GC_IMPL_H */