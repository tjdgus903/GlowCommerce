package com.mcl.mini_commerce_lab.product.service

import com.mcl.mini_commerce_lab.product.domain.Order
import com.mcl.mini_commerce_lab.product.repository.OrderRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Service
class OrderQueryService(
    private val orderRepository: OrderRepository
) {
    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    fun findByIdempotencyKeyOrNull(key: String): Order? {
        return orderRepository.findByIdempotencyKey(key).orElse(null)
    }
}