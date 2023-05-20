
#ifndef UT_SPINLOCK_H
#define UT_SPINLOCK_H


#include <sgx_spinlock.h>
/*
static inline void _mm_pause(void);
static inline int _InterlockedExchange(int volatile * dst, int val);
inline uint32_t sgx_spin_lock(sgx_spinlock_t *lock){
    while(_InterlockedExchange((volatile int *)lock, 1) != 0) {
        while (*lock) {
            _mm_pause();
        } 
    }

    return (0);
}

inline uint32_t sgx_spin_unlock(sgx_spinlock_t *lock);


*/

static inline void _mm_pause(void)
{
    __asm __volatile(
        "pause"
    );
}

static inline int _InterlockedExchange(int volatile * dst, int val)
{
    int res;

    __asm __volatile(
        "lock xchg %2, %1;"
        "mov %2, %0"
        : "=m" (res)
        : "m" (*dst),
        "r" (val) 
        : "memory"
    );

    return (res);
   
}

inline uint32_t sgx_spin_lock(sgx_spinlock_t *lock)
{
    while(_InterlockedExchange((volatile int *)lock, 1) != 0) {
        while (*lock) {
            /* tell cpu we are spinning */
            _mm_pause();
        } 
    }

    return (0);
}

inline uint32_t sgx_spin_unlock(sgx_spinlock_t *lock)
{
    *lock = 0;

    return (0);
}
#endif
