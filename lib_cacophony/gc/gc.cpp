#include "gc.h"
#include "gcimpl.h"

#include <cstring>
#include <functional>
#include <set>

#include <cassert>

#ifndef MEMORY_BLOCK_SIZE
#define MEMORY_BLOCK_SIZE (1 << 12)
#endif

void memoryManager::createNewPage(int size) {
    assert(size % sizeof(ll*) == 0);
    ll *ptr = static_cast<ll*>(malloc(size));
    auto page = memoryPage{size, 0, ptr};
    allocated_pages.push_back(page);
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
            int previous_page_index = allocated_pages.size()-2;
            int free_space_in_previous_page = allocated_pages[previous_page_index].size - allocated_pages[previous_page_index].occupied;
            if (free_space_in_previous_page > free_space_in_last_page) {
                std::swap(allocated_pages.back(), allocated_pages[previous_page_index]);
            }
        }
        return result;
    }
}

std::unordered_map<ll*, ll*> memoryManager::cleanup(std::vector<ll*> &alive_objects) {
    std::unordered_map<ll*, ll*> translation_map;

    std::vector<memoryPage> pages_to_process;
    std::swap(pages_to_process, allocated_pages);

    std::set<ll*> alive_objects_set(alive_objects.begin(), alive_objects.end());
    std::set<ll*> processed_pages;
    
    auto traverse_pages = [&](memoryPage page, std::function<void(ll*)> handler) {
        for (auto it = page.ptr + 1; it < page.ptr + page.occupied / sizeof(ll*); ) {
            handler(it);
            ll *outline = reinterpret_cast<ll*>(*(it-1));
            it += (*outline + 1);
        }
        
    };
    for (auto &page : pages_to_process) {
        bool is_page_untouched = true;
        traverse_pages(page, [&](ll* ptr) {
            if (alive_objects_set.find(ptr) == alive_objects_set.end())
                is_page_untouched = false;
        });
        if (is_page_untouched) {
            allocated_pages.push_back(page);
            processed_pages.insert(page.ptr);
        }
    }
    memoryPage* free_page = nullptr; 
    for (auto &page : pages_to_process) {
        if (processed_pages.find(page.ptr) != processed_pages.end())
            continue;

        std::vector<ll*> alive_objects_on_page;
        traverse_pages(page, [&](ll* ptr) {
            if (alive_objects_set.find(ptr) != alive_objects_set.end())
                alive_objects_on_page.push_back(ptr);
        });
        if (alive_objects_on_page.empty()) {
            // Deallocate empty page or store it for later usage
            if (free_page == nullptr)
                free_page = &page;
            else
                free(page.ptr);
            continue;
        }
        if(allocated_pages.empty()) {
            if (free_page != nullptr) {
                allocated_pages.push_back(*free_page);
                free_page = nullptr;
            } else 
                createNewPage(MEMORY_BLOCK_SIZE);
        }
        traverse_pages(page, [&](ll* ptr) {
            if (alive_objects_set.find(ptr) == alive_objects_set.end()) 
                return;
            ll *outline = reinterpret_cast<ll*>(*(ptr-1));
            int size = *outline + 1;
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
            translation_map[last_page->ptr + last_page->occupied / sizeof(ll*)] = ptr - 1;
            std::memcpy(last_page->ptr + last_page->occupied / sizeof(ll*), ptr - 1, size / sizeof(ll*));
            last_page->occupied += size;
        });
        if (free_page == nullptr) 
            free_page = &page;
        else
            free(page.ptr);
    }
    if (free_page != nullptr)
        free(free_page->ptr);
    // Last page can be empty, but we don't free it, as it may be useful in next cleanup to have clean page to copy into.

    return translation_map;
}

void objectTraversal::traverseObjects(ll *object, bool is_stack_frame) {
    if(LOG_GC) {
        std::cerr << "traversing " << object << ", is stack " << is_stack_frame << std::endl;
    }
    if(!is_stack_frame) markVisited(object);

    ll *outline = reinterpret_cast<ll*>(*(object - 1));
    std::vector<ll> offsets = offsetsFromOutline(outline);
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