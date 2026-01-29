package com.mcl.mini_commerce_lab.product.api

import com.mcl.mini_commerce_lab.product.api.dto.ProductDetailResponse
import com.mcl.mini_commerce_lab.product.service.ProductQueryService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController

@RestController
class ProductController(
    private val productQueryService: ProductQueryService,
) {

    @GetMapping("/products/{productId}")
    fun getProductDetail(@PathVariable productId: Long): ProductDetailResponse{
        return productQueryService.getProductDetail(productId)
    }
}