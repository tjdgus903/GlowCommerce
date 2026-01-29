package com.mcl.mini_commerce_lab.product.repository

import com.mcl.mini_commerce_lab.product.domain.Sku
import org.springframework.data.jpa.repository.JpaRepository

interface SkuRepository: JpaRepository<Sku, Long>{
    fun findAllByProductId(productId: Long): List<Sku>
}