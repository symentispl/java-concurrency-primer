// Demonstrates deadlock prevention: consistent lock ordering.
// Run: mise run java:exec -- DeadlockFixed
class DeadlockFixed {

    static final Object lockA = new Object();
    static final Object lockB = new Object();

    static class ThreadA implements Runnable {
        public void run() {
            synchronized (lockA) {
                try { Thread.sleep(50); } catch (InterruptedException e) { return; }
                synchronized (lockB) {
                    System.out.println("Thread A: done");
                }
            }
        }
    }

    static class ThreadB implements Runnable {
        public void run() {
            synchronized (lockA) {  // same order as Thread A — no cycle possible
                try { Thread.sleep(50); } catch (InterruptedException e) { return; }
                synchronized (lockB) {
                    System.out.println("Thread B: done");
                }
            }
        }
    }

    void main() throws InterruptedException {
        var a = new Thread(new ThreadA());
        var b = new Thread(new ThreadB());
        System.out.println("Starting");
        a.start();
        b.start();
        a.join();
        b.join();
        System.out.println("Done.");
    }
}
