import org.junit.jupiter.api.*;
import java.sql.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class LibraryRepositoryTest {

    private LibraryRepository repository;

    @BeforeEach
    void setUp() {
        repository = new LibraryRepository();
        
        // 각 테스트가 독립적으로 실행되도록 기존 데이터를 지우고 기본 계정만 세팅
        clearTables();
    }

    private void clearTables() {
        try (Connection conn = DriverManager.getConnection(
                "jdbc:mariadb://localhost:3306/library", "cjulib", "security");
             Statement stmt = conn.createStatement()) {

            // 테이블은 이미 schema.sql이 만들어 뒀으므로, 안의 내용물만 비웁니다.
            stmt.executeUpdate("DELETE FROM books");
            stmt.executeUpdate("DELETE FROM users");

            // 테스트를 위한 기본 사용자 다시 추가
            stmt.executeUpdate("INSERT INTO users (user_id, password, type) VALUES ('admin', '1111', 'ADMIN')");
            stmt.executeUpdate("INSERT INTO users (user_id, password, type) VALUES ('user', '2222', 'USER')");

        } catch (SQLException e) {
            System.err.println("테스트 환경 초기화 실패: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("DB 도서 데이터 저장 및 동기화 테스트 (saveBooks)")
    void saveBooks() {
        Map<Integer, Book> bookMap = new HashMap<>();
        bookMap.put(1, new Book(1, "DB 테스트 도서", "저자A", true, "null"));

        repository.saveBooks(bookMap);

        Map<Integer, Book> loadedMap = repository.loadBooks();
        assertTrue(loadedMap.containsKey(1), "ID 1번 도서가 DB에 존재해야 합니다.");

        Book savedBook = loadedMap.get(1);
        assertEquals("DB 테스트 도서", savedBook.getTitle());
        assertEquals("저자A", savedBook.getAuthor());
    }

    @Test
    @DisplayName("DB로부터 도서 데이터 로드 테스트 (loadBooks)")
    void loadBooks() {
        Map<Integer, Book> originalMap = new HashMap<>();
        originalMap.put(100, new Book(100, "SQL 입문", "저자B", false, "admin"));
        repository.saveBooks(originalMap);

        Map<Integer, Book> loadedMap = repository.loadBooks();

        assertNotNull(loadedMap);
        assertTrue(loadedMap.size() >= 1);

        Book loadedBook = loadedMap.get(100);
        assertEquals("SQL 입문", loadedBook.getTitle());
        assertEquals("저자B", loadedBook.getAuthor());
        assertFalse(loadedBook.isAvailable(), "대출 중 상태(false)가 유지되어야 합니다.");
        assertEquals("admin", loadedBook.getBorrowerId());
    }

    @Test
    @DisplayName("DB로부터 사용자 데이터 로드 테스트 (loadUsers)")
    void loadUsers() {
        User user = repository.loadUser("admin", "1111");

        assertNotNull(user, "조회된 사용자 객체는 null일 수 없습니다.");
        assertEquals("admin", user.getUserId(), "조회된 ID가 'admin'이어야 합니다.");
        assertEquals("ADMIN", user.getRole(), "사용자 권한이 'ADMIN'이어야 합니다.");
    }
}
