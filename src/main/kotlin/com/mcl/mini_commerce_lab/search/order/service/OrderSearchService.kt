package com.mcl.mini_commerce_lab.search.order.service

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.mcl.mini_commerce_lab.observability.MclMetrics
import com.mcl.mini_commerce_lab.search.order.OrderSearchRepository
import com.mcl.mini_commerce_lab.search.order.api.dto.OrderSearchResponse
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration

/**
 * 주문 검색 서비스
 * - Elasticsearch 조회 전용
 * - CQRS 에서 Query Model 담당
 */
@Service
class OrderSearchService(
    // es 조회용 repository
    private val orderSearchRepository: OrderSearchRepository,
    // redis 문자열 기반 Template(key-value 형태)
    private val redisTemplate: StringRedisTemplate,
    // 객체 <-> JSON 변환용
    private val objectMapper: ObjectMapper,
    private val metrics: MclMetrics,
) {
    private val log = LoggerFactory.getLogger(this::class.java)

    companion object{
        // 캐시 유효 시간(Time To Life : TTL)
        private val TTL = Duration.ofSeconds(20)
        // Redis Key 생성 규칙
        fun userKey(userId: Long) = "cache:search:orders:user:$userId"
    }

    /**
     * 주문 검색 메서드
     * - userId / skuId / correlationId 중 조건에 맞게 조회
     * - userId 기준 검색 + Redis 캐싱 예제
     */
    @Transactional(readOnly = true)
    fun search(userId: Long?, skuId: Long?, correlationId: String?): List<OrderSearchResponse>{
        // 1) userId 가 있는 경우에만 처리
        if(userId != null){
            // Redis 에 저장할 Key
            // 같은 사용가자 같은 검색을 하면 항상 이 key 를 사용
            val key = userKey(userId)

            // 2) redis 캐시 먼저 조회
            redisTemplate.opsForValue().get(key)?.let { cached ->
                log.info("[CACHE] HIT key={}", key)
                metrics.cacheSearchHit.increment()
                // redis 에 값이 있으면(JSON 문자열)
                // => DB/ES 조회 없이 바로 반환
                return objectMapper.readValue(
                    cached,
                    object : TypeReference<List<OrderSearchResponse>>() {}
                )
            }

            // 3) Redis 에 값이 없으면 ES 조회
            log.info("[CACHE] MISS key={}", key)
            metrics.cacheSearchMiss.increment()
            val result = orderSearchRepository.findAllByUserId(userId)
                .map {
                    OrderSearchResponse(
                    orderId = it.orderId,
                    skuId = it.skuId,
                    quantity = it.quantity,
                    userId = it.userId,
                    status = it.status,
                    correlationId = it.correlationId,
                    createdAt = it.createdAt
                    )
                }

            // 4) 조회 결과를 Redis 에 저장
            // - JSON 문자열로 저장
            // - TTL 20초(너무 오래된 데이터 방지)
            redisTemplate.opsForValue().set(
                key,
                objectMapper.writeValueAsString(result),
                TTL
            )
            return result
        }

        // userId 가 없을 경우 -> 캐싱 없이 조건별 ES 조회
        val docs = when {
            userId != null -> orderSearchRepository.findAllByUserId(userId)
            skuId != null -> orderSearchRepository.findAllBySkuId(skuId)
            !correlationId.isNullOrBlank() -> orderSearchRepository.findAllByCorrelationId(correlationId)
            else -> orderSearchRepository.findAll().toList() // 조건 없으면 전체 조회
        }

        // ES Document -> API Reposonse DTO 변환
        return docs.map {
            OrderSearchResponse(
                orderId = it.orderId,
                skuId = it.skuId,
                quantity = it.quantity,
                userId = it.userId,
                status = it.status,
                correlationId = it.correlationId,
                createdAt = it.createdAt
            )
        }
    }
}