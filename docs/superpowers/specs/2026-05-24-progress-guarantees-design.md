# Progress Guarantees — Design Spec

**Date:** 2026-05-24
**Section:** Act 2 — Correctness (third part)
**Follows:** wait()/notify() slides (slide 37)

---

## Goal

Teach first-year CS students the three classic progress failures in concurrent programs: deadlock, livelock, and starvation. Each problem gets a "what it is" beat and a "how to prevent it" beat, matching the hands-on tone of the rest of the deck.

---

## Audience Assumptions

- Know: Java syntax, OOP, threads, interrupt(), data races, synchronized, wait()/notify()
- Don't know: lambdas, generics, java.util.concurrent
- No prior exposure to deadlock, livelock, or starvation

---

## Slides — 10 total

### Beat 1 — Deadlock (4 slides)

**Slide 1: "Deadlock"**
- Each thread holds a lock the other needs
- Neither thread can proceed — both block forever
- The program hangs: no output, no crash, no error
- Requires four conditions: mutual exclusion, hold-and-wait, no preemption, circular wait

Graphviz resource-allocation graph showing circular dependency:
- Thread A holds Lock 1, waits for Lock 2
- Thread B holds Lock 2, waits for Lock 1
- Arrows form a cycle — the hallmark of deadlock

Colour scheme: blue (#4a90d9) for threads, orange (#c97c2e) for locks (waiting/blocked).

**Slide 2: "Deadlock in Java" (code slide)**

```java
// Thread A acquires Lock 1 then Lock 2
// Thread B acquires Lock 2 then Lock 1
// → circular wait → deadlock
```

Full code in `Deadlock.java`. Program hangs — no output after "Starting".

**Slide 3: "Preventing Deadlock — Lock Ordering"**
- Always acquire multiple locks in the same fixed order across all threads
- If every thread takes Lock 1 before Lock 2, circular wait is impossible
- Establish a consistent global order (e.g., by lock identity or natural ordering)
- Document which order your code uses — it is a contract

**Slide 4: "Lock Ordering — Fixed Code" (code slide)**

Same two-thread, two-lock scenario as Slide 2, with both threads acquiring locks in the same order. Program completes without hanging.

---

### Beat 2 — Livelock (3 slides)

**Slide 5: "Livelock"**
- Threads are active — not blocked, not waiting
- Each thread keeps reacting to the other's state
- No progress is made despite constant CPU activity
- Harder to detect than deadlock: the program does not hang, it just spins

**Slide 6: "Livelock in Java" (code slide)**

Two threads each back off when they detect the other is "active", both loop indefinitely. Program never makes progress — must be killed externally (Ctrl+C or timeout).

Full code in `Livelock.java`.

**Slide 7: "Preventing Livelock"**
- Add randomness: random backoff delay so threads don't mirror each other
- Impose a retry limit: after N attempts, one thread proceeds regardless
- Use a coordinator: a third party decides who goes first (used in network protocols)
- Timeout: if you cannot acquire what you need within a deadline, back off and signal an error

---

### Beat 3 — Starvation (2 slides)

**Slide 8: "Starvation"**
- A thread is perpetually denied access even though others keep making progress
- The system works — but one thread never gets its turn
- Causes: `notify()` waking an arbitrary thread (never yours), long-held locks, thread priorities
- Silent: no hang, no error — the starved thread simply never runs

**Slide 9: "Preventing Starvation"**
- Use `notifyAll()` instead of `notify()` — all threads get a chance to re-check
- Keep synchronized blocks short — reduce time others are locked out
- Do not rely on thread priorities — the OS scheduler does not honour them reliably
- `java.util.concurrent` provides fair lock implementations for production use

**Slide 10: "The Three Failure Modes" (summary)**
- Deadlock: threads block forever waiting for each other → prevent with lock ordering
- Livelock: threads spin forever reacting to each other → prevent with randomness or limits
- Starvation: one thread never gets access → prevent with fairness and short critical sections

---

## Code Examples

### Deadlock.java

Two threads, two locks, opposite acquisition order. Program prints "Starting" then hangs.

```java
// Demonstrates deadlock: two threads acquire two locks in opposite order.
// Run: mise run java:exec -- Deadlock
// WARNING: this program hangs — kill with Ctrl+C
class Deadlock {

    static final Object lockA = new Object();
    static final Object lockB = new Object();

    static class ThreadA implements Runnable {
        public void run() {
            synchronized (lockA) {
                System.out.println("Thread A: holds lockA, waiting for lockB");
                try { Thread.sleep(50); } catch (InterruptedException e) { return; }
                synchronized (lockB) {
                    System.out.println("Thread A: done");
                }
            }
        }
    }

    static class ThreadB implements Runnable {
        public void run() {
            synchronized (lockB) {
                System.out.println("Thread B: holds lockB, waiting for lockA");
                try { Thread.sleep(50); } catch (InterruptedException e) { return; }
                synchronized (lockA) {
                    System.out.println("Thread B: done");
                }
            }
        }
    }

    void main() throws InterruptedException {
        var a = new Thread(new ThreadA());
        var b = new Thread(new ThreadB());
        System.out.println("Starting");
        a.start();
        b.start();
        a.join();
        b.join();
        System.out.println("Done.");
    }
}
```

Expected: prints "Starting", then the two "holds" lines in some order, then hangs forever.

### DeadlockFixed.java

Same scenario with both threads acquiring locks in the same order (lockA then lockB). Program completes.

```java
// Demonstrates deadlock prevention: consistent lock ordering.
// Run: mise run java:exec -- DeadlockFixed
class DeadlockFixed {

    static final Object lockA = new Object();
    static final Object lockB = new Object();

    static class ThreadA implements Runnable {
        public void run() {
            synchronized (lockA) {
                try { Thread.sleep(50); } catch (InterruptedException e) { return; }
                synchronized (lockB) {
                    System.out.println("Thread A: done");
                }
            }
        }
    }

    static class ThreadB implements Runnable {
        public void run() {
            synchronized (lockA) {  // same order as Thread A
                try { Thread.sleep(50); } catch (InterruptedException e) { return; }
                synchronized (lockB) {
                    System.out.println("Thread B: done");
                }
            }
        }
    }

    void main() throws InterruptedException {
        var a = new Thread(new ThreadA());
        var b = new Thread(new ThreadB());
        System.out.println("Starting");
        a.start();
        b.start();
        a.join();
        b.join();
        System.out.println("Done.");
    }
}
```

Expected: "Starting", then "Thread A: done" and "Thread B: done" in either order, then "Done."

### Livelock.java

Two threads each back off when they detect the other is active. Both exhaust their retry limit and give up — neither ever makes progress. The `volatile` keyword ensures each thread sees the other's latest state (briefly explained in a comment; covered fully in Act 3).

```java
// Demonstrates livelock: two threads react to each other and make no progress.
// Run: mise run java:exec -- Livelock
class Livelock {

    // volatile: writes by one thread are immediately visible to the other
    static volatile boolean active1 = false;
    static volatile boolean active2 = false;

    static class Worker1 implements Runnable {
        public void run() {
            active1 = true;
            int attempts = 0;
            while (attempts < 20) {
                if (!active2) {
                    System.out.println("Worker 1: done");
                    active1 = false;
                    return;
                }
                active1 = false;
                System.out.println("Worker 1: backing off (attempt " + (++attempts) + ")");
                try { Thread.sleep(10); } catch (InterruptedException e) { return; }
                active1 = true;
            }
            active1 = false;
            System.out.println("Worker 1: gave up after " + attempts + " attempts");
        }
    }

    static class Worker2 implements Runnable {
        public void run() {
            active2 = true;
            int attempts = 0;
            while (attempts < 20) {
                if (!active1) {
                    System.out.println("Worker 2: done");
                    active2 = false;
                    return;
                }
                active2 = false;
                System.out.println("Worker 2: backing off (attempt " + (++attempts) + ")");
                try { Thread.sleep(10); } catch (InterruptedException e) { return; }
                active2 = true;
            }
            active2 = false;
            System.out.println("Worker 2: gave up after " + attempts + " attempts");
        }
    }

    void main() throws InterruptedException {
        var one = new Thread(new Worker1());
        var two = new Thread(new Worker2());
        one.start();
        two.start();
        one.join();
        two.join();
        System.out.println("Done — neither worker made progress.");
    }
}
```

Expected: interleaved "backing off" lines from both workers, then both print "gave up after 20 attempts", then "Done — neither worker made progress." Neither worker ever prints "done".

---

## Slide Placement

Appended to `slides/src/main/slides/index.adoc` after the existing 37 slides. Final count: 47 slides.

---

## File Map

| File | Action | Purpose |
|------|--------|---------|
| `code/src/main/java/Deadlock.java` | Create | Two-thread deadlock that hangs |
| `code/src/main/java/DeadlockFixed.java` | Create | Same scenario with consistent lock ordering |
| `code/src/main/java/Livelock.java` | Create | Two-thread livelock with 2-second timeout |
| `slides/src/main/slides/index.adoc` | Modify | Append 10 new slides |

---

## Out of Scope

- `java.util.concurrent.locks.ReentrantLock` with fairness parameter
- `tryLock()` with timeout as a deadlock-avoidance strategy
- Deadlock detection algorithms (wait-for graphs)
- Banker's algorithm
- JVM deadlock detection via thread dumps (covered in Act 3 diagnostics section)
