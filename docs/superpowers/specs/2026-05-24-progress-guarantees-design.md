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

Two workers each need two resources. Each acquires its first resource (via `synchronized tryAcquire()`), then tries the second. If the second is taken, it releases the first and backs off — being "polite". Both back off at the same time and retry in sync → livelock. Uses only `synchronized`, no `volatile`.

```java
// Demonstrates livelock: two workers keep backing off and make no progress.
// Run: mise run java:exec -- Livelock
class Livelock {

    static class Resource {
        private final String name;
        private boolean inUse = false;

        Resource(String name) { this.name = name; }

        synchronized boolean tryAcquire() {
            if (inUse) return false;
            inUse = true;
            return true;
        }

        synchronized void release() { inUse = false; }
    }

    static class Worker implements Runnable {
        private final String name;
        private final Resource first;
        private final Resource second;

        Worker(String name, Resource first, Resource second) {
            this.name = name;
            this.first = first;
            this.second = second;
        }

        public void run() {
            int attempts = 0;
            while (attempts < 20) {
                if (!first.tryAcquire()) {
                    try { Thread.sleep(10); } catch (InterruptedException e) { return; }
                    continue;
                }
                if (!second.tryAcquire()) {
                    first.release(); // be polite — release and back off
                    System.out.println(name + ": backing off (attempt " + (++attempts) + ")");
                    try { Thread.sleep(10); } catch (InterruptedException e) { return; }
                    continue;
                }
                // got both resources
                second.release();
                first.release();
                System.out.println(name + ": done");
                return;
            }
            System.out.println(name + ": gave up after " + attempts + " attempts");
        }
    }

    void main() throws InterruptedException {
        var r1 = new Resource("R1");
        var r2 = new Resource("R2");
        // Worker A wants r1 then r2; Worker B wants r2 then r1
        // Each acquires its first, finds the second taken, backs off — in sync
        var a = new Thread(new Worker("Worker A", r1, r2));
        var b = new Thread(new Worker("Worker B", r2, r1));
        a.start();
        b.start();
        a.join();
        b.join();
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
