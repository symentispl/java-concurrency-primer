class ReadySignal {
    private boolean ready = false;

    synchronized void signal() {
        ready = true;
        notifyAll();
    }

    synchronized void await() throws InterruptedException {
        while (!ready) wait();
    }

    void main() throws InterruptedException {
        var signal = new ReadySignal();

        var waiter = new Thread(() -> {
            try {
                System.out.println("Waiter: parking until signal...");
                signal.await();
                System.out.println("Waiter: received — proceeding");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        waiter.start();
        Thread.sleep(500);
        System.out.println("Main:   firing signal");
        signal.signal();
        waiter.join();
        System.out.println("Main:   waiter finished");
    }
}
