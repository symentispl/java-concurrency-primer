// Demonstrates livelock: two workers keep backing off and make no progress.
// Run: mise run java:exec -- Livelock
//
// Two workers need both resources to do their work.
// Each acquires its first resource then politely yields when it finds
// the second resource taken — only to collide again after backing off.
// Workers are busy (not blocked), but neither makes progress: livelock.
class Livelock {

    static class Resource {
        private boolean inUse = false;

        synchronized boolean tryAcquire() {
            if (inUse) return false;
            inUse = true;
            return true;
        }

        synchronized void release() { inUse = false; }
    }

    // Reusable two-party barrier implemented with wait/notifyAll.
    // broken flag allows one party to signal exit so the other is not stuck.
    static class Barrier {
        private int waiting = 0;
        private int generation = 0;
        private boolean broken = false;

        // Returns true if barrier completed normally, false if broken.
        synchronized boolean await() throws InterruptedException {
            if (broken) return false;
            int gen = generation;
            waiting++;
            if (waiting == 2) {
                waiting = 0;
                generation++;
                notifyAll();
                return true;
            }
            while (gen == generation && !broken) wait();
            return !broken;
        }

        synchronized void breakNow() {
            broken = true;
            notifyAll();
        }
    }

    static class Worker implements Runnable {
        private final String name;
        private final Resource first;
        private final Resource second;
        private final Barrier acquireBarrier;  // both hold first resource before trying second
        private final Barrier releaseBarrier;  // both observed failure before either releases first

        Worker(String name, Resource first, Resource second,
               Barrier acquireBarrier, Barrier releaseBarrier) {
            this.name  = name;
            this.first = first;
            this.second = second;
            this.acquireBarrier = acquireBarrier;
            this.releaseBarrier = releaseBarrier;
        }

        public void run() {
            int attempts = 0;
            while (attempts < 20) {
                // Step 1: acquire first resource — spin-wait
                while (!first.tryAcquire()) {
                    try { Thread.sleep(1); } catch (InterruptedException e) { return; }
                }

                // Step 2: rendezvous — wait until both workers hold their first resource.
                // Guarantees: when we try second, the other worker still holds it.
                try {
                    if (!acquireBarrier.await()) { first.release(); return; }
                } catch (InterruptedException e) { first.release(); acquireBarrier.breakNow(); return; }

                // Step 3: try second resource — the other worker holds it right now.
                boolean gotSecond = second.tryAcquire();

                // Step 4: rendezvous — wait until both workers have tried second.
                // Guarantees: neither releases first until the other has observed the conflict.
                try {
                    if (!releaseBarrier.await()) {
                        if (gotSecond) { second.release(); }
                        first.release();
                        return;
                    }
                } catch (InterruptedException e) {
                    if (gotSecond) { second.release(); }
                    first.release();
                    releaseBarrier.breakNow();
                    return;
                }

                if (!gotSecond) {
                    // Livelock: politely release first and back off
                    first.release();
                    System.out.println(name + ": backing off (attempt " + (++attempts) + ")");
                    try { Thread.sleep(5); } catch (InterruptedException e) { return; }
                } else {
                    // Should not happen in a livelock, but handle gracefully
                    second.release();
                    first.release();
                    System.out.println(name + ": done (livelock broken by scheduler)");
                    acquireBarrier.breakNow();
                    releaseBarrier.breakNow();
                    return;
                }
            }
            System.out.println(name + ": gave up after " + attempts + " attempts");
            acquireBarrier.breakNow();
            releaseBarrier.breakNow();
        }
    }

    void main() throws InterruptedException {
        var r1 = new Resource();
        var r2 = new Resource();
        var acquireBarrier = new Barrier();
        var releaseBarrier = new Barrier();
        // Worker A wants r1 then r2; Worker B wants r2 then r1
        // Each acquires its first, finds the second taken, backs off — in sync
        var a = new Thread(new Worker("Worker A", r1, r2, acquireBarrier, releaseBarrier));
        var b = new Thread(new Worker("Worker B", r2, r1, acquireBarrier, releaseBarrier));
        a.start();
        b.start();
        a.join();
        b.join();
        System.out.println("Done — neither worker made progress.");
    }
}
