package src;

import java.util.Objects;

/**
 * Immutable record of a single flash-sale order attempt made by one thread.
 *
 * <p>
 * Each task corresponds to one customer trying to purchase one flash-sale item.
 * Aggregated {@code TaskResult}s are used to build a {@link BenchmarkResult} at
 * the
 * end of a benchmark run.
 *
 * <p>
 * Use {@link #success} or {@link #failure} factory methods for readability:
 * 
 * <pre>{@code
 * TaskResult r = TaskResult.success("SYNCHRONIZED", 3, "O0001234", "FI00042", 12);
 * TaskResult f = TaskResult.failure("ATOMIC", 1, "O0001235", "FI00042", "OUT_OF_STOCK");
 * }</pre>
 */
public class TaskResult {

    // -----------------------------------------------------------------------
    // Constants
    // -----------------------------------------------------------------------

    /** Sentinel latency value used when a task has not yet completed. */
    public static final long LATENCY_NOT_SET = -1L;

    // -----------------------------------------------------------------------
    // Fields
    // -----------------------------------------------------------------------

    /**
     * Concurrency strategy under which this task was executed (e.g.
     * "SYNCHRONIZED").
     */
    private final String strategy;

    /** Thread ID (1-based) that executed this task. */
    private final int threadId;

    /**
     * Order ID generated for this attempt (e.g. "O0001234"). {@code null} on
     * failure.
     */
    private final String orderId;

    /** Flash-sale item ID targeted by this task (e.g. "FI00042"). */
    private final String itemId;

    /** Number of units successfully reserved; 0 on failure. */
    private final int quantityReserved;

    /** Whether this task ultimately succeeded. */
    private final boolean succeeded;

    /**
     * Human-readable reason the task failed; {@code null} when {@link #succeeded}
     * is {@code true}.
     * Typical values: "OUT_OF_STOCK", "TIMEOUT", "REJECTED", "OVERSELL_DETECTED".
     */
    private final String failReason;

    /**
     * Wall-clock time in milliseconds from task submission to completion.
     * {@value #LATENCY_NOT_SET} means the value has not been recorded.
     */
    private final long latencyMs;

    /** Absolute timestamp (epoch ms) when this task finished. */
    private final long completedAtEpochMs;

    /**
     * Number of retry attempts made before the final outcome (0 = first attempt
     * succeeded/failed).
     */
    private final int retryCount;

    // -----------------------------------------------------------------------
    // Private constructor
    // -----------------------------------------------------------------------

    private TaskResult(Builder b) {
        this.strategy = b.strategy;
        this.threadId = b.threadId;
        this.orderId = b.orderId;
        this.itemId = b.itemId;
        this.quantityReserved = b.quantityReserved;
        this.succeeded = b.succeeded;
        this.failReason = b.failReason;
        this.latencyMs = b.latencyMs;
        this.completedAtEpochMs = b.completedAtEpochMs;
        this.retryCount = b.retryCount;
    }

    // -----------------------------------------------------------------------
    // Convenience factory methods
    // -----------------------------------------------------------------------

    /**
     * Creates a successful {@code TaskResult} with default latency/retry values.
     *
     * @param strategy         concurrency strategy name
     * @param threadId         thread executing the task (1-based)
     * @param orderId          generated order ID
     * @param itemId           flash-sale item ID
     * @param quantityReserved units reserved
     * @return a new successful {@code TaskResult}
     */
    public static TaskResult success(String strategy, int threadId,
            String orderId, String itemId,
            int quantityReserved) {
        return new Builder(strategy, threadId, itemId)
                .orderId(orderId)
                .quantityReserved(quantityReserved)
                .succeeded(true)
                .completedAtEpochMs(System.currentTimeMillis())
                .build();
    }

    /**
     * Creates a failed {@code TaskResult} with a reason and default latency/retry
     * values.
     *
     * @param strategy   concurrency strategy name
     * @param threadId   thread executing the task (1-based)
     * @param orderId    order ID (may be {@code null} if never created)
     * @param itemId     flash-sale item ID
     * @param failReason short reason code (e.g. "OUT_OF_STOCK")
     * @return a new failed {@code TaskResult}
     */
    public static TaskResult failure(String strategy, int threadId,
            String orderId, String itemId,
            String failReason) {
        return new Builder(strategy, threadId, itemId)
                .orderId(orderId)
                .quantityReserved(0)
                .succeeded(false)
                .failReason(failReason)
                .completedAtEpochMs(System.currentTimeMillis())
                .build();
    }

    // -----------------------------------------------------------------------
    // Builder
    // -----------------------------------------------------------------------

    /**
     * Fluent builder for {@link TaskResult}.
     *
     * <pre>{@code
     * TaskResult r = new TaskResult.Builder("ATOMIC", 5, "FI00042")
     *         .orderId("O0001234")
     *         .quantityReserved(1)
     *         .succeeded(true)
     *         .latencyMs(14)
     *         .retryCount(2)
     *         .completedAtEpochMs(System.currentTimeMillis())
     *         .build();
     * }</pre>
     */
    public static class Builder {
        // Required
        private final String strategy;
        private final int threadId;
        private final String itemId;
        // Optional (defaults)
        private String orderId = null;
        private int quantityReserved = 0;
        private boolean succeeded = false;
        private String failReason = null;
        private long latencyMs = LATENCY_NOT_SET;
        private long completedAtEpochMs = 0L;
        private int retryCount = 0;

        public Builder(String strategy, int threadId, String itemId) {
            this.strategy = strategy;
            this.threadId = threadId;
            this.itemId = itemId;
        }

        public Builder orderId(String v) {
            this.orderId = v;
            return this;
        }

        public Builder quantityReserved(int v) {
            this.quantityReserved = v;
            return this;
        }

        public Builder succeeded(boolean v) {
            this.succeeded = v;
            return this;
        }

        public Builder failReason(String v) {
            this.failReason = v;
            return this;
        }

        public Builder latencyMs(long v) {
            this.latencyMs = v;
            return this;
        }

        public Builder completedAtEpochMs(long v) {
            this.completedAtEpochMs = v;
            return this;
        }

        public Builder retryCount(int v) {
            this.retryCount = v;
            return this;
        }

        /**
         * Validates and builds the {@link TaskResult}.
         *
         * @throws IllegalArgumentException if required fields are invalid
         */
        public TaskResult build() {
            if (strategy == null || strategy.trim().isEmpty())
                throw new IllegalArgumentException("strategy must not be blank");
            if (threadId <= 0)
                throw new IllegalArgumentException("threadId must be > 0, got: " + threadId);
            if (itemId == null || itemId.trim().isEmpty())
                throw new IllegalArgumentException("itemId must not be blank");
            if (succeeded && quantityReserved <= 0)
                throw new IllegalArgumentException("A successful task must have quantityReserved > 0");
            if (!succeeded && (failReason == null || failReason.trim().isEmpty()))
                System.err.println("[WARN] TaskResult: failed task has no failReason set for item " + itemId);
            if (retryCount < 0)
                throw new IllegalArgumentException("retryCount must be >= 0, got: " + retryCount);
            return new TaskResult(this);
        }
    }

    // -----------------------------------------------------------------------
    // Derived / convenience methods
    // -----------------------------------------------------------------------

    /**
     * Returns {@code true} if this task was successful and reserved at least one
     * unit.
     * (Alias for {@link #isSucceeded()} — keeps code readable.)
     */
    public boolean isOrder() {
        return succeeded && quantityReserved > 0;
    }

    /**
     * Returns a CSV row matching the {@code transactions.csv} schema used by
     * {@link DataGenerator#initTransactions()}.
     *
     * <p>
     * Column order:
     * txId, orderId, itemId, customerId, requestedQty, mechanism,
     * success, negativeStock, tps, timestamp, retryCount, failReason
     *
     * <p>
     * Note: {@code txId}, {@code customerId}, {@code negativeStock}, and
     * {@code tps}
     * are not tracked per-task and are output as {@code "?"} placeholders; the
     * caller
     * is expected to substitute real values when writing the full transaction log.
     */
    public String[] toCsv() {
        return new String[] {
                "?", // txId — assigned by aggregator
                orderId != null ? orderId : "", // orderId
                itemId, // itemId
                "?", // customerId — not tracked here
                String.valueOf(quantityReserved), // requestedQty
                strategy, // mechanism
                String.valueOf(succeeded), // success
                "false", // negativeStock — aggregator fills
                "?", // tps — computed post-run
                String.valueOf(completedAtEpochMs), // timestamp (epoch ms)
                String.valueOf(retryCount), // retryCount
                failReason != null ? failReason : "" // failReason
        };
    }

    /** Returns a comma-separated CSV line (no header). */
    public String toCsvString() {
        return String.join(",", toCsv());
    }

    /** Returns a compact one-line summary for logging. */
    public String getSummary() {
        String status = succeeded
                ? String.format("[OK]  qty=%-2d", quantityReserved)
                : String.format("[FAIL] reason=%-18s", failReason != null ? failReason : "?");
        return String.format(
                "| Thread-%-3d | %-14s | item=%-8s | %s | latency=%s ms | retries=%d |",
                threadId, strategy, itemId, status,
                latencyMs == LATENCY_NOT_SET ? "?" : String.valueOf(latencyMs),
                retryCount);
    }

    // -----------------------------------------------------------------------
    // Object overrides
    // -----------------------------------------------------------------------

    /** Delegates to {@link #getSummary()} for convenient debug printing. */
    @Override
    public String toString() {
        return getSummary();
    }

    /**
     * Two task results are equal when they share the same strategy, threadId,
     * orderId, itemId, and outcome.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof TaskResult))
            return false;
        TaskResult that = (TaskResult) o;
        return threadId == that.threadId
                && succeeded == that.succeeded
                && Objects.equals(strategy, that.strategy)
                && Objects.equals(orderId, that.orderId)
                && Objects.equals(itemId, that.itemId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(strategy, threadId, orderId, itemId, succeeded);
    }

    // -----------------------------------------------------------------------
    // Getters
    // -----------------------------------------------------------------------

    public String getStrategy() {
        return strategy;
    }

    public int getThreadId() {
        return threadId;
    }

    public String getOrderId() {
        return orderId;
    }

    public String getItemId() {
        return itemId;
    }

    public int getQuantityReserved() {
        return quantityReserved;
    }

    public boolean isSucceeded() {
        return succeeded;
    }

    public String getFailReason() {
        return failReason;
    }

    public long getLatencyMs() {
        return latencyMs;
    }

    public long getCompletedAtEpochMs() {
        return completedAtEpochMs;
    }

    public int getRetryCount() {
        return retryCount;
    }
}
