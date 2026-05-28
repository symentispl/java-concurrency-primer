# Act 2 Gradual On-Ramp Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Reduce the conceptual jump in Act 2 by adding a "Waiting for a Condition" stepping stone, a single-slot buffer, and a new Exercise 2 (OneTimeEvent), then relocate the lock-internals deep-dive to Act 3 where it fits the "understand what's happening" theme.

**Architecture:** All slide changes are in `index.adoc` via content-anchor edits (not line numbers — lines shift as edits accumulate). Exercise files follow the existing skeleton pattern. The lock-internals slides are cut from Act 2 and pasted into Act 3; no content is deleted, only relocated.

**Tech Stack:** AsciiDoctor RevealJS (slides), Java 25 instance-main style (exercises), mise tasks for running.

---

## Task 1: Remove lock-internals sub-slides from Act 2

**Files:**
- Modify: `slides/src/main/slides/index.adoc`

Cut the three sub-slides (`=== Spin-Lock — The Naive Approach`, `=== Spin-Lock Downsides`, `=== How Java Implements Locks`, `=== futex — Fast Userspace Mutex`) from under `== synchronized — Mutual Exclusion`. Leave `=== lock? what is lock?` in place — it belongs there.

- [ ] **Step 1: Remove spin-lock and lock-internals sub-slides**

In `index.adoc`, replace this block (anchor: starts right after `=== lock? what is lock?` content, ends before `== Every Object Has a Lock`):

Old content to remove — everything from `=== Spin-Lock — The Naive Approach` through the end of `=== futex — Fast Userspace Mutex`, i.e.:

```asciidoc
=== Spin-Lock — The Naive Approach

[source,java]
----
// broken — read-then-write on 'locked' is not atomic
class SpinLock {
    private volatile boolean locked = false;

    void lock() {
        while (locked) {}  // busy-wait — burns a full CPU core
        locked = true;     // BUG: another thread can sneak in between check and set
    }
    void unlock() { locked = false; }
}

// correct: hardware CAS makes acquire a single atomic operation
class SpinLock {
    private final AtomicBoolean locked = new AtomicBoolean(false);

    void lock() {
        while (!locked.compareAndSet(false, true)) {} // spin until CAS wins
    }
    void unlock() { locked.set(false); }
}
----

=== Spin-Lock Downsides

* *CPU waste* — every waiting thread burns a full core doing nothing useful
* *Cache line storm* — CAS from N cores repeatedly invalidates the same cache line across all L1 caches; contention grows with thread count
* *No OS awareness* — the scheduler keeps allocating time slices to spinning threads; they consume quota they cannot use
* *Unfair* — no ordering; under high load a thread may spin indefinitely while others keep winning CAS

Spin-locks are useful only when the critical section is *very short* and contention is *rare* — a few dozen CPU cycles at most.

=== How Java Implements Locks

Default since JDK 23: *LM_LIGHTWEIGHT* (JDK-8291555 / JDK-8315880). +
Two tiers — lightweight lock in userspace, heavyweight monitor when contended.

[graphviz]
----
digraph java_lock {
    rankdir=TB
    graph [bgcolor="#1a1a2e", size="18,9"]
    node [shape=rect, style=filled, fontname="sans-serif", fontsize=11, margin="0.3,0.15"]
    edge [fontname="sans-serif", fontsize=10, color="#aaaaaa", fontcolor="#dddddd"]

    LOCK  [label="synchronized / lock()",                                     fillcolor="#4a90d9", fontcolor=white]
    CAS   [label="CAS header tag bits 01→00\n(lightweight lock — userspace)",  fillcolor="#5ba05b", fontcolor=white, shape=diamond]
    LS    [label="push object ref\nto thread-local lock stack",                fillcolor="#5ba05b", fontcolor=white]
    CS    [label="critical section",                                            fillcolor="#5ba05b", fontcolor=white]
    INF   [label="inflate: CAS header to ObjectMonitor ptr (tag=10)\n(heavyweight monitor — contended path)", fillcolor="#6a3a7a", fontcolor=white]
    SPIN  [label="adaptive spin on monitor\n(HotSpot tunes iteration count)",  fillcolor="#c97c2e", fontcolor=white, shape=diamond]
    FUTEX [label="futex(FUTEX_WAIT)\nkernel parks thread — zero CPU",          fillcolor="#7a3a3a", fontcolor=white]
    WAKE  [label="unlock → futex(FUTEX_WAKE)\nkernel moves thread to runqueue", fillcolor="#6a3a7a", fontcolor=white]
    UNL   [label="pop lock stack\nCAS header 00→01 (unlock)",                  fillcolor="#4a90d9", fontcolor=white]

    LOCK  -> CAS
    CAS   -> LS    [label="won"]
    LS    -> CS
    CS    -> UNL
    CAS   -> INF   [label="lost — contended"]
    INF   -> SPIN
    SPIN  -> CS    [label="won during spin"]
    SPIN  -> FUTEX [label="still contended"]
    FUTEX -> WAKE  [label="owner unlocks"]
    WAKE  -> SPIN  [label="re-compete"]
}
----

=== futex — Fast Userspace Mutex

`futex(2)` is the Linux kernel primitive underlying every JVM heavyweight monitor and `java.util.concurrent` lock:

* *Fast path* (lightweight lock): CAS in userspace — *no syscall, no kernel entry at all*
* *Slow path*: inflated monitor calls `futex(FUTEX_WAIT, &lock, expected)` — kernel checks value atomically and parks the thread; CPU freed immediately
* *Wake path*: `futex(FUTEX_WAKE, &lock, 1)` — kernel picks one waiter and moves it back to the run queue
* *Zero CPU while parked* — unlike a spin-lock, a blocked thread consumes no scheduler time

The result: uncontended locks cost ~1 CAS; contended locks pay a syscall only for threads that actually have to wait.
```

