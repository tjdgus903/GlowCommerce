package com.mcl.mini_commerce_lab.product.api.dto

// 어디서든 import 하여 사용가능한 접근제어자
data class ProductDetailResponse(
    val productId: Long,
    val name: String,
    val skus: List<SkuResponse>,
)

data class SkuResponse(
    val skuId: Long,
    val stock: Int,
)