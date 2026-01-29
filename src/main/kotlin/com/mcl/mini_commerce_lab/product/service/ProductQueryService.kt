package com.mcl.mini_commerce_lab.product.service

import com.mcl.mini_commerce_lab.product.api.dto.ProductDetailResponse
import com.mcl.mini_commerce_lab.product.api.dto.SkuResponse
import com.mcl.mini_commerce_lab.product.repository.ProductRepository
import com.mcl.mini_commerce_lab.product.repository.SkuRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ProductQueryService (
    private val productRepository: ProductRepository,
    private val skuRepository: SkuRepository,
){

    @Transactional(readOnly = true)
    fun getProductDetail(productId: Long): ProductDetailResponse{
        val product = productRepository.findById(productId)
            .orElseThrow{ NoSuchElementException("Product not found. productId=$productId") }

        val skus = skuRepository.findAllByProductId(productId)

        return ProductDetailResponse(
            productId = product.id!!,
            name = product.name,
            skus = skus.map { SkuResponse(skuId = it.id!!, stock = it.stock) }
        )
    }
}