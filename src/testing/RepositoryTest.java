package testing;

import model.*;
import model.enums.*;
import repository.*;
import exception.*;

import java.io.File;
import java.util.List;
import java.util.Optional;

/**
 * Test toàn bộ Repository Layer — kiểm tra:
 * 1. CRUD cơ bản cho mọi repository
 * 2. Đọc 10k dòng products.csv < 1 giây
 * 3. FlashSaleItemRepository — 4 cơ chế lock
 * 4. Query helpers (findByCategory, findByTier, v.v.)
 *
 * Cách chạy:
 * javac -encoding UTF-8 -d out -sourcepath src src/model/BaseEntity.java
 * src/model/enums/*.java src/model/*.java src/exception/*.java
 * src/repository/*.java src/testing/RepositoryTest.java
 * java -cp out testing.RepositoryTest
 */
public class RepositoryTest {

    private static int totalTests = 0;
    private static int passedTests = 0;
    private static int failedTests = 0;

    public static void main(String[] args) {
        try {
            System.setOut(new java.io.PrintStream(System.out, true, "UTF-8"));
        } catch (Exception e) {
        }
        System.out.println("╔══════════════════════════════════════════════════╗");
        System.out.println("║   🧪 REPOSITORY LAYER TEST — Tuần 4            ║");
        System.out.println("╠══════════════════════════════════════════════════╣");
        System.out.println("║   Kiểm tra CRUD, Performance, 4 Lock Mechanisms ║");
        System.out.println("╚══════════════════════════════════════════════════╝");
        System.out.println();

        // Kiểm tra data đã tồn tại chưa
        if (!new File("data/products.csv").exists()) {
            System.out.println("⚠ Chưa có dữ liệu! Hãy chạy DataGenerator trước:");
            System.out.println(
                    "  javac -encoding UTF-8 -d out -sourcepath src src/model/BaseEntity.java src/model/enums/*.java src/model/*.java src/util/DataGenerator.java");
            System.out.println("  java -cp out util.DataGenerator");
            System.out.println();
            System.out.println("Sau đó chạy lại test này.");
            return;
        }

        // ======================== TEST TỪNG REPOSITORY ========================
        testProductRepository();
        testCustomerRepository();
        testFlashSaleEventRepository();
        testFlashSaleItemRepository();
        testOrderRepository();
        testOrderDetailRepository();
        testOrderTransactionRepository();

        // ======================== PERFORMANCE TEST ========================
        testPerformance();

        // ======================== FLASH SALE ITEM — 4 LOCK MECHANISMS
        // ========================
        testLockMechanisms();

        // ======================== KẾT QUẢ ========================
        System.out.println();
        System.out.println("═══════════════════════ KẾT QUẢ ═══════════════════════");
        System.out.printf("  Tổng test : %d%n", totalTests);
        System.out.printf("  ✅ PASS   : %d%n", passedTests);
        System.out.printf("  ❌ FAIL   : %d%n", failedTests);
        System.out.println("═══════════════════════════════════════════════════════");

        if (failedTests == 0) {
            System.out.println("  🎉 TẤT CẢ PASS — Repository Layer hoạt động chính xác!");
        } else {
            System.out.println("  ⚠ CÓ LỖI — Kiểm tra lại các test FAIL ở trên.");
        }
    }

    // ======================== PRODUCT REPOSITORY ========================

    private static void testProductRepository() {
        printSection("ProductRepository");
        ProductRepository repo = new ProductRepository("data/products.csv");

        // Test findAll
        List<Product> all = repo.findAll();
        assertTest("findAll() đọc được dữ liệu", all.size() > 0);
        assertTest("findAll() đọc đúng 5000 dòng", all.size() == 5000);

        // Test findById
        Optional<Product> first = repo.findById("PRD-00001");
        assertTest("findById('PRD-00001') tìm thấy", first.isPresent());
        if (first.isPresent()) {
            assertTest("findById → productId đúng", "PRD-00001".equals(first.get().getProductId()));
            assertTest("findById → có tên sản phẩm", first.get().getName() != null && !first.get().getName().isEmpty());
            assertTest("findById → giá > 0", first.get().getOriginalPrice() > 0);
        }

        // Test findByCategory
        List<Product> dienTu = repo.findByCategory(ProductCategory.DIEN_TU);
        assertTest("findByCategory(DIEN_TU) có kết quả", dienTu.size() > 0);
        assertTest("findByCategory → tất cả đều DIEN_TU",
                dienTu.stream().allMatch(p -> p.getCategory() == ProductCategory.DIEN_TU));

        // Test findByPriceRange
        List<Product> priceRange = repo.findByPriceRange(100_000, 500_000);
        assertTest("findByPriceRange(100k-500k) có kết quả", priceRange.size() > 0);
        assertTest("findByPriceRange → tất cả trong khoảng giá",
                priceRange.stream().allMatch(p -> p.getOriginalPrice() >= 100_000 && p.getOriginalPrice() <= 500_000));

        // Test findByName
        List<Product> searchName = repo.findByName("Tai nghe");
        assertTest("findByName('Tai nghe') có kết quả", searchName.size() > 0);

        // Test findInStock
        List<Product> inStock = repo.findInStock();
        assertTest("findInStock() có kết quả", inStock.size() > 0);
    }

    // ======================== CUSTOMER REPOSITORY ========================

    private static void testCustomerRepository() {
        printSection("CustomerRepository");
        CustomerRepository repo = new CustomerRepository("data/customers.csv");

        List<Customer> all = repo.findAll();
        assertTest("findAll() đọc được dữ liệu", all.size() > 0);
        assertTest("findAll() đọc đúng 2000 dòng", all.size() == 2000);

        // Test findById
        Optional<Customer> first = repo.findById("CUS-00001");
        assertTest("findById('CUS-00001') tìm thấy", first.isPresent());

        // Test findByTier
        List<Customer> vips = repo.findByTier(CustomerTier.VIP);
        assertTest("findByTier(VIP) có kết quả", vips.size() > 0);
        assertTest("findByTier → tất cả đều VIP",
                vips.stream().allMatch(c -> c.getTier() == CustomerTier.VIP));

        // Test findByEmail
        if (!all.isEmpty()) {
            String email = all.get(0).getEmail();
            Optional<Customer> byEmail = repo.findByEmail(email);
            assertTest("findByEmail() tìm thấy", byEmail.isPresent());
        }
    }

    // ======================== FLASH SALE EVENT REPOSITORY ========================

    private static void testFlashSaleEventRepository() {
        printSection("FlashSaleEventRepository");
        FlashSaleEventRepository repo = new FlashSaleEventRepository("data/flash_events.csv");

        List<FlashSaleEvent> all = repo.findAll();
        assertTest("findAll() đọc được dữ liệu", all.size() > 0);
        assertTest("findAll() đọc đúng 20 sự kiện", all.size() == 20);

        // Test findByStatus
        List<FlashSaleEvent> dangDienRa = repo.findDangDienRa();
        assertTest("findDangDienRa() có kết quả", dangDienRa.size() > 0);
        assertTest("findDangDienRa → tất cả DANG_DIEN_RA",
                dangDienRa.stream().allMatch(e -> e.getStatus() == SaleStatus.DANG_DIEN_RA));

        List<FlashSaleEvent> daKetThuc = repo.findDaKetThuc();
        assertTest("findDaKetThuc() có kết quả", daKetThuc.size() > 0);
    }

    // ======================== FLASH SALE ITEM REPOSITORY ========================

    private static void testFlashSaleItemRepository() {
        printSection("FlashSaleItemRepository");
        FlashSaleItemRepository repo = new FlashSaleItemRepository("data/flash_items.csv");

        List<FlashSaleItem> all = repo.findAll();
        assertTest("findAll() đọc được dữ liệu", all.size() > 0);
        assertTest("findAll() đọc đúng 500 flash items", all.size() == 500);

        // Test findById
        Optional<FlashSaleItem> first = repo.findById("FSI-00001");
        assertTest("findById('FSI-00001') tìm thấy", first.isPresent());
        if (first.isPresent()) {
            FlashSaleItem item = first.get();
            assertTest("findById → flashItemId đúng", "FSI-00001".equals(item.getFlashItemId()));
            assertTest("findById → limitedQty > 0", item.getLimitedQty() > 0);
            assertTest("findById → soldQty >= 0", item.getSoldQty() >= 0);
            assertTest("findById → BẤT BIẾN soldQty <= limitedQty", item.getSoldQty() <= item.getLimitedQty());
        }

        // Test findByEvent
        if (!all.isEmpty()) {
            String eventId = all.get(0).getEventId();
            List<FlashSaleItem> byEvent = repo.findByEvent(eventId);
            assertTest("findByEvent() có kết quả", byEvent.size() > 0);
        }

        // Test findAvailable
        List<FlashSaleItem> available = repo.findAvailable();
        assertTest("findAvailable() có kết quả", available.size() > 0);
        assertTest("findAvailable → tất cả còn hàng",
                available.stream().allMatch(item -> item.soLuongConLai() > 0));
    }

    // ======================== ORDER REPOSITORY ========================

    private static void testOrderRepository() {
        printSection("OrderRepository");
        OrderRepository repo = new OrderRepository("data/orders.csv");

        List<Order> all = repo.findAll();
        assertTest("findAll() đọc được dữ liệu", all.size() > 0);
        assertTest("findAll() đọc đúng 2500 đơn hàng", all.size() == 2500);

        // Test findByCustomer
        if (!all.isEmpty()) {
            String customerId = all.get(0).getCustomerId();
            List<Order> byCustomer = repo.findByCustomer(customerId);
            assertTest("findByCustomer() có kết quả", byCustomer.size() > 0);
        }

        // Test findByStatus
        List<Order> choXuLy = repo.findByStatus(OrderStatus.CHO_XU_LY);
        assertTest("findByStatus(CHO_XU_LY) có kết quả", choXuLy.size() > 0);
    }

    // ======================== ORDER DETAIL REPOSITORY ========================

    private static void testOrderDetailRepository() {
        printSection("OrderDetailRepository");
        OrderDetailRepository repo = new OrderDetailRepository("data/order_details.csv");

        List<OrderDetail> all = repo.findAll();
        assertTest("findAll() đọc được dữ liệu", all.size() > 0);
        assertTest("findAll() đọc đúng 2500 chi tiết", all.size() == 2500);

        // Test findByOrder
        if (!all.isEmpty()) {
            String orderId = all.get(0).getOrderId();
            List<OrderDetail> byOrder = repo.findByOrder(orderId);
            assertTest("findByOrder() có kết quả", byOrder.size() > 0);

            // Test tinhTongTien
            double tongTien = repo.tinhTongTien(orderId);
            assertTest("tinhTongTien() > 0", tongTien > 0);
        }
    }

    // ======================== ORDER TRANSACTION REPOSITORY
    // ========================

    private static void testOrderTransactionRepository() {
        printSection("OrderTransactionRepository (CRUD trên file tạm)");

        // Dùng file tạm để test CRUD — không ảnh hưởng data thật
        String testFile = "data/test_transactions.csv";
        OrderTransactionRepository repo = new OrderTransactionRepository(testFile);

        // Test save
        OrderTransaction txn = new OrderTransaction(
                "TXN-TEST-001", "ORD-00001", LockMechanism.SYNCHRONIZED,
                "TestThread-1", 1000000L, 2000000L, true, "");
        repo.save(txn);
        assertTest("save() thành công", repo.count() >= 1);

        // Test findById
        Optional<OrderTransaction> found = repo.findById("TXN-TEST-001");
        assertTest("findById() tìm thấy sau save", found.isPresent());
        if (found.isPresent()) {
            assertTest("findById → orderId đúng", "ORD-00001".equals(found.get().getOrderId()));
            assertTest("findById → mechanism đúng", LockMechanism.SYNCHRONIZED == found.get().getMechanism());
            assertTest("findById → success đúng", found.get().isSuccess());
        }

        // Test findByMechanism
        List<OrderTransaction> bySynced = repo.findByMechanism(LockMechanism.SYNCHRONIZED);
        assertTest("findByMechanism(SYNCHRONIZED) có kết quả", bySynced.size() > 0);

        // Test update
        OrderTransaction updated = new OrderTransaction(
                "TXN-TEST-001", "ORD-00001", LockMechanism.SYNCHRONIZED,
                "TestThread-1", 1000000L, 3000000L, false, "Test error");
        repo.update(updated);
        Optional<OrderTransaction> afterUpdate = repo.findById("TXN-TEST-001");
        assertTest("update() → endTime thay đổi",
                afterUpdate.isPresent() && afterUpdate.get().getEndTime() == 3000000L);
        assertTest("update() → success thay đổi", afterUpdate.isPresent() && !afterUpdate.get().isSuccess());

        // Test deleteById
        boolean deleted = repo.deleteById("TXN-TEST-001");
        assertTest("deleteById() trả về true", deleted);
        assertTest("deleteById() → không còn tồn tại", !repo.findById("TXN-TEST-001").isPresent());

        // Test clearAll
        repo.save(txn);
        repo.clearAll();
        assertTest("clearAll() → count = 0", repo.count() == 0);

        // Cleanup file test
        new File(testFile).delete();
    }

    // ======================== PERFORMANCE TEST ========================

    private static void testPerformance() {
        printSection("PERFORMANCE — Đọc 10k dòng");

        // Test đọc products.csv (5000 dòng)
        ProductRepository productRepo = new ProductRepository("data/products.csv");
        long start = System.currentTimeMillis();
        List<Product> products = productRepo.findAll();
        long elapsed = System.currentTimeMillis() - start;
        System.out.printf("    products.csv  : %,d dòng trong %,d ms%n", products.size(), elapsed);
        assertTest("Đọc 5000 products < 1 giây", elapsed < 1000);

        // Test đọc customers.csv (2000 dòng)
        CustomerRepository customerRepo = new CustomerRepository("data/customers.csv");
        start = System.currentTimeMillis();
        List<Customer> customers = customerRepo.findAll();
        elapsed = System.currentTimeMillis() - start;
        System.out.printf("    customers.csv : %,d dòng trong %,d ms%n", customers.size(), elapsed);
        assertTest("Đọc 2000 customers < 1 giây", elapsed < 1000);

        // Test đọc orders.csv (2500 dòng)
        OrderRepository orderRepo = new OrderRepository("data/orders.csv");
        start = System.currentTimeMillis();
        List<Order> orders = orderRepo.findAll();
        elapsed = System.currentTimeMillis() - start;
        System.out.printf("    orders.csv    : %,d dòng trong %,d ms%n", orders.size(), elapsed);
        assertTest("Đọc 2500 orders < 1 giây", elapsed < 1000);

        // Tổng
        int totalLines = products.size() + customers.size() + orders.size();
        System.out.printf("    TỔNG          : %,d dòng%n", totalLines);
        assertTest("Tổng đọc ≥ 9500 dòng", totalLines >= 9500);
    }

    // ======================== 4 LOCK MECHANISMS TEST ========================

    private static void testLockMechanisms() {
        printSection("4 CƠ CHẾ LOCK — FlashSaleItemRepository");

        // Copy flash_items.csv thành file tạm để test — không ảnh hưởng data gốc
        String testFile = "data/test_flash_items.csv";
        copyFile("data/flash_items.csv", testFile);

        FlashSaleItemRepository repo = new FlashSaleItemRepository(testFile);

        // Lấy item đầu tiên để test
        List<FlashSaleItem> all = repo.findAll();
        if (all.isEmpty()) {
            System.out.println("    ⚠ Không có flash item để test!");
            return;
        }

        FlashSaleItem testItem = all.get(0);
        String itemId = testItem.getFlashItemId();
        int limitedQty = testItem.getLimitedQty();
        System.out.printf("    Test item: %s (limitedQty=%d, soldQty=%d)%n",
                itemId, limitedQty, testItem.getSoldQty());

        // --- Test 1: NO_LOCK ---
        System.out.println();
        System.out.println("    --- 1. NO_LOCK ---");
        repo.resetAllSoldQty();
        try {
            repo.sellNoLock(itemId, 1);
            FlashSaleItem after = repo.findById(itemId).orElse(null);
            assertTest("NO_LOCK: sellNoLock() thành công", after != null && after.getSoldQty() == 1);
            assertTest("NO_LOCK: version tăng", after != null && after.getVersion() == 2);
        } catch (Exception e) {
            assertTest("NO_LOCK: sellNoLock() không lỗi", false);
            System.out.println("      Lỗi: " + e.getMessage());
        }

        // Test NO_LOCK OutOfStock
        repo.resetAllSoldQty();
        try {
            // Bán hết hàng
            FlashSaleItem item = repo.findById(itemId).orElse(null);
            if (item != null) {
                item.setSoldQty(item.getLimitedQty()); // set full
                item.setVersion(item.getVersion() + 1);
                repo.update(item);
            }
            repo.sellNoLock(itemId, 1); // should throw
            assertTest("NO_LOCK: OutOfStockException khi hết hàng", false);
        } catch (OutOfStockException e) {
            assertTest("NO_LOCK: OutOfStockException khi hết hàng", true);
        } catch (Exception e) {
            assertTest("NO_LOCK: OutOfStockException khi hết hàng", false);
        }

        // --- Test 2: SYNCHRONIZED ---
        System.out.println();
        System.out.println("    --- 2. SYNCHRONIZED ---");
        repo.resetAllSoldQty();
        try {
            repo.sellWithSynchronized(itemId, 2);
            FlashSaleItem after = repo.findById(itemId).orElse(null);
            assertTest("SYNCHRONIZED: sell(qty=2) thành công", after != null && after.getSoldQty() == 2);
            assertTest("SYNCHRONIZED: version tăng", after != null && after.getVersion() == 2);
        } catch (Exception e) {
            assertTest("SYNCHRONIZED: sell() không lỗi", false);
            System.out.println("      Lỗi: " + e.getMessage());
        }

        // --- Test 3: FILE_LOCK ---
        System.out.println();
        System.out.println("    --- 3. FILE_LOCK ---");
        repo.resetAllSoldQty();
        try {
            repo.sellWithFileLock(itemId, 3);
            FlashSaleItem after = repo.findById(itemId).orElse(null);
            assertTest("FILE_LOCK: sell(qty=3) thành công", after != null && after.getSoldQty() == 3);
            assertTest("FILE_LOCK: version tăng", after != null && after.getVersion() == 2);
        } catch (Exception e) {
            assertTest("FILE_LOCK: sell() không lỗi", false);
            System.out.println("      Lỗi: " + e.getMessage());
        }

        // --- Test 4: OPTIMISTIC LOCK ---
        System.out.println();
        System.out.println("    --- 4. OPTIMISTIC LOCK ---");
        repo.resetAllSoldQty();
        try {
            repo.sellWithOptimisticLock(itemId, 1);
            FlashSaleItem after = repo.findById(itemId).orElse(null);
            assertTest("OPTIMISTIC: sell(qty=1) thành công", after != null && after.getSoldQty() == 1);
            assertTest("OPTIMISTIC: version tăng", after != null && after.getVersion() == 2);
        } catch (Exception e) {
            assertTest("OPTIMISTIC: sell() không lỗi", false);
            System.out.println("      Lỗi: " + e.getMessage());
        }

        // --- Test BẤT BIẾN: soldQty <= limitedQty ---
        System.out.println();
        System.out.println("    --- BẤT BIẾN KIỂM TRA ---");
        repo.resetAllSoldQty();
        List<FlashSaleItem> finalAll = repo.findAll();
        boolean invariantHolds = finalAll.stream()
                .allMatch(item -> item.getSoldQty() <= item.getLimitedQty());
        assertTest("BẤT BIẾN: soldQty <= limitedQty cho tất cả items", invariantHolds);

        // Cleanup
        new File(testFile).delete();
    }

    // ======================== HELPER METHODS ========================

    private static void assertTest(String testName, boolean condition) {
        totalTests++;
        if (condition) {
            passedTests++;
            System.out.println("    ✅ " + testName);
        } else {
            failedTests++;
            System.out.println("    ❌ " + testName);
        }
    }

    private static void printSection(String name) {
        System.out.println();
        System.out.println("  ┌─────────────────────────────────────────────");
        System.out.println("  │ 🧪 " + name);
        System.out.println("  └─────────────────────────────────────────────");
    }

    /** Copy file đơn giản (dùng cho test isolation). */
    private static void copyFile(String src, String dest) {
        try {
            java.nio.file.Files.copy(
                    java.nio.file.Paths.get(src),
                    java.nio.file.Paths.get(dest),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        } catch (java.io.IOException e) {
            System.err.println("Lỗi copy file: " + e.getMessage());
        }
    }
}
