# Data Races and synchronized — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add 8 slides covering data races and `synchronized`, plus two runnable Java examples (`DataRace.java` and `SynchronizedCounter.java`) that let students see the race happen and then see it fixed.

**Architecture:** Code examples go in `code/src/main/java/` as single-file Java 25 source programs runnable via `mise run java:exec -- ClassName`. Slides append to `slides/src/main/slides/index.adoc` after the existing 23 slides (final count: 31). Beat 1 (5 slides) covers data races — what they are, why `i++` isn't atomic, and CPU cache visibility. Beat 2 (3 slides) covers `synchronized` — mutual exclusion, every-object-is-a-monitor, and the fixed counter.

**Tech Stack:** Java 25 (simplified `void main()`, no `--enable-preview`), mise tasks, Asciidoctor RevealJS with graphviz.

---

## File Map

| File | Action | Purpose |
|------|--------|---------|
| `code/src/main/java/DataRace.java` | Create | Broken counter — shows race non-determinism |
| `code/src/main/java/SynchronizedCounter.java` | Create | Fixed counter — always 20000 |
| `slides/src/main/slides/index.adoc` | Modify | Append 8 new slides |

---

### Task 1: Create DataRace.java

**Files:**
- Create: `code/src/main/java/DataRace.java`

- [ ] **Step 1: Create the file**

Create `code/src/main/java/DataRace.java` with exactly this content:

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

- [ ] **Step 2: Run the example and verify the race manifests**

```bash
mise run java:exec -- DataRace
```

Expected output format:
```
Expected: 20000
Actual:   17843
```

The "Actual" value must be less than 20000 to confirm the race. On modern multi-core hardware this almost always happens with 10_000 iterations. If you see 20000, run it again — the race is non-deterministic.

Key invariant: "Actual" is unpredictable and differs between runs.

- [ ] **Step 3: Commit**

```bash
git add code/src/main/java/DataRace.java
git commit -m "feat: add DataRace example — demonstrates counter data race"
```

---

### Task 2: Create SynchronizedCounter.java

**Files:**
- Create: `code/src/main/java/SynchronizedCounter.java`

- [ ] **Step 1: Create the file**

Create `code/src/main/java/SynchronizedCounter.java` with exactly this content:

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

- [ ] **Step 2: Run the example and verify it always prints 20000**

```bash
mise run java:exec -- SynchronizedCounter
```

Expected output (every run, deterministic):
```
Expected: 20000
Actual:   20000
```

Run it 3 times to confirm it's always correct.

- [ ] **Step 3: Commit**

```bash
git add code/src/main/java/SynchronizedCounter.java
git commit -m "feat: add SynchronizedCounter example — synchronized fixes the race"
```

---

### Task 3: Add Beat 1 — Data Race slides (5 slides)

**Files:**
- Modify: `slides/src/main/slides/index.adoc`

- [ ] **Step 1: Append the five data race slides to the end of `index.adoc`**

```asciidoc
== Shared Mutable State

* Two threads, one variable, at least one writes → data race
* Result depends on scheduling — different every run
* The counter problem: two threads each add 1, ten thousand times
* Expected: 20000 — Actual: less, and unpredictable

== i++ is Three Steps

[graphviz]
----
digraph interleave {
    rankdir=TB
    node [shape=rect, style=filled, fontname="monospace", fontsize=11, margin="0.25,0.15"]
    edge [color=white, fontcolor="#cccccc", fontname="sans-serif", fontsize=10]

    A1 [label="Thread A: LOAD counter → 5",  fillcolor="#4a90d9", fontcolor=white]
    B1 [label="Thread B: LOAD counter → 5",  fillcolor="#c97c2e", fontcolor=white]
    A2 [label="Thread A: ADD  5+1=6",         fillcolor="#4a90d9", fontcolor=white]
    B2 [label="Thread B: ADD  5+1=6",         fillcolor="#c97c2e", fontcolor=white]
    A3 [label="Thread A: STORE counter=6",    fillcolor="#4a90d9", fontcolor=white]
    B3 [label="Thread B: STORE counter=6",    fillcolor="#c97c2e", fontcolor=white]
    R  [label="counter=6  ← expected 7, one increment lost", fillcolor="#7a3a3a", fontcolor=white]

    A1 -> B1
    B1 -> A2
    A2 -> B2
    B2 -> A3
    A3 -> B3
    B3 -> R
}
----

== Why CPU Caches Exist

* RAM access: ~100 CPU cycles — a CPU operation: ~1 cycle
* Without caches, every instruction stalls waiting for memory
* Each core has its own private L1/L2 cache — fast, small
* Shared L3 cache, then main memory — slow, large

== Caches and Visibility

* Thread A writes `counter` — stored in its L1 cache first
* Thread B reads `counter` from its own L1 — may see a stale value
* No guarantee writes are immediately visible to other cores
* `synchronized` flushes and invalidates caches — ensures visibility

== The Broken Counter

[source,java]
----
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
    t1.start(); t2.start();
    t1.join();  t2.join();
    System.out.println("Expected: 20000");
    System.out.println("Actual:   " + counter); // usually less
}
----
```

- [ ] **Step 2: Build slides and verify**

```bash
mise run build
```

Expected: exits 0. Then verify slide count:

```bash
grep -c "^== " slides/src/main/slides/index.adoc
```

Expected output: `28`

- [ ] **Step 3: Commit**

```bash
git add slides/src/main/slides/index.adoc
git commit -m "feat: add Beat 1 — data race slides with interleaving diagram"
```

---

### Task 4: Add Beat 2 — synchronized slides (3 slides)

**Files:**
- Modify: `slides/src/main/slides/index.adoc`

- [ ] **Step 1: Append the three synchronized slides to the end of `index.adoc`**

```asciidoc
== synchronized — Mutual Exclusion

* Acquires a lock before entering the protected block
* Only one thread holds the lock at a time — others block
* Guarantees: mutual exclusion AND memory visibility
* Lock released automatically when the block exits

== Every Object Has a Lock

[source,java]
----
// synchronized method — locks on 'this'
synchronized void increment() { counter++; }

// synchronized block — explicit lock object (preferred)
synchronized (lock) { counter++; }
----

* Every Java object can serve as a lock
* Prefer synchronized block — makes the lock object explicit

== The Fixed Counter

[source,java]
----
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
    t1.start(); t2.start();
    t1.join();  t2.join();
    System.out.println("Expected: 20000");
    System.out.println("Actual:   " + counter); // always 20000
}
----
```

- [ ] **Step 2: Build slides and verify final slide count**

```bash
mise run build
```

Expected: exits 0. Then verify:

```bash
grep -c "^== " slides/src/main/slides/index.adoc
```

Expected output: `31`

- [ ] **Step 3: Commit**

```bash
git add slides/src/main/slides/index.adoc
git commit -m "feat: add Beat 2 — synchronized slides with fixed counter"
```
