# Java Concurrency Primer — Training Design

**Date:** 2026-05-23
**Format:** 4-6 hour workshop
**Audience:** First-year CS students
**Tool:** Asciidoctor RevealJS (`slides/src/main/slides/index.adoc`)

---

## Audience

- Know: Java syntax, OOP basics (classes, inheritance, interfaces), collections
- Don't know: generics, lambdas, functional interfaces
- Threading is brand new for all students
- Mixed skill level — slides must be accessible to beginners without boring stronger students

---

## Goals

Provide solid foundations for further self-learning. Students leave able to:
- Understand why and how concurrency works at the OS and JVM level
- Create and manage Java threads
- Recognize and fix common concurrency bugs
- Know what to look for when investigating thread issues in production

---

## Training Structure — 3 Acts

### Act 1 — Foundation (~90 min)
*What threads are and how to use them*

1. Intro/Theory (this spec)
2. Java Thread API — creating and managing threads
3. Thread lifecycle — start(), join(), interrupt()

### Act 2 — Correctness (~90 min)
*What goes wrong and how to prevent it*

4. Data races
5. synchronized
6. Conditional variables — wait/notify

### Act 3 — Production (~60 min)
*Patterns, tools, and investigation*

7. Daemon threads & thread priorities
8. Observability — jcmd and JFR
9. Concurrency patterns overview

Act 3 can be trimmed if time runs short. Daemon/priorities and patterns are the most compressible.

---

## Slide Conventions

- Simple bullet points, max 4-5 per slide
- No full sentences — short phrases only
- Code shown sparingly; live coding and demos fill the rest
- Diagrams (graphviz) used where a visual beats bullets
- No slides overloaded with text

---

## Intro Section — Detailed Slide Design

The intro section opens Act 1. Three beats, 8 slides total (~20-25 min).

### Beat 1 — Why Concurrency (2 slides + 1 diagram)

**Slide: "The Hardware Reality"**
- Modern CPUs: 4, 8, 16+ cores
- Each core executes instructions independently
- Hyper-threading: 2 hardware threads per physical core
- The hardware has been parallel for decades

**Slide: "Concurrency is Everywhere"**
- Web servers handle thousands of simultaneous requests
- UIs stay responsive while work runs in the background
- Databases serve multiple clients at once
- Writing sequential software ignores the machine under your feet

**Diagram (graphviz):** CPU die → cores → hardware threads

### Beat 2 — OS Foundations (3 slides)

**Slide: "Processes"**
- A running instance of a program
- Has its own isolated memory space
- Owns resources: files, sockets, handles
- Heavy to create, expensive to context-switch

**Slide: "Threads"**
- A unit of execution inside a process
- Shares memory with sibling threads
- In Linux: implemented as tasks via `clone()`
- Scheduled by the kernel preemptively

**Diagram (graphviz):** Two processes, each with multiple threads sharing a heap but with separate stacks

### Beat 3 — Java's Abstraction (2 slides)

**Slide: "Java Threads"**
- `java.lang.Thread` — one class, one abstraction
- One Java thread = one OS thread (1:1 mapping on Linux)
- JVM handles the platform-specific details
- Same threading code runs on Linux, macOS, Windows

**Slide: "What Java Gives Us"**
- Create and control threads
- Synchronize access to shared memory
- Coordinate between threads
- Inspect and monitor what's running

### Closing — Agenda (1 slide)

**Slide: "Today's Journey"**
- Act 1 — Foundation: what threads are and how to use them
- Act 2 — Correctness: what goes wrong and how to prevent it
- Act 3 — Production: patterns, tools, and how to investigate

---

## Expandable Areas (deferred)

These were noted as "good for now, can expand later":
- Individual slides within each beat can be split or deepened
- Graphviz diagrams are placeholders — exact layout TBD during implementation
- Acts 2 and 3 slide designs not yet specified
