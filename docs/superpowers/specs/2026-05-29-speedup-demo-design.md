# Speedup Demo — Design Spec

**Date:** 2026-05-29  
**Context:** Java Concurrency Primer training slides + companion code

---

## Goal

Add a compelling, runnable demonstration that shows why concurrency matters: the same computation runs dramatically faster with multiple threads, but scaling is not infinite. The demo backs two new sub-slides inserted under the existing "Why you should care?" section.

---

## Slide Changes

Insert two new sub-slides between "The Hardware Reality" and "CPU Architecture" inside the `== Why you should care?` section.

### Slide 1: `=== the hardware reward`

Shows a pre-run output table (1 thread → `availableProcessors()` threads). Key bullets:

- Speedup tracks thread count on CPU-bound work
- The hardware has 16 cores; ignoring it leaves most of the machine idle
- This is free performance — it just requires understanding concurrency

Example expected output on an 8-core machine:

```
threads    time(ms)    speedup
      1        1823       1.0x
      2         934       1.9x
      4         479       3.8x
      8         251       7.3x
```

Run instruction: `java code/src/main/java/SpeedupDemo.java`

### Slide 2: `=== scaling has limits`

Continues the table past core count. Introduces two laws:

**Amdahl's Law**  
`S(n) = 1 / (1 − p + p/n)`  
Even 5% serial code caps maximum speedup at 20× regardless of core count. The serial fraction — setup, coordination, I/O — dominates at scale.

**Universal Scalability Law** (Neil Gunther)  
`C(n) = n / (1 + α(n−1) + βn(n−1))`  
Extends Amdahl with a coherency penalty term `β`. At high thread counts, coordination overhead causes throughput to *decrease*. This is why thread pools have a size limit.

Example expected output continued (same machine):

```
     16         265       6.9x   ← beyond core count
     32         290       6.3x   ← worse, not better
```

Key message: more threads is not free. Every thread pool size limit exists because of the USL.

---

## Code: `SpeedupDemo.java`

**Location:** `code/src/main/java/SpeedupDemo.java`

**Computation:** Sum of `Math.sqrt(x)` for every element in a 100-million-element `double[]`. CPU-bound, embarrassingly parallel, no shared state during computation.

### Structure

```
class SpeedupDemo {
    static final int SIZE = 100_000_000;
    static double[] data;               // pre-filled once, shared read-only

    // initialise: fill data[i] = i + 1.0

    // sequential(int threads): single loop over full array
    // parallel(int threads):   split into `threads` equal chunks,
    //                          one Thread per chunk, join all,
    //                          sum partial results in main thread

    void main():
        - fill array
        - print header
        - for each thread count in: powers of 2 up to availableProcessors()
          (plus availableProcessors() itself if not a power of 2),
          then availableProcessors()*2, availableProcessors()*4
          (deduplicated, in ascending order):
            - warm up once (discard result)
            - measure 3 runs, take median
            - print row: threads | time_ms | speedup_vs_1_thread
            - mark row if threads > availableProcessors()
}
```

### Threading model

Raw `Thread` objects with manual chunking — consistent with course style, no ForkJoin or streams. Each thread receives start/end indices and a `double[]` result slot. Main thread sums the slots after `join()`.

Chunk boundaries: `start = chunk * (SIZE / threads)`, `end = (chunk == threads-1) ? SIZE : start + (SIZE / threads)`. Last chunk absorbs any remainder from integer division.

### Output format

```
Array size: 100,000,000 doubles
Available processors: 8

threads    time(ms)    speedup
      1        1823       1.0x
      2         934       1.9x
      4         479       3.8x
      8         251       7.3x
     16 *       265       6.9x   (* beyond core count)
     32 *       290       6.3x
```

### Measurement

- Three timed runs per thread count, median taken
- One warm-up run before measurement (JIT stabilisation)
- `System.nanoTime()` for timing

---

## Constraints

- No ForkJoin, no parallel streams — raw threads only, consistent with course style
- Java 21+ instance-main pattern (`void main()`)
- No external dependencies
- Must complete in under 20 seconds total (for live demo use)
- Array pre-filled once and reused across all thread counts (fair comparison)
