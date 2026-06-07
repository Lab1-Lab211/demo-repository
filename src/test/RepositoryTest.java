package test;

import model.Product;
import model.enums.ProductCategory;
import repository.ProductRepository;

import java.io.File;
import java.util.List;
import java.util.Optional;

/**
 * Unit Test cho CRUD Repository
 */
public class RepositoryTest {

    private static int tongTest = 0;
    private static int tongPass = 0;
    private static int tongFail = 0;

    private static final String TEST_FILE = "data/temp_test_repo.csv";

    public static void main(String[] args) {
        System.out.println("╔══════════════════════════════════════════════════════════╗");
        System.out.println("║   🧪 UNIT TEST — CRUD REPOSITORY (CSV)                 ║");
        System.out.println("╚══════════════════════════════════════════════════════════╝");
        System.out.println();

        // Xóa file cũ nếu có
        new File(TEST_FILE).delete();

        ProductRepository repo = new ProductRepository(TEST_FILE);

        // 1. Test Create (save)
        printHeader("TEST CREATE");
        Product p1 = new Product("PRD-001", "Tai nghe A", ProductCategory.DIEN_TU, 500000.0, 10, 1);
        Product p2 = new Product("PRD-002", "Tai nghe B", ProductCategory.DIEN_TU, 200000.0, 5, 1);
        repo.save(p1);
        repo.save(p2);
        
        long count = repo.count();
        assertLongEqual("Create: save 2 entities, count must be 2", 2, count);

        // 2. Test Read (findById, findAll)
        printHeader("TEST READ");
        Optional<Product> found = repo.findById("PRD-001");
        assertTrue("Read: findById must return present for PRD-001", found.isPresent());
        if (found.isPresent()) {
            assertEqual("Read: name must match", "Tai nghe A", found.get().getName());
        }
        
        Optional<Product> notFound = repo.findById("PRD-999");
        assertFalse("Read: findById must return empty for PRD-999", notFound.isPresent());

        List<Product> all = repo.findAll();
        assertLongEqual("Read: findAll size must be 2", 2, all.size());

        // 3. Test Update
        printHeader("TEST UPDATE");
        Product updatedP1 = new Product("PRD-001", "Tai nghe A Updated", ProductCategory.DIEN_TU, 550000.0, 15, 2);
        repo.update(updatedP1);
        
        Optional<Product> afterUpdate = repo.findById("PRD-001");
        assertTrue("Update: findById must return present", afterUpdate.isPresent());
        if (afterUpdate.isPresent()) {
            assertEqual("Update: name must be updated", "Tai nghe A Updated", afterUpdate.get().getName());
            assertDoubleEqual("Update: price must be updated", 550000.0, afterUpdate.get().getOriginalPrice());
        }

        // 4. Test Delete
        printHeader("TEST DELETE");
        boolean deleted = repo.deleteById("PRD-002");
        assertTrue("Delete: deleteById must return true for existing entity", deleted);
        
        assertLongEqual("Delete: count must be 1 after deletion", 1, repo.count());
        
        boolean deletedAgain = repo.deleteById("PRD-002");
        assertFalse("Delete: deleteById must return false for already deleted entity", deletedAgain);

        // Cleanup
        new File(TEST_FILE).delete();

        // ======= TỔNG KẾT =======
        System.out.println();
        System.out.println("═══════════════════════ TỔNG KẾT ═══════════════════════");
        System.out.printf("  Tổng test : %d%n", tongTest);
        System.out.printf("  ✅ Pass   : %d%n", tongPass);
        System.out.printf("  ❌ Fail   : %d%n", tongFail);
        System.out.println("═════════════════════════════════════════════════════════");

        if (tongFail > 0) {
            System.exit(1);
        }
    }

    // ======================== ASSERTION HELPERS ========================

    private static void assertEqual(String testName, Object expected, Object actual) {
        tongTest++;
        if (expected == null && actual == null) {
            tongPass++;
            printPass(testName);
        } else if (expected != null && expected.equals(actual)) {
            tongPass++;
            printPass(testName);
        } else {
            tongFail++;
            printFail(testName, String.valueOf(expected), String.valueOf(actual));
        }
    }

    private static void assertLongEqual(String testName, long expected, long actual) {
        tongTest++;
        if (expected == actual) {
            tongPass++;
            printPass(testName);
        } else {
            tongFail++;
            printFail(testName, String.valueOf(expected), String.valueOf(actual));
        }
    }

    private static void assertDoubleEqual(String testName, double expected, double actual) {
        tongTest++;
        if (Math.abs(expected - actual) < 0.01) {
            tongPass++;
            printPass(testName);
        } else {
            tongFail++;
            printFail(testName, String.valueOf(expected), String.valueOf(actual));
        }
    }

    private static void assertTrue(String testName, boolean condition) {
        tongTest++;
        if (condition) {
            tongPass++;
            printPass(testName);
        } else {
            tongFail++;
            printFail(testName, "true", "false");
        }
    }

    private static void assertFalse(String testName, boolean condition) {
        tongTest++;
        if (!condition) {
            tongPass++;
            printPass(testName);
        } else {
            tongFail++;
            printFail(testName, "false", "true");
        }
    }

    private static void printHeader(String section) {
        System.out.println("  ┌─────────────────────────────────────────────┐");
        System.out.printf("  │  📦 %-40s │%n", section);
        System.out.println("  └─────────────────────────────────────────────┘");
    }

    private static void printPass(String testName) {
        System.out.printf("    ✅ %s%n", testName);
    }

    private static void printFail(String testName, String expected, String actual) {
        System.out.printf("    ❌ %s%n", testName);
        System.out.printf("       Expected: %s%n", expected);
        System.out.printf("       Actual  : %s%n", actual);
    }
}