Replace with: *(nothing — the block is deleted; Task 4 will re-insert it in Act 3)*

- [ ] **Step 2: Verify the file still looks correct around the cut point**

After the edit, the sequence should be:
`=== lock? what is lock?` content → `== Every Object Has a Lock`

---

## Task 2: Add "Waiting for a Condition" slides after `synchronized Isn't Enough`

**Files:**
- Modify: `slides/src/main/slides/index.adoc`

- [ ] **Step 1: Insert two new slides**

Anchor: find `== synchronized Isn't Enough` block. It ends just before `== Monitor = Lock + Wait Set`. Insert the following two slides between them:

```asciidoc
== Waiting for a Condition

`synchronized` protects shared state. But sometimes a thread needs to *wait for something to be true* before it can proceed.

The pattern is always the same:

[source,java]
----
// inside synchronized on the same object:
while (!conditionIsTrue()) {
    wait();             // releases the lock atomically, parks this thread
}
// condition is now true — proceed safely
----

* `wait()` releases the lock *and* parks the thread in a single atomic step
* Another thread changes state, then calls `notifyAll()` to wake waiters
* Always `while` — not `if` — because spurious wakeups and racing conditions both exist
* Re-checking after every wakeup costs nothing; missing a changed condition is a bug

=== ReadySignal — The Simplest Case

One thread waits until another says "go". The condition is a single boolean — no data structure involved.

[source,java]
----
class ReadySignal {
    private boolean ready = false;

    synchronized void signal() {
        ready = true;
        notifyAll();            // wake all waiters
    }

    synchronized void await() throws InterruptedException {
        while (!ready) {
            wait();             // release lock, park; re-check on wakeup
        }
    }
}
----

_Run:_ `mise run java:exec -- ReadySignal`

This is the building block every blocking coordination pattern is built from.
```

---

## Task 3: Add single-slot buffer slides after `notify() vs notifyAll()`

**Files:**
- Modify: `slides/src/main/slides/index.adoc`

- [ ] **Step 1: Insert single-slot buffer slides**

Anchor: find `== notify() vs notifyAll()` block (ends with "Use `notifyAll()` as the safe default"). Insert the following slides immediately after, before `== BoundedBuffer — Producer Side`:

