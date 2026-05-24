# Lock and Condition — Beyond synchronized — Design Spec

**Date:** 2026-05-24
**Section:** Act 2 — Correctness (Lock/Condition addition)
**Follows:** "The Three Failure Modes" (current slide 50)

---

## Goal

Introduce `java.util.concurrent.locks` as a more capable evolution of `synchronized`. Students have learned synchronized, wait()/notify(), BoundedBuffer, and the three progress failure modes. This section frames Lock as the answer to synchronized's shortcomings, builds up to the key insight of multiple Conditions, and pays it off with a BoundedBuffer rewrite.

---

## Audience Assumptions

- Know: Java syntax, OOP, threads, synchronized, wait()/notify()/notifyAll(), BoundedBuffer, deadlock, livelock, starvation
- Don't know: java.util.concurrent, lambdas, generics beyond basic usage
- No prior exposure to Lock, Condition, tryLock, fairness, ReadWriteLock

---

## Slides — 10 total

Final slide count: **60** (up from 50).

All slides appended after "The Three Failure Modes".

---

### Slide 1: "synchronized Has Limits"

Four gaps that motivate the Lock API:

- Single wait set — all waiting threads share one queue; no way to target producers vs consumers separately
- `notifyAll()` wakes every waiting thread — even those whose condition is still false
- No way to attempt locking without blocking — if the lock is held, you wait
- No fairness guarantee — a thread can be skipped indefinitely

---

### Slide 2: "The Lock Interface"

The `java.util.concurrent.locks.Lock` contract:

- `lock()` — acquire the lock; blocks until available
- `unlock()` — release the lock; must always be called
- `tryLock()` — returns true if acquired, false immediately if not
- `tryLock(time, unit)` — times out instead of blocking forever
- `lockInterruptibly()` — blocks but responds to interrupt

Mandatory pattern — `unlock()` must go in `finally` to guarantee release even if an exception is thrown:

```java
lock.lock();
try {
    // critical section
} finally {
    lock.unlock();
}
```

---

### Slide 3: "ReentrantLock"

- Drop-in replacement for `synchronized` blocks — same mutual exclusion, explicit API
- Reentrant like `synchronized`: the same thread can acquire it again without blocking
- Explicit lock/unlock — more control, more responsibility
- `new ReentrantLock()` — default (non-fair); `new ReentrantLock(true)` — fair mode (previewed here, explained on slide 8)

```java
private final Lock lock = new ReentrantLock();

void increment() {
    lock.lock();
    try { count++; }
    finally { lock.unlock(); }
}
```

---

### Slide 4: "Condition — Per-Queue Waiting"

- `lock.newCondition()` creates a separate wait queue bound to the lock
- `await()` — releases the lock and parks the thread (like `wait()`)
- `signal()` — wakes one thread waiting on this Condition only (like `notify()`)
- `signalAll()` — wakes all threads waiting on this Condition (like `notifyAll()`)
- One lock can have multiple independent Conditions — each with its own queue

```java
Lock lock = new ReentrantLock();
Condition ready = lock.newCondition();

// inside lock:
while (!condition) ready.await();  // releases lock, parks here
ready.signal();                    // wakes one waiter on 'ready' only
```

---

### Slide 5: "Multiple Conditions — The Key Win"

Concept slide — no code beyond field declarations.

The problem with `synchronized`:
- One wait set → `notifyAll()` wakes producers AND consumers
- `notify()` may wake the wrong type of thread

The solution with Lock:
- Declare two separate Conditions: `notFull` and `notEmpty`
- `put()` waits on `notFull`; signals `notEmpty` when done
- `take()` waits on `notEmpty`; signals `notFull` when done
- `signal()` is now safe — only the right threads are woken

```java
private final Condition notFull  = lock.newCondition();
private final Condition notEmpty = lock.newCondition();
```

---

### Slide 6: "BoundedBuffer — Producer Side (Lock)"

Full `put()` method using the two-condition pattern. Fields shown at top.

```java
private final Lock lock      = new ReentrantLock();
private final Condition notFull  = lock.newCondition();
private final Condition notEmpty = lock.newCondition();

void put(int item) throws InterruptedException {
    lock.lock();
    try {
        while (count == buffer.length) notFull.await();
        buffer[head] = item;
        head = (head + 1) % buffer.length;
        count++;
        notEmpty.signal(); // wake one consumer — no need for signalAll
    } finally {
        lock.unlock();
    }
}
```

---

### Slide 7: "BoundedBuffer — Consumer Side (Lock)"

Full `take()` method.

```java
int take() throws InterruptedException {
    lock.lock();
    try {
        while (count == 0) notEmpty.await();
        int item = buffer[tail];
        tail = (tail + 1) % buffer.length;
        count--;
        notFull.signal(); // wake one producer — no need for signalAll
        return item;
    } finally {
        lock.unlock();
    }
}
```

---

### Slide 8: "Fairness and tryLock"

- `new ReentrantLock(true)` — fair lock: threads acquire in FIFO arrival order
- Fair mode prevents starvation; costs throughput (no "barging" by newly arriving threads)
- `lock.tryLock()` — returns `true` if acquired, `false` immediately if not; never blocks
- `lock.tryLock(100, TimeUnit.MILLISECONDS)` — times out instead of blocking forever; useful for deadlock avoidance

