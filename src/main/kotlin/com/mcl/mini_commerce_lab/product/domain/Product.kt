package com.mcl.mini_commerce_lab.product.domain

import jakarta.persistence.*
import java.time.OffsetDateTime

@Entity
@Table(name = "products")
class Product (

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // DB 에서 PK 값을 생성(시퀀스나 각종 값들로 사용 가능)
    @Column(name = "product_id")
    val id: Long? = null,

    @Column(name="name", nullable = false, length = 200)
    var name: String,

    @Column(name="created_at", nullable = false)
    val createdAt: OffsetDateTime = OffsetDateTime.now(),

    @Column(name="updated_at", nullable = false)
    var updatedAt: OffsetDateTime = OffsetDateTime.now(),
)