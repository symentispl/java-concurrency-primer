# Reentrancy, Daemon Threads, and Thread Priorities — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add 3 slides to the existing deck — one on synchronized reentrancy (inserted into the synchronized section) and two on daemon threads and thread priorities (inserted into the thread section).

**Architecture:** All changes are insertions into `slides/src/main/slides/index.adoc`. No new files. Task 1 inserts after "The Fixed Counter"; Task 2 inserts two slides after "Cooperative Shutdown — In Practice". Use exact text anchors (not line numbers) so insertions are order-independent.

**Tech Stack:** Asciidoctor RevealJS, `mise run build` to verify.

---

## File Map

| File | Action | Purpose |
|------|--------|---------|
| `slides/src/main/slides/index.adoc` | Modify (2 insertions) | Add 3 slides at correct positions |

---

### Task 1: Insert "synchronized is Reentrant" after "The Fixed Counter"

**Files:**
- Modify: `slides/src/main/slides/index.adoc`

Current slide count: **47**. After this task: **48**.

The new slide goes between "The Fixed Counter" (ends with the counter code block) and "synchronized Isn't Enough". Use the unique text anchor below.

- [ ] **Step 1: Insert the slide**

Find this exact text in `slides/src/main/slides/index.adoc`:

```
    System.out.println("Actual:   " + counter); // always 20000
}
----

== synchronized Isn't Enough
```

Replace it with:

```
    System.out.println("Actual:   " + counter); // always 20000
}
----

== synchronized is Reentrant

* A thread that holds a lock can acquire it again without blocking
* Java tracks a re-entry counter per thread on each monitor
* The lock releases only when the counter reaches zero
* Recursive calls and synchronized-to-synchronized delegation are safe

[source,java]
----
synchronized void increment() { count++; }

synchronized void incrementTwice() {
    increment(); // same thread, same lock — re-entry, not deadlock
    increment();
}
----

== synchronized Isn't Enough
```

- [ ] **Step 2: Build and verify**

```bash
mise run build
```

Expected: exits 0. Then verify slide count:

```bash
grep -c "^== " slides/src/main/slides/index.adoc
```

Expected output: `48`

- [ ] **Step 3: Commit**

```bash
git add slides/src/main/slides/index.adoc
git commit -m "feat: add synchronized is Reentrant slide"
```

---

### Task 2: Insert "Daemon Threads" and "Thread Priorities" after "Cooperative Shutdown — In Practice"

**Files:**
- Modify: `slides/src/main/slides/index.adoc`

Current slide count: **48**. After this task: **50**.

The two new slides go between "Cooperative Shutdown — In Practice" (ends with the worker code block) and "Shared Mutable State". Use the unique text anchor below.

- [ ] **Step 1: Insert the two slides**

Find this exact text in `slides/src/main/slides/index.adoc`:

```
    worker.join();      // wait for clean exit
}
----

== Shared Mutable State
```

Replace it with:

```
    worker.join();      // wait for clean exit
}
----

== Daemon Threads

* Run in the background — JVM exits when only daemon threads remain
* `thread.setDaemon(true)` must be called before `thread.start()`
* Non-daemon (user) threads — JVM waits for all of them to finish
* Typical uses: garbage collector, background timers, monitoring

== Thread Priorities

* Values from 1 (`MIN_PRIORITY`) to 10 (`MAX_PRIORITY`), default 5 (`NORM_PRIORITY`)
* `thread.setPriority(n)` — a hint to the OS scheduler, not a guarantee
* OS schedulers frequently ignore Java priorities entirely
* Never use priorities as a synchronization or coordination mechanism

== Shared Mutable State
```

- [ ] **Step 2: Build and verify**

```bash
mise run build
```

Expected: exits 0. Then verify slide count:

```bash
grep -c "^== " slides/src/main/slides/index.adoc
```

Expected output: `50`

- [ ] **Step 3: Commit**

```bash
git add slides/src/main/slides/index.adoc
git commit -m "feat: add Daemon Threads and Thread Priorities slides"
```
