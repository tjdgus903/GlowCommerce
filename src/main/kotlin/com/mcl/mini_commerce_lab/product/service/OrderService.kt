package com.mcl.mini_commerce_lab.product.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.mcl.mini_commerce_lab.common.error.NotFoundException
import com.mcl.mini_commerce_lab.product.api.dto.CreateOrderRequest
import com.mcl.mini_commerce_lab.product.api.dto.CreatedOrderResponse
import com.mcl.mini_commerce_lab.product.domain.Order
import com.mcl.mini_commerce_lab.product.domain.OrderStatus
import com.mcl.mini_commerce_lab.product.domain.OutboxEvent
import com.mcl.mini_commerce_lab.product.repository.OrderRepository
import com.mcl.mini_commerce_lab.product.repository.OutboxRepository
import com.mcl.mini_commerce_lab.product.repository.SkuRepository
import org.slf4j.MDC
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration

@Service
class OrderService(
    private val orderRepository: OrderRepository,
    private val skuRepository: SkuRepository,
    private val outboxRepository: OutboxRepository,
    private val objectMapper: ObjectMapper,
    private val orderQueryService: OrderQueryService,
    private val redisTemplate: StringRedisTemplate,
) {
    companion object{
        /**
         * IDEM_TTL : 멱등키(idempotencyKey) 처리 기록 유지 시간
         * - 120초 동안 같은 idempotencyKey로 요청하면 중복 주문 생성이 방지됨
         * - 너무 짧으면 네트워크 재시도 시 중복 생성 위험
         * - 너무 길면 Redis 메모리 사용 증가
         */
        private val IDEM_TTL = Duration.ofSeconds(120)

        /**
         * Redis Key 규칙
         * idem:orders:<idempotencyKey>
         *
         * ex) idempotencyKey = "abc-123"
         * -> "idem:orders:abc-123"
         *
         * 이렇게 계층형으로 만들면
         * - 의미가 명확하고
         * - SCAN/MATCH 로 관리하기 쉬움
         */
        fun idemKey(idem:String) = "idem:orders:$idem"
    }

    /**
     * 주문 생성(멱등 처리 포함)
     *
     * 목표
     * - 같은 idempotencyKey로 요청이 여러 번 들어와도 "주문 1개만" 생성이 됨
     * - 중복 요청은 기존 생성 결과를 그대로 반환
     */
    @Transactional
    fun createOrder(req: CreateOrderRequest): CreatedOrderResponse{
        // 0) 요청값 검증(req 의 필드가 nullable 이라서 직접 체크)
        val skuId = req.skuId ?: throw IllegalArgumentException("skuId is required")
        val quantity = req.quantity ?: throw IllegalArgumentException("quantity is required")

        // 1) SKU 존재 여부 체크(존재하지 않으면 404 처리)
        if(!skuRepository.existsById(skuId)) throw NotFoundException("Sku not found. skuId=$skuId")

        // correlationId 는 로그 추적/분산 추적용(없으면 null 일 수 있음)
        val correlationId = MDC.get("correlationId")
        val userId = 1L     // 서비스 적용 시 실제 로그인 사용자로 교체

        // 2) Redis 에서 사용할 멱등 키 생성
        val idemRedisKey = idemKey(req.idempotencyKey)

        // 3) Redis 락(처리권) 획득 시도
        // setIfAbsent = SETNX
        // - 키가 없을 때만 "processing" 값을 세팅하고 true 반환
        // - 이미 키가 있으면 false 반환(즉, 누군가 처리 중이거나 이미 처리 완료)
        val acquired = redisTemplate.opsForValue()
            .setIfAbsent(idemRedisKey, "processing", IDEM_TTL) == true

        // 4) 처리권 획득 실패 = 이미 누군가 처리 중/완료
        if (!acquired){
            // 4-1) Redis 값 확인
            // - 처리 완료된 경우: "orderId:<숫자>" 형태로 저장해뒀다고 가정
            val v = redisTemplate.opsForValue().get(idemRedisKey)

            // 4-2) 이미 주문 생성 완료된 경우 -> 바로 결과 반환
            if(v != null && v.startsWith("orderId:")){
                val orderId = v.removePrefix("orderId:").toLong()

                // status 는 상황에 따라 DB에서 정확히 읽어오고 싶으면 조회해서 채워도 됨
                return CreatedOrderResponse(orderId, "CREATED", correlationId)
            }

            // 4-3) Redis 값이 "processing" 이거나 애매한 값이면
            //      DB 를 조회해서 이미 생성된 주문이 있는지 확인(fallback)
            val existing = orderQueryService.findByIdempotencyKeyOrNull(req.idempotencyKey)
            if (existing != null){
                // 4-4) DB에 이미 존재하면 Redis에 결과를 저장(캐싱)해두고
                //      다음 중복 호출에서 더 빠르게 반환 가능
                redisTemplate.opsForValue().set(idemRedisKey, "orderId:${existing.id}", IDEM_TTL)
                return CreatedOrderResponse(existing.id!!, existing.status.name, existing.correlationId)
            }

            // 4-5) 여기까지 오면
            // - Redis는 processing 이라고 했는데 DB에 없거나
            // - 아주 타이밍이 안맞는 캐이스일 수 있음
            // -> 클라이언트에게 "지금 처리 중" 이라고 409를 보내고 재시도 유도
            throw IllegalStateException("Order is being processed. idempotencyKey=${req.idempotencyKey}")
        }

        // 5) 처리권을 얻은 상태(처리 필요)
        // 5-1) DB 에 이미 같은 멱등키 주문이 있다면 그대로 반환(이중 안전장치)
        orderRepository.findByIdempotencyKey(req.idempotencyKey).orElse(null)?.let { existing ->
            redisTemplate.opsForValue().set(idemRedisKey, "orderId:${existing.id}", IDEM_TTL)
            return CreatedOrderResponse(
                orderId = existing.id!!,
                status = existing.status.name,
                correlationId = existing.correlationId,
            )
        }

        // 6) 실제 주문 생성 시도
        return try{
            val order = orderRepository.save(
                Order(
                    userId = userId,
                    skuId = skuId,
                    quantity = quantity,
                    status = OrderStatus.CREATED,
                    idempotencyKey = req.idempotencyKey,
                    correlationId = correlationId,
                )
            )

            // 7) Outbox 패턴 : "주문 생성 이벤트" 를 DB에 함께 저장
            // - Kafka 에 바로 보내지 않고 Outbox 테이블에 적재
            // - 별도 배치/컨슈머가 Outbox를 읽어서 Kafka 로 publish
            val payload = objectMapper.createObjectNode().apply {
                put("orderId", order.id!!)
                put("skuId", skuId)
                put("quantity", quantity)
                put("userId", userId)
                put("correlationId", correlationId)
                put("eventType", "ORDER_CREATED")
            }

            outboxRepository.save(
                OutboxEvent(
                    aggregateType = "ORDER",
                    aggregateId = order.id.toString(),
                    eventType = "ORDER_CREATED",
                    payload = payload,
                    correlationId = correlationId,
                )
            )

            // 8) Redis 에 "완료 결과" 저장
            // - 다음에 같은 멱등키로 들어오면 Redis에서 바로 orderId를 꺼내 반환 가능
            redisTemplate.opsForValue().set(idemRedisKey, "orderId:${order.id}", IDEM_TTL)

            // 9) 최종 응답 반환
            CreatedOrderResponse(
                orderId = order.id!!,
                status = order.status.name,
                correlationId= correlationId
                )
        } catch (e: DataIntegrityViolationException){
            // 10) 레이스 컨디션:
            // - 동시에 2개 트랜잭션이 insert하려다 unique constraint로 한쪽이 실패할 수 있음
            // - 이때는 "실패한 쪽" 이 DB에서 기존 주문을 조회해서 결과를 회수(recover)
            val existing = orderQueryService.findByIdempotencyKeyOrNull(req.idempotencyKey) ?: throw e

            // 회수한 결과를 Redis 에 저장하면 다음 호출이 빨라짐
            redisTemplate.opsForValue().set(idemRedisKey, "orderId:${existing.id}", IDEM_TTL)

            CreatedOrderResponse(
                existing.id!!,
                existing.status.name,
                existing.correlationId
            )
        }
    }
}