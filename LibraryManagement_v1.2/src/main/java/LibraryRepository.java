import java.sql.*;
import java.util.*;

public class LibraryRepository {
    // 🌟 [수정 완료] 깃허브 환경변수(DB_URL 등)가 있으면 그걸 쓰고, 없으면 기존 도커 주소를 씁니다.
    private final String URL = System.getenv().getOrDefault("DB_URL", "jdbc:mariadb://host.docker.internal:3306/library");
    private final String USER = System.getenv().getOrDefault("DB_USER", "cjulib");
    private final String PASSWORD = System.getenv().getOrDefault("DB_PASSWORD", "security");

    /**
     * MariaDB 연결을 위한 전용 메소드입니다.
     */
    private Connection getConnection() throws SQLException {
        try {
            Class.forName("org.mariadb.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("드라이버 로드 실패: " + e.getMessage());
        }
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    /**
     * 메모리의 모든 도서 정보를 MariaDB에 동기화(저장)합니다.
     * [수정 완료] schema.sql 구조와 완벽히 일치하도록 'book_id'를 'id'로 수정했습니다.
     */
    public void saveBooks(Map<Integer, Book> bookMap) {
        String sql = "INSERT INTO books (id, title, author, is_available, borrower_id) " +
                "VALUES (?, ?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE " +
                "title = VALUES(title), " +
                "author = VALUES(author), " +
                "is_available = VALUES(is_available), " +
                "borrower_id = VALUES(borrower_id)";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            for (Book book : bookMap.values()) {
                pstmt.setInt(1, book.getId());
                pstmt.setString(2, book.getTitle());
                pstmt.setString(3, book.getAuthor());
                pstmt.setBoolean(4, book.isAvailable());

                if (book.getBorrowerId() == null || "null".equals(book.getBorrowerId())) {
                    pstmt.setNull(5, java.sql.Types.VARCHAR);
                } else {
                    pstmt.setString(5, book.getBorrowerId());
                }
                pstmt.addBatch();
            }

            pstmt.executeBatch();
            System.out.println("[시스템] 모든 도서 데이터가 MariaDB에 동기화되었습니다.");

        } catch (SQLException e) {
            System.err.println("[오류] DB 저장(saveBooks) 실패: " + e.getMessage());
        }
    }

    /**
     * 데이터베이스로부터 모든 도서 정보를 조회하여 메모리에 로드합니다.
     * [수정 완료] schema.sql 구조와 일치하도록 rs.getInt("book_id")를 "id"로 수정했습니다.
     */
    public Map<Integer, Book> loadBooks() {
        Map<Integer, Book> bookMap = new HashMap<>();
        String sql = "SELECT * FROM books";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                int id = rs.getInt("id");
                String title = rs.getString("title");
                String author = rs.getString("author");
                boolean available = rs.getBoolean("is_available");
                String mid = rs.getString("borrower_id");

                bookMap.put(id, new Book(id, title, author, available, mid == null ? "null" : mid));
            }
        } catch (SQLException e) {
            System.err.println("[오류] 로드 실패: " + e.getMessage());
        }
        return bookMap;
    }

    /**
     * 사용자 로그인을 위한 정보를 조회합니다.
     * 취약점 테스트(SQL Injection)가 올바르게 작동할 수 있도록 순수 Statement 방식으로 유지합니다.
     */
    public User loadUser(String id, String pw) {
        String sql = "SELECT * FROM users WHERE user_id = '" + id + "' AND password = '" + pw + "'";

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                return new User(
                        rs.getString("user_id"),
                        rs.getString("password"),
                        rs.getString("type")
                );
            }
        } catch (SQLException e) {
            System.err.println("[오류] 로그인 조회 실패: " + e.getMessage());
        }
        return null;
    }
}
