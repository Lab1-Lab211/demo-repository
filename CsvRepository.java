package src;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Generic base repository cho việc đọc/ghi dữ liệu từ file CSV.
 *
 * <p>
 * Subclass phải implement 4 method abstract:
 * <ul>
 * <li>{@link #getEntityId(Object)} — trả về ID duy nhất của entity</li>
 * <li>{@link #parseLine(String)} — parse một dòng CSV thành object T</li>
 * <li>{@link #toLine(Object)} — chuyển object T thành một dòng CSV</li>
 * <li>{@link #headerLine()} — trả về dòng header của file CSV</li>
 * </ul>
 *
 * @param <T> kiểu dữ liệu entity được quản lý
 */
public abstract class CsvRepository<T> {

    // -----------------------------------------------------------------------
    // State
    // -----------------------------------------------------------------------

    /** Đường dẫn tới file CSV. */
    private final String filePath;

    // -----------------------------------------------------------------------
    // Constructor
    // -----------------------------------------------------------------------

    /**
     * Khởi tạo repository.
     * Nếu file chưa tồn tại hoặc rỗng, tự động tạo file và ghi header.
     *
     * @param filePath đường dẫn tuyệt đối hoặc tương đối tới file CSV
     */
    protected CsvRepository(String filePath) {
        this.filePath = filePath;
        ensureFileWithHeader();
    }

    // -----------------------------------------------------------------------
    // Abstract — subclass must implement
    // -----------------------------------------------------------------------

    /**
     * Trả về ID định danh duy nhất của một entity.
     * Dùng để tìm kiếm, cập nhật và xóa record.
     */
    public abstract String getEntityId(T entity);

    /**
     * Parse một dòng CSV thành object T.
     * Trả về {@code null} nếu dòng không hợp lệ (sẽ bị bỏ qua khi đọc).
     */
    protected abstract T parseLine(String csvLine);

    /**
     * Chuyển một entity T thành một dòng CSV (không có newline ở cuối).
     */
    protected abstract String toLine(T entity);

    /**
     * Trả về dòng header của file CSV (không có newline ở cuối).
     */
    protected abstract String headerLine();

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    /** Trả về đường dẫn file CSV. */
    public String getFilePath() {
        return filePath;
    }

    /**
     * Ghi thêm một entity vào cuối file CSV (append mode).
     *
     * @param entity entity cần lưu — không được {@code null}
     */
    public void save(T entity) {
        if (entity == null)
            throw new IllegalArgumentException("Entity không được null");
        try (BufferedWriter bw = Files.newBufferedWriter(
                Paths.get(filePath), StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
            bw.write(toLine(entity));
            bw.newLine();
        } catch (IOException e) {
            System.err.println("[CsvRepository] Lỗi khi lưu: " + e.getMessage());
        }
    }

    /**
     * Đọc toàn bộ dữ liệu từ file CSV (bỏ qua header và dòng lỗi).
     *
     * @return danh sách entity, không bao giờ {@code null}
     */
    public List<T> findAll() {
        try {
            return Files.readAllLines(Paths.get(filePath), StandardCharsets.UTF_8)
                    .stream()
                    .skip(1) // bỏ qua header
                    .filter(line -> !line.trim().isEmpty())
                    .map(this::parseLine)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            System.err.println("[CsvRepository] Lỗi khi đọc: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Tìm entity theo ID.
     *
     * @param id ID cần tìm
     * @return {@link Optional} chứa entity nếu tìm thấy, rỗng nếu không
     */
    public Optional<T> findById(String id) {
        return findAll().stream()
                .filter(e -> getEntityId(e).equals(id))
                .findFirst();
    }

    /**
     * Xóa một entity theo ID, ghi lại toàn bộ file (không có record bị xóa).
     *
     * @param id ID của entity cần xóa
     * @return {@code true} nếu xóa thành công, {@code false} nếu không tìm thấy
     */
    public boolean deleteById(String id) {
        List<T> all = findAll();
        List<T> remaining = all.stream()
                .filter(e -> !getEntityId(e).equals(id))
                .collect(Collectors.toList());
        if (remaining.size() == all.size())
            return false; // không tìm thấy
        rewriteAll(remaining);
        return true;
    }

    /**
     * Cập nhật một entity (xóa record cũ theo ID, ghi record mới).
     *
     * @param updated entity đã cập nhật
     */
    public void update(T updated) {
        List<T> all = findAll();
        String targetId = getEntityId(updated);
        List<T> rewritten = all.stream()
                .map(e -> getEntityId(e).equals(targetId) ? updated : e)
                .collect(Collectors.toList());
        rewriteAll(rewritten);
    }

    /**
     * Đếm số lượng record trong file (không tính header).
     */
    public long count() {
        return findAll().size();
    }

    // -----------------------------------------------------------------------
    // Protected helpers (subclass có thể override nếu cần)
    // -----------------------------------------------------------------------

    /**
     * Ghi lại toàn bộ file với danh sách entity mới (header + data).
     * Dùng khi cần xóa/cập nhật record.
     */
    protected void rewriteAll(List<T> entities) {
        try (BufferedWriter bw = Files.newBufferedWriter(
                Paths.get(filePath), StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            bw.write(headerLine());
            bw.newLine();
            for (T e : entities) {
                bw.write(toLine(e));
                bw.newLine();
            }
        } catch (IOException e) {
            System.err.println("[CsvRepository] Lỗi khi ghi lại file: " + e.getMessage());
        }
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    /** Tạo file và ghi header nếu file chưa tồn tại hoặc rỗng. */
    private void ensureFileWithHeader() {
        File f = new File(filePath);
        // Tạo thư mục cha nếu chưa có
        if (f.getParentFile() != null)
            f.getParentFile().mkdirs();
        if (!f.exists() || f.length() == 0) {
            try (BufferedWriter bw = Files.newBufferedWriter(
                    Paths.get(filePath), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE)) {
                bw.write(headerLine());
                bw.newLine();
            } catch (IOException e) {
                System.err.println("[CsvRepository] Lỗi khi khởi tạo file: " + e.getMessage());
            }
        }
    }
}
