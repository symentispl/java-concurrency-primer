// Demonstrates ReentrantLock + two Conditions replacing wait()/notifyAll().
// Run: mise run java:exec -- BoundedBufferLock
import java.util.concurrent.locks.*;

class BoundedBufferLock {

    static class Buffer {
        private final int[] data;
        private int head, tail, count;
        private final Lock lock = new ReentrantLock();
        private final Condition notFull  = lock.newCondition();
        private final Condition notEmpty = lock.newCondition();

        Buffer(int capacity) { data = new int[capacity]; }

        void put(int item) throws InterruptedException {
            lock.lock();
            try {
                while (count == data.length) notFull.await();
                data[head] = item;
                head = (head + 1) % data.length;
                count++;
                notEmpty.signal();
            } finally { lock.unlock(); }
        }

        int take() throws InterruptedException {
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

    void main() throws InterruptedException {
        var buf = new Buffer(4);
        int total = 198;
        var threads = new Thread[6];

        for (int i = 0; i < 3; i++) {
            int id = i;
            threads[i] = new Thread(() -> {
                try {
                    for (int j = id * (total / 3); j < (id + 1) * (total / 3); j++)
                        buf.put(j);
                } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            });
        }
        for (int i = 0; i < 3; i++) {
            threads[3 + i] = new Thread(() -> {
                try {
                    for (int j = 0; j < total / 3; j++)
                        System.out.println("took: " + buf.take());
                } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            });
        }

        for (var t : threads) t.start();
        for (var t : threads) t.join();
        System.out.println("Done.");
    }
}
