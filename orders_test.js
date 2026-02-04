import http from "k6/http";
import { check, sleep } from "k6";
import { randomIntBetween, uuidv4 } from "https://jslib.k6.io/k6-utils/1.4.0/index.js";

export const options = {
  scenarios: {
    // 1) 워밍업: 가볍게
    warmup: {
      executor: "ramping-vus",
      startVUs: 0,
      stages: [
        { duration: "10s", target: 5 },
        { duration: "10s", target: 5 },
        { duration: "10s", target: 0 },
      ],
      gracefulRampDown: "5s",
    },

    // 2) 본 테스트: 주문 70% + 검색 30%
    main: {
      executor: "constant-vus",
      vus: 20,
      duration: "60s",
      startTime: "35s",
    },
  },

  thresholds: {
    http_req_failed: ["rate<0.01"],          // 실패율 1% 미만 목표
    http_req_duration: ["p(95)<800"],        // p95 800ms 미만 목표(학습용 기준)
  },
};

const BASE_URL = __ENV.BASE_URL || "http://host.docker.internal:8080";

function postOrder() {
  const idempotencyKey = `k6-${uuidv4()}`;
  const skuId = 1; // seed된 sku 사용
  const qty = randomIntBetween(1, 3);

  const payload = JSON.stringify({
    skuId: skuId,
    quantity: qty,
    idempotencyKey: idempotencyKey,
  });

  const params = {
    headers: {
      "Content-Type": "application/json",
      "X-Correlation-Id": `k6-${idempotencyKey}`,
    },
    timeout: "10s",
  };

  const res = http.post(`${BASE_URL}/orders`, payload, params);

  check(res, {
    "POST /orders status 200": (r) => r.status === 200,
  });

  // 응답에서 orderId 뽑아서 search에 활용할 수도 있지만, 일단 단순하게 간다.
}

function searchOrders() {
  // userId=1로 검색(consumer가 ES 색인 완료 전이면 결과가 비어도 OK)
  const res = http.get(`${BASE_URL}/search/orders?userId=1`, { timeout: "10s" });

  check(res, {
    "GET /search/orders status 200": (r) => r.status === 200,
  });
}

export default function () {
  // 70% write, 30% read
  const r = randomIntBetween(1, 10);
  if (r <= 7) postOrder();
  else searchOrders();

  sleep(0.2); // 너무 빡세게만 돌리면 초보 단계에서 분석이 어려워서 약간 텀
}

