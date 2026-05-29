// Exercise 4: Bank Transfer Deadlock
//
// TOPIC: Deadlock prevention — consistent lock ordering
//
// Transferring money between two accounts requires locking both accounts.
// The current implementation locks 'from' first, then 'to'.
// When two threads transfer in opposite directions at the same time:
//
//   Thread 1: transfer(A → B)  locks A, waits for B
//   Thread 2: transfer(B → A)  locks B, waits for A  ← circular wait → deadlock!
//
// THE FIX: establish a global lock order — always lock the account with the
// smaller id first, regardless of the transfer direction.
//
//   Account first  = (from.id < to.id) ? from : to;
//   Account second = (from.id < to.id) ? to   : from;
//   synchronized (first) {
//       synchronized (second) {
//           from.balance -= amount;
//           to.balance   += amount;
//       }
//   }
//
// YOUR TASK
// ---------
// Replace the broken synchronized blocks in transfer() with the lock-ordered version.
// Keep the debit/credit logic unchanged — only the locking order needs to change.
//
// Run:  mise run java:exec -- exercises/Exercise4TransferDeadlock
// Pass: PASS — 1000 transfers completed, balances correct, no deadlock

class Exercise4TransferDeadlock {

    static class Account {
        final int id;
        int balance;

        Account(int id, int balance) { this.id = id; this.balance = balance; }
    }

    static void transfer(Account from, Account to, int amount) {
        // BUG: Thread 1 calls transfer(A, B) → locks A then B
        //      Thread 2 calls transfer(B, A) → locks B then A  ← opposite order!
        //
        // TODO: replace the two synchronized blocks below with lock-ordered acquisition:
        //
        //   Account first  = (from.id < to.id) ? from : to;
        //   Account second = (from.id < to.id) ? to   : from;
        //   synchronized (first) {
        //       synchronized (second) {
        //           from.balance -= amount;
        //           to.balance   += amount;
        //       }
        //   }

        synchronized (from) {                     // ← remove this block ...
            try { Thread.sleep(1); }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); return; }
            synchronized (to) {                   // ← ... and this nested block
                from.balance -= amount;           //   keep only these two lines
                to.balance   += amount;           //   inside the new lock-ordered blocks
            }
        }
    }

    void main(String[] args) throws InterruptedException {
        Account accountA = new Account(1, 500);
        Account accountB = new Account(2, 500);
        int transfers = 500;

        Thread t1 = new Thread(() -> {
            for (int i = 0; i < transfers; i++)
                transfer(accountA, accountB, 1);
        }, "Thread-AtoB");

        Thread t2 = new Thread(() -> {
            for (int i = 0; i < transfers; i++)
                transfer(accountB, accountA, 1);
        }, "Thread-BtoA");

        t1.start();
        t2.start();
        t1.join(2_000); // a deadlock will never finish in 2 seconds
        t2.join(2_000);

        if (t1.isAlive() || t2.isAlive()) {
            System.out.println("FAIL — thread(s) still blocked after timeout (deadlock not fixed)");
            System.exit(1); // deadlocked threads are in BLOCKED state and cannot be interrupted
        }

        int balanceA = accountA.balance;
        int balanceB = accountB.balance;
        if (balanceA == 500 && balanceB == 500) {
            System.out.println("PASS — " + (transfers * 2)
                + " transfers completed, balances correct, no deadlock");
        } else {
            System.out.println("FAIL — transfers completed but balances are wrong: "
                + "A=" + balanceA + ", B=" + balanceB + " (expected A=500, B=500)");
        }
    }
}
