# Lock and Condition — Beyond synchronized — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add 10 slides on `java.util.concurrent.locks` (Lock, ReentrantLock, Condition, fairness, tryLock, ReadWriteLock) appended after "The Three Failure Modes", plus a runnable `BoundedBufferLock.java` that reimplements the wait/notify BoundedBuffer using two Conditions.

**Architecture:** Code example first (Task 1), then slides in three batches — slides 51–54 (Task 2), slides 55–57 (Task 3), slides 58–60 (Task 4). All slides appended to the end of `index.adoc` using exact text anchors. Each task builds and verifies slide count before committing.

**Tech Stack:** Java 25 (simplified `void main()`, no `--enable-preview`), `java.util.concurrent.locks.*`, Asciidoctor RevealJS, `mise run build`, `mise run java:exec -- ClassName`.

---

## File Map

| File | Action | Purpose |
|------|--------|---------|
| `code/src/main/java/BoundedBufferLock.java` | Create | Runnable BoundedBuffer rewritten with ReentrantLock + two Conditions |
| `slides/src/main/slides/index.adoc` | Modify (3 appends) | Add 10 Lock/Condition slides after "The Three Failure Modes" |

---

### Task 1: Create BoundedBufferLock.java

**Files:**
- Create: `code/src/main/java/BoundedBufferLock.java`

- [ ] **Step 1: Create the file**

Create `code/src/main/java/BoundedBufferLock.java` with exactly this content:

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

- [ ] **Step 2: Run and verify**

```bash
mise run java:exec -- BoundedBufferLock
```

Expected: 198 lines of `took: N` (order varies), followed by `Done.` Program must not hang.

- [ ] **Step 3: Commit**

```bash
git add code/src/main/java/BoundedBufferLock.java
git commit -m "feat: add BoundedBufferLock — ReentrantLock + two Conditions replacing wait/notifyAll"
```

---

### Task 2: Append slides 51–54 (synchronized Has Limits → Condition)

**Files:**
- Modify: `slides/src/main/slides/index.adoc`

Current slide count: **50**. After this task: **54**.

- [ ] **Step 1: Append the four slides**

Find this exact text at the end of `slides/src/main/slides/index.adoc`:

```
* *Starvation* — one thread never gets access → prevent with fairness and short critical sections
```

Replace it with:

```
* *Starvation* — one thread never gets access → prevent with fairness and short critical sections

== synchronized Has Limits

* Single wait set — all waiting threads share one queue; producers and consumers cannot be targeted separately
* `notifyAll()` wakes every waiting thread — even those whose condition is still false
* No way to attempt locking without blocking — if the lock is held, you wait
* No fairness guarantee — a thread can be skipped indefinitely

== The Lock Interface

* `lock()` — acquire the lock; blocks until available
* `unlock()` — release the lock; must always be called
* `tryLock()` — returns `true` if acquired, `false` immediately if not
* `tryLock(time, unit)` — times out instead of blocking forever
* `lockInterruptibly()` — blocks but responds to interrupt

[source,java]
----
lock.lock();
try {
    // critical section
} finally {
    lock.unlock(); // guaranteed even if an exception is thrown
}
----

== ReentrantLock

* Drop-in replacement for `synchronized` blocks — same mutual exclusion, explicit API
* Reentrant like `synchronized`: the same thread can acquire it again without blocking
* Explicit lock/unlock — more control, more responsibility
* `new ReentrantLock(true)` enables fair mode (FIFO order — previewed here, explained later)

[source,java]
----
private final Lock lock = new ReentrantLock();

void increment() {
    lock.lock();
    try { count++; }
    finally { lock.unlock(); }
}
----

== Condition — Per-Queue Waiting

* `lock.newCondition()` creates a separate wait queue bound to the lock
* `await()` — releases the lock and parks the thread (like `wait()`)
* `signal()` — wakes one thread waiting on this Condition only (like `notify()`)
* `signalAll()` — wakes all threads waiting on this Condition (like `notifyAll()`)
* One lock can have multiple independent Conditions — each with its own queue

[source,java]
----
Lock lock = new ReentrantLock();
Condition ready = lock.newCondition();

// inside lock:
while (!condition) ready.await();  // releases lock, parks here
ready.signal();                    // wakes one waiter on 'ready' only
----
```

- [ ] **Step 2: Build and verify**

```bash
mise run build
```

Expected: exits 0. Then verify slide count:

```bash
grep -c "^== " slides/src/main/slides/index.adoc
```

Expected output: `54`

- [ ] **Step 3: Commit**

```bash
git add slides/src/main/slides/index.adoc
git commit -m "feat: add slides 51-54 — synchronized limits, Lock interface, ReentrantLock, Condition"
```

---

### Task 3: Append slides 55–57 (Multiple Conditions → BoundedBuffer Producer → Consumer)

**Files:**
- Modify: `slides/src/main/slides/index.adoc`

Current slide count: **54**. After this task: **57**.

- [ ] **Step 1: Append the three slides**

Find this exact text at the end of `slides/src/main/slides/index.adoc`:

```
// inside lock:
while (!condition) ready.await();  // releases lock, parks here
ready.signal();                    // wakes one waiter on 'ready' only
----
```

Replace it with:

```
// inside lock:
while (!condition) ready.await();  // releases lock, parks here
ready.signal();                    // wakes one waiter on 'ready' only
----

== Multiple Conditions — The Key Win

* `synchronized`: one wait set → `notifyAll()` wakes producers AND consumers
* With Lock: declare separate conditions — `notFull` and `notEmpty`
* `put()` waits on `notFull`; signals `notEmpty` when done
* `take()` waits on `notEmpty`; signals `notFull` when done
* `signal()` is now safe — only the right threads are woken

[source,java]
----
private final Condition notFull  = lock.newCondition();
private final Condition notEmpty = lock.newCondition();
----

== BoundedBuffer — Producer Side (Lock)

[source,java]
----
private final Lock lock          = new ReentrantLock();
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
----

== BoundedBuffer — Consumer Side (Lock)

[source,java]
----
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
----
```

- [ ] **Step 2: Build and verify**

```bash
mise run build
```

Expected: exits 0. Then verify slide count:

```bash
grep -c "^== " slides/src/main/slides/index.adoc
```

Expected output: `57`

- [ ] **Step 3: Commit**

```bash
git add slides/src/main/slides/index.adoc
git commit -m "feat: add slides 55-57 — multiple conditions and BoundedBuffer Lock rewrite"
```

---

### Task 4: Append slides 58–60 (Fairness and tryLock → ReentrantReadWriteLock → Lock Implementations)

**Files:**
- Modify: `slides/src/main/slides/index.adoc`

Current slide count: **57**. After this task: **60**.

- [ ] **Step 1: Append the three slides**

Find this exact text at the end of `slides/src/main/slides/index.adoc`:

```
        notFull.signal(); // wake one producer — no need for signalAll
        return item;
    } finally {
        lock.unlock();
    }
}
----
```

Replace it with:

```
        notFull.signal(); // wake one producer — no need for signalAll
        return item;
    } finally {
        lock.unlock();
    }
}
----

== Fairness and tryLock

* `new ReentrantLock(true)` — fair lock: threads acquire in FIFO arrival order
* Fair mode prevents starvation; costs throughput (no "barging" by newly arriving threads)
* `lock.tryLock()` — returns `true` if acquired, `false` immediately if not; never blocks
* `lock.tryLock(100, TimeUnit.MILLISECONDS)` — times out instead of blocking forever; useful for deadlock avoidance

[source,java]
----
if (lock.tryLock()) {
    try { /* got it */ }
    finally { lock.unlock(); }
} else {
    // lock was held — do something else
}
----

== ReentrantReadWriteLock

* `rwLock.readLock().lock()` — shared: many readers can hold simultaneously
* `rwLock.writeLock().lock()` — exclusive: blocks all readers and other writers
* Use when reads are frequent and writes are rare (caches, configuration, lookup tables)
* Both locks are separate `Lock` objects — each needs its own try-finally

[source,java]
----
ReadWriteLock rwLock = new ReentrantReadWriteLock();

void read()  { rwLock.readLock().lock();  try { /* ... */ } finally { rwLock.readLock().unlock();  } }
void write() { rwLock.writeLock().lock(); try { /* ... */ } finally { rwLock.writeLock().unlock(); } }
----

== Lock Implementations

* `ReentrantLock` — general-purpose; replaces `synchronized`; supports tryLock, fairness, multiple conditions
* `ReentrantReadWriteLock` — optimises read-heavy workloads; many readers or one exclusive writer
* `StampedLock` (Java 8+) — maximum throughput; adds optimistic reads — more complex API, not reentrant
* All in `java.util.concurrent.locks`
```

- [ ] **Step 2: Build and verify**

```bash
mise run build
```

Expected: exits 0. Then verify slide count:

```bash
grep -c "^== " slides/src/main/slides/index.adoc
```

Expected output: `60`

- [ ] **Step 3: Commit**

```bash
git add slides/src/main/slides/index.adoc
git commit -m "feat: add slides 58-60 — fairness, tryLock, ReadWriteLock, Lock implementations"
```
