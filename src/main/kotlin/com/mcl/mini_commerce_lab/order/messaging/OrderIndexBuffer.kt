package com.mcl.mini_commerce_lab.order.messaging

import com.mcl.mini_commerce_lab.observability.MclMetrics
import com.mcl.mini_commerce_lab.search.order.OrderDocument
import com.mcl.mini_commerce_lab.search.order.OrderSearchRepository
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class OrderIndexBuffer(
    private val orderSearchRepository: OrderSearchRepository,
    private val metrics: MclMetrics,
) {
    private val queue = java.util.concurrent.LinkedBlockingQueue<OrderDocument>()

    fun offer(doc: OrderDocument){
        queue.offer(doc)
    }

    @Scheduled(fixedDelay = 1000)
    fun flush() {
        val batch = mutableListOf<OrderDocument>()
        queue.drainTo(batch, 200)
        if (batch.isEmpty()) return

        try {
            orderSearchRepository.saveAll(batch)
            metrics.consumerIndexedSuccess.increment(batch.size.toDouble())
        } catch (e: Exception) {
            metrics.consumerIndexedFailed.increment(batch.size.toDouble())
        }
    }
}