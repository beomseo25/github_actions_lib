-- 1. 사용자(users) 테이블 생성
CREATE TABLE IF NOT EXISTS users (
    user_id VARCHAR(50) PRIMARY KEY,
    password VARCHAR(50),
    type VARCHAR(20)
);

-- 2. 도서(books) 테이블 생성
CREATE TABLE IF NOT EXISTS books (
    id INT PRIMARY KEY,
    title VARCHAR(100),
    author VARCHAR(100),
    is_available BOOLEAN,
    borrower_id VARCHAR(50)
);

-- 3. 테스트 환경 고립을 위해 기존 데이터가 있다면 싹 지우기 (초기화)
DELETE FROM books;
DELETE FROM users;

-- 4. 테스트에 필수적인 기본 계정 2개(관리자, 일반유저) 미리 발급
INSERT INTO users (user_id, password, type) VALUES ('admin', '1111', 'ADMIN');
INSERT INTO users (user_id, password, type) VALUES ('user', '2222', 'USER');
