// Exercise 5: Read/Write Cache with ReentrantReadWriteLock
//
// TOPIC: ReentrantReadWriteLock — concurrent reads, exclusive writes
//
// A cache (HashMap) is shared by many reader threads and occasional writers.
// The current implementation uses synchronized, which is correct but wasteful:
// it serializes ALL reads even though reads do not modify state and could run
// concurrently with each other.
//
// ReentrantReadWriteLock separates the two concerns:
//   lock.readLock()  — shared: many readers can hold it at the same time
//   lock.writeLock() — exclusive: blocks all readers and other writers
//
// PATTERN for each method (replace the synchronized keyword):
//
//   get():                             put():
//     lock.readLock().lock();            lock.writeLock().lock();
//     try     { return map.get(k); }     try     { map.put(k, v); }
//     finally { lock.readLock()          finally { lock.writeLock()
//                 .unlock(); }                       .unlock(); }
//
// YOUR TASK
// ---------
//  1. Remove the 'synchronized' keyword from BOTH get() and put().
//  2. In get():  add lock.readLock().lock() / try / finally lock.readLock().unlock()
//     The Thread.sleep(100) already in get() stays — it simulates a slow lookup.
//     With readLock, 10 callers hold the read lock simultaneously and all sleep in parallel.
//     With synchronized, only one caller holds the monitor at a time → serialized.
//  3. In put():  add lock.writeLock().lock() / try / finally lock.writeLock().unlock()
//
// Run:  mise run java:exec -- exercises/Exercise5ReadWriteCache
// Pass: PASS — 10 concurrent reads finished in under 400 ms (reads were parallel)
//
// The test check:
//   10 readers each simulate a 100 ms read (Thread.sleep).
//   Serialized (synchronized): 10 × 100 ms = ~1000 ms → FAIL
//   Concurrent  (readLock):            ~100 ms total  → PASS

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

class Exercise5ReadWriteCache {

    static class Cache {
        private final Map<String, Integer> map = new HashMap<>();
        private final ReadWriteLock lock = new ReentrantReadWriteLock();

        // TODO: remove 'synchronized', use lock.readLock() instead
        synchronized Integer get(String key) {
            try { Thread.sleep(100); }  // simulates a slow lookup (I/O, deserialization)
            catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            return map.get(key);
        }

        // TODO: remove 'synchronized', use lock.writeLock() instead
        synchronized void put(String key, Integer value) {
            map.put(key, value);
        }
    }

    void main(String[] args) throws InterruptedException {
        Cache cache = new Cache();

        // Pre-populate
        for (int i = 0; i < 10; i++) cache.put("key_" + i, i);

        int readers = 10;
        AtomicReference<Throwable> error = new AtomicReference<>();
        CountDownLatch startGun = new CountDownLatch(1); // release all readers at once
        Thread[] threads = new Thread[readers];

        for (int r = 0; r < readers; r++) {
            int id = r % 10; // which key to read
            threads[r] = new Thread(() -> {
                try {
                    startGun.await();
                    Integer val = cache.get("key_" + id); // the 100 ms delay is inside get()
                    if (val == null || val != id)
                        error.compareAndSet(null,
                            new AssertionError("key_" + id + ": expected " + id + " got " + val));
                } catch (Throwable t) { error.compareAndSet(null, t); }
            });
            threads[r].start();
        }

        long t0 = System.currentTimeMillis();
        startGun.countDown();                            // all 10 readers released simultaneously
        for (Thread t : threads) t.join(5_000);
        long elapsed = System.currentTimeMillis() - t0;

        boolean anyHung = false;
        for (Thread t : threads) if (t.isAlive()) { anyHung = true; t.interrupt(); }

        if (anyHung) {
            System.out.println("FAIL — reader thread(s) did not finish within 5 s");
        } else if (error.get() != null) {
            System.out.println("FAIL — incorrect value: " + error.get().getMessage());
        } else if (elapsed > 400) {
            System.out.printf(
                "FAIL — %d concurrent reads took %d ms (expected < 400 ms)%n", readers, elapsed);
            System.out.println(
                "       Reads are being serialized — replace 'synchronized' with lock.readLock()");
        } else {
            System.out.printf(
                "PASS — %d concurrent reads finished in %d ms (reads were parallel)%n",
                readers, elapsed);
        }
    }
}
