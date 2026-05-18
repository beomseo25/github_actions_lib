import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBconn {
    // 환경 변수(DB_URL, DB_USER, DB_PASSWORD)가 있으면 그것을 쓰고, 없으면 기존 기본값을 사용합니다.
    private static final String URL = getEnvOrProperty("DB_URL", "jdbc:mariadb://192.168.100.20:3306/library");
    private static final String USER = getEnvOrProperty("DB_USER", "cjulib");
    private static final String PASSWORD = getEnvOrProperty("DB_PASSWORD", "security");

    /**
     * 환경 변수 값을 읽어오는 헬퍼 메소드
     */
    private static String getEnvOrProperty(String key, String defaultValue) {
        String value = System.getenv(key);
        return (value != null && !value.isEmpty()) ? value : defaultValue;
    }

    /**
     * 데이터베이스 연결 객체를 반환합니다.
     * @return Connection 객체
     */
    public static Connection getConnection() {
        Connection conn = null;
        try {
            // 드라이버 명시적 로드
            Class.forName("org.mariadb.jdbc.Driver");

            // 연결 수행
            conn = DriverManager.getConnection(URL, USER, PASSWORD);
            System.out.println("[시스템] MariaDB 연결 성공!");

        } catch (ClassNotFoundException e) {
            System.err.println("[오류] 드라이버를 찾을 수 없습니다: " + e.getMessage());
        } catch (SQLException e) {
            System.err.println("[오류] DB 연결 실패: " + e.getMessage());
        }
        return conn;
    }

    // 연결 테스트용 main
    public static void main(String[] args) {
        Connection testConn = getConnection();
        if (testConn != null) {
            try {
                testConn.close(); 
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}
