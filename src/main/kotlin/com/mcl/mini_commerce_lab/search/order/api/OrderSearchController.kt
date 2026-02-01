package com.mcl.mini_commerce_lab.search.order.api

import com.mcl.mini_commerce_lab.search.order.api.dto.OrderSearchResponse
import com.mcl.mini_commerce_lab.search.order.service.OrderSearchService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * 주문 검색 API
 * - RDB 가 아닌 Elasticsearch 를 조회
 * - CQRS 구조에서 Query 전용 Controller
 */
@RestController
class OrderSearchController(
    private val orderSearchService: OrderSearchService
) {

    @GetMapping("/search/orders")
    fun searchOrders(
        // 선택 조건들(null 허용)
        @RequestParam(required = false) userId: Long?,
        @RequestParam(required = false) skuId: Long?,
        @RequestParam(required = false) correlationId: String?,
    ): List<OrderSearchResponse> {
        // 조건에 따라 ES 조회
        return orderSearchService.search(userId, skuId, correlationId)
    }
}