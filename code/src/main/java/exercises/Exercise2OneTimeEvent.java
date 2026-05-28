// Exercise 2: One-Time Event (Ready Signal)
//
// TOPIC: wait() and notifyAll() — waiting for a condition
//
// A OneTimeEvent lets any number of threads wait until a signal is fired.
// Once fired, any subsequent await() calls must return immediately without
// blocking — the event is permanent.
//
// This is the simplest possible use of wait/notifyAll: one boolean condition,
// two methods, no data structure involved.
//
// IMPLEMENTATION SKETCH
//
//   signal() — synchronized:
//     fired = true;
//     notifyAll();           // wake all parked threads
//
//   await() — synchronized:
//     while (!fired) wait(); // while — not if — guards spurious wakeups
//                            // returns immediately if already fired
//
// YOUR TASK
// ---------
// Implement signal() and await() in OneTimeEvent below.
//
// Run:  mise run java:exec -- exercises/Exercise2OneTimeEvent
// Pass: PASS — all 10 threads received the signal; late joiner returned immediately

import java.util.concurrent.atomic.AtomicInteger;

class Exercise2OneTimeEvent {

    static class OneTimeEvent {
        private boolean fired = false;

        synchronized void signal() {
            // TODO: set fired = true and wake all waiters
        }

        synchronized void await() throws InterruptedException {
            // TODO: wait in a loop while not yet fired
        }
    }

    void main() throws InterruptedException {
        var event   = new OneTimeEvent();
        int count   = 10;
        var arrived = new AtomicInteger(0);
        var threads = new Thread[count];

        for (int i = 0; i < count; i++) {
            threads[i] = new Thread(() -> {
                try {
                    event.await();
                    arrived.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
            threads[i].start();
        }

        Thread.sleep(200); // let all threads park before firing
        event.signal();
        for (var t : threads) t.join();

        // Late joiner — event already fired; must not block
        var late = new Thread(() -> {
            try { event.await(); }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        });
        late.start();
        late.join(500);
        boolean lateOk = !late.isAlive();
        late.interrupt();

        if (arrived.get() == count && lateOk) {
            System.out.println("PASS — all " + count + " threads received the signal; late joiner returned immediately");
        } else {
            System.out.println("FAIL — arrived=" + arrived.get() + "/" + count + ", lateJoinerFinished=" + lateOk);
        }
    }
}
