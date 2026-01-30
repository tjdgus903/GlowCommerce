package com.mcl.mini_commerce_lab.product.api.dto

import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

// 주문 생성 시 보내는 요청 DTO
data class CreateOrderRequest (
    // @field 를 붙혀야 코틀린 data class 필드에 Validation 적용됨
    @field:NotNull
    val skuId: Long?,

    @field:NotNull
    @field:Min(1)
    val quantity: Int?,

    // 멱등키 : 같은 요청을 여러번 보내도 한번만 생성되는 키
    @field:NotBlank
    val idempotencyKey: String
)

// 주문 생성 응답 DTO
data class CreatedOrderResponse(
    val orderId: Long,
    val status: String,
    val correlationId: String?,
)