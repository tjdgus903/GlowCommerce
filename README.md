# Mini Commerce Lab

대용량 주문 트래픽 환경을 가정하여  
**PostgreSQL → Outbox → Kafka → Elasticsearch → Redis** 로 이어지는  
비동기 주문 처리 및 조회 파이프라인을 설계·구현·검증한 프로젝트입니다.

---

## 🔄 End-to-End Data Flow

이 프로젝트의 핵심은  
**RDB에서 발생한 데이터 변경이 이벤트를 통해 비동기 파이프라인으로 전달되고,  
조회 시에는 Cache → Search Engine 순으로 소비되는 전체 흐름**을 명확히 분리하는 것입니다.

---

### 1️. 주문 생성 (Write Path)

① Client가 `POST /orders` 요청을 전송

② Spring Boot 애플리케이션은 PostgreSQL 트랜잭션 안에서 다음 작업을 수행
   - `orders` 테이블에 주문 데이터 INSERT
   - `outbox_events` 테이블에 이벤트 데이터 INSERT (`status = NEW`)
     
③ 트랜잭션 커밋 시 주문 데이터와 이벤트 데이터가 원자적으로 저장됨
```text
orders
outbox_events (NEW)
```
이 단계에서는 Kafka, Elasticsearch를 직접 호출하지 않으며
주문 생성의 정합성은 RDB 트랜잭션으로만 보장한다.

### 2. Outbox → Kafka 이벤트 발행
④ OutboxPublisher가 주기적으로 outbox_events 테이블을 조회

⑤ status = NEW 인 이벤트를 Kafka로 발행

⑥ 발행 성공 시 status = SENT 로 상태 변경
```bash
outbox_events (NEW → SENT)
```
Kafka 장애 또는 네트워크 오류가 발생하더라도 이벤트는 DB에 남아 재처리가 가능하다.

### 3️. Kafka Consumer → Elasticsearch 색인
⑦ Kafka Consumer가 order.created 토픽을 consume

⑧ 이벤트 데이터를 기반으로 Elasticsearch orders index에 문서 저장

⑨ 색인은 Bulk 방식으로 처리하여 처리량을 확보
```bash
Kafka → Elasticsearch (orders index)
```
이 시점부터 주문 데이터는 검색 및 조회가 가능해진다.

### 4️. 주문 조회 (Read Path + Cache)
⑩ Client가 GET /search/orders 요청

⑪ 서버는 Redis 캐시를 먼저 조회
   - Cache HIT → Redis에서 즉시 응답
   - Cache MISS → Elasticsearch 조회

⑫ Elasticsearch 조회 결과를 Redis에 TTL 기반으로 저장 후 Client에 응답
```bash
Redis (HIT)
Redis (MISS) → Elasticsearch → Redis (TTL) → Response
```
반복 조회 트래픽은 Redis가 흡수하며
Elasticsearch는 Cache MISS 시에만 접근한다.

---

## Architecture Overview

### Write Path
POST /orders  
→ PostgreSQL (orders + outbox_events)  
→ OutboxPublisher  
→ Kafka (order.created)  
→ Consumer  
→ Elasticsearch (orders index)

### Read Path
GET /search/orders  
→ Redis Cache  
→ (MISS) Elasticsearch → Redis TTL 저장  
→ Response

---

## Key Features
- Outbox 패턴을 통한 이벤트 유실 방지
- Redis 기반 Idempotency (중복 주문 방지)
- Kafka 비동기 처리
- Elasticsearch 검색 모델 분리
- Redis 캐시로 조회 성능 최적화
- Actuator/Micrometer 기반 관측 지표
- k6 부하 테스트로 성능 검증

---

## Tech Stack
- Kotlin, Spring Boot
- PostgreSQL 16
- Kafka, Elasticsearch
- Redis
- Docker / Docker Compose
- k6

---

## How to Run

