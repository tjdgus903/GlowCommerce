package com.mcl.mini_commerce_lab.product.repository

import com.mcl.mini_commerce_lab.product.domain.OutboxEvent
import com.mcl.mini_commerce_lab.product.domain.OutboxStatus
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface OutboxRepository : JpaRepository<OutboxEvent, Long> {
    fun findByStatusOrderByCreatedAtAsc(status: OutboxStatus, pageable: Pageable): List<OutboxEvent>
}