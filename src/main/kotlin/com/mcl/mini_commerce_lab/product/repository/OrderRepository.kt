package com.mcl.mini_commerce_lab.product.repository

import com.mcl.mini_commerce_lab.product.domain.Order
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface OrderRepository : JpaRepository<Order, Long> {
    fun findByIdempotencyKey(idempotencyKey: String): Optional<Order>
}