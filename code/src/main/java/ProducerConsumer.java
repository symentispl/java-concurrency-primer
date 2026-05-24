// Demonstrates wait()/notify() with a bounded buffer.
// Run: mise run java:exec -- ProducerConsumer
class ProducerConsumer {

    static class BoundedBuffer {
        private final int[] buffer;
        private int head = 0;
        private int tail = 0;
        private int count = 0;

        BoundedBuffer(int capacity) {
            buffer = new int[capacity];
        }

        synchronized void put(int item) throws InterruptedException {
            while (count == buffer.length) {
                wait(); // buffer full — release lock and sleep
            }
            buffer[head] = item;
            head = (head + 1) % buffer.length;
            count++;
            notifyAll(); // wake consumers
        }

        synchronized int take() throws InterruptedException {
            while (count == 0) {
                wait(); // buffer empty — release lock and sleep
            }
            int item = buffer[tail];
            tail = (tail + 1) % buffer.length;
            count--;
            notifyAll(); // wake producers
            return item;
        }
    }

    static class Producer implements Runnable {
        private final BoundedBuffer buffer;

        Producer(BoundedBuffer buffer) {
            this.buffer = buffer;
        }

        public void run() {
            for (int i = 0; i < 10; i++) {
                try {
                    buffer.put(i + 1);
                    System.out.println("Produced: " + (i + 1)); // printed after lock released — consumer may print first
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }

    static class Consumer implements Runnable {
        private final BoundedBuffer buffer;

        Consumer(BoundedBuffer buffer) {
            this.buffer = buffer;
        }

        public void run() {
            for (int i = 0; i < 10; i++) {
                try {
                    int item = buffer.take();
                    System.out.println("Consumed: " + item);
                    Thread.sleep(80);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }

    void main() throws InterruptedException {
        var buffer = new BoundedBuffer(5);
        var producer = new Thread(new Producer(buffer));
        var consumer = new Thread(new Consumer(buffer));
        producer.start();
        consumer.start();
        producer.join();
        consumer.join();
        System.out.println("Done.");
    }
}
