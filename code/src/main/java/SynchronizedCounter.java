// Demonstrates synchronized fixing the data race.
// Run: mise run java:exec -- SynchronizedCounter
class SynchronizedCounter {

    static int counter = 0;
    static final Object lock = new Object();

    static class Incrementer implements Runnable {
        public void run() {
            for (int i = 0; i < 10_000; i++) {
                synchronized (lock) {
                    counter++; // protected: only one thread at a time
                }
            }
        }
    }

    void main() throws InterruptedException {
        var t1 = new Thread(new Incrementer());
        var t2 = new Thread(new Incrementer());
        t1.start();
        t2.start();
        t1.join();
        t2.join();
        System.out.println("Expected: 20000");
        System.out.println("Actual:   " + counter);
    }
}
