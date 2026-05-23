# Cooperative Shutdown — Working Example + Slides Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a `mise run exec` task for running Java examples, create `CooperativeShutdown.java` as the first working code example, and add 2 slides showing the cooperative shutdown pattern in practice.

**Architecture:** Java examples live in `code/src/main/java/` as single-file source programs using Java 25's standard `void main()`. The `exec` mise task runs any example by class name: `mise run exec -- CooperativeShutdown`. Two new slides (scenario + condensed code) follow the existing interruption slides, with the code slide referencing the runnable file.

**Tech Stack:** Java 25 (simplified main, standard), mise tasks, Asciidoctor RevealJS.

---

## File Map

| File | Action | Purpose |
|------|--------|---------|
| `.mise.toml` | Modify | Add `exec` task |
| `code/src/main/java/CooperativeShutdown.java` | Create | Full working cooperative shutdown example |
| `slides/src/main/slides/index.adoc` | Modify | Add 2 slides: scenario + condensed code |

---

### Task 1: Add `exec` mise task

**Files:**
- Modify: `.mise.toml`

- [ ] **Step 1: Add the `exec` task to `.mise.toml`**

Add this block at the end of `.mise.toml`, after the existing `[tasks.package]` block:

```toml
[tasks.exec]
description = "Run a Java example: mise run exec -- ClassName"
run = "java code/src/main/java/$1.java"
```

- [ ] **Step 2: Verify the task is registered**

```bash
mise tasks | grep exec
```

Expected output contains: `exec  Run a Java example: mise run exec -- ClassName`

- [ ] **Step 3: Commit**

```bash
git add .mise.toml
git commit -m "feat: add mise exec task for running Java examples"
```

---

### Task 2: Create CooperativeShutdown.java

**Files:**
- Create: `code/src/main/java/CooperativeShutdown.java`

- [ ] **Step 1: Create the directory and the file**

Create `code/src/main/java/CooperativeShutdown.java` with exactly this content:

```java
// Demonstrates cooperative thread shutdown using interrupt() and Thread.interrupted().
// Run: mise run exec -- CooperativeShutdown
class CooperativeShutdown {

    static class Worker implements Runnable {

        public void run() {
            System.out.println("Worker: started");
            while (!Thread.interrupted()) {
                doWork();
            }
            System.out.println("Worker: flag seen, stopping cleanly");
            Thread.currentThread().interrupt(); // restore flag for caller
        }

        private void doWork() {
            try {
                Thread.sleep(100);
                System.out.println("Worker: item processed");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); // restore so outer loop exits
            }
        }
    }

    void main() throws InterruptedException {
        var worker = new Thread(new Worker());
        worker.start();

        Thread.sleep(350); // let worker process a few items

        System.out.println("Main:   requesting shutdown");
        worker.interrupt();
        worker.join();     // wait for clean exit
        System.out.println("Main:   worker stopped cleanly");
    }
}
```

- [ ] **Step 2: Run the example and verify output**

```bash
mise run exec -- CooperativeShutdown
```

Expected output (order of Worker lines may vary slightly by timing):
```
Worker: started
Worker: item processed
Worker: item processed
Worker: item processed
Main:   requesting shutdown
Worker: flag seen, stopping cleanly
Main:   worker stopped cleanly
```

The key invariants to check:
- "Worker: started" appears first
- Several "Worker: item processed" lines appear (at least 2)
- "Main: requesting shutdown" appears before "Worker: flag seen, stopping cleanly"
- "Main: worker stopped cleanly" appears last

- [ ] **Step 3: Commit**

```bash
git add code/src/main/java/CooperativeShutdown.java
git commit -m "feat: add CooperativeShutdown example — first Java 25 working example"
```

---

### Task 3: Add 2 cooperative shutdown slides

**Files:**
- Modify: `slides/src/main/slides/index.adoc`

- [ ] **Step 1: Append the two slides to the end of `index.adoc`**

```asciidoc
== Cooperative Shutdown — The Pattern

* Worker checks `Thread.interrupted()` in its loop
* Main calls `worker.interrupt()` to signal shutdown
* Worker exits the loop, restores the flag, returns
* Main calls `worker.join()` — waits for clean exit

== Cooperative Shutdown — In Practice

[source,java]
----
class Worker implements Runnable {
    public void run() {
        while (!Thread.interrupted()) {
            doWork();
        }
        Thread.currentThread().interrupt(); // restore flag
    }
    private void doWork() {
        try { Thread.sleep(100); }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // restore, loop will exit
        }
    }
}

void main() throws InterruptedException {
    var worker = new Thread(new Worker());
    worker.start();
    Thread.sleep(350);
    worker.interrupt(); // request shutdown
    worker.join();      // wait for clean exit
}
----
```

- [ ] **Step 2: Build slides and verify slide count**

```bash
mise run build
```

Expected: exits 0. Then verify:

```bash
grep -c "^== " slides/src/main/slides/index.adoc
```

Expected output: `23`

- [ ] **Step 3: Commit**

```bash
git add slides/src/main/slides/index.adoc
git commit -m "feat: add cooperative shutdown slides with two-thread interaction"
```
