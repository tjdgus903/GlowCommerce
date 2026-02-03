package com.mcl.mini_commerce_lab.observability

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.stereotype.Component
import java.util.concurrent.atomic.AtomicInteger

@Component
class MclMetrics(
    registry: MeterRegistry
) {
    // Orders
    val ordersCreateAttempt: Counter = Counter.builder("mcl.orders.create.attempt").register(registry)
    val ordersCreateSuccess: Counter = Counter.builder("mcl.orders.create.success").register(registry)
    val ordersCreateRecovered: Counter = Counter.builder("mcl.orders.create.recovered").register(registry) // 중복 회수
    val ordersCreateFailed: Counter = Counter.builder("mcl.orders.create.failed").register(registry)

    // Cache
    val cacheSearchHit: Counter = Counter.builder("mcl.cache.search.hit").register(registry)
    val cacheSearchMiss: Counter = Counter.builder("mcl.cache.search.miss").register(registry)

    // Kafka publish
    val kafkaPublishSuccess: Counter = Counter.builder("mcl.kafka.publish.success").register(registry)
    val kafkaPublishFailed: Counter = Counter.builder("mcl.kafka.publish.failed").register(registry)

    // Consumer(ES indexing)
    val consumerIndexedSuccess: Counter = Counter.builder("mcl.es.index.success").register(registry)
    val consumerIndexedFailed: Counter = Counter.builder("mcl.es.index.failed").register(registry)

    // Optional: 간단 상태(현재 처리 중 outbox batch 크기 등)
    private val outboxLastBatchSize = AtomicInteger(0)
    init {
        registry.gauge("mcl.outbox.batch.size", outboxLastBatchSize)
    }
    fun setOutboxBatchSize(n: Int) = outboxLastBatchSize.set(n)
}