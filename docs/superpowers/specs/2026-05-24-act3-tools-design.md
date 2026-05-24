# Act 3 — Investigative Tools Design Spec

**Date:** 2026-05-24
**Section:** Act 3 — Production (tools sub-section)
**Follows:** "Lock Implementations" (current slide 58 / last slide in deck)

---

## Goal

Introduce students to the tools used to investigate and diagnose Java multithreaded programs in production. Students have completed Act 1 (threads, lifecycle, interruption) and Act 2 (correctness — synchronized, Lock/Condition). This section closes the loop: they now know what can go wrong and how to find it when it does.

---

## Audience Assumptions

- Know: Java syntax, OOP, threads, synchronized, Lock/Condition, deadlock, livelock, starvation
- Don't know: JVM diagnostic tools, thread dumps, JFR, profiling, Linux process/thread inspection
- Context: first-year CS students; some may have used `top` or `ps`, most have not used JFR or JMC

---

## Narrative Arc

Two problem scenarios drive the section. Each scenario introduces the right class of tool for that problem.

**Scenario 1 — "Your app is hung"**: JVM is running, threads are alive, nothing is making progress. Tool: `jcmd` thread dump.

**Scenario 2 — "Your app is slow"**: Throughput is lower than expected, latency is high. Tool: JFR + JDK Mission Control.

Linux TSA tools appear at the end as "going deeper" — for when JVM-level tools say threads are `RUNNABLE` but CPU is still suspiciously low.

---

## Slides — 10 total

Final slide count: **68** (up from 58).

All slides appended after "Lock Implementations" (current last slide).

---

### Slide 1: "When Your App Hangs"

Frame the problem. JVM process is running, nothing is making progress, logs have stopped. Students need a mental model for what to do first.

Key points:
- JVM is alive but threads are stuck — this is not a crash, it is a hang
- A thread dump shows the state of every thread at one moment in time
- Reading a thread dump is the first skill for any Java developer dealing with production incidents
- Tool: `jcmd`

---

### Slide 2: "jcmd — Thread Dumps"

`jcmd` is the modern JVM diagnostic command (ships with the JDK, replaces `jstack`).

Key points:
- `jcmd` — list all JVM processes with their PIDs
- `jcmd <pid> Thread.print` — print a full thread dump to stdout
- `jcmd <pid> Thread.print -l` — include lock information
- `jcmd <pid> help` — list all available commands

```
jcmd                            # list JVM processes
jcmd <pid> Thread.print         # thread dump
jcmd <pid> Thread.print -l      # with lock info
```

---

### Slide 3: "Reading a Thread Dump"

Thread states and what each means. Students need to know what to look for before they see the deadlock example.

Key points:
- `RUNNABLE` — actively executing or ready to run
- `BLOCKED` — waiting to acquire a monitor (`synchronized` lock)
- `WAITING` — parked indefinitely (`Object.wait()`, `LockSupport.park()`)
- `TIMED_WAITING` — parked with a timeout (`Thread.sleep()`, `wait(timeout)`)
- The key signal: multiple threads `BLOCKED` on the same monitor object address

```
"Thread-0" #13 prio=5 os_prio=0 cpu=1.23ms elapsed=2.01s tid=0x... nid=0x...
   java.lang.Thread.State: BLOCKED (on object monitor)
    - waiting to lock <0x000...> (a java.lang.Object)
    - locked <0x000...> (a java.lang.Object)
```

---

### Slide 4: "Deadlock in a Thread Dump"

Live demo slide. Instructor runs `Deadlock.java` (already in the repo — it hangs by design), then runs `jcmd` against it.

Key points:
- `jcmd` detects deadlocks automatically: prints "Found one Java-level deadlock:"
- Shows both threads: what each holds, what each is waiting for
- This is the exact pattern from the Act 2 "Deadlock" slides — now they can see it in a real tool

Live demo steps:
1. `mise run java:exec -- Deadlock` in one terminal (hangs)
2. `jcmd` in another terminal to get the PID
3. `jcmd <pid> Thread.print -l`
4. Point out the "Found one Java-level deadlock:" section

---

### Slide 5: "Thread States — Cheat Sheet"

Quick reference. One slide students can photograph and keep.

| State | Meaning | Common cause |
|---|---|---|
| `RUNNABLE` | Running or ready to run | Normal execution, tight loop, I/O |
| `BLOCKED` | Waiting for a `synchronized` lock | Contention on monitor |
| `WAITING` | Parked indefinitely | `wait()`, `park()`, `join()` |
| `TIMED_WAITING` | Parked with timeout | `sleep()`, `wait(n)`, `parkNanos()` |

Rule of thumb: many threads `BLOCKED` = lock contention. Many threads `WAITING` = something upstream is slow or stuck.

---

### Slide 6: "When Your App Is Slow"

Frame the second scenario. The app is running but throughput is lower than expected or latency is higher than it should be. A thread dump shows a snapshot — it doesn't tell you where time is being spent over a period. You need a profiler.

Key points:
- A thread dump is a snapshot — useful for hangs, not for performance
- Profiling records what threads are doing over time
- Java Flight Recorder (JFR): built into the JDK, low overhead (~1-2%), safe to use in production
- JDK Mission Control (JMC): GUI for analyzing JFR recordings

---

### Slide 7: "Java Flight Recorder"

How to start, dump, and stop a JFR recording using `jcmd`.

Key points:
- JFR is built into the JDK (Java 11+, backported to 8u262)
- Continuously records JVM events: thread states, GC, I/O, locks, method samples
- `settings=profile` — higher-frequency sampling (suitable for short recordings)
- Output: `.jfr` file, opened in JDK Mission Control

```
# start recording
jcmd <pid> JFR.start name=demo settings=profile

# dump to file (keeps recording running)
jcmd <pid> JFR.dump name=demo filename=demo.jfr

# stop and dump
jcmd <pid> JFR.stop name=demo filename=demo.jfr

# or: start at JVM launch
java -XX:StartFlightRecording=duration=60s,filename=demo.jfr MyApp
```

---

### Slide 8: "JMC — Three Views That Matter"

What to look for in JDK Mission Control after opening a `.jfr` file. Live demo slide — instructor opens the recording in JMC.

Key points:
- **Thread sampling** (`Flame Graph` / `Method Profiling`): where CPU time goes — which methods are hot
- **Lock contention** (`Java Monitors & Locks`): which locks threads spend the most time waiting to acquire, how long on average
- **Thread states over time** (`Threads` view): timeline of each thread's state — see when threads go from `RUNNABLE` to `BLOCKED`

Live demo steps:
1. Open `demo.jfr` in JMC
2. Navigate to `Java Monitors & Locks` — show the contention table
3. Navigate to `Flame Graph` — show hot methods
4. Navigate to `Threads` — show state timeline

---

### Slide 9: "BoundedBuffer Contention Demo"

Live demo comparing `synchronized` vs `ReentrantLock` under high contention using JFR.

New code: `ContentionDemo.java` — high-load producer/consumer (8 threads, buffer capacity 2, 10 000 items). Two modes controlled by a command-line argument:
- `mise run java:exec -- ContentionDemo sync` — uses the synchronized BoundedBuffer
- `mise run java:exec -- ContentionDemo lock` — uses the ReentrantLock BoundedBuffer

Key points:
- Record JFR for each run; open both in JMC
- `synchronized` view: one contended monitor object (`BoundedBuffer` instance)
- `ReentrantLock` view: `AbstractQueuedSynchronizer` shows up; contention often lower because `ReentrantLock` uses a queue-based handoff
- Teaching point: the tool makes the difference between the two implementations visible — not just theoretical

Live demo steps:
1. `mise run java:exec -- ContentionDemo sync` (with JFR)
2. `mise run java:exec -- ContentionDemo lock` (with JFR)
3. Open both `.jfr` files in JMC, compare the `Java Monitors & Locks` view

---

### Slide 10: "Going Deeper — Linux TSA Tools"

Thread State Analysis (TSA) methodology: analyze thread states at the OS level when JVM tools are inconclusive.

When to use: JVM tools say threads are `RUNNABLE` but overall CPU utilization is still lower than expected — the OS scheduler or I/O may be the bottleneck.

Key tools:
- `top -H -p <pid>` — per-thread CPU usage; shows native thread IDs (match to thread dump)
- `ps -eLf | grep <pid>` — list all LWPs (light-weight processes = OS threads) with states
- `pidstat -t -p <pid> 1` — per-thread CPU/context-switch stats, sampled every 1 second
- Native thread ID in `jcmd` output (`nid=0x...`) maps to `ps`/`top` TID (convert hex → decimal)

TSA process: thread dump → identify suspect threads → map `nid` to OS TID → `top -H` to see CPU → `pidstat` for context-switch rate.

---

## New Code File

### ContentionDemo.java

`code/src/main/java/ContentionDemo.java`

High-load producer/consumer designed to produce visible lock contention in JFR.

- 8 threads total: 4 producers, 4 consumers
- Buffer capacity: 2 (maximizes contention)
- Items: 10 000
- Runs ~3-5 seconds to give JFR enough data
- Mode selected by first command-line argument: `sync` or `lock`
- `sync` mode: uses `BoundedBuffer` from `ProducerConsumer.java` (copy of the inner class, standalone)
- `lock` mode: uses `Buffer` from `BoundedBufferLock.java` (copy of the inner class, standalone)
- Prints total time so students can compare throughput as well as contention

Run:
```
mise run java:exec -- ContentionDemo sync
mise run java:exec -- ContentionDemo lock
```

---

## Slide Placement

All 10 slides appended after "Lock Implementations" (current slide 58).

| Slide | Title |
|-------|-------|
| 59 | When Your App Hangs |
| 60 | jcmd — Thread Dumps |
| 61 | Reading a Thread Dump |
| 62 | Deadlock in a Thread Dump |
| 63 | Thread States — Cheat Sheet |
| 64 | When Your App Is Slow |
| 65 | Java Flight Recorder |
| 66 | JMC — Three Views That Matter |
| 67 | BoundedBuffer Contention Demo |
| 68 | Going Deeper — Linux TSA Tools |

Final slide count: **68**

---

## Out of Scope

- VisualVM / JConsole
- `jstack` (replaced by `jcmd`)
- async-profiler (mentioned in "Going Deeper" context only if time permits)
- GC profiling (separate topic)
- `java.lang.management` MXBeans
- JFR custom events
- Continuous monitoring / APM tools (Datadog, New Relic, etc.)
