# Java Concurrency Primer — Intro Section Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the 9-slide intro section for the Java Concurrency Primer training in `slides/src/main/slides/index.adoc`, replacing the bootstrap placeholder content.

**Architecture:** All slides live in a single AsciiDoc file using Asciidoctor RevealJS. The intro section has three beats (Why Concurrency, OS Foundations, Java's Abstraction) plus a closing Agenda slide. Two graphviz diagrams illustrate the CPU hardware model and the process/thread memory model. Each `==` heading produces one horizontal slide. Diagrams are embedded inline using the `[graphviz]` block.

**Tech Stack:** Asciidoctor RevealJS, Graphviz (via Asciidoctor Diagram), mise for build/serve tasks.

---

## File Map

| File | Action | Purpose |
|------|--------|---------|
| `slides/src/main/slides/index.adoc` | Modify | The single source of truth for all slides |

---

### Task 1: Update presentation title and clear bootstrap content

**Files:**
- Modify: `slides/src/main/slides/index.adoc`

- [ ] **Step 1: Replace the file contents with a clean title-only document**

Replace the entire file content with the following. This preserves all existing header attributes but updates the title and removes the three bootstrap example slides (`== code`, `== graphviz`, `== mermaid`):

```asciidoc
= Java Concurrency Primer
:idprefix:
:stem: asciimath
:backend: html
:source-highlighter: highlightjs
:highlightjs-style: github
:revealjs_history: true
:revealjs_theme: night
:revealjs_controls: false
:revealjs_width: 1920
:revealjs_height: 1080
:revealjs_plugins: revealjs-plugins.js
:imagesdir: images
:customcss: css/custom.css
:icons: font
:title-slide-background-image: pexels-pixabay-327049.jpg
```

The `=` heading auto-generates the title slide. No `==` slides yet.

- [ ] **Step 2: Build and verify the title slide renders**

```bash
mise run build
```

Expected: exits 0, `build/slides/index.html` exists. Open it in a browser and confirm the title slide shows "Java Concurrency Primer" with the background image and no example slides.

- [ ] **Step 3: Commit**

```bash
git add slides/src/main/slides/index.adoc
git commit -m "chore: replace bootstrap placeholder with Java Concurrency Primer title"
```

---

### Task 2: Add Beat 1 — Why Concurrency (2 slides + CPU diagram)

**Files:**
- Modify: `slides/src/main/slides/index.adoc`

- [ ] **Step 1: Append the two bullet slides and CPU diagram after the header**

Add the following to the end of `index.adoc`:

```asciidoc
== The Hardware Reality

* Modern CPUs: 4, 8, 16+ cores
* Each core executes instructions independently
* Hyper-threading: 2 hardware threads per physical core
* The hardware has been parallel for decades

== Concurrency is Everywhere

* Web servers handle thousands of simultaneous requests
* UIs stay responsive while work runs in the background
* Databases serve multiple clients at once
* Writing sequential software ignores the machine under your feet

== CPU Architecture

[graphviz]
----
digraph cpu {
    rankdir=TB
    node [shape=rect, style=filled, fontname="sans-serif", margin="0.3,0.15"]
    edge [color=white]

    CPU [label="CPU", fillcolor="#4a90d9", fontcolor=white, width=6]

    C1 [label="Core 1", fillcolor="#5ba05b", fontcolor=white]
    C2 [label="Core 2", fillcolor="#5ba05b", fontcolor=white]
    CN [label="Core N", fillcolor="#5ba05b", fontcolor=white, style="filled,dashed"]

    T1a [label="HW Thread", fillcolor="#c97c2e", fontcolor=white]
    T1b [label="HW Thread", fillcolor="#c97c2e", fontcolor=white]
    T2a [label="HW Thread", fillcolor="#c97c2e", fontcolor=white]
    T2b [label="HW Thread", fillcolor="#c97c2e", fontcolor=white]
    Tna [label="HW Thread", fillcolor="#c97c2e", fontcolor=white]
    Tnb [label="HW Thread", fillcolor="#c97c2e", fontcolor=white]

    CPU -> C1
    CPU -> C2
    CPU -> CN
    C1 -> T1a
    C1 -> T1b
    C2 -> T2a
    C2 -> T2b
    CN -> Tna
    CN -> Tnb
}
----
```

- [ ] **Step 2: Build and verify the three slides render**

```bash
mise run build
```

Expected: exits 0. Open `build/slides/index.html`. Navigate past the title slide — you should see "The Hardware Reality", "Concurrency is Everywhere", and the CPU hierarchy diagram on a dark background.

- [ ] **Step 3: Commit**

```bash
git add slides/src/main/slides/index.adoc
git commit -m "feat: add Beat 1 — Why Concurrency slides and CPU diagram"
```

---

### Task 3: Add Beat 2 — OS Foundations (2 slides + process/thread diagram)

**Files:**
- Modify: `slides/src/main/slides/index.adoc`

- [ ] **Step 1: Append OS Foundations slides after the Beat 1 content**

Add the following to the end of `index.adoc`:

```asciidoc
== Processes

* A running instance of a program
* Has its own isolated memory space
* Owns resources: open files, network sockets
* Heavy to create, expensive to context-switch

== Threads

* A unit of execution inside a process
* Shares memory with sibling threads in the same process
* In Linux: implemented as tasks via `clone()`
* Scheduled by the kernel preemptively

== Processes vs Threads

[graphviz]
----
digraph mem {
    rankdir=LR
    compound=true
    node [shape=rect, style=filled, fontname="sans-serif", margin="0.3,0.15"]
    edge [color=white]

    subgraph cluster_p1 {
        label="Process A"
        style=filled
        fillcolor="#1a3a5c"
        fontcolor=white
        color=white

        H1 [label="Heap\n(shared)", fillcolor="#4a90d9", fontcolor=white, shape=cylinder]

        subgraph cluster_t1 {
            label="Thread 1"
            style=filled
            fillcolor="#2a4a2a"
            color=white
            fontcolor=white
            S1 [label="Stack", fillcolor="#5ba05b", fontcolor=white]
        }
        subgraph cluster_t2 {
            label="Thread 2"
            style=filled
            fillcolor="#2a4a2a"
            color=white
            fontcolor=white
            S2 [label="Stack", fillcolor="#5ba05b", fontcolor=white]
        }
    }

    subgraph cluster_p2 {
        label="Process B"
        style=filled
        fillcolor="#3a1a1a"
        fontcolor=white
        color=white

        H2 [label="Heap\n(shared)", fillcolor="#c97c2e", fontcolor=white, shape=cylinder]

        subgraph cluster_t3 {
            label="Thread 1"
            style=filled
            fillcolor="#2a4a2a"
            color=white
            fontcolor=white
            S3 [label="Stack", fillcolor="#5ba05b", fontcolor=white]
        }
        subgraph cluster_t4 {
            label="Thread 2"
            style=filled
            fillcolor="#2a4a2a"
            color=white
            fontcolor=white
            S4 [label="Stack", fillcolor="#5ba05b", fontcolor=white]
        }
    }
}
----
```

- [ ] **Step 2: Build and verify**

```bash
mise run build
```

Expected: exits 0. Navigate to slides 5, 6, 7 in the browser. "Processes" and "Threads" should have clean bullet lists. The diagram slide should show two distinct process clusters (A and B), each containing a heap cylinder and two thread stacks. The two processes must have no connecting edges between them — they are isolated.

- [ ] **Step 3: Commit**

```bash
git add slides/src/main/slides/index.adoc
git commit -m "feat: add Beat 2 — OS Foundations slides and process/thread diagram"
```

---

### Task 4: Add Beat 3 — Java's Abstraction (2 slides)

**Files:**
- Modify: `slides/src/main/slides/index.adoc`

- [ ] **Step 1: Append Java abstraction slides**

Add the following to the end of `index.adoc`:

```asciidoc
== Java Threads

* `java.lang.Thread` — one class, one abstraction
* One Java thread = one OS thread (1:1 mapping on Linux)
* JVM handles the platform-specific details
* Same threading code runs on Linux, macOS, Windows

== What Java Gives Us

* Create and control threads
* Synchronize access to shared memory
* Coordinate between threads
* Inspect and monitor what is running
```

- [ ] **Step 2: Build and verify**

```bash
mise run build
```

Expected: exits 0. Slides 8 and 9 render with clean bullet lists. The backtick-formatted `java.lang.Thread` should appear in monospace on the slide.

- [ ] **Step 3: Commit**

```bash
git add slides/src/main/slides/index.adoc
git commit -m "feat: add Beat 3 — Java abstraction slides"
```

---

### Task 5: Add Agenda slide and final review

**Files:**
- Modify: `slides/src/main/slides/index.adoc`

- [ ] **Step 1: Append the closing Agenda slide**

Add the following to the end of `index.adoc`:

```asciidoc
== Today's Journey

* *Act 1 — Foundation:* what threads are and how to use them
* *Act 2 — Correctness:* what goes wrong and how to prevent it
* *Act 3 — Production:* patterns, tools, and how to investigate
```

- [ ] **Step 2: Build and do a full visual pass**

```bash
mise run build
```

Expected: exits 0. Open `build/slides/index.html` and walk through all 10 slides (title + 9 content slides) in order:

| # | Title | Check |
|---|-------|-------|
| 1 | Title slide | "Java Concurrency Primer" + background image |
| 2 | The Hardware Reality | 4 bullets |
| 3 | Concurrency is Everywhere | 4 bullets |
| 4 | CPU Architecture | Graphviz tree: CPU → Cores → HW Threads |
| 5 | Processes | 4 bullets |
| 6 | Threads | 4 bullets, `clone()` in monospace |
| 7 | Processes vs Threads | Graphviz: 2 process clusters with heaps and stacks |
| 8 | Java Threads | 4 bullets, `java.lang.Thread` in monospace |
| 9 | What Java Gives Us | 4 bullets |
| 10 | Today's Journey | 3 bullets in bold Act labels |

- [ ] **Step 3: Commit**

```bash
git add slides/src/main/slides/index.adoc
git commit -m "feat: add Agenda slide — intro section complete"
```
