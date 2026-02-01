package com.mcl.mini_commerce_lab.order.messaging

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.mcl.mini_commerce_lab.search.order.OrderDocument
import com.mcl.mini_commerce_lab.search.order.OrderSearchRepository
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

/**
 * Kafka 의 order.created 토픽을 구독하는 Consumer
 * - RDB 에서 발생한 주문 생성 이벤트를 수신
 * - 검색/조회 용도로 Elasticsearch 에 색인
 */
@Component
class OrderCreatedConsumer(
    // Kafka 메시지(JSON)를 파싱하기 위한 Jackson ObjectMapper
    private val objectMapper: ObjectMapper,
    // es 에 OrderDocument 를 저장하기 위한 Repository
    private val orderSearchRepository: OrderSearchRepository,
) {
    private val log = LoggerFactory.getLogger(this::class.java)

    /**
     * Kafka Listener
     * - topics: 구독할 토픽 이름
     * - groupId: 컨슈머 그룹(같은 그룹 내에서는 메시지가 1번만 처리됨)
     * - 컨트롤러를 타지않고 백그라운드에서 독립적으로 실행
     */
    @KafkaListener(topics = ["order.created"], groupId = "mcl-consumer")
    fun consume(message: String){
        // message = OutboxPublisher 가 보낸 payload(JSON 문자열)
        // ex) {"orderId":9,"skuId":1,"quantity":1,"userId":1,"correlationId":"xxx"}
        val node: JsonNode = objectMapper.readTree(message)

        // 로그 추적을 위한 correllationId 추출
        val correlationId = node.path("correlationId").asText(null)

        // MDC(Thread Local)에 correlationId 저장
        MDC.put("correlationId", correlationId)

        try {
            // Kafka 메시지를 기반으로 ES에 저장할 검색용 Document 생성
            val doc = OrderDocument(
                orderId = node.path("orderId").asText(),    // ES 문서의 _id
                skuId = node.path("skuId").asLong(),
                quantity = node.path("quantity").asInt(),
                userId = node.path("userId").asLong(),
                status = "CREATED",
                correlationId = correlationId,
            )

            orderSearchRepository.save(doc)

            log.info("[Kafka -> ES] indexed orderId={}, skuId={}", doc.orderId, doc.skuId)
        } finally {
            MDC.clear()
        }
    }
}