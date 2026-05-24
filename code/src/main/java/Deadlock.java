// Demonstrates deadlock: two threads acquire two locks in opposite order.
// Run: mise run java:exec -- Deadlock
// WARNING: this program hangs — kill with Ctrl+C
class Deadlock {

    static final Object lockA = new Object();
    static final Object lockB = new Object();

    static class ThreadA implements Runnable {
        public void run() {
            synchronized (lockA) {
                System.out.println("Thread A: holds lockA, waiting for lockB");
                try { Thread.sleep(50); } catch (InterruptedException e) { return; }
                synchronized (lockB) {
                    System.out.println("Thread A: done");
                }
            }
        }
    }

    static class ThreadB implements Runnable {
        public void run() {
            synchronized (lockB) {
                System.out.println("Thread B: holds lockB, waiting for lockA");
                try { Thread.sleep(50); } catch (InterruptedException e) { return; }
                synchronized (lockA) {
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
