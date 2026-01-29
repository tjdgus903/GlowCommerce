# GlowCommerce

DB 접속 방법
DB 서버 IP : 192.168.56.112

① docker compose 실행
   
cd /root/mini-commerce-lab/infra

docker compose up -d

② docker db 확인

docker ps

③ 컨테이너 접속

docker exec -it f bash

④ db 접속

psql mcl mcl


