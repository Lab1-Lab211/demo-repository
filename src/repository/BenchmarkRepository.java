package src;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Repository chuyên biệt cho {@link BenchmarkResult}, kế thừa {@link CsvRepository}.
 *
 * <p>Chiến lược tối ưu:
 * <ul>
 *   <li>Kế thừa {@code CsvRepository<BenchmarkResult>} — tái sử dụng toàn bộ
 *       logic đọc/ghi/xóa/cập nhật CSV mà không viết lại.</li>
 *   <li>{@link #findBy(Predicate)} tổng quát — một method duy nhất thay thế
 *       mọi {@code findByXxx()}, không cần thêm method mới khi có tiêu chí lọc mới.</li>
 *   <li>{@link #clearResults()} dùng {@code java.nio.file.Files} + charset UTF-8
 *       rõ ràng — tránh phụ thuộc platform encoding, không bị resource leak.</li>
 * </ul>
 */
public class BenchmarkRepository extends CsvRepository<BenchmarkResult> {

    // -----------------------------------------------------------------------
    // Constructor
    // -----------------------------------------------------------------------

    /**
     * Khởi tạo repository với đường dẫn file CSV.
     *
     * @param filePath đường dẫn tuyệt đối hoặc tương đối tới file CSV
     */
    public BenchmarkRepository(String filePath) {
        super(filePath);
    }

    // -----------------------------------------------------------------------
    // Abstract method implementations — required by CsvRepository
    // -----------------------------------------------------------------------

    /**
     * Trả về ID định danh duy nhất cho một {@link BenchmarkResult}.
     *
     * <p>ID được ghép từ các trường BẤT BIẾN (strategy, threadCount, wallClockMs)
     * để CsvRepository có thể tìm/cập nhật/xóa đúng record.
     * Không dùng {@code System.currentTimeMillis()} vì giá trị thay đổi mỗi lần gọi.</p>
     */
    @Override
    public String getEntityId(BenchmarkResult r) {
        return r.getStrategy() + "_" + r.getThreadCount() + "_" + r.getWallClockMs();
    }

    /**
     * Parse một dòng CSV thành {@link BenchmarkResult}.
     * Trả về {@code null} nếu dòng không hợp lệ (sẽ bị bỏ qua bởi {@code findAll()}).
     *
     * <p>Thứ tự cột: Strategy, ThreadCount, TotalTasks, SuccessOrders, FailedOrders,
     * OversellCount, Throughput_TPS (derived — bỏ qua), AvgLatency_ms,
     * MaxLatency_ms, MinLatency_ms, WallClock_ms</p>
     */
    @Override
    protected BenchmarkResult parseLine(String csvLine) {
        try {
            String[] p = csvLine.split(",");
            return new BenchmarkResult.Builder(
                        p[0],                       // strategy
                        Integer.parseInt(p[1]),     // threadCount
                        Integer.parseInt(p[2]))     // totalTasks
                    .successOrders(Long.parseLong(p[3]))
                    .failedOrders(Long.parseLong(p[4]))
                    .oversellCount(Long.parseLong(p[5]))
                    // p[6] = Throughput_TPS — derived, bỏ qua khi parse
                    .avgLatencyMs(Double.parseDouble(p[7]))
                    .maxLatencyMs(Double.parseDouble(p[8]))
                    .minLatencyMs(Double.parseDouble(p[9]))
                    .wallClockMs(Long.parseLong(p[10]))
                    .build();
        } catch (Exception e) {
            System.err.println("[WARN] Bỏ qua dòng CSV không hợp lệ: " + csvLine);
            return null;
        }
    }

    /**
     * Chuyển một {@link BenchmarkResult} thành dòng CSV (dùng method sẵn có của entity).
     */
    @Override
    protected String toLine(BenchmarkResult r) {
        return r.toCsvString();
    }

    /**
     * Trả về dòng header của file benchmark CSV.
     */
    @Override
    protected String headerLine() {
        return String.join(",", BenchmarkResult.getHeaders());
    }

    // -----------------------------------------------------------------------
    // Write
    // -----------------------------------------------------------------------

    /**
     * Validate cơ bản rồi ủy quyền lưu cho {@code CsvRepository.save()}.
     *
     * @param r kết quả cần lưu — không được {@code null}, {@code totalTasks} phải {@code > 0}
     * @throws IllegalArgumentException nếu {@code r} null hoặc {@code totalTasks <= 0}
     */
    public void saveResult(BenchmarkResult r) {
        if (r == null)
            throw new IllegalArgumentException("BenchmarkResult không được null");
        if (r.getTotalTasks() <= 0)
            throw new IllegalArgumentException("totalTasks phải > 0 trước khi lưu");
        super.save(r);
    }

    // -----------------------------------------------------------------------
    // Read — generic Predicate replaces all findByXxx() variants
    // -----------------------------------------------------------------------

    /**
     * Lọc tất cả kết quả theo tiêu chí bất kỳ (functional style).
     *
     * <p>Một method duy nhất thay thế mọi {@code findByXxx()} — không cần thêm
     * method mới khi xuất hiện tiêu chí lọc mới.</p>
     *
     * <pre>{@code
     * repo.findBy(r -> r.getStrategy().equalsIgnoreCase("NO_LOCK"));
     * repo.findBy(r -> r.getThreadCount() == 100);
     * repo.findBy(BenchmarkResult::isOversold);
     * repo.findBy(r -> r.getThroughput() > 500);
     * repo.findBy(r -> r.getStrategy().equalsIgnoreCase("OPTIMISTIC")
     *                && r.getThreadCount() >= 200
     *                && !r.isOversold());
     * }</pre>
     *
     * @param filter {@link Predicate} xác định điều kiện lọc
     * @return danh sách kết quả thỏa điều kiện, không bao giờ {@code null}
     */
    public List<BenchmarkResult> findBy(Predicate<BenchmarkResult> filter) {
        return super.findAll().stream()
                .filter(filter)
                .collect(Collectors.toList());
    }

    /**
     * Convenience method — lọc theo tên strategy (không phân biệt hoa/thường).
     */
    public List<BenchmarkResult> findByStrategy(String strategy) {
        return findBy(r -> r.getStrategy().equalsIgnoreCase(strategy));
    }

    /**
     * Convenience method — lọc theo số lượng thread.
     */
    public List<BenchmarkResult> findByThreadCount(int threadCount) {
        return findBy(r -> r.getThreadCount() == threadCount);
    }

    // -----------------------------------------------------------------------
    // Maintenance
    // -----------------------------------------------------------------------

    /**
     * Xóa toàn bộ data, chỉ giữ lại dòng header — dùng trước mỗi đợt benchmark mới.
     *
     * <p>Dùng {@code java.nio.file.Files} + {@link StandardCharsets#UTF_8}:
     * charset UTF-8 cố định, không phụ thuộc platform, atomic truncate-then-write.</p>
     */
    public void clearResults() {
        String header = headerLine() + System.lineSeparator();
        try {
            Files.write(
                Paths.get(getFilePath()),
                header.getBytes(StandardCharsets.UTF_8),
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE
            );
        } catch (IOException e) {
            System.err.println("Lỗi khi xóa file benchmark: " + e.getMessage());
        }
    }
}
