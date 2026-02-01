package com.mcl.mini_commerce_lab.search.order.service

import com.mcl.mini_commerce_lab.search.order.OrderSearchRepository
import com.mcl.mini_commerce_lab.search.order.api.dto.OrderSearchResponse
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 주문 검색 서비스
 * - Elasticsearch 조회 전용
 * - CQRS 에서 Query Model 담당
 */
@Service
class OrderSearchService(
    private val orderSearchRepository: OrderSearchRepository,
) {

    @Transactional(readOnly = true)
    fun search(userId: Long?, skuId: Long?, correlationId: String?): List<OrderSearchResponse>{
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