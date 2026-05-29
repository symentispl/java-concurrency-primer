// Exercise 1: Thread-Safe Bank Account
//
// TOPIC: synchronized — mutual exclusion and memory visibility
//
// A bank account has a balance. One thread deposits 1 exactly N times;
// another withdraws 1 exactly N times. The net effect should be zero —
// the balance should be unchanged.
//
// Without synchronization, deposit/withdraw each compile to three steps
// (LOAD balance, ADD/SUBTRACT, STORE balance). The OS can preempt a thread
// between any two steps, so updates from one thread overwrite the other's:
//
//   Thread A (deposit):   LOAD(1000) ADD→1001  [preempted]
//   Thread B (withdraw):  LOAD(1000) SUB→999   STORE(999)
//   Thread A:                                  STORE(1001) ← B's update lost
//
// YOUR TASK
// ---------
// Add the synchronized keyword to deposit() and withdraw() in BankAccount.
//
// Run:  mise run java:exec -- exercises/Exercise1BankAccount
// Pass: PASS — all 10 trials ended with balance 1000

class Exercise1BankAccount {

    static class BankAccount {
        private int balance;

        BankAccount(int initialBalance) { this.balance = initialBalance; }

        // TODO: add 'synchronized'
        void deposit(int amount) {
            balance += amount;
        }

        // TODO: add 'synchronized'
        void withdraw(int amount) {
            balance -= amount;
        }

        int getBalance() { return balance; }
    }

    void main(String[] args) throws InterruptedException {
        int trials  = 10;
        int ops     = 10_000;
        int initial = 1_000;
        int passed  = 0;

        for (int t = 0; t < trials; t++) {
            BankAccount account = new BankAccount(initial);

            Thread depositor = new Thread(() -> {
                for (int i = 0; i < ops; i++) account.deposit(1);
            });
            Thread withdrawer = new Thread(() -> {
                for (int i = 0; i < ops; i++) account.withdraw(1);
            });

            depositor.start();
            withdrawer.start();
            depositor.join();
            withdrawer.join();

            if (account.getBalance() == initial) passed++;
        }

        if (passed == trials) {
            System.out.println("PASS — all " + trials + " trials ended with balance " + initial);
        } else {
            System.out.println("FAIL — " + (trials - passed) + "/" + trials
                + " trial(s) ended with wrong balance (expected " + initial + ")");
        }
    }
}