```asciidoc
== Single-Slot Buffer

Before the full ring buffer: capacity of *one item*. Same coordination pattern, zero index arithmetic.

The two conditions mirror each other:

* Producer waits while `full` — consumer must take the item first
* Consumer waits while `!full` — producer must put an item first

=== Single-Slot Buffer — Producer Side

[source,java]
----
class SingleSlotBuffer {
    private int  item;
    private boolean full = false;

    synchronized void put(int value) throws InterruptedException {
        while (full) wait();    // slot taken — wait for consumer
        item = value;
        full = true;
        notifyAll();            // wake consumer
    }
}
----

=== Single-Slot Buffer — Consumer Side

[source,java]
----
    synchronized int take() throws InterruptedException {
        while (!full) wait();   // slot empty — wait for producer
        int value = item;
        full  = false;
        notifyAll();            // wake producer
        return value;
    }
----

`BoundedBuffer` is this pattern with a ring buffer replacing the single slot. +
The `while / wait / notifyAll` structure is identical.
```

---

## Task 4: Add lock-internals slides to Act 3

**Files:**
- Modify: `slides/src/main/slides/index.adoc`

- [ ] **Step 1: Insert a new section header and the lock-internals slides before `== Going Deeper — Linux TSA Tools`**

Anchor: find `== Going Deeper — Linux TSA Tools`. Insert immediately before it:

```asciidoc
== Under the Hood — How Java Locks Work

Understanding what `synchronized` does at the hardware level explains why it is fast when uncontended and what "contention" actually costs.

=== Spin-Lock — The Naive Approach

[source,java]
----
// broken — read-then-write on 'locked' is not atomic
class SpinLock {
    private volatile boolean locked = false;

    void lock() {
        while (locked) {}  // busy-wait — burns a full CPU core
        locked = true;     // BUG: another thread can sneak in between check and set
    }
    void unlock() { locked = false; }
}

// correct: hardware CAS makes acquire a single atomic operation
class SpinLock {
    private final AtomicBoolean locked = new AtomicBoolean(false);

    void lock() {
        while (!locked.compareAndSet(false, true)) {} // spin until CAS wins
    }
    void unlock() { locked.set(false); }
}
----

=== Spin-Lock Downsides

* *CPU waste* — every waiting thread burns a full core doing nothing useful
* *Cache line storm* — CAS from N cores repeatedly invalidates the same cache line across all L1 caches; contention grows with thread count
* *No OS awareness* — the scheduler keeps allocating time slices to spinning threads; they consume quota they cannot use
* *Unfair* — no ordering; under high load a thread may spin indefinitely while others keep winning CAS

Spin-locks are useful only when the critical section is *very short* and contention is *rare* — a few dozen CPU cycles at most.

=== How Java Implements Locks

Default since JDK 23: *LM_LIGHTWEIGHT* (JDK-8291555 / JDK-8315880). +
Two tiers — lightweight lock in userspace, heavyweight monitor when contended.

[graphviz]
----
digraph java_lock {
    rankdir=TB
    graph [bgcolor="#1a1a2e", size="18,9"]
    node [shape=rect, style=filled, fontname="sans-serif", fontsize=11, margin="0.3,0.15"]
    edge [fontname="sans-serif", fontsize=10, color="#aaaaaa", fontcolor="#dddddd"]

    LOCK  [label="synchronized / lock()",                                     fillcolor="#4a90d9", fontcolor=white]
    CAS   [label="CAS header tag bits 01→00\n(lightweight lock — userspace)",  fillcolor="#5ba05b", fontcolor=white, shape=diamond]
    LS    [label="push object ref\nto thread-local lock stack",                fillcolor="#5ba05b", fontcolor=white]
    CS    [label="critical section",                                            fillcolor="#5ba05b", fontcolor=white]
    INF   [label="inflate: CAS header to ObjectMonitor ptr (tag=10)\n(heavyweight monitor — contended path)", fillcolor="#6a3a7a", fontcolor=white]
    SPIN  [label="adaptive spin on monitor\n(HotSpot tunes iteration count)",  fillcolor="#c97c2e", fontcolor=white, shape=diamond]
    FUTEX [label="futex(FUTEX_WAIT)\nkernel parks thread — zero CPU",          fillcolor="#7a3a3a", fontcolor=white]
    WAKE  [label="unlock → futex(FUTEX_WAKE)\nkernel moves thread to runqueue", fillcolor="#6a3a7a", fontcolor=white]
    UNL   [label="pop lock stack\nCAS header 00→01 (unlock)",                  fillcolor="#4a90d9", fontcolor=white]

    LOCK  -> CAS
    CAS   -> LS    [label="won"]
    LS    -> CS
    CS    -> UNL
    CAS   -> INF   [label="lost — contended"]
    INF   -> SPIN
    SPIN  -> CS    [label="won during spin"]
    SPIN  -> FUTEX [label="still contended"]
    FUTEX -> WAKE  [label="owner unlocks"]
    WAKE  -> SPIN  [label="re-compete"]
}
----

=== futex — Fast Userspace Mutex

`futex(2)` is the Linux kernel primitive underlying every JVM heavyweight monitor and `java.util.concurrent` lock:

* *Fast path* (lightweight lock): CAS in userspace — *no syscall, no kernel entry at all*
* *Slow path*: inflated monitor calls `futex(FUTEX_WAIT, &lock, expected)` — kernel checks value atomically and parks the thread; CPU freed immediately
* *Wake path*: `futex(FUTEX_WAKE, &lock, 1)` — kernel picks one waiter and moves it back to the run queue
* *Zero CPU while parked* — unlike a spin-lock, a blocked thread consumes no scheduler time

The result: uncontended locks cost ~1 CAS; contended locks pay a syscall only for threads that actually have to wait.

```

---

## Task 5: Create `ReadySignal.java` demo

**Files:**
- Create: `code/src/main/java/ReadySignal.java`

- [ ] **Step 1: Write the demo file**

```java
class ReadySignal {
    private boolean ready = false;

    synchronized void signal() {
        ready = true;
        notifyAll();
    }

    synchronized void await() throws InterruptedException {
        while (!ready) wait();
    }

    void main() throws InterruptedException {
        var signal = new ReadySignal();

        var waiter = new Thread(() -> {
            try {
                System.out.println("Waiter: parking until signal...");
                signal.await();
                System.out.println("Waiter: received — proceeding");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        waiter.start();
        Thread.sleep(500);
        System.out.println("Main:   firing signal");
        signal.signal();
        waiter.join();
        System.out.println("Main:   waiter finished");
    }
}
```

- [ ] **Step 2: Verify it runs**

```bash
mise run java:exec -- ReadySignal
```

Expected output (order of first two lines may vary):
```
Waiter: parking until signal...
Main:   firing signal
Waiter: received — proceeding
Main:   waiter finished
```

---

## Task 6: Create `Exercise2OneTimeEvent.java`

**Files:**
- Create: `code/src/main/java/exercises/Exercise2OneTimeEvent.java`

- [ ] **Step 1: Write the exercise skeleton**

```java
// Exercise 2: One-Time Event (Ready Signal)
//
// TOPIC: wait() and notifyAll() — waiting for a condition
//
// A OneTimeEvent lets any number of threads wait until a signal is fired.
// Once fired, any subsequent await() calls must return immediately without
// blocking — the event is permanent.
//
// This is the simplest possible use of wait/notifyAll: one boolean condition,
// two methods, no data structure involved.
//
// IMPLEMENTATION SKETCH
//
//   signal() — synchronized:
//     fired = true;
//     notifyAll();           // wake all parked threads
//
//   await() — synchronized:
//     while (!fired) wait(); // while — not if — guards spurious wakeups
//                            // returns immediately if already fired
//
// YOUR TASK
// ---------
// Implement signal() and await() in OneTimeEvent below.
//
// Run:  mise run java:exec -- exercises/Exercise2OneTimeEvent
// Pass: PASS — all 10 threads received the signal; late joiner returned immediately

import java.util.concurrent.atomic.AtomicInteger;

class Exercise2OneTimeEvent {

    static class OneTimeEvent {
        private boolean fired = false;

        synchronized void signal() {
            // TODO: set fired = true and wake all waiters
        }

        synchronized void await() throws InterruptedException {
            // TODO: wait in a loop while not yet fired
        }
    }

    void main() throws InterruptedException {
        var event   = new OneTimeEvent();
        int count   = 10;
        var arrived = new AtomicInteger(0);
        var threads = new Thread[count];

        for (int i = 0; i < count; i++) {
            threads[i] = new Thread(() -> {
                try {
                    event.await();
                    arrived.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
            threads[i].start();
        }

        Thread.sleep(200); // let all threads park before firing
        event.signal();
        for (var t : threads) t.join();

        // Late joiner — event already fired; must not block
        var late = new Thread(() -> {
            try { event.await(); }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        });
        late.start();
        late.join(500);
        boolean lateOk = !late.isAlive();
        late.interrupt();

        if (arrived.get() == count && lateOk) {
            System.out.println("PASS — all " + count + " threads received the signal; late joiner returned immediately");
        } else {
            System.out.println("FAIL — arrived=" + arrived.get() + "/" + count + ", lateJoinerFinished=" + lateOk);
        }
    }
}
```

