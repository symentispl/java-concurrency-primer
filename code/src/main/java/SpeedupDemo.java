// Demonstrates parallel speedup: sum of Math.sqrt() over 100M doubles.
// Shows scaling from 1 thread to beyond core count — illustrates Amdahl's Law and USL.
class SpeedupDemo {

    static final int SIZE = 100_000_000;
    static final double[] data = new double[SIZE];

    static double sumSequential() {
        double sum = 0;
        for (int i = 0; i < SIZE; i++) sum += Math.sqrt(data[i]);
        return sum;
    }

    static double sumParallel(int threads) throws InterruptedException {
        double[] partial = new double[threads];
        Thread[] workers = new Thread[threads];
        int chunk = SIZE / threads;

        for (int t = 0; t < threads; t++) {
            int start = t * chunk;
            int end   = (t == threads - 1) ? SIZE : start + chunk;
            int tid   = t;
            workers[t] = new Thread(() -> {
                double sum = 0;
                for (int i = start; i < end; i++) sum += Math.sqrt(data[i]);
                partial[tid] = sum;
            });
            workers[t].start();
        }
        for (Thread w : workers) w.join();

        double total = 0;
        for (double p : partial) total += p;
        return total;
    }

    static long measure(int threads) throws InterruptedException {
        // one warm-up run — JIT stabilisation
        if (threads == 1) sumSequential(); else sumParallel(threads);

        long[] times = new long[3];
        for (int i = 0; i < 3; i++) {
            long t0 = System.nanoTime();
            if (threads == 1) sumSequential(); else sumParallel(threads);
            times[i] = System.nanoTime() - t0;
        }
        java.util.Arrays.sort(times);
        return times[1] / 1_000_000; // median in ms
    }

    void main() throws InterruptedException {
        for (int i = 0; i < SIZE; i++) data[i] = i + 1.0;

        int cores = Runtime.getRuntime().availableProcessors();

        // powers of 2 up to cores, plus cores itself, then cores*2 and cores*4
        java.util.TreeSet<Integer> counts = new java.util.TreeSet<>();
        for (int n = 1; n <= cores; n *= 2) counts.add(n);
        counts.add(cores);
        counts.add(cores * 2);
        counts.add(cores * 4);

        System.out.printf("Array size: %,d doubles%n", SIZE);
        System.out.printf("Available processors: %d%n%n", cores);
        System.out.printf("%10s   %10s   %8s%n", "threads", "time(ms)", "speedup");
        System.out.println("-".repeat(36));

        long baseTime = -1;
        for (int threads : counts) {
            long ms = measure(threads);
            if (baseTime < 0) baseTime = ms;
            double speedup = (double) baseTime / ms;
            String label = threads > cores ? threads + " *" : String.valueOf(threads);
            System.out.printf("%10s   %10d   %7.1fx%n", label, ms, speedup);
        }
        System.out.println();
        System.out.println("* beyond available core count");
    }
}
