#!/bin/bash

echo "도서 관리 시스템 서버를 종료합니다..."

# 실행 중인 컨테이너를 강제로 멈추고 삭제합니다.
docker rm -f hub-library-container

echo "서버가 안전하게 완전히 종료되었습니다! 🛑"
