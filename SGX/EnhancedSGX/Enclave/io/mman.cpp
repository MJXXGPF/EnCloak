#include <stdarg.h>
#include "Enclave.h"
#include "Enclave_t.h"
#include "stdio.h"
#include "unistd.h"
#include "mman.h"
//TODO   need modify ! 
void *mmap(void *addr, size_t length, int prot, int flags, int fd, off_t offset){
    fprintf(stderr, "mmap is not supported using SGX!\n");
    return (void *)-1;
 
}
int munmap(void *addr, size_t length){
    fprintf(stderr, "munmap is not supported using SGX!\n");
    return -1;
 
}

void *mremap(void *old_address, size_t old_size, size_t new_size, int flags, ... /* void *new_address */){
    fprintf(stderr, "mremap is not supported using SGX!\n");
    return (void *)-1;
   
}