# wait()/notify() and Conditional Variables — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add 6 slides covering `wait()`/`notify()` and conditional variables, plus a runnable `ProducerConsumer.java` example with a bounded buffer that shows threads blocking and waking each other.

**Architecture:** Code example goes in `code/src/main/java/` as a single-file Java 25 source program runnable via `mise run java:exec -- ProducerConsumer`. Slides append to `slides/src/main/slides/index.adoc` after the existing 31 slides (final count: 37). Beat 1 (2 slides) establishes why `synchronized` alone isn't enough and introduces the monitor model. Beat 2 (2 slides) covers the API contract and `notify()` vs `notifyAll()`. Beat 3 (2 slides) shows the `BoundedBuffer` producer and consumer methods as code slides.

**Tech Stack:** Java 25 (simplified `void main()`, no `--enable-preview`), mise tasks, Asciidoctor RevealJS with graphviz.

---

## File Map

| File | Action | Purpose |
|------|--------|---------|
| `code/src/main/java/ProducerConsumer.java` | Create | Bounded buffer with producer and consumer threads |
| `slides/src/main/slides/index.adoc` | Modify | Append 6 new slides |

---

### Task 1: Create ProducerConsumer.java

**Files:**
- Create: `code/src/main/java/ProducerConsumer.java`

- [ ] **Step 1: Create the file**

Create `code/src/main/java/ProducerConsumer.java` with exactly this content:

```java
// Demonstrates wait()/notify() with a bounded buffer.
// Run: mise run java:exec -- ProducerConsumer
class ProducerConsumer {

    static class BoundedBuffer {
        private final int[] buffer;
        private int head = 0;
        private int tail = 0;
        private int count = 0;

        BoundedBuffer(int capacity) {
            buffer = new int[capacity];
        }

        synchronized void put(int item) throws InterruptedException {
            while (count == buffer.length) {
                wait(); // buffer full — release lock and sleep
            }
            buffer[head] = item;
            head = (head + 1) % buffer.length;
            count++;
            notifyAll(); // wake consumers
        }

        synchronized int take() throws InterruptedException {
            while (count == 0) {
                wait(); // buffer empty — release lock and sleep
            }
            int item = buffer[tail];
            tail = (tail + 1) % buffer.length;
            count--;
            notifyAll(); // wake producers
            return item;
        }
    }

    static class Producer implements Runnable {
        private final BoundedBuffer buffer;

        Producer(BoundedBuffer buffer) {
            this.buffer = buffer;
        }

        public void run() {
            for (int i = 1; i <= 10; i++) {
                try {
                    buffer.put(i);
                    System.out.println("Produced: " + i);
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }

    static class Consumer implements Runnable {
        private final BoundedBuffer buffer;

        Consumer(BoundedBuffer buffer) {
            this.buffer = buffer;
        }

        public void run() {
            for (int i = 0; i < 10; i++) {
                try {
                    int item = buffer.take();
                    System.out.println("Consumed: " + item);
                    Thread.sleep(80);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }

    void main() throws InterruptedException {
        var buffer = new BoundedBuffer(5);
        var producer = new Thread(new Producer(buffer));
        var consumer = new Thread(new Consumer(buffer));
        producer.start();
        consumer.start();
        producer.join();
        consumer.join();
        System.out.println("Done.");
    }
}
```

- [ ] **Step 2: Run the example and verify output**

```bash
mise run java:exec -- ProducerConsumer
```

Expected output (interleaved "Produced" and "Consumed" lines, order may vary slightly):
```
Produced: 1
Consumed: 1
Produced: 2
Produced: 3
Consumed: 2
...
Done.
```

Key invariants to check:
- All 10 items are produced (Produced: 1 through Produced: 10)
- All 10 items are consumed (Consumed: 1 through Consumed: 10)
- "Done." appears last
- Producer and consumer lines are interleaved (not all producer then all consumer)
- Consumer is slower (80ms) than producer (50ms), so producer will occasionally block on a full buffer

- [ ] **Step 3: Commit**

```bash
git add code/src/main/java/ProducerConsumer.java
git commit -m "feat: add ProducerConsumer example — bounded buffer with wait/notify"
```

---

### Task 2: Add Beat 1 slides — The Problem (2 slides)

