// Demonstrates a data race on a shared counter.
// Run: mise run java:exec -- DataRace
class DataRace {

    static int counter = 0;

    static class Incrementer implements Runnable {
        public void run() {
            for (int i = 0; i < 10_000; i++) {
                counter++; // not atomic: load, add, store
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
