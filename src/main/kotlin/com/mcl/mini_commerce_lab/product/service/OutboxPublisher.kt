package com.mcl.mini_commerce_lab.product.service

import com.mcl.mini_commerce_lab.observability.MclMetrics
import com.mcl.mini_commerce_lab.product.domain.OutboxStatus
import com.mcl.mini_commerce_lab.product.repository.OutboxRepository
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime

@Component
class OutboxPublisher(
    private val outboxRepository: OutboxRepository,
    private val kafkaTemplate: KafkaTemplate<String, String>,
    private val metrics: MclMetrics
) {
    private val log = LoggerFactory.getLogger(this::class.java)

    companion object{
        private const val TOPIC = "order.created"
    }

    @Scheduled(fixedDelay = 1000)   // 1초마다 NEW 이벤트 발행 시도
    @Transactional
    fun publishNewEvents(){
        // 1) Outbox 테이블에서 NEW 상태인 이벤트를 오래된(CreatedAt ASC) 순으로 가져오기
        val events = outboxRepository.findByStatusOrderByCreatedAtAsc(
            OutboxStatus.NEW,                          // outbox 테이블에서 status 가 NEW 인 데이터
            PageRequest.of(0, 50)   // 매번 DB 에서 오래된 데이터 최대 20 개를 끊어서 읽어오기
        )
        metrics.setOutboxBatchSize(events.size)

        // 2) 처리할 이벤트가 없으면 종료
        if (events.isEmpty()) return

        // 3) 이벤트 하나씩 kafka 로 발행 시도
        for (event in events) {
            // appregateId 를 통해 Kafka 파티션 분배 및 순서 보장
            val key = event.aggregateId
            // Kafka 에 보낼 메시지 본문(payload)
            val value = event.payload.toString()

            kafkaTemplate.send(TOPIC, key, value)
                .whenComplete{ result, ex ->
                if (ex == null) {
                    // 성공: 상태만 바꿔두면 트랜잭션 커밋 때 반영
                    event.status = OutboxStatus.SENT
                    event.sentAt = OffsetDateTime.now()
                    metrics.kafkaPublishSuccess.increment()
                } else {
                    event.status = OutboxStatus.FAILED
                    metrics.kafkaPublishFailed.increment()
                }
            }
        }
        // @Transactional 이라 상태 반영이 commit 시점에 반영됨
    }
}