- [ ] **Step 2: Verify the skeleton compiles and fails as expected**

```bash
mise run java:exec -- exercises/Exercise2OneTimeEvent
```

Expected: the program hangs (all threads call `await()` which does nothing, so they never unblock). Kill with Ctrl+C. That confirms the skeleton is correctly broken.

---

## Task 7: Reorder exercise files

**Files:**
- Rename: `code/src/main/java/exercises/Exercise2BlockingStack.java` → `Exercise3BlockingStack.java`
- Rename: `code/src/main/java/exercises/Exercise3Barrier.java` → `Exercise6Barrier.java`
- Keep: `Exercise4TransferDeadlock.java`, `Exercise5ReadWriteCache.java` (numbers already correct)

The effective exercise order becomes:
1. Exercise1BankAccount — synchronized
2. Exercise2OneTimeEvent — wait/notifyAll, simplest case *(new)*
3. Exercise3BlockingStack — wait/notifyAll, bounded structure
4. Exercise4TransferDeadlock — deadlock prevention
5. Exercise5ReadWriteCache — ReentrantReadWriteLock
6. Exercise6Barrier — capstone: count-based condition *(was Ex3)*

- [ ] **Step 1: Rename files**

```bash
cd code/src/main/java/exercises
mv Exercise2BlockingStack.java Exercise3BlockingStack.java
mv Exercise3Barrier.java Exercise6Barrier.java
```

- [ ] **Step 2: Update the class name inside Exercise3BlockingStack.java**

Open `Exercise3BlockingStack.java` and rename the top-level class from `Exercise2BlockingStack` to `Exercise3BlockingStack`.

- [ ] **Step 3: Update the class name inside Exercise6Barrier.java**

Open `Exercise6Barrier.java` and rename the top-level class from `Exercise3Barrier` to `Exercise6Barrier`.

- [ ] **Step 4: Update the Run line in each renamed file's comment**

In `Exercise3BlockingStack.java`, change:
```
// Run:  mise run java:exec -- exercises/Exercise2BlockingStack
```
to:
```
// Run:  mise run java:exec -- exercises/Exercise3BlockingStack
```

In `Exercise6Barrier.java`, change:
```
// Run:  mise run java:exec -- exercises/Exercise3Barrier
```
to:
```
// Run:  mise run java:exec -- exercises/Exercise6Barrier
```

- [ ] **Step 5: Verify renamed files still run**

```bash
mise run java:exec -- exercises/Exercise3BlockingStack
mise run java:exec -- exercises/Exercise6Barrier
```

Both should compile and run (they hang waiting for implementation, which is correct).

---

## Task 8: Commit and push

- [ ] **Step 1: Stage and commit**

```bash
git add slides/src/main/slides/index.adoc \
        code/src/main/java/ReadySignal.java \
        code/src/main/java/exercises/Exercise2OneTimeEvent.java \
        code/src/main/java/exercises/Exercise3BlockingStack.java \
        code/src/main/java/exercises/Exercise6Barrier.java
git rm code/src/main/java/exercises/Exercise2BlockingStack.java
git rm code/src/main/java/exercises/Exercise3Barrier.java
git commit -m "refactor: gradual Act 2 on-ramp — latch pattern, single-slot buffer, reordered exercises

- Remove lock-internals (spin-lock, LM_LIGHTWEIGHT, futex) from Act 2;
  move to new 'Under the Hood' section in Act 3
- Add 'Waiting for a Condition' concept slide + ReadySignal sub-slide
  as stepping stone before Monitor/wait/notifyAll
- Add single-slot buffer slides as intermediate step before BoundedBuffer
- Add ReadySignal.java demo (mise run java:exec -- ReadySignal)
- Add Exercise2OneTimeEvent.java: simplest wait/notifyAll exercise
- Reorder exercises: BlockingStack→Ex3, Barrier→Ex6 (capstone)

Co-Authored-By: Claude Sonnet 4.6 <noreply@anthropic.com>"
```

- [ ] **Step 2: Push**

```bash
git push
```
