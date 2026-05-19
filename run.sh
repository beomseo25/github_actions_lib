#!/bin/bash

# 1. 오늘의 IP를 알아서 찾아서 변수에 저장해라! (awk는 첫 번째 숫자만 딱 잘라내는 마법의 명령어입니다)
MY_IP=$(hostname -I | awk '{print $1}')

echo "오늘의 리눅스 IP는 $MY_IP 입니다. 서버 구동을 시작합니다!"

# 2. 기존 도커 찌꺼기 삭제
docker rm -f hub-library-container

# 3. 아까 찾은 MY_IP 변수를 명령어에 자동으로 꽂아 넣고 실행!
docker run -d -p 8080:8080 \
  --name hub-library-container \
  --add-host=host.docker.internal:$MY_IP \
  beomseo25/library-app:latest

echo "서버 구동 완료! http://localhost:8080 으로 접속하세요."