```java
if (lock.tryLock()) {
    try { /* got it */ }
    finally { lock.unlock(); }
} else {
    // lock was held — do something else
}
```

---

### Slide 9: "ReentrantReadWriteLock"

- `rwLock.readLock().lock()` — shared: many readers can hold simultaneously
- `rwLock.writeLock().lock()` — exclusive: blocks all readers and other writers
- Use when reads are frequent and writes are rare (caches, configuration, lookup tables)
- Both locks are separate `Lock` objects — each needs its own try-finally

```java
ReadWriteLock rwLock = new ReentrantReadWriteLock();

void read()  { rwLock.readLock().lock();  try { /* ... */ } finally { rwLock.readLock().unlock();  } }
void write() { rwLock.writeLock().lock(); try { /* ... */ } finally { rwLock.writeLock().unlock(); } }
```

---

### Slide 10: "Lock Implementations"

Summary table — three implementations in `java.util.concurrent.locks`:

| Implementation | Use when |
|---|---|
| `ReentrantLock` | General-purpose replacement for `synchronized`; need tryLock, fairness, or multiple conditions |
| `ReentrantReadWriteLock` | Reads heavily outnumber writes; sharing is safe for reads |
| `StampedLock` (Java 8+) | Maximum throughput; adds optimistic reads — more complex API, not reentrant |

---

## Runnable Code Example

### BoundedBufferLock.java

`code/src/main/java/BoundedBufferLock.java` — same producer/consumer scenario as the wait/notify BoundedBuffer, rewritten using `ReentrantLock` and two `Condition` objects (`notFull`, `notEmpty`). Three producers, three consumers, 198 items total (66 per producer, divisible evenly).

Run: `mise run java:exec -- BoundedBufferLock`

Expected output: all 198 items produced and consumed, "Done." at end, no hangs.

```java
// Demonstrates ReentrantLock + two Conditions replacing wait()/notifyAll().
// Run: mise run java:exec -- BoundedBufferLock
import java.util.concurrent.locks.*;

class BoundedBufferLock {

    static class Buffer {
        private final int[] data;
        private int head, tail, count;
        private final Lock lock = new ReentrantLock();
        private final Condition notFull  = lock.newCondition();
        private final Condition notEmpty = lock.newCondition();

        Buffer(int capacity) { data = new int[capacity]; }

        void put(int item) throws InterruptedException {
            lock.lock();
            try {
                while (count == data.length) notFull.await();
                data[head] = item;
                head = (head + 1) % data.length;
                count++;
                notEmpty.signal();
            } finally { lock.unlock(); }
        }

        int take() throws InterruptedException {
            lock.lock();
            try {
                while (count == 0) notEmpty.await();
                int item = data[tail];
                tail = (tail + 1) % data.length;
                count--;
                notFull.signal();
                return item;
            } finally { lock.unlock(); }
        }
    }

    void main() throws InterruptedException {
        var buf = new Buffer(4);
        int total = 198;
        var threads = new Thread[6];

        for (int i = 0; i < 3; i++) {
            int id = i;
            threads[i] = new Thread(() -> {
                try {
                    for (int j = id * (total / 3); j < (id + 1) * (total / 3); j++)
                        buf.put(j);
                } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            });
        }
        for (int i = 0; i < 3; i++) {
            threads[3 + i] = new Thread(() -> {
                try {
                    for (int j = 0; j < total / 3; j++)
                        System.out.println("took: " + buf.take());
                } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            });
        }

        for (var t : threads) t.start();
        for (var t : threads) t.join();
        System.out.println("Done.");
    }
}
```

---

## File Map

| File | Action | Purpose |
|------|--------|---------|
| `slides/src/main/slides/index.adoc` | Modify (append 10 slides) | Add Lock/Condition section after slide 50 |
| `code/src/main/java/BoundedBufferLock.java` | Create | Runnable BoundedBuffer rewrite with ReentrantLock + two Conditions |

---

## Slide Placement

All 10 slides appended after "The Three Failure Modes" (current last slide).

| Slide | Title | After |
|-------|-------|-------|
| 51 | synchronized Has Limits | The Three Failure Modes |
| 52 | The Lock Interface | synchronized Has Limits |
| 53 | ReentrantLock | The Lock Interface |
| 54 | Condition — Per-Queue Waiting | ReentrantLock |
| 55 | Multiple Conditions — The Key Win | Condition — Per-Queue Waiting |
| 56 | BoundedBuffer — Producer Side (Lock) | Multiple Conditions — The Key Win |
| 57 | BoundedBuffer — Consumer Side (Lock) | BoundedBuffer — Producer Side (Lock) |
| 58 | Fairness and tryLock | BoundedBuffer — Consumer Side (Lock) |
| 59 | ReentrantReadWriteLock | Fairness and tryLock |
| 60 | Lock Implementations | ReentrantReadWriteLock |

Final slide count: **60**

---

## Out of Scope

- `StampedLock` optimistic read pattern (API too complex for this level)
- `Lock.lockInterruptibly()` usage beyond the API listing
- `Condition.awaitUninterruptibly()`, `awaitUntil()`, `awaitNanos()`
- `java.util.concurrent` higher-level abstractions (BlockingQueue, Executor, etc.) — Act 3
- `synchronized` vs `Lock` performance benchmarking
