// Exercise 2: Bounded Blocking Stack
//
// TOPIC: wait() and notifyAll() — blocking on a condition
//
// A BlockingStack stores items in a fixed-size array (LIFO: last-in, first-out).
//   push(item) must BLOCK when the stack is full.
//   pop()      must BLOCK when the stack is empty.
//
// The standard pattern — same as with any bounded buffer:
//
//   push:                              pop:
//     while (top == capacity) wait();    while (top == 0) wait();
//     data[top++] = item;               int item = data[--top];
//     notifyAll();                       notifyAll();
//                                        return item;
//
// KEY RULES
//   - Always while (not if) before wait() — guards against spurious wakeups
//   - wait() and notifyAll() are called on 'this' (both methods are synchronized)
//   - Let InterruptedException propagate — do not catch it here
//
// YOUR TASK
// ---------
// Implement push() and pop() in BlockingStack following the pattern above.
//
// Run:  mise run java:exec -- exercises/Exercise3BlockingStack
// Pass: PASS — all 1000 items pushed and popped correctly

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

class Exercise3BlockingStack {

    static class BlockingStack {
        private final int[] data;
        private int top = 0; // top == 0 → empty; top == data.length → full

        BlockingStack(int capacity) { data = new int[capacity]; }

        synchronized void push(int item) throws InterruptedException {
            // TODO: wait while the stack is full
            //         while (top == data.length) { wait(); }

            // TODO: store item and advance top
            //         data[top++] = item;

            // TODO: wake any threads blocked in pop()
            //         notifyAll();
        }

        synchronized int pop() throws InterruptedException {
            // TODO: wait while the stack is empty
            //         while (top == 0) { wait(); }

            // TODO: retrieve the top item and shrink the stack
            //         int item = data[--top];

            // TODO: wake any threads blocked in push()
            //         notifyAll();

            // TODO: return the retrieved item
            return -1; // placeholder — remove once implemented
        }
    }

    void main(String[] args) throws InterruptedException {
        int total    = 1000;
        int capacity = 5;

        BlockingStack stack      = new BlockingStack(capacity);
        long expectedSum         = (long) total * (total - 1) / 2; // sum of 0..999 = 499500
        AtomicLong actualSum     = new AtomicLong(0);
        AtomicInteger remaining  = new AtomicInteger(total);

        Thread[] threads = new Thread[4];

        // 2 producers — push items 0..499 and 500..999
        for (int p = 0; p < 2; p++) {
            int base = p * (total / 2);
            threads[p] = new Thread(() -> {
                try {
                    for (int i = base; i < base + total / 2; i++)
                        stack.push(i);
                } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            });
        }

        // 2 consumers — pop until all 1000 items have been claimed
        for (int c = 0; c < 2; c++) {
            threads[2 + c] = new Thread(() -> {
                try {
                    while (remaining.getAndDecrement() > 0)
                        actualSum.addAndGet(stack.pop());
                } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            });
        }

        for (Thread t : threads) t.start();
        for (Thread t : threads) t.join(5_000); // 5-second safety net

        boolean anyHung = false;
        for (Thread t : threads) {
            if (t.isAlive()) { anyHung = true; t.interrupt(); }
        }

        if (anyHung) {
            System.out.println("FAIL — thread(s) still running after 5 s "
                + "(missing wait() or notifyAll()?)");
        } else if (actualSum.get() == expectedSum) {
            System.out.println("PASS — all " + total + " items pushed and popped correctly");
        } else {
            System.out.println("FAIL — item sum mismatch: expected " + expectedSum
                + " but got " + actualSum.get()
                + " (pop() may still be returning the -1 placeholder)");
        }
    }
}