**Files:**
- Modify: `slides/src/main/slides/index.adoc`

- [ ] **Step 1: Append the two Beat 1 slides to the end of `index.adoc`**

```asciidoc
== synchronized Isn't Enough

* You enter the critical section — but the data isn't ready
* Spinning (busy-waiting) wastes CPU and holds the lock
* Other threads cannot make progress while you spin
* You need to release the lock and sleep until things change

== Monitor = Lock + Wait Set

[graphviz]
----
digraph monitor {
    rankdir=TB
    node [shape=rect, style=filled, fontname="sans-serif", fontsize=11, margin="0.25,0.15"]
    edge [fontname="sans-serif", fontsize=10, color=white, fontcolor="#cccccc"]

    ENTER [label="Thread: entering",            fillcolor="#4a90d9", fontcolor=white]
    LOCK  [label="Lock\n(one thread at a time)", fillcolor="#5ba05b", fontcolor=white]
    WAIT  [label="Wait Set\n(parked threads)",   fillcolor="#c97c2e", fontcolor=white]
    EXIT  [label="Thread: done",                 fillcolor="#7a3a3a", fontcolor=white]

    ENTER -> LOCK [label="acquire"]
    LOCK  -> WAIT [label="wait()\nreleases lock"]
    WAIT  -> LOCK [label="notify()\nre-compete for lock"]
    LOCK  -> EXIT [label="release"]
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

Expected output: `33`

- [ ] **Step 3: Commit**

```bash
git add slides/src/main/slides/index.adoc
git commit -m "feat: add Beat 1 — synchronized isn't enough and monitor model"
```

---

### Task 3: Add Beat 2 slides — The API (2 slides)

**Files:**
- Modify: `slides/src/main/slides/index.adoc`

- [ ] **Step 1: Append the two Beat 2 slides to the end of `index.adoc`**

```asciidoc
== wait() and notify() — The Contract

* Must be called inside `synchronized` on the same object
* `wait()` releases the lock atomically — no window between release and sleep
* Thread can wake up without being notified (spurious wakeup)
* Always check condition in a `while` loop — never `if`

[source,java]
----
// correct — re-checks condition after every wakeup
while (!condition) { object.wait(); }

// wrong — misses spurious wakeups
if (!condition) { object.wait(); }
----

== notify() vs notifyAll()

* `notify()` — wakes one arbitrary waiting thread
* `notifyAll()` — wakes all waiting threads; each re-checks its condition
* `notify()` is risky when multiple conditions share one monitor
* Use `notifyAll()` as the safe default
```

- [ ] **Step 2: Build and verify**

```bash
mise run build
```

Expected: exits 0. Then verify slide count:

```bash
grep -c "^== " slides/src/main/slides/index.adoc
```

Expected output: `35`

- [ ] **Step 3: Commit**

```bash
git add slides/src/main/slides/index.adoc
git commit -m "feat: add Beat 2 — wait/notify contract and notifyAll"
```

---

### Task 4: Add Beat 3 slides — The Example (2 slides)

**Files:**
- Modify: `slides/src/main/slides/index.adoc`

- [ ] **Step 1: Append the two Beat 3 slides to the end of `index.adoc`**

```asciidoc
== BoundedBuffer — Producer Side

[source,java]
----
synchronized void put(int item) throws InterruptedException {
    while (count == buffer.length) {
        wait(); // buffer full — release lock and sleep
    }
    buffer[head] = item;
    head = (head + 1) % buffer.length;
    count++;
    notifyAll(); // wake consumers
}
----

== BoundedBuffer — Consumer Side

[source,java]
----
synchronized int take() throws InterruptedException {
    while (count == 0) {
        wait(); // buffer empty — release lock and sleep
    }
    int item = buffer[tail];
    tail = (tail + 1) % buffer.length;
    count--;
    notifyAll(); // wake producers
    return item;
}
----
```

- [ ] **Step 2: Build and verify final slide count**

```bash
mise run build
```

Expected: exits 0. Then verify:

```bash
grep -c "^== " slides/src/main/slides/index.adoc
```

Expected output: `37`

- [ ] **Step 3: Commit**

```bash
git add slides/src/main/slides/index.adoc
git commit -m "feat: add Beat 3 — BoundedBuffer producer and consumer code slides"
```
