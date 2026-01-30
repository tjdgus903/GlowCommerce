package com.mcl.mini_commerce_lab.product.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.mcl.mini_commerce_lab.common.error.NotFoundException
import com.mcl.mini_commerce_lab.product.api.dto.CreateOrderRequest
import com.mcl.mini_commerce_lab.product.api.dto.CreatedOrderResponse
import com.mcl.mini_commerce_lab.product.domain.Order
import com.mcl.mini_commerce_lab.product.domain.OrderStatus
import com.mcl.mini_commerce_lab.product.domain.OutboxEvent
import com.mcl.mini_commerce_lab.product.repository.OrderRepository
import com.mcl.mini_commerce_lab.product.repository.OutboxRepository
import com.mcl.mini_commerce_lab.product.repository.SkuRepository
import org.slf4j.MDC
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class OrderService(
    private val orderRepository: OrderRepository,
    private val skuRepository: SkuRepository,
    private val outboxRepository: OutboxRepository,
    private val objectMapper: ObjectMapper,
    private val orderQueryService: OrderQueryService,
) {

    @Transactional
    fun createOrder(req: CreateOrderRequest): CreatedOrderResponse{
        val skuId = req.skuId ?: throw IllegalArgumentException("skuId is required")
        val quantity = req.quantity ?: throw IllegalArgumentException("quantity is required")

        val skuExists = skuRepository.existsById(skuId)
        if(!skuExists) throw NotFoundException("Sku not found. skuId=$skuId")

        val correlationId = MDC.get("correlationId")
        val userId = 1L

        // 1) 중복 데이터가 있으면 그대로 반환
        orderRepository.findByIdempotencyKey(req.idempotencyKey).orElse(null)?.let { existing ->
            return CreatedOrderResponse(
                orderId = existing.id!!,
                status = existing.status.name,
                correlationId = existing.correlationId,
            )
        }

        try{

            // 2) 신규 생성 시도(충돌이 나면 catch에서 새 트랜잭션 조회로 회수)
            val order = orderRepository.save(
                Order(
                    userId = userId,
                    skuId = skuId,
                    quantity = quantity,
                    status = OrderStatus.CREATED,
                    idempotencyKey = req.idempotencyKey,
                    correlationId = correlationId,
                )
            )

            // outbox 적재
            val payload = objectMapper.createObjectNode().apply {
                put("orderId", order.id!!)
                put("skuId", skuId)
                put("quantity", quantity)
                put("userId", userId)
                put("correlationId", correlationId)
                put("eventType", "ORDER_CREATED")
            }

            outboxRepository.save(
                OutboxEvent(
                    aggregateType = "ORDER",
                    aggregateId = order.id.toString(),
                    eventType = "ORDER_CREATED",
                    payload = payload,
                    correlationId = correlationId,
                )
            )

            return CreatedOrderResponse(
                orderId = order.id!!,
                status = order.status.name,
                correlationId = correlationId,
            )
        } catch (e: DataIntegrityViolationException){
            // 3) 충돌 회수(새 트랜잭션으로 조회하여 반환)
            val existing = orderQueryService.findByIdempotencyKeyOrNull(req.idempotencyKey)
                ?: throw e

            return CreatedOrderResponse(
                orderId = existing.id!!,
                status = existing.status.name,
                correlationId = existing.correlationId
            )
        }
    }
}