# Reentrancy, Daemon Threads, and Thread Priorities — Design Spec

**Date:** 2026-05-24
**Sections:** Act 2 — Correctness (synchronized addition), Act 1 — Foundation (thread additions)

---

## Goal

Fill three gaps in the existing slide deck:
1. `synchronized` is reentrant — a property students need to understand to avoid confusion with recursive and delegating synchronized calls
2. Daemon threads — how JVM shutdown interacts with thread lifecycle
3. Thread priorities — a hint mechanism that the OS scheduler frequently ignores

---

## Audience Assumptions

- Know: Java syntax, OOP, threads, lifecycle, interruption, synchronized, wait()/notify()
- Don't know: lambdas, generics, java.util.concurrent
- No prior exposure to reentrancy, daemon threads, or priorities

---

## Slides — 3 total

### Insertion 1 — After "The Fixed Counter", before "synchronized Isn't Enough"

**Slide 1: "synchronized is Reentrant"**

- A thread that already holds a lock can acquire it again without blocking
- Java tracks a re-entry counter per thread on each monitor; the lock releases only when the counter reaches zero
- Enables recursive calls and synchronized-to-synchronized delegation on the same object
- Without reentrancy, calling a synchronized method from another synchronized method on `this` would deadlock

Code block showing re-entry in action:

```java
synchronized void increment() { count++; }

synchronized void incrementTwice() {
    increment(); // same thread, same lock — re-entry, not deadlock
    increment();
}
```

---

### Insertion 2 — After "Cooperative Shutdown — In Practice", before "Shared Mutable State"

**Slide 2: "Daemon Threads"**

- Run in the background — JVM exits when only daemon threads remain
- `thread.setDaemon(true)` must be called before `thread.start()`
- Non-daemon (user) threads: JVM waits for all of them to finish before exiting
- Typical uses: garbage collector, background timers, monitoring, housekeeping

**Slide 3: "Thread Priorities"**

- Values from 1 (`MIN_PRIORITY`) to 10 (`MAX_PRIORITY`), default 5 (`NORM_PRIORITY`)
- `thread.setPriority(n)` — a hint to the OS scheduler, not a guarantee
- OS schedulers frequently ignore Java priorities entirely; behaviour varies by platform
- Never use priorities as a synchronization or coordination mechanism

---

## Code Example

No separate Java file. The reentrancy concept is illustrated with a self-contained code block on the slide. The two methods fit on one slide and are simpler to follow without the surrounding class boilerplate.

---

## Slide Placement

Insertions into `slides/src/main/slides/index.adoc`:

| Insertion | After slide | Before slide | New slide count |
|-----------|-------------|--------------|-----------------|
| "synchronized is Reentrant" | "The Fixed Counter" | "synchronized Isn't Enough" | 48 |
| "Daemon Threads" | "Cooperative Shutdown — In Practice" | "Shared Mutable State" | 49 |
| "Thread Priorities" | "Daemon Threads" | "Shared Mutable State" | 50 |

Final slide count: **50**

---

## File Map

| File | Action | Purpose |
|------|--------|---------|
| `slides/src/main/slides/index.adoc` | Modify (3 insertions) | Add 3 slides at correct positions |

---

## Out of Scope

- Static synchronized (class-level locks) — covered in a future pitfalls section
- `Thread.yield()` — too platform-specific to be useful at this level
- Virtual threads (Java 21+) — Act 3 topic
- `ThreadGroup` — deprecated, not covered
