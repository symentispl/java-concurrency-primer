# Act 1 — Thread Creation, Control, and States Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add 7 slides covering how to create a thread (Runnable vs extending Thread), how to start and join threads (with stop() deprecation warning), and the full thread state lifecycle with a state-transition diagram.

**Architecture:** All slides appended to `slides/src/main/slides/index.adoc` after the existing intro section (9 content slides). Three beats: creating threads (3 slides), controlling threads (2 slides), thread states (2 slides). One sparse code slide shows the Runnable-vs-extends-Thread contrast. One graphviz diagram shows the 6-state lifecycle.

**Tech Stack:** Asciidoctor RevealJS, Graphviz (via Asciidoctor Diagram), mise for build tasks.

---

## File Map

| File | Action | Purpose |
|------|--------|---------|
| `slides/src/main/slides/index.adoc` | Modify | Append 7 new slides after existing 9 intro slides |

---

### Task 1: Add Beat 1 — Creating a Thread (3 slides)

**Files:**
- Modify: `slides/src/main/slides/index.adoc`

- [ ] **Step 1: Append the three Creating a Thread slides to the end of `index.adoc`**

```asciidoc
== Creating a Thread

* `Thread` wraps a unit of work
* Pass work as a `Runnable` to the constructor
* Runnable: one method, no state, easy to test
* Start execution with `thread.start()`

== Why Not Extend Thread?

* Mixes WHAT to do with HOW to run it
* Prevents reuse in thread pools later
* Composition over inheritance
* Bad practice — avoid it

== The Right Way

[source,java]
----
// avoid: mixes work with execution
class Task extends Thread {
    public void run() { /* work */ }
}

// prefer: Runnable separates concerns
class Task implements Runnable {
    public void run() { /* work */ }
}

Thread t = new Thread(new Task());
t.start();
----
```

- [ ] **Step 2: Build and verify**

```bash
mise run build
```

Expected: exits 0. Three new slides appear after "Today's Journey". The code slide should render with syntax highlighting (Java). Verify in `build/slides/index.html` that slides 11, 12, 13 have the correct titles.

- [ ] **Step 3: Commit**

```bash
git add slides/src/main/slides/index.adoc
git commit -m "feat: add Beat 1 — creating a Thread (Runnable vs extends Thread)"
```

---

### Task 2: Add Beat 2 — Controlling Threads (2 slides)

**Files:**
- Modify: `slides/src/main/slides/index.adoc`

- [ ] **Step 1: Append the two controlling-threads slides to the end of `index.adoc`**

```asciidoc
== Starting a Thread

* `thread.start()` — requests a new OS thread from the JVM
* Returns immediately — does not block the caller
* Never call `run()` directly — it runs in the current thread
* `thread.stop()` — deprecated, scheduled for removal, never use

== Waiting for Completion

* `thread.join()` — blocks until the thread terminates
* `thread.join(millis)` — with a time limit
* `thread.isAlive()` — non-blocking check
* Throws `InterruptedException` — always handle it
```

- [ ] **Step 2: Build and verify**

```bash
mise run build
```

Expected: exits 0. Two new slides appear after "The Right Way". Check that monospace formatting renders on `thread.start()`, `thread.stop()`, `thread.join()`, `thread.isAlive()`, and `InterruptedException`.

- [ ] **Step 3: Commit**

```bash
git add slides/src/main/slides/index.adoc
git commit -m "feat: add Beat 2 — starting and joining threads"
```

---

### Task 3: Add Beat 3 — Thread States (1 bullet slide + 1 diagram)

**Files:**
- Modify: `slides/src/main/slides/index.adoc`

- [ ] **Step 1: Append the thread states slides to the end of `index.adoc`**

```asciidoc
== Thread States

* 6 states defined in `Thread.State` enum
* Inspect with `thread.getState()`
* Visible in thread dumps and monitoring tools
* State changes are driven by JVM and OS scheduler

== Thread Lifecycle

[graphviz]
----
digraph thread_states {
    rankdir=LR
    node [shape=rect, style=filled, fontname="sans-serif", margin="0.25,0.15"]
    edge [fontname="sans-serif", fontsize=11, color=white, fontcolor="#cccccc"]

    NEW          [label="NEW",          fillcolor="#4a90d9", fontcolor=white]
    RUNNABLE     [label="RUNNABLE",     fillcolor="#5ba05b", fontcolor=white]
    BLOCKED      [label="BLOCKED",      fillcolor="#c97c2e", fontcolor=white]
    WAITING      [label="WAITING",      fillcolor="#c97c2e", fontcolor=white]
    TIMED        [label="TIMED\nWAITING", fillcolor="#c97c2e", fontcolor=white]
    TERMINATED   [label="TERMINATED",   fillcolor="#7a3a3a", fontcolor=white]

    NEW      -> RUNNABLE   [label="start()"]
    RUNNABLE -> BLOCKED    [label="waiting\nfor lock"]
    BLOCKED  -> RUNNABLE   [label="lock\nacquired"]
    RUNNABLE -> WAITING    [label="wait()\njoin()"]
    WAITING  -> RUNNABLE   [label="notify()\ninterrupt()"]
    RUNNABLE -> TIMED      [label="sleep(t)\nwait(t)"]
    TIMED    -> RUNNABLE   [label="timeout\ninterrupt()"]
    RUNNABLE -> TERMINATED [label="run()\nreturns"]
}
----
```

- [ ] **Step 2: Build and verify**

```bash
mise run build
```

Expected: exits 0. Two new slides appear. The graphviz diagram must render without errors. Verify the state-transition graph shows all 6 states (NEW, RUNNABLE, BLOCKED, WAITING, TIMED WAITING, TERMINATED) and all 8 transitions are present with edge labels. Check `build/slides/index.html` — the full deck should now have 16 content slides (9 intro + 7 Act 1).

Count all `==` headings to confirm: run `grep -c "^== " slides/src/main/slides/index.adoc` — expected output: `16`.

- [ ] **Step 3: Commit**

```bash
git add slides/src/main/slides/index.adoc
git commit -m "feat: add Beat 3 — thread states and lifecycle diagram"
```
