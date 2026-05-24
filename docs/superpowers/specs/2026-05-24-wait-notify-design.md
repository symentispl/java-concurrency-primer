# wait()/notify() and Conditional Variables — Design Spec

**Date:** 2026-05-24
**Section:** Act 2 — Correctness (second part)
**Follows:** synchronized slides (slide 31)

---

## Goal

Teach first-year CS students how threads coordinate when one must wait for a condition produced by another. Builds directly on `synchronized` — students know mutual exclusion, now they need conditional waiting.

---

## Audience Assumptions

- Know: Java syntax, OOP, threads, interrupt(), data races, synchronized
- Don't know: lambdas, generics, Java Memory Model
- No prior exposure to wait()/notify() or monitor model

---

## Slides — 6 total

### Beat 1 — The Problem (2 slides)

**Slide 1: "synchronized Isn't Enough"**
- You enter the critical section — but the data isn't ready yet
- Spinning (busy-waiting) wastes CPU and holds the lock
- Other threads can't make progress while you spin
- You need a way to release the lock and sleep until conditions change

**Slide 2: "Monitor = Lock + Wait Set"**
Graphviz diagram showing two zones inside every Java object:
- Lock zone — threads competing to enter the monitor
- Wait set — threads parked after calling `wait()`
- `wait()` — releases the lock AND moves thread to wait set (atomic)
- `notify()`/`notifyAll()` — moves threads from wait set back to lock competition

Diagram style: two labelled boxes inside a larger "Monitor" box, arrows showing thread flow. Colour scheme consistent with existing diagrams (blue for running threads, orange for waiting).

### Beat 2 — The API (2 slides)

**Slide 3: "wait() and notify() — The Contract"**
- Must be called inside `synchronized` on the same object
- `wait()` releases the lock atomically — no window between release and sleep
- Thread can wake up without being notified (spurious wakeup)
- Always check condition in a `while` loop — never `if`

```java
// correct
while (!condition) { object.wait(); }

// wrong — misses spurious wakeups
if (!condition) { object.wait(); }
```

**Slide 4: "notify() vs notifyAll()"**
- `notify()` — wakes one arbitrary waiting thread
- `notifyAll()` — wakes all waiting threads; each re-checks its condition
- `notify()` is risky when multiple conditions share one monitor
- Use `notifyAll()` as the safe default

### Beat 3 — The Example (2 slides)

**Slide 5: "BoundedBuffer — Producer Side"**

```java
synchronized void put(int item) throws InterruptedException {
    while (count == buffer.length) {
        wait(); // buffer full — release lock and sleep
    }
    buffer[head] = item;
    head = (head + 1) % buffer.length;
    count++;
    notifyAll(); // wake consumers
}
```

**Slide 6: "BoundedBuffer — Consumer Side"**

```java
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
```

---

## Code Example

### ProducerConsumer.java

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

Expected output: "Produced: N" and "Consumed: N" lines interleaved, producer occasionally blocks when buffer is full (consumer is slower at 80ms vs producer at 50ms), all 10 items produced and consumed, ends with "Done."

---

## Slide Placement

Appended to `slides/src/main/slides/index.adoc` after the existing 31 slides. Final count: 37 slides.

---

## Out of Scope

- `java.util.concurrent.locks.Condition` (explicit condition objects)
- `BlockingQueue` from `java.util.concurrent`
- `ReentrantLock`
- Deadlock analysis
- Performance characteristics of `notifyAll()`
