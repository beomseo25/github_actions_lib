import java.sql.*;
import java.util.*;

public class LibraryRepository {
    // [수정 완료] 옛날 외부 IP를 도커 컨테이너가 호스트 PC의 DB를 바라볼 수 있도록 가상 주소로 변경
    private final String URL = "jdbc:mariadb://host.docker.internal:3306/library";
    private final String USER = "cjulib";
    private final String PASSWORD = "security";

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
     * [수정 완료] DB 테이블 생성 스크립트와 일치하도록 'member_id'를 'borrower_id'로 일괄 변경했습니다.
     */
    public void saveBooks(Map<Integer, Book> bookMap) {
        String sql = "INSERT INTO books (book_id, title, author, is_available, borrower_id) " +
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
     * [수정 완료] rs.getString("member_id") 대신 테이블 구조에 맞게 "borrower_id"를 읽도록 수정했습니다.
     */
    public Map<Integer, Book> loadBooks() {
        Map<Integer, Book> bookMap = new HashMap<>();
        String sql = "SELECT * FROM books";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                int id = rs.getInt("book_id");
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
     * [수정 완료] SQL Injection 실습을 위해 의도적으로 취약하게 짰던 부분의 구동 오류를 해결했습니다.
     * 기존 코드처럼 쿼리문은 문자열 결합문인데 pstmt.setString()이 남아있으면 드라이버 레벨에서 Parameter index 에러가 터집니다.
     * 취약점 테스트가 올바르게 작동할 수 있도록 순성 Statement 방식으로 코드를 정상화했습니다.
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