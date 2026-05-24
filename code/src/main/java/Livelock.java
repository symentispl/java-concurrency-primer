// Demonstrates livelock: two workers keep backing off and make no progress.
// Run: mise run java:exec -- Livelock
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

    static class Worker implements Runnable {
        private final String name;
        private final Resource first;
        private final Resource second;

        Worker(String name, Resource first, Resource second) {
            this.name = name;
            this.first = first;
            this.second = second;
        }

        public void run() {
            int attempts = 0;
            while (attempts < 20) {
                if (!first.tryAcquire()) {
                    try { Thread.sleep(10); } catch (InterruptedException e) { return; }
                    continue;
                }
                // brief pause — let the other worker grab its first resource too
                try { Thread.sleep(5); } catch (InterruptedException e) { return; }
                if (!second.tryAcquire()) {
                    first.release(); // be polite — release and back off
                    System.out.println(name + ": backing off (attempt " + (++attempts) + ")");
                    try { Thread.sleep(10); } catch (InterruptedException e) { return; }
                    continue;
                }
                // got both resources
                second.release();
                first.release();
                System.out.println(name + ": done");
                return;
            }
            System.out.println(name + ": gave up after " + attempts + " attempts");
        }
    }

    void main() throws InterruptedException {
        var r1 = new Resource();
        var r2 = new Resource();
        // Worker A wants r1 then r2; Worker B wants r2 then r1
        // Each acquires its first, finds the second taken, backs off — in sync
        var a = new Thread(new Worker("Worker A", r1, r2));
        var b = new Thread(new Worker("Worker B", r2, r1));
        a.start();
        b.start();
        a.join();
        b.join();
        System.out.println("Done — neither worker made progress.");
    }
}
