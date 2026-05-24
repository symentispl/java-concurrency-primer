// Demonstrates lock contention difference between synchronized and ReentrantLock.
// Run: mise run java:exec -- ContentionDemo sync
//      mise run java:exec -- ContentionDemo lock
import java.util.concurrent.atomic.*;
import java.util.concurrent.locks.*;

class ContentionDemo {

    interface Buffer {
        void put(int item) throws InterruptedException;
        int take() throws InterruptedException;
    }

    static class SyncBuffer implements Buffer {
        private final int[] data;
        private int head, tail, count;

        SyncBuffer(int capacity) { data = new int[capacity]; }

        public synchronized void put(int item) throws InterruptedException {
            while (count == data.length) wait();
            data[head] = item;
            head = (head + 1) % data.length;
            count++;
            notifyAll();
        }

        public synchronized int take() throws InterruptedException {
            while (count == 0) wait();
            int item = data[tail];
            tail = (tail + 1) % data.length;
            count--;
            notifyAll();
            return item;
        }
    }

    static class LockBuffer implements Buffer {
        private final int[] data;
        private int head, tail, count;
        private final Lock lock = new ReentrantLock();
        private final Condition notFull  = lock.newCondition();
        private final Condition notEmpty = lock.newCondition();

        LockBuffer(int capacity) { data = new int[capacity]; }

        public void put(int item) throws InterruptedException {
            lock.lock();
            try {
                while (count == data.length) notFull.await();
                data[head] = item;
                head = (head + 1) % data.length;
                count++;
                notEmpty.signal();
            } finally { lock.unlock(); }
        }

        public int take() throws InterruptedException {
            lock.lock();
            try {
                while (count == 0) notEmpty.await();
                int item = data[tail];
                tail = (tail + 1) % data.length;
                count--;
                notFull.signal();
                return item;
            } finally { lock.unlock(); }
        }
    }

    static void run(Buffer buf, int total, int producers, int consumers)
            throws InterruptedException {
        var threads = new Thread[producers + consumers];
        int perProducer = total / producers;
        var consumed = new AtomicInteger(0); // shared counter — consumers race to claim items

        for (int i = 0; i < producers; i++) {
            int id = i;
            threads[i] = new Thread(() -> {
                try {
                    for (int j = id * perProducer; j < (id + 1) * perProducer; j++)
                        buf.put(j);
                } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            });
        }
        for (int i = 0; i < consumers; i++) {
            threads[producers + i] = new Thread(() -> {
                try {
                    while (consumed.getAndIncrement() < total) // atomically claim one item
                        buf.take();
                } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            });
        }
        for (var t : threads) t.start();
        for (var t : threads) t.join();
    }

    void main(String[] args) throws InterruptedException {
        String mode = (args.length > 0) ? args[0] : "both";
        int total = 50_000, producers = 4, consumers = 4;

        if (mode.equals("sync") || mode.equals("both")) {
            long start = System.currentTimeMillis();
            run(new SyncBuffer(2), total, producers, consumers);
            System.out.println("sync  | items: " + total + " | time: "
                    + (System.currentTimeMillis() - start) + "ms");
        }
        if (mode.equals("lock") || mode.equals("both")) {
            long start = System.currentTimeMillis();
            run(new LockBuffer(2), total, producers, consumers);
            System.out.println("lock  | items: " + total + " | time: "
                    + (System.currentTimeMillis() - start) + "ms");
        }
    }
}
