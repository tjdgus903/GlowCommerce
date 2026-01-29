package com.mcl.mini_commerce_lab.product.domain

import jakarta.persistence.*
import java.time.OffsetDateTime

@Entity
@Table(name = "skus")
class Sku (

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sku_id")
    val id: Long? = null,

    // 다대일매핑
    // product 테이블의 product_id 값을 외래키로 가지고와서 사용
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    val product: Product,

    @Column(name = "stock", nullable = false)
    var stock: Int,

    @Version
    @Column(name = "version", nullable = false)
    var version: Long = 0,

    @Column(name = "created_at", nullable = false)
    var createdAt: OffsetDateTime = OffsetDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: OffsetDateTime = OffsetDateTime.now(),
)
