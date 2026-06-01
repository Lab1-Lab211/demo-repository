package src;

import java.util.Locale;
import java.util.Objects;

/**
 * Immutable data class representing the result of a single benchmark run.
 * Use {@link Builder} to construct instances instead of the direct constructor.
 */
public class BenchmarkResult {

    // -----------------------------------------------------------------------
    // Constants
    // -----------------------------------------------------------------------

    /** Shared locale for all numeric formatting — avoids repeated reference. */
    private static final Locale FMT_LOCALE = Locale.US;

    /** CSV header row, cached once and cloned on each call to getHeaders(). */
    private static final String[] HEADERS = {
            "Strategy", "ThreadCount", "TotalTasks",
            "SuccessOrders", "FailedOrders", "OversellCount",
            "Throughput_TPS", "AvgLatency_ms", "MaxLatency_ms",
            "MinLatency_ms", "WallClock_ms"
    };

    // -----------------------------------------------------------------------
    // Fields
    // -----------------------------------------------------------------------

    private final String strategy;
    private final int threadCount;
    private final int totalTasks;
    private final long successOrders;
    private final long failedOrders;
    private final long oversellCount;
    private final double throughput; // derived — calculated in constructor
    private final double avgLatencyMs;
    private final double maxLatencyMs;
    private final double minLatencyMs;
    private final long wallClockMs;

    // -----------------------------------------------------------------------
    // Private constructor — called only by Builder.build()
    // -----------------------------------------------------------------------

    private BenchmarkResult(Builder b) {
        this.strategy = b.strategy;
        this.threadCount = b.threadCount;
        this.totalTasks = b.totalTasks;
        this.successOrders = b.successOrders;
        this.failedOrders = b.failedOrders;
        this.oversellCount = b.oversellCount;
        this.avgLatencyMs = b.avgLatencyMs;
        this.maxLatencyMs = b.maxLatencyMs;
        this.minLatencyMs = b.minLatencyMs;
        this.wallClockMs = b.wallClockMs;
        this.throughput = calculateThroughput();
    }

    // -----------------------------------------------------------------------
    // Builder
    // -----------------------------------------------------------------------

    /**
     * Fluent builder for {@link BenchmarkResult}.
     *
     * <pre>{@code
     * BenchmarkResult r = new BenchmarkResult.Builder("SYNCHRONIZED", 10, 1000)
     *         .successOrders(950).failedOrders(50).oversellCount(0)
     *         .avgLatencyMs(5.2).maxLatencyMs(20.1).minLatencyMs(1.0)
     *         .wallClockMs(100)
     *         .build();
     * }</pre>
     */
    public static class Builder {
        // Required
        private final String strategy;
        private final int threadCount;
        private final int totalTasks;
        // Optional (default = 0)
        private long successOrders;
        private long failedOrders;
        private long oversellCount;
        private double avgLatencyMs;
        private double maxLatencyMs;
        private double minLatencyMs;
        private long wallClockMs;

        public Builder(String strategy, int threadCount, int totalTasks) {
            this.strategy = strategy;
            this.threadCount = threadCount;
            this.totalTasks = totalTasks;
        }

        public Builder successOrders(long v) {
            this.successOrders = v;
            return this;
        }

        public Builder failedOrders(long v) {
            this.failedOrders = v;
            return this;
        }

        public Builder oversellCount(long v) {
            this.oversellCount = v;
            return this;
        }

        public Builder avgLatencyMs(double v) {
            this.avgLatencyMs = v;
            return this;
        }

        public Builder maxLatencyMs(double v) {
            this.maxLatencyMs = v;
            return this;
        }

        public Builder minLatencyMs(double v) {
            this.minLatencyMs = v;
            return this;
        }

        public Builder wallClockMs(long v) {
            this.wallClockMs = v;
            return this;
        }

        /**
         * Validates all fields then constructs the {@link BenchmarkResult}.
         *
         * @throws IllegalArgumentException if any field has an invalid value
         */
        public BenchmarkResult build() {
            if (strategy == null || strategy.trim().isEmpty())
                throw new IllegalArgumentException("strategy must not be blank");
            if (threadCount <= 0)
                throw new IllegalArgumentException("threadCount must be > 0, got: " + threadCount);
            if (totalTasks <= 0)
                throw new IllegalArgumentException("totalTasks must be > 0, got: " + totalTasks);
            if (successOrders < 0)
                throw new IllegalArgumentException("successOrders must be >= 0");
            if (failedOrders < 0)
                throw new IllegalArgumentException("failedOrders must be >= 0");
            if (successOrders + failedOrders > totalTasks)
                System.err.printf(
                        "[WARN] successOrders + failedOrders (%d) > totalTasks (%d) — possible counting error%n",
                        successOrders + failedOrders, totalTasks);
            return new BenchmarkResult(this);
        }
    }

