package src;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Repository quản lý tồn kho Flash-Sale Item ({@code flash_items.csv}).
 *
 * <p>Kế thừa {@link CsvRepository} để tái sử dụng toàn bộ logic đọc/ghi CSV.
 *
 * <p>Schema CSV ({@code flash_items.csv}):
 * <pre>
 * itemId, eventId, productId, salePrice, limitedQty, soldQty, maxPerCustomer, status, version
 * </pre>
 *
 * <p>Bất biến cốt lõi: {@code soldQty ≤ limitedQty}.
 */
public class InventoryRepository extends CsvRepository<FlashItem> {

    // -----------------------------------------------------------------------
    // Constructor
    // -----------------------------------------------------------------------

    /**
     * Khởi tạo repository với đường dẫn file CSV.
     *
     * @param filePath đường dẫn tuyệt đối hoặc tương đối tới {@code flash_items.csv}
     */
    public InventoryRepository(String filePath) {
        super(filePath);
    }

    // -----------------------------------------------------------------------
    // Abstract method implementations — required by CsvRepository
    // -----------------------------------------------------------------------

    /**
     * Trả về {@code itemId} làm khóa định danh duy nhất (e.g. {@code "FI00042"}).
     */
    @Override
    public String getEntityId(FlashItem item) {
        return item.getItemId();
    }

    /**
     * Parse một dòng CSV thành {@link FlashItem}.
     * Trả về {@code null} nếu dòng không hợp lệ (sẽ bị bỏ qua bởi {@code findAll()}).
     *
     * <p>Thứ tự cột:
     * itemId, eventId, productId, salePrice, limitedQty, soldQty,
     * maxPerCustomer, status, version
     */
    @Override
    protected FlashItem parseLine(String csvLine) {
        try {
            String[] p = csvLine.split(",");
            return new FlashItem(
                p[0].trim(),                    // itemId
                p[1].trim(),                    // eventId
                p[2].trim(),                    // productId
                Double.parseDouble(p[3].trim()), // salePrice
                Integer.parseInt(p[4].trim()),   // limitedQty
                Integer.parseInt(p[5].trim()),   // soldQty
                Integer.parseInt(p[6].trim()),   // maxPerCustomer
                p[7].trim(),                    // status
                Integer.parseInt(p[8].trim())    // version
            );
        } catch (Exception e) {
            System.err.println("[WARN] Bỏ qua dòng flash_items không hợp lệ: " + csvLine);
            return null;
        }
    }

    /**
     * Chuyển một {@link FlashItem} thành dòng CSV.
     */
    @Override
    protected String toLine(FlashItem item) {
        return item.toCsvString();
    }

    /**
     * Trả về dòng header của {@code flash_items.csv}.
     */
    @Override
    protected String headerLine() {
        return "itemId,eventId,productId,salePrice,limitedQty,soldQty,maxPerCustomer,status,version";
    }

    // -----------------------------------------------------------------------
    // Read — Predicate-based queries
    // -----------------------------------------------------------------------

    /**
     * Lọc tất cả flash item theo tiêu chí bất kỳ (functional style).
     *
     * <pre>{@code
     * repo.findBy(item -> item.getEventId().equals("E0001"));
     * repo.findBy(item -> item.getRemainingQty() > 0);
     * repo.findBy(FlashItem::isActive);
     * }</pre>
     *
     * @param filter {@link Predicate} xác định điều kiện lọc
     * @return danh sách flash item thỏa điều kiện, không bao giờ {@code null}
     */
    public List<FlashItem> findBy(Predicate<FlashItem> filter) {
        return super.findAll().stream()
                .filter(filter)
                .collect(Collectors.toList());
    }

    /**
     * Convenience method — lấy tất cả flash item thuộc một event.
     *
     * @param eventId ID của flash-sale event (e.g. {@code "E0001"})
     * @return danh sách flash item của event đó
     */
    public List<FlashItem> findByEvent(String eventId) {
        return findBy(item -> item.getEventId().equalsIgnoreCase(eventId));
    }

    /**
     * Convenience method — lấy tất cả flash item còn hàng ({@code soldQty < limitedQty}).
     *
     * @return danh sách flash item còn tồn kho
     */
    public List<FlashItem> findAvailable() {
        return findBy(item -> item.getRemainingQty() > 0 && item.isActive());
    }

    /**
     * Convenience method — lấy tất cả flash item đã hết hàng ({@code soldQty >= limitedQty}).
     *
     * @return danh sách flash item đã bán hết
     */
    public List<FlashItem> findSoldOut() {
        return findBy(item -> item.getRemainingQty() <= 0);
    }

    // -----------------------------------------------------------------------
    // Write — inventory mutation
    // -----------------------------------------------------------------------

    /**
     * Cập nhật {@code soldQty} của một flash item (ghi đè record cũ).
     *
     * <p>Kiểm tra bất biến: {@code newSoldQty} không được vượt quá {@code limitedQty}.
     *
     * @param itemId     ID của flash item cần cập nhật
     * @param newSoldQty số lượng đã bán mới
     * @throws IllegalArgumentException nếu {@code newSoldQty < 0} hoặc vượt quá {@code limitedQty}
     * @throws IllegalStateException    nếu không tìm thấy item với {@code itemId} đã cho
     */
    public void updateSoldQty(String itemId, int newSoldQty) {
        FlashItem item = super.findById(itemId)
                .orElseThrow(() -> new IllegalStateException(
                        "Không tìm thấy flash item: " + itemId));

        if (newSoldQty < 0)
            throw new IllegalArgumentException("soldQty không được âm: " + newSoldQty);
        if (newSoldQty > item.getLimitedQty())
            throw new IllegalArgumentException(
                    "soldQty (" + newSoldQty + ") vượt quá limitedQty ("
                    + item.getLimitedQty() + ") cho item " + itemId);

        String newStatus = newSoldQty >= item.getLimitedQty() ? "SOLD_OUT" : "ACTIVE";
        FlashItem updated = new FlashItem(
                item.getItemId(), item.getEventId(), item.getProductId(),
                item.getSalePrice(), item.getLimitedQty(), newSoldQty,
                item.getMaxPerCustomer(), newStatus, item.getVersion() + 1
        );
        super.update(updated);
    }

    /**
     * Thử đặt hàng {@code qty} đơn vị cho flash item {@code itemId}.
     *
     * <p>Nếu thành công, tăng {@code soldQty} lên {@code qty} và trả về {@code true}.
     * Nếu không đủ hàng, trả về {@code false} (không ném exception).
     *
     * @param itemId ID của flash item
     * @param qty    số lượng muốn đặt (phải {@code > 0})
     * @return {@code true} nếu đặt hàng thành công, {@code false} nếu không đủ hàng
     */
    public boolean tryReserve(String itemId, int qty) {
        if (qty <= 0) throw new IllegalArgumentException("qty phải > 0, got: " + qty);

        return super.findById(itemId).map(item -> {
            if (item.getRemainingQty() < qty) return false;
            updateSoldQty(itemId, item.getSoldQty() + qty);
            return true;
        }).orElse(false);
    }
}

// ---------------------------------------------------------------------------
// FlashItem — entity class cho flash_items.csv
// ---------------------------------------------------------------------------

/**
 * Immutable entity đại diện cho một flash-sale item (một dòng trong {@code flash_items.csv}).
 */
class FlashItem {

    private final String itemId;
    private final String eventId;
    private final String productId;
    private final double salePrice;
    private final int    limitedQty;
    private final int    soldQty;
    private final int    maxPerCustomer;
    private final String status;
    private final int    version;

    public FlashItem(String itemId, String eventId, String productId,
                     double salePrice, int limitedQty, int soldQty,
                     int maxPerCustomer, String status, int version) {
        this.itemId         = itemId;
        this.eventId        = eventId;
        this.productId      = productId;
        this.salePrice      = salePrice;
        this.limitedQty     = limitedQty;
        this.soldQty        = soldQty;
        this.maxPerCustomer = maxPerCustomer;
        this.status         = status;
        this.version        = version;
    }

    // -----------------------------------------------------------------------
    // Derived
    // -----------------------------------------------------------------------

    /** Số lượng còn lại có thể bán ({@code limitedQty - soldQty}). */
    public int getRemainingQty() {
        return limitedQty - soldQty;
    }

    /** Trả về {@code true} nếu item đang ở trạng thái {@code ACTIVE}. */
    public boolean isActive() {
        return "ACTIVE".equalsIgnoreCase(status);
    }

    // -----------------------------------------------------------------------
    // CSV output
    // -----------------------------------------------------------------------

    /** Chuyển thành mảng String theo thứ tự cột CSV. */
    public String[] toCsv() {
        return new String[]{
            itemId,
            eventId,
            productId,
            String.format("%.2f", salePrice),
            String.valueOf(limitedQty),
            String.valueOf(soldQty),
            String.valueOf(maxPerCustomer),
            status,
            String.valueOf(version)
        };
    }

    /** Trả về dòng CSV (không có newline). */
    public String toCsvString() {
        return String.join(",", toCsv());
    }

    @Override
    public String toString() {
        return String.format("FlashItem{id=%s, event=%s, remaining=%d/%d, status=%s}",
                itemId, eventId, getRemainingQty(), limitedQty, status);
    }

    // -----------------------------------------------------------------------
    // Getters
    // -----------------------------------------------------------------------

    public String getItemId()         { return itemId; }
    public String getEventId()        { return eventId; }
    public String getProductId()      { return productId; }
    public double getSalePrice()      { return salePrice; }
    public int    getLimitedQty()     { return limitedQty; }
    public int    getSoldQty()        { return soldQty; }
    public int    getMaxPerCustomer() { return maxPerCustomer; }
    public String getStatus()         { return status; }
    public int    getVersion()        { return version; }
}