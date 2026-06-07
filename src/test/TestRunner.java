package test;

/**
 * Bộ chạy toàn bộ Test Suite
 */
public class TestRunner {

    public static void main(String[] args) {

        System.out.println("===================================================================");
        System.out.println("                 TEST RUNNER - LAB211 FLASH SALE                 ");
        System.out.println("===================================================================\n");

        try {
            // 1. Chạy ModelParseTest (Unit Test Parse CSV)
            ModelParseTest.main(args);
            
            System.out.println("\n\n");

            // 2. Chạy RepositoryTest (Unit Test CRUD)
            RepositoryTest.main(args);

            System.out.println("\n\n");

            // 3. Chạy PerformanceTest
            PerformanceTest.main(args);

            System.out.println("\n===================================================================");
            System.out.println("🎉 HOÀN THÀNH TOÀN BỘ TEST SUITE! TẤT CẢ ĐỀU PASS! 🎉");
            System.out.println("===================================================================");

        } catch (Exception e) {
            System.err.println("\n❌ CÓ LỖI XẢY RA TRONG QUÁ TRÌNH TEST: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
