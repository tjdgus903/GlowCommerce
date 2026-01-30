package com.mcl.mini_commerce_lab.product.domain

import jakarta.persistence.*
import java.time.OffsetDateTime

@Entity
@Table(name = "orders")
class Order(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id")
    val id: Long? = null,

    @Column(name = "user_id", nullable = false)
    val userId: Long,

    @Column(name = "sku_id", nullable = false)
    val skuId: Long,

    @Column(name = "quantity", nullable = false)
    val quantity: Int,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    var status: OrderStatus,

    @Column(name = "idempotency_key", nullable = false, length = 80, unique = true)
    val idempotencyKey: String,

    @Column(name = "correlation_id", length = 80)
    val correlationId: String?,

    @Column(name = "created_at", nullable = false)
    val createdAt: OffsetDateTime = OffsetDateTime.now()
)

enum class OrderStatus{
    CREATED
}