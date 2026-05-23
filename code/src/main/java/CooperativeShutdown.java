// Demonstrates cooperative thread shutdown using interrupt() and Thread.interrupted().
// Run: mise run java:exec -- CooperativeShutdown
class CooperativeShutdown {

    static class Worker implements Runnable {

        public void run() {
            System.out.println("Worker: started");
            while (!Thread.interrupted()) {
                doWork();
            }
            System.out.println("Worker: flag seen, stopping cleanly");
            Thread.currentThread().interrupt(); // restore flag for caller
        }

        private void doWork() {
            try {
                Thread.sleep(100);
                System.out.println("Worker: item processed");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); // restore so outer loop exits
            }
        }
    }

    void main() throws InterruptedException {
        var worker = new Thread(new Worker());
        worker.start();

        Thread.sleep(350); // let worker process a few items

        System.out.println("Main:   requesting shutdown");
        worker.interrupt();
        worker.join();     // wait for clean exit
        System.out.println("Main:   worker stopped cleanly");
    }
}
