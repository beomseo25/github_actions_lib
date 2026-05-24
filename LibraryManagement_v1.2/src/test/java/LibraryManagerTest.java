import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.sql.*;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LibraryManagerTest {

    private LibraryManager manager;
    private LibraryRepository repository;
    private User currentUser;

    @BeforeEach
    void setUp() {
        // 독립적인 테스트 환경 구축을 위해 가상 데이터베이스 초기화 진행
        clearTables();

        repository = new LibraryRepository();
        manager = new LibraryManager(repository);

        manager.initialize();

        manager.getBookMap().clear();
        manager.addBook("테스트 자바", "저자A");
    }

    private void clearTables() {
        try (Connection conn = DriverManager.getConnection(
                "jdbc:mariadb://localhost:3306/library", "cjulib", "security");
             Statement stmt = conn.createStatement()) {

            String createUsersTable = "CREATE TABLE IF NOT EXISTS users (" +
                                      "user_id VARCHAR(50) PRIMARY KEY, " +
                                      "password VARCHAR(50), " +
                                      "type VARCHAR(20))";
                                      
            String createBooksTable = "CREATE TABLE IF NOT EXISTS books (" +
                                      "id INT PRIMARY KEY, " +
                                      "title VARCHAR(100), " +
                                      "author VARCHAR(100), " +
                                      "is_available BOOLEAN, " +
                                      "borrower_id VARCHAR(50))";
                                      
            stmt.executeUpdate(createUsersTable);
            stmt.executeUpdate(createBooksTable);

            stmt.executeUpdate("DELETE FROM books");
            stmt.executeUpdate("DELETE FROM users");

            // 권한 제어 취약점 검증에 필요한 사용자 및 관리자 데이터 확보
            stmt.executeUpdate("INSERT INTO users (user_id, password, type) VALUES ('admin', '1111', 'ADMIN')");
            stmt.executeUpdate("INSERT INTO users (user_id, password, type) VALUES ('user', '2222', 'USER')");

        } catch (SQLException e) {
            System.err.println("DB 초기화 실패: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("로그인 성공 및 실패 테스트")
    void login() {
        assertTrue(manager.login("admin", "1111"), "관리자 로그인이 성공해야 합니다.");
        assertFalse(manager.login("admin", "wrong"), "비밀번호가 틀리면 실패해야 합니다.");
    }

    @Test
    @DisplayName("현재 로그인한 사용자 정보 확인")
    void getCurrentUser() {
        manager.login("admin", "1111");
        User user = manager.getCurrentUser();

        assertNotNull(user);
        assertEquals("admin", user.getUserId());
        assertTrue(user.isAdmin());
    }

    @Test
    @DisplayName("새로운 도서 등록 확인")
    void addBook() {
        int beforeSize = manager.getAllBooks().size();
        manager.addBook("새로운 책", "새로운 저자");

        assertEquals(beforeSize + 1, manager.getAllBooks().size());

        int target_id = manager.getBookCount();
        Book book = manager.getBookMap().get(target_id);
        assertEquals("새로운 책", book.getTitle());
    }

    @Test
    @DisplayName("도서 삭제 확인")
    void deleteBook() {
        int target_id = manager.getBookCount();
        boolean result = manager.deleteBook(target_id);

        assertTrue(result);
        assertNull(manager.getBookMap().get(target_id));
    }

    @Test
    @DisplayName("도서 대출 로직 확인")
    void borrowBook() {
        manager.login("user", "2222");

        int target_id = manager.getBookCount();
        boolean success = manager.borrowBook(target_id);
        assertTrue(success);
        assertFalse(manager.getBookMap().get(target_id).isAvailable());
        assertEquals("user", manager.getBookMap().get(target_id).getBorrowerId());

        boolean fail = manager.borrowBook(1);
        assertFalse(fail);
    }

    @Test
    @DisplayName("도서 반납 로직 확인")
    void returnBook() {
        manager.login("user", "2222");
        manager.borrowBook(1);

        int target_id = manager.getBookCount();
        manager.borrowBook(target_id);

        boolean result = manager.returnBook(target_id);
        assertTrue(result);
        assertTrue(manager.getBookMap().get(target_id).isAvailable());
        assertEquals("null", manager.getBookMap().get(target_id).getBorrowerId());
    }

    @Test
    @DisplayName("키워드 기반 도서 검색 확인")
    void searchBook() {
        manager.addBook("파이썬 입문", "저자B");

        List<Book> results = manager.searchBook("자바");
        assertEquals(1, results.size());
        assertEquals("테스트 자바", results.get(0).getTitle());
    }

    @Test
    @DisplayName("전체 도서 목록 반환 확인")
    void getAllBooks() {
        Collection<Book> books = manager.getAllBooks();
        assertNotNull(books);
        assertFalse(books.isEmpty());
    }

    @Test
    @DisplayName("보안 테스트: SQL Injection을 이용한 인증 우회")
    void loginSqlInjectionTest() {
        String attackId = "' OR 1=1 #";
        String attackPw = "wrong_password";

        boolean result = manager.login(attackId, attackPw);

        assertTrue(result, "취약점 발견: SQL Injection 페이로드로 인증이 우회되었습니다.");

        if (result) {
            System.out.println("[경고] SQL Injection 공격 성공: 유효하지 않은 계정으로 로그인되었습니다.");
        }
    }

    @Test
    @DisplayName("보안 테스트: OS Command Injection을 통한 임의 파일 생성")
    void osCommandInjectionTest() {
        String fileName = "vuln.txt";
        String payload;

        // 시스템 정보를 읽어와 운영체제 환경별 최적의 공격 구문을 선택합니다.
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            payload = "127.0.0.1 && echo hacked > " + fileName; 
        } else {
            payload = "127.0.0.1 ; echo hacked > " + fileName; 
        }

        manager.checkServerStatus(payload);

        File injectedFile = new File(fileName);
        boolean isVulnerable = injectedFile.exists();

        if (isVulnerable) {
            injectedFile.delete();
        }

        assertTrue(isVulnerable, "취약점 발견: OS 명령어가 주입되어 임의의 파일이 생성되었습니다.");

        if (isVulnerable) {
            System.out.println("[경고] OS Command Injection 공격 성공: 서버 내에서 임의 명령어가 실행되었습니다.");
        }
    }
}