### 1. Infra (VM)
```bash

[root@vbox infra]# docker compose up -d
[+] Running 6/6
 ✔ Network infra_default        Created                                                                                                                                                                                            0.4s
 ✔ Container mcl-zookeeper      Started                                                                                                                                                                                            1.5s
 ✔ Container mcl-elasticsearch  Started                                                                                                                                                                                            1.5s
 ✔ Container mcl-redis          Started                                                                                                                                                                                            1.9s
 ✔ Container mcl-postgres       Started                                                                                                                                                                                            2.2s
 ✔ Container mcl-kafka          Started                                                                                                                                                                                            2.2s
[root@vbox infra]# docker ps
CONTAINER ID   IMAGE                                                  COMMAND                   CREATED         STATUS                            PORTS                                                           NAMES
1bd160c7e020   confluentinc/cp-kafka:7.6.1                            "/etc/confluent/dock…"   5 seconds ago   Up 2 seconds                      0.0.0.0:9092->9092/tcp, :::9092->9092/tcp                       mcl-kafka
bc33f5505b05   redis:7                                                "docker-entrypoint.s…"   5 seconds ago   Up 3 seconds                      0.0.0.0:6379->6379/tcp, :::6379->6379/tcp                       mcl-redis
441772590a4f   postgres:16                                            "docker-entrypoint.s…"   5 seconds ago   Up 2 seconds (health: starting)   0.0.0.0:5432->5432/tcp, :::5432->5432/tcp                       mcl-postgres
99c9e85ea17c   confluentinc/cp-zookeeper:7.6.1                        "/etc/confluent/dock…"   5 seconds ago   Up 3 seconds                      2888/tcp, 0.0.0.0:2181->2181/tcp, :::2181->2181/tcp, 3888/tcp   mcl-zookeeper
c70bd2cd23a8   docker.elastic.co/elasticsearch/elasticsearch:8.13.4   "/bin/tini -- /usr/l…"   5 seconds ago   Up 3 seconds                      0.0.0.0:9200->9200/tcp, :::9200->9200/tcp, 9300/tcp             mcl-elasticsearch
[root@vbox infra]#


## DB 접속
[root@vbox infra]# docker exec -it mcl-postgres bash
root@441772590a4f:/# psql mcl mcl
psql (16.11 (Debian 16.11-1.pgdg13+1))
Type "help" for help.

mcl=# \dt
           List of relations
 Schema |     Name      | Type  | Owner
--------+---------------+-------+-------
 public | orders        | table | mcl
 public | outbox_events | table | mcl
 public | products      | table | mcl
 public | skus          | table | mcl
(4 rows)

mcl=#


## kafka 조회
[root@vbox infra]# docker exec -it mcl-kafka kafka-topics \
>   --bootstrap-server localhost:9092 \
>   --list
__consumer_offsets
order.created
[root@vbox infra]#


## redis 조회
[root@vbox k6]# docker exec -it mcl-redis redis-cli
127.0.0.1:6379> keys *
(empty array)
127.0.0.1:6379> get cache:search:orders:*
(nil)
127.0.0.1:6379> ttl cache:search:orders:*
(integer) -2
127.0.0.1:6379>
```

### 2. 과부화 테스트
```bash

[root@vbox k6]# docker run --rm -i   -e BASE_URL=http://192.168.56.1:8080   -v "$(pwd)":/scripts   grafana/k6 run /scripts/orders_test.js

         /\      Grafana   /‾‾/
    /\  /  \     |\  __   /  /
   /  \/    \    | |/ /  /   ‾‾\
  /          \   |   (  |  (‾)  |
 / __________ \  |_|\_\  \_____/

     execution: local
        script: /scripts/orders_test.js
        output: -

     scenarios: (100.00%) 2 scenarios, 20 max VUs, 2m5s max duration (incl. graceful stop):
              * warmup: Up to 5 looping VUs for 30s over 3 stages (gracefulRampDown: 5s, gracefulStop: 30s)
              * main: 20 looping VUs for 1m0s (startTime: 35s, gracefulStop: 30s)
```

<img width="589" height="357" alt="image" src="https://github.com/user-attachments/assets/08e61639-95c2-4ab1-96f9-f53372b17145" />
<img width="539" height="344" alt="image" src="https://github.com/user-attachments/assets/6b2e72d5-1bd2-412a-bb19-3803dc4d3074" />
<img width="583" height="362" alt="image" src="https://github.com/user-attachments/assets/20a23d76-3796-436c-a266-420cd81bcbab" />
<img width="532" height="344" alt="image" src="https://github.com/user-attachments/assets/c8d60e07-3415-4805-85ca-d68cfc8cfece" />
![Uploading image.png…]()


