package com.mcl.mini_commerce_lab.observability

import com.mcl.mini_commerce_lab.product.domain.OutboxStatus
import com.mcl.mini_commerce_lab.product.repository.OutboxRepository
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.concurrent.atomic.AtomicLong

@Component
class OutboxBacklogGauge(
    private val outboxRepository: OutboxRepository,
    registry: MeterRegistry,
) {
    private val newCount = AtomicLong(0)
    private val sentCount = AtomicLong(0)
    private val failedCount = AtomicLong(0)

    init {
        registry.gauge("mcl.outbox.backlog.new", newCount)
        registry.gauge("mcl.outbox.backlog.sent", sentCount)
        registry.gauge("mcl.outbox.backlog.failed", failedCount)
    }

    @Scheduled(fixedDelay = 2000)   // 2초마다
    fun refresh(){
        newCount.set(outboxRepository.countByStatus(OutboxStatus.NEW))
        sentCount.set(outboxRepository.countByStatus(OutboxStatus.SENT))
        failedCount.set(outboxRepository.countByStatus(OutboxStatus.FAILED))
    }
}