#include "gc.h"
#include "gcimpl.h"

#include <cstring>
#include <cassert>

#ifndef MEMORY_BLOCK_SIZE
#define MEMORY_BLOCK_SIZE (1 << 12)
#endif

static ll getObjectSize(ll *outline) {
    return sizeof(ll*) * (1 + *outline);
}

void memoryManager::createNewPage(int size) {
    assert(size % sizeof(ll*) == 0);
    if (LOG_GC)
        std::cerr << "creating page of size: " << size << std::endl;
    ll *ptr = static_cast<ll*>(malloc(size));
    auto page = memoryPage{size, 0, ptr};
    allocated_pages.push_back(page);
    occupied_memory += size;
}

// This step is performed during cleanup, where allocated_pages is being modified, therefore no need to modify it here.
void memoryManager::deallocatePage(memoryPage page) {
    occupied_memory -= page.size;
    free(page.ptr);
}

ll memoryManager::getOccupiedMemory() {
    return occupied_memory;
}

ll* memoryManager::allocateMemory(int size) {
    assert(size % sizeof(ll*) == 0);
    if (size > MEMORY_BLOCK_SIZE) {
        // Big struct, it'll get stored on special big page.
        createNewPage(size);
        return allocated_pages.back().ptr;
    } else {
        memoryPage *last_page = nullptr;
        if (!allocated_pages.empty())
            last_page = &allocated_pages.back();
        if (last_page == nullptr || last_page->size - last_page->occupied < size) {
            createNewPage(MEMORY_BLOCK_SIZE);
            last_page = &allocated_pages.back();
        }
        auto result = last_page->ptr + last_page->occupied / sizeof(ll*);
        last_page->occupied += size;
        if (allocated_pages.size() > 1) {
            int free_space_in_last_page =  allocated_pages.back().size - allocated_pages.back().occupied;
            int previous_page_index = allocated_pages.size() - 2;
            int free_space_in_previous_page = allocated_pages[previous_page_index].size - allocated_pages[previous_page_index].occupied;
            if (free_space_in_previous_page > free_space_in_last_page) {
                std::swap(allocated_pages.back(), allocated_pages[previous_page_index]);
            }
        }
        return result;
    }
}

std::unordered_map<ll*, ll*> memoryManager::cleanup(std::set<ll*> &alive_objects) {
    std::unordered_map<ll*, ll*> translation_map;
    std::vector<memoryPage> pages_to_process;
    std::swap(pages_to_process, allocated_pages);
    std::set<ll*> processed_pages;
    
    auto traverseAliveObjectsOnPage = [&](memoryPage page, auto && handler /* action to perform on each object */) {
        for (auto it = alive_objects.lower_bound(page.ptr+1); it != alive_objects.end() && *it < page.ptr + page.size / sizeof(ll*); it++) {
            handler(*it);
        }
    };
    for (auto &page : pages_to_process) {
        int alive_memory_size = 0;
        traverseAliveObjectsOnPage(page, [&](ll* ptr) {
            ll *outline = reinterpret_cast<ll*>(*(ptr - 1));
            alive_memory_size += getObjectSize(outline);
        });
        if (alive_memory_size == page.occupied) {
            allocated_pages.push_back(page);
            processed_pages.insert(page.ptr);
        }
    }
    memoryPage* free_page = nullptr; 
    for (auto &page : pages_to_process) {
        if (processed_pages.find(page.ptr) != processed_pages.end())
            continue;

        std::vector<ll*> alive_objects_on_page;
        traverseAliveObjectsOnPage(page, [&](ll* ptr) {
            alive_objects_on_page.push_back(ptr);
        });
        if (alive_objects_on_page.empty()) {
            // Deallocate empty page or store it for later usage
            if (free_page == nullptr)
                free_page = &page;
            else
                deallocatePage(page);
            continue;
        }
        if(allocated_pages.empty()) {
            if (free_page != nullptr) {
                allocated_pages.push_back(*free_page);
                free_page = nullptr;
            } else 
                createNewPage(MEMORY_BLOCK_SIZE);
        }
        traverseAliveObjectsOnPage(page, [&](ll* ptr) {
            ll *outline = reinterpret_cast<ll*>(*(ptr - 1));
            int size = getObjectSize(outline);
            memoryPage *last_page = nullptr;
            if (!allocated_pages.empty())
                last_page = &allocated_pages.back();
            if (last_page == nullptr || last_page->size - last_page->occupied < size) {
                if (free_page != nullptr) {
                    last_page = free_page;
                    free_page = nullptr;
                } else {
                    createNewPage(MEMORY_BLOCK_SIZE);
                    last_page = &allocated_pages.back();
                }
            }
            translation_map[ptr] = last_page->ptr + last_page->occupied / sizeof(ll*) + 1;
            std::memcpy(last_page->ptr + last_page->occupied / sizeof(ll*), ptr - 1, size);
            last_page->occupied += size;
        });
        if (free_page == nullptr) 
            free_page = &page;
        else
            deallocatePage(page);
    }
    if (free_page != nullptr)
        deallocatePage(*free_page);
    // Last page can be empty, but we don't free it, as it may be useful in next cleanup to have clean page to copy into.

    if(LOG_GC) {
        std::cerr << "translation map: ";
        for(auto kv: translation_map) std::cerr << kv.first << " to " << kv.second << ", ";
        std::cerr << std::endl;
    }
    return translation_map;
}

static std::vector<ll> offsetsFromOutline(ll *outline) {
    const int MASK_SIZE = sizeof(ll) * 8;
    if(LOG_GC) std::cerr << "outline ptr: " << outline;
    ll size = *outline;
    if(LOG_GC) std::cerr << ", size: " << size << std::endl;
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

void objectTraversal::traverseObjects(ll *object, bool is_stack_frame) {
    if(LOG_GC) {
        std::cerr << "traversing " << object << ", is stack " << is_stack_frame << std::endl;
    }
    if(!is_stack_frame) markVisited(object);

    int outline_offset = is_stack_frame ? 1 : -1;
    ll *outline = reinterpret_cast<ll*>(*(object + outline_offset));
    std::vector<ll> offsets = offsetsFromOutline(outline);
    if(is_stack_frame) {
        for(ll &offset: offsets) offset = -offset;
    }
    if(LOG_GC) {
        std::cerr << "reference offsets:";
        for(int offset: offsets) std::cerr << " " << offset;
        std::cerr << std::endl;
    }

    for(int offset: offsets) {
        ll **reference = reinterpret_cast<ll**>(object + offset);
        if(*reference == nullptr) continue;
        if(remap_refs && reference_mapping.count(*reference)) {
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
    remap_refs = true;
    reference_mapping = std::move(mapping);
    traverseObjects(rbp, true);
}

std::set<ll*> objectTraversal::getAliveReferences(ll *rbp) {
    clear();
    traverseObjects(rbp, true);
    std::set<ll*> alive_objects(visited_objects.begin(), visited_objects.end());
    if(LOG_GC) {
        std::cerr << "alive objects:";
        for(ll* object: alive_objects) std::cerr << " " << object;
        std::cerr << std::endl;
    }
    return alive_objects;
}

static void runGc(ll *rbp) {
    if(LOG_GC) std::cerr << "running gc" << std::endl;
    objectTraversal object_traversal;
    std::set<ll*> alive_objects = object_traversal.getAliveReferences(rbp);
    std::unordered_map<ll*, ll*> reference_mapping = memory_manager.cleanup(alive_objects); 
    object_traversal.remapReferences(rbp, std::move(reference_mapping));
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