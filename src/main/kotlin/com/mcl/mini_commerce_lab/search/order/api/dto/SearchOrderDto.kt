package com.mcl.mini_commerce_lab.search.order.api.dto

import java.time.OffsetDateTime

data class OrderSearchResponse(
    val orderId: String,
    val skuId: Long,
    val quantity: Int,
    val userId: Long,
    val status: String,
    val correlationId: String?,
    val createdAt: OffsetDateTime,
)