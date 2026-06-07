package test;

import model.Product;
import model.enums.ProductCategory;
import repository.ProductRepository;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Performance Test
 * Mục tiêu: Đo thời gian đọc 10,000 dòng CSV < 1 giây
 */
public class PerformanceTest {

    private static final String BENCHMARK_FILE = "data/benchmark_10k.csv";
    private static final int TARGET_ROWS = 10000;

    public static void main(String[] args) {
        System.out.println("╔══════════════════════════════════════════════════════════╗");
        System.out.println("║   🚀 PERFORMANCE TEST — BENCHMARK 10,000 DÒNG          ║");
        System.out.println("╚══════════════════════════════════════════════════════════╝");
        System.out.println();

        // 1. Chuẩn bị dữ liệu (Data prep)
        System.out.println("  [1/3] Đang chuẩn bị dữ liệu test (10,000 dòng)...");
        File file = new File(BENCHMARK_FILE);
        if (file.exists()) {
            file.delete();
        }

        ProductRepository repo = new ProductRepository(BENCHMARK_FILE);
        List<Product> products = new ArrayList<>();
        for (int i = 1; i <= TARGET_ROWS; i++) {
            products.add(new Product(
                    String.format("PRD-%05d", i),
                    "Sản phẩm test " + i,
                    ProductCategory.DIEN_TU,
                    100000.0,
                    10,
                    1
            ));
        }
        repo.saveAll(products); // Bulk insert
        System.out.println("    ✅ Đã tạo file " + BENCHMARK_FILE + " với " + repo.count() + " dòng.\n");

        // 2. Warm-up JVM (Khởi động JVM để compile JIT)
        System.out.println("  [2/3] Đang warm-up JVM...");
        for (int i = 0; i < 3; i++) {
            repo.findAll();
        }
        System.out.println("    ✅ Đã warm-up xong.\n");

        // 3. Thực hiện đo Performance
        System.out.println("  [3/3] Bắt đầu đo hiệu năng đọc 10,000 dòng...");
        long start = System.currentTimeMillis();
        
        List<Product> readProducts = repo.findAll();
        
        long end = System.currentTimeMillis();
        long elapsed = end - start;

        System.out.println("    📊 Số dòng đã đọc: " + readProducts.size());
        System.out.println("    ⏱ Thời gian đọc : " + elapsed + " ms");

        // 4. Assertion (Kiểm tra pass)
        System.out.println("\n══════════════════════ KẾT QUẢ ══════════════════════");
        if (elapsed < 1000 && readProducts.size() == TARGET_ROWS) {
            System.out.println("  ✅ PASS: Đọc 10,000 dòng mất < 1 giây (" + elapsed + " ms)");
        } else {
            System.out.println("  ❌ FAIL: Thời gian đọc (" + elapsed + " ms) vượt quá 1 giây hoặc số dòng không đủ!");
            System.exit(1);
        }
        System.out.println("═════════════════════════════════════════════════════\n");
    }
}