    // -----------------------------------------------------------------------
    // Core logic
    // -----------------------------------------------------------------------

    private double calculateThroughput() {
        if (wallClockMs == 0)
            return 0.0;
        return successOrders / (wallClockMs / 1000.0);
    }

    // -----------------------------------------------------------------------
    // Convenience methods
    // -----------------------------------------------------------------------

    /** Returns {@code true} if any oversell occurred (race condition detected). */
    public boolean isOversold() {
        return oversellCount > 0;
    }

    /** Returns the success rate as a value between 0.0 and 1.0. */
    public double getSuccessRate() {
        return totalTasks == 0 ? 0.0 : (double) successOrders / totalTasks;
    }

    /**
     * Returns the success rate as a formatted percentage string, e.g.
     * {@code "95.00%"}.
     */
    public String getSuccessRateFormatted() {
        return String.format(FMT_LOCALE, "%.2f%%", getSuccessRate() * 100);
    }

    // -----------------------------------------------------------------------
    // Output methods
    // -----------------------------------------------------------------------

    /**
     * Returns the CSV header columns. Returns a defensive copy of the cached array.
     */
    public static String[] getHeaders() {
        return HEADERS.clone();
    }

    /**
     * Returns field values as a CSV row matching the order of
     * {@link #getHeaders()}.
     */
    public String[] toCsv() {
        return new String[] {
                strategy,
                String.valueOf(threadCount),
                String.valueOf(totalTasks),
                String.valueOf(successOrders),
                String.valueOf(failedOrders),
                String.valueOf(oversellCount),
                String.format(FMT_LOCALE, "%.2f", throughput),
                String.format(FMT_LOCALE, "%.2f", avgLatencyMs),
                String.format(FMT_LOCALE, "%.2f", maxLatencyMs),
                String.format(FMT_LOCALE, "%.2f", minLatencyMs),
                String.valueOf(wallClockMs)
        };
    }

    /** Returns a single comma-separated CSV line (no header). */
    public String toCsvString() {
        return String.join(",", toCsv());
    }

    /** Returns a human-readable one-line summary of this benchmark result. */
    public String getSummary() {
        String baselineStatus = "NO_LOCK".equals(strategy) ? "(Baseline)" : "";
        String oversellStatus = isOversold() ? "[!!! OVERSELL !!!]" : "[SAFE]";

        return String.format(
                "| %-12s %-10s | Threads: %-4d | Total: %-5d | Success: %-5d | Failed: %-5d | Oversell: %-5d %-18s | TPS: %-8.2f | Time: %d ms |",
                strategy, baselineStatus, threadCount, totalTasks,
                successOrders, failedOrders, oversellCount, oversellStatus,
                throughput, wallClockMs);
    }

    // -----------------------------------------------------------------------
    // Object overrides
    // -----------------------------------------------------------------------

    /** Delegates to {@link #getSummary()} for easy debug printing. */
    @Override
    public String toString() {
        return getSummary();
    }

    /**
     * Two results are equal if they share the same strategy, thread count,
     * total tasks, and wall-clock time.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof BenchmarkResult))
            return false;
        BenchmarkResult that = (BenchmarkResult) o;
        return threadCount == that.threadCount
                && totalTasks == that.totalTasks
                && wallClockMs == that.wallClockMs
                && strategy.equals(that.strategy);
    }

    @Override
    public int hashCode() {
        return Objects.hash(strategy, threadCount, totalTasks, wallClockMs);
    }

    // -----------------------------------------------------------------------
    // Getters
    // -----------------------------------------------------------------------

    public String getStrategy() {
        return strategy;
    }

    public int getThreadCount() {
        return threadCount;
    }

    public int getTotalTasks() {
        return totalTasks;
    }

    public long getSuccessOrders() {
        return successOrders;
    }

    public long getFailedOrders() {
        return failedOrders;
    }

    public long getOversellCount() {
        return oversellCount;
    }

    public double getThroughput() {
        return throughput;
    }

    public double getAvgLatencyMs() {
        return avgLatencyMs;
    }

    public double getMaxLatencyMs() {
        return maxLatencyMs;
    }

    public double getMinLatencyMs() {
        return minLatencyMs;
    }

    public long getWallClockMs() {
        return wallClockMs;
    }
}
