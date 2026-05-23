# Data Races and synchronized — Design Spec

**Date:** 2026-05-24
**Section:** Act 2 — Correctness (first part)
**Follows:** Cooperative Shutdown (slide 23)

---

## Goal

Teach first-year CS students what a data race is, why it happens (non-atomic operations + CPU cache visibility), and how `synchronized` fixes it. Two runnable code examples — one broken, one fixed — build hands-on intuition.

---

## Audience Assumptions

- Know: Java syntax, OOP, threads, interrupt()
- Don't know: lambdas, generics, Java Memory Model
- No prior exposure to shared mutable state problems

---

## Slides — 8 total

### Beat 1 — Data Races (5 slides)

**Slide 1: "Shared Mutable State"**
- Two threads reading and writing the same variable
- At least one thread writes → data race
- Result depends on thread scheduling — unpredictable
- The broken counter: two threads each add 1, ten thousand times

**Slide 2: "i++ is Three Steps"**
Graphviz diagram showing thread interleaving:
- Thread A loads `counter = 5`
- Thread B loads `counter = 5` (interleaves before A writes)
- Thread A stores `6`
- Thread B stores `6`
- One increment silently lost — expected 7, got 6

**Slide 3: "Why CPU Caches Exist"**
- RAM access: ~100 CPU cycles; CPU operation: ~1 cycle
- Each core has its own private L1/L2 cache (fast, small)
- Shared L3 cache, then main memory (slow, large)
- Without caches every instruction would stall waiting for RAM

**Slide 4: "Caches and Visibility"**
- Thread A writes `counter` — stored in its L1 cache
- Thread B reads `counter` from its own L1 — may see a stale value
- No guarantee writes are immediately visible to other cores
- `synchronized` flushes and invalidates caches — restores visibility

**Slide 5: Code — DataRace.java (key lines)**
Shows: `Incrementer implements Runnable`, two threads, "Expected: 20000 / Actual: ?" output pattern

### Beat 2 — synchronized (3 slides)

**Slide 6: "synchronized — Mutual Exclusion"**
- Acquires a lock before entering the protected block
- Only one thread holds the lock at a time — others block
- Guarantees both: mutual exclusion AND memory visibility
- Lock is released automatically when the block exits

**Slide 7: "Every Object Has a Lock"**
Code snippet (two forms):
```java
// synchronized method — locks on 'this'
synchronized void increment() { counter++; }

// synchronized block — explicit lock object
synchronized (lock) { counter++; }
```
- Prefer synchronized block — makes the lock object explicit
- Synchronized method is shorthand for `synchronized (this)`

**Slide 8: Code — SynchronizedCounter.java (key lines)**
Shows: same `Incrementer` structure, `synchronized (lock)` around `counter++`, always prints 20000

---

## Code Examples

### DataRace.java

```java
// Demonstrates a data race on a shared counter.
// Run: mise run java:exec -- DataRace
class DataRace {

    static int counter = 0;

    static class Incrementer implements Runnable {
        public void run() {
            for (int i = 0; i < 10_000; i++) {
                counter++; // not atomic: load, add, store
            }
        }
    }

    void main() throws InterruptedException {
        var t1 = new Thread(new Incrementer());
        var t2 = new Thread(new Incrementer());
        t1.start();
        t2.start();
        t1.join();
        t2.join();
        System.out.println("Expected: 20000");
        System.out.println("Actual:   " + counter);
    }
}
```

Expected: prints a value less than 20000 (non-deterministic). Occasionally may print 20000 if threads don't interleave — re-run to see the race.

### SynchronizedCounter.java

```java
// Demonstrates synchronized fixing the data race.
// Run: mise run java:exec -- SynchronizedCounter
class SynchronizedCounter {

    static int counter = 0;
    static final Object lock = new Object();

    static class Incrementer implements Runnable {
        public void run() {
            for (int i = 0; i < 10_000; i++) {
                synchronized (lock) {
                    counter++; // protected: only one thread at a time
                }
            }
        }
    }

    void main() throws InterruptedException {
        var t1 = new Thread(new Incrementer());
        var t2 = new Thread(new Incrementer());
        t1.start();
        t2.start();
        t1.join();
        t2.join();
        System.out.println("Expected: 20000");
        System.out.println("Actual:   " + counter);
    }
}
```

Expected: always prints `Actual: 20000`.

---

## Slide Placement

Appended to `slides/src/main/slides/index.adoc` after the existing 23 slides. Final count: 31 slides.

---

## Out of Scope

- `volatile` keyword
- Java Memory Model / happens-before
- `java.util.concurrent` classes
- Deadlock (covered later in Act 2 or Act 3)
- Performance implications of synchronized
