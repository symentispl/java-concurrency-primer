# Progress Guarantees Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add 10 slides and 3 runnable code examples covering deadlock, livelock, and starvation — each with a "what it is" beat and a "how to prevent it" beat.

**Architecture:** Code examples go in `code/src/main/java/` as single-file Java 25 programs runnable via `mise run java:exec -- ClassName`. Slides append to `slides/src/main/slides/index.adoc` after the existing 37 slides (final count: 47). Deadlock and livelock each get a code example; starvation is covered by slides only.

**Tech Stack:** Java 25 (simplified `void main()`, no `--enable-preview`), mise tasks, Asciidoctor RevealJS with graphviz.

---

## File Map

| File | Action | Purpose |
|------|--------|---------|
| `code/src/main/java/Deadlock.java` | Create | Two-thread deadlock that hangs visibly |
| `code/src/main/java/DeadlockFixed.java` | Create | Same scenario with consistent lock ordering — completes |
| `code/src/main/java/Livelock.java` | Create | Two-thread livelock using synchronized Resource — exhausts retries |
| `slides/src/main/slides/index.adoc` | Modify | Append 10 new slides (37→47) |

---

### Task 1: Create Deadlock.java

**Files:**
- Create: `code/src/main/java/Deadlock.java`

- [ ] **Step 1: Create the file**

Create `code/src/main/java/Deadlock.java` with exactly this content:

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

- [ ] **Step 2: Run the example and verify it hangs**

```bash
timeout 5 mise run java:exec -- Deadlock; echo "exit: $?"
```

Expected output (then hangs until timeout kills it):
```
Starting
Thread A: holds lockA, waiting for lockB
Thread B: holds lockB, waiting for lockA
exit: 124
```

Order of the two "holds" lines may vary. Exit code 124 means `timeout` killed the process — the deadlock is confirmed. "Done." must NOT appear.

- [ ] **Step 3: Commit**

```bash
git add code/src/main/java/Deadlock.java
git commit -m "feat: add Deadlock example — two threads acquire locks in opposite order"
```

---

### Task 2: Create DeadlockFixed.java

**Files:**
- Create: `code/src/main/java/DeadlockFixed.java`

- [ ] **Step 1: Create the file**

