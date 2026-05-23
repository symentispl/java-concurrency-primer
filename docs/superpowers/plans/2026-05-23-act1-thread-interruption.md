# Act 1 — Thread Interruption Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add 5 slides covering Java thread interruption — what it is, how blocking and non-blocking code respond to it, and the contract for handling InterruptedException correctly.

**Architecture:** All slides appended to `slides/src/main/slides/index.adoc` after the existing 16 content slides. Two tasks: Task 1 adds the three conceptual slides (Beats 1 + 2), Task 2 adds the contract slide and code example (Beat 3). Final slide count: 21.

**Tech Stack:** Asciidoctor RevealJS, mise for build tasks.

---

## File Map

| File | Action | Purpose |
|------|--------|---------|
| `slides/src/main/slides/index.adoc` | Modify | Append 5 new slides after existing 16 |

---

### Task 1: Beats 1 + 2 — What Interruption Is and How to Respond (3 slides)

**Files:**
- Modify: `slides/src/main/slides/index.adoc`

- [ ] **Step 1: Append the three slides to the end of `index.adoc`**

```asciidoc
== Interruption is a Request, Not a Command

* `thread.interrupt()` sets a flag on the target thread
* The thread must check and respond — it is cooperative
* Cannot forcibly stop a running thread (why `stop()` was removed)
* Safe shutdown requires the thread's cooperation

== Blocking Methods React Automatically

* `sleep()`, `wait()`, `join()` throw `InterruptedException` when interrupted
* The interrupted flag is cleared before the exception is thrown
* Catch it, handle it, or let it propagate

== Non-Blocking Code Must Check the Flag

* `Thread.interrupted()` — checks and clears the flag (static)
* `thread.isInterrupted()` — checks without clearing (instance)
* Check in long-running loops to stay responsive
```

- [ ] **Step 2: Build and verify**

```bash
mise run build
```

Expected: exits 0. Verify `build/slides/index.html` contains slide headings "Interruption is a Request, Not a Command", "Blocking Methods React Automatically", and "Non-Blocking Code Must Check the Flag".

- [ ] **Step 3: Commit**

```bash
git add slides/src/main/slides/index.adoc
git commit -m "feat: add thread interruption slides — what it is and how to respond"
```

---

### Task 2: Beat 3 — The Interruption Contract (1 bullet slide + 1 code slide)

**Files:**
- Modify: `slides/src/main/slides/index.adoc`

- [ ] **Step 1: Append the contract and code slides to the end of `index.adoc`**

```asciidoc
== Don't Swallow InterruptedException

* Catching and ignoring it hides the shutdown signal
* Option 1: re-throw — `throws InterruptedException`
* Option 2: restore the flag — `Thread.currentThread().interrupt()`
* Never: `catch (InterruptedException e) { }` with empty body

== Handling Interruption

[source,java]
----
// blocking code — let the exception propagate
void waitForResult() throws InterruptedException {
    Thread.sleep(1000);
}

// non-blocking loop — check the flag manually
void run() {
    while (!Thread.interrupted()) {
        processNextItem();
    }
}
----
```

- [ ] **Step 2: Build and verify slide count**

```bash
mise run build
```

Expected: exits 0. Then verify final slide count:

```bash
grep -c "^== " slides/src/main/slides/index.adoc
```

Expected output: `21`

- [ ] **Step 3: Commit**

```bash
git add slides/src/main/slides/index.adoc
git commit -m "feat: add interruption contract slide and code example"
```
