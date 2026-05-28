// Exercise 3: Simple Barrier (Thread Rendezvous)
//
// TOPIC: wait() and notifyAll() — count-based condition
//
// A barrier makes N threads synchronize at a common checkpoint:
// no thread may proceed past it until ALL N have arrived.
//
//   Thread 0 (slow):  ─────────────────────────■ arrives ──────────► phase 2
//   Thread 1 (fast):  ──────────■ arrives, WAITS ──────────────────► phase 2
//   Thread 2 (fast):  ──────■ arrives, WAITS ──────────────────────► phase 2
//                                               ↑ last thread calls notifyAll()
//
// IMPLEMENTATION SKETCH
//
//   synchronized void arrive():
//     count++;
//     if (count == parties) {          // I'm the last thread
//         count = 0;                   // reset for potential reuse
//         notifyAll();                 // release all waiters
//     } else {
//         while (count > 0) wait();    // wait until the last thread resets count
//     }
//
// YOUR TASK
// ---------
// Implement arrive() in SimpleBarrier following the sketch above.
//
// Run:  mise run java:exec -- exercises/Exercise6Barrier
// Pass: PASS — barrier held: all phase-1 work finished before any phase-2 started

import java.util.Arrays;

class Exercise6Barrier {

    static class SimpleBarrier {
        private final int parties;
        private int count = 0;

        SimpleBarrier(int parties) { this.parties = parties; }

        synchronized void arrive() throws InterruptedException {
            // TODO: increment count — one more thread has arrived

            // TODO: if this is the last thread (count == parties):
            //         reset count to 0, then call notifyAll()
            //       otherwise:
            //         wait until the last thread resets count: while (count > 0) { wait(); }
        }
    }

    void main(String[] args) throws InterruptedException {
        int n = 4;
        SimpleBarrier barrier = new SimpleBarrier(n);

        long[] phase1Done  = new long[n]; // when each thread completes phase 1
        long[] phase2Start = new long[n]; // when each thread starts phase 2

        Thread[] threads = new Thread[n];
        long start = System.currentTimeMillis();

        for (int i = 0; i < n; i++) {
            int id = i;
            threads[i] = new Thread(() -> {
                try {
                    // Phase 1: staggered work — thread 0 takes 10ms, thread 3 takes 40ms
                    Thread.sleep(10L * (id + 1));
                    phase1Done[id] = System.currentTimeMillis() - start;

                    barrier.arrive(); // ← all threads rendezvous here

                    phase2Start[id] = System.currentTimeMillis() - start;
                } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            });
        }

        for (Thread t : threads) t.start();
        for (Thread t : threads) t.join(5_000); // 5-second safety net

        boolean anyHung = false;
        for (Thread t : threads) {
            if (t.isAlive()) { anyHung = true; t.interrupt(); }
        }

        if (anyHung) {
            System.out.println("FAIL — thread(s) still blocked after 5 s "
                + "(missing notifyAll() or wait() in arrive()?)");
            return;
        }

        long lastPhase1  = Arrays.stream(phase1Done).max().getAsLong();
        long firstPhase2 = Arrays.stream(phase2Start).min().getAsLong();

        System.out.println("Phase 1 done  (ms each thread): " + Arrays.toString(phase1Done));
        System.out.println("Phase 2 start (ms each thread): " + Arrays.toString(phase2Start));
        System.out.printf( "Last phase-1 finished: %d ms  |  First phase-2 started: %d ms%n",
                           lastPhase1, firstPhase2);

        if (firstPhase2 >= lastPhase1) {
            System.out.println("PASS — barrier held: all phase-1 work finished before any phase-2 started");
        } else {
            System.out.println("FAIL — barrier broken: a thread entered phase 2 at " + firstPhase2
                + " ms before the slowest thread finished phase 1 at " + lastPhase1 + " ms");
        }
    }
}