Create `code/src/main/java/DeadlockFixed.java` with exactly this content:

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
            synchronized (lockA) {  // same order as Thread A — no cycle possible
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

- [ ] **Step 2: Run the example and verify it completes**

```bash
mise run java:exec -- DeadlockFixed
```

Expected output:
```
Starting
Thread A: done
Thread B: done
Done.
```

Order of the two "done" lines may vary. "Done." must appear — the program must not hang.

- [ ] **Step 3: Commit**

```bash
git add code/src/main/java/DeadlockFixed.java
git commit -m "feat: add DeadlockFixed example — consistent lock ordering prevents deadlock"
```

---

### Task 3: Create Livelock.java

**Files:**
- Create: `code/src/main/java/Livelock.java`

- [ ] **Step 1: Create the file**

Create `code/src/main/java/Livelock.java` with exactly this content:

```java
// Demonstrates livelock: two workers keep backing off and make no progress.
// Run: mise run java:exec -- Livelock
class Livelock {

    static class Resource {
        private boolean inUse = false;

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
        var r1 = new Resource();
        var r2 = new Resource();
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

- [ ] **Step 2: Run the example and verify output**

```bash
mise run java:exec -- Livelock
```

Expected output (interleaved "backing off" lines from both workers):
```
Worker A: backing off (attempt 1)
Worker B: backing off (attempt 1)
Worker A: backing off (attempt 2)
Worker B: backing off (attempt 2)
...
Worker A: gave up after 20 attempts
Worker B: gave up after 20 attempts
Done — neither worker made progress.
```

Key invariants:
- Neither worker prints "done"
- Both print "gave up after 20 attempts"
- "Done — neither worker made progress." appears last
- "backing off" lines from both workers are interleaved

- [ ] **Step 3: Commit**

```bash
git add code/src/main/java/Livelock.java
git commit -m "feat: add Livelock example — synchronized Resource pattern with polite backoff"
```

---

### Task 4: Add Beat 1 slides — Deadlock (4 slides)

**Files:**
- Modify: `slides/src/main/slides/index.adoc`

- [ ] **Step 1: Append the four Beat 1 slides to the end of `index.adoc`**

```asciidoc
== Deadlock

* Each thread holds a lock the other needs
* Neither thread can proceed — both block forever
* The program hangs: no output, no crash, no error
* Requires four conditions: mutual exclusion, hold-and-wait, no preemption, circular wait

[graphviz]
----
digraph deadlock {
    rankdir=LR
    node [shape=rect, style=filled, fontname="sans-serif", fontsize=11, margin="0.25,0.15"]
    edge [fontname="sans-serif", fontsize=10, color=white, fontcolor="#cccccc"]

    TA [label="Thread A", fillcolor="#4a90d9", fontcolor=white]
    TB [label="Thread B", fillcolor="#4a90d9", fontcolor=white]
    L1 [label="Lock 1",   fillcolor="#c97c2e", fontcolor=white]
    L2 [label="Lock 2",   fillcolor="#c97c2e", fontcolor=white]

    TA -> L1 [label="holds"]
    TA -> L2 [label="waiting for", style=dashed]
    TB -> L2 [label="holds"]
    TB -> L1 [label="waiting for", style=dashed]
}
----

== Deadlock in Java

[source,java]
----
// Thread A: lockA → lockB
synchronized (lockA) {
    Thread.sleep(50);
    synchronized (lockB) { /* never reached */ }
}

// Thread B: lockB → lockA  ← opposite order → deadlock
synchronized (lockB) {
    Thread.sleep(50);
    synchronized (lockA) { /* never reached */ }
}
----

== Preventing Deadlock — Lock Ordering

* Always acquire multiple locks in the same fixed order across all threads
* If every thread takes Lock 1 before Lock 2, circular wait is impossible
* Establish a consistent global order and document it — it is a contract
* `System.identityHashCode(lock)` gives a stable ordering when locks aren't naturally ordered

== Lock Ordering — Fixed

[source,java]
----
// both threads: lockA → lockB — no cycle possible

// Thread A:
synchronized (lockA) {
    Thread.sleep(50);
    synchronized (lockB) { System.out.println("Thread A: done"); }
}

// Thread B — same order:
synchronized (lockA) {
    Thread.sleep(50);
    synchronized (lockB) { System.out.println("Thread B: done"); }
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

Expected output: `41`

- [ ] **Step 3: Commit**

```bash
git add slides/src/main/slides/index.adoc
git commit -m "feat: add Beat 1 — deadlock slides with resource graph and lock ordering fix"
```

---

### Task 5: Add Beat 2 slides — Livelock (3 slides)

**Files:**
- Modify: `slides/src/main/slides/index.adoc`

- [ ] **Step 1: Append the three Beat 2 slides to the end of `index.adoc`**

```asciidoc
== Livelock

* Threads are active — not blocked, not waiting
* Each thread keeps reacting to the other's state
* No progress is made despite constant CPU activity
* Harder to detect than deadlock: the program does not hang, it just spins

== Livelock in Java

[source,java]
----
// Worker A wants r1→r2, Worker B wants r2→r1
// each acquires its first, finds the second taken, backs off — in sync

if (!second.tryAcquire()) {
    first.release();  // politely step aside
    System.out.println(name + ": backing off");
    Thread.sleep(10); // wait... so does the other thread
    continue;         // retry — same result
}
----

== Preventing Livelock

* Add randomness: random backoff delay breaks the mirroring
* Impose a retry limit: after N attempts, one thread proceeds regardless
* Use a coordinator: a third party decides who goes first
* Timeout: if resources cannot be acquired within a deadline, signal an error
```

- [ ] **Step 2: Build and verify**

```bash
mise run build
```

Expected: exits 0. Then verify slide count:

```bash
grep -c "^== " slides/src/main/slides/index.adoc
```

Expected output: `44`

- [ ] **Step 3: Commit**

```bash
git add slides/src/main/slides/index.adoc
git commit -m "feat: add Beat 2 — livelock slides with backoff pattern and prevention"
```

---

### Task 6: Add Beat 3 slides — Starvation + Summary (3 slides)

**Files:**
- Modify: `slides/src/main/slides/index.adoc`

- [ ] **Step 1: Append the three Beat 3 slides to the end of `index.adoc`**

```asciidoc
== Starvation

* A thread is perpetually denied access even though others keep making progress
* The system works — but one thread never gets its turn
* Causes: `notify()` wakes an arbitrary thread, long-held locks, thread priorities
* Silent: no hang, no error — the starved thread simply never runs

== Preventing Starvation

* Use `notifyAll()` instead of `notify()` — all threads get a chance to re-check
* Keep `synchronized` blocks short — reduce time others are locked out
* Do not rely on thread priorities — the OS scheduler does not honour them reliably
* `java.util.concurrent` provides fair lock implementations for production use

== The Three Failure Modes

* *Deadlock* — threads block forever waiting for each other → prevent with lock ordering
* *Livelock* — threads spin forever reacting to each other → prevent with randomness or limits
* *Starvation* — one thread never gets access → prevent with fairness and short critical sections
```

- [ ] **Step 2: Build and verify final slide count**

```bash
mise run build
```

Expected: exits 0. Then verify:

```bash
grep -c "^== " slides/src/main/slides/index.adoc
```

Expected output: `47`

- [ ] **Step 3: Commit**

```bash
git add slides/src/main/slides/index.adoc
git commit -m "feat: add Beat 3 — starvation slides and three failure modes summary"
```
