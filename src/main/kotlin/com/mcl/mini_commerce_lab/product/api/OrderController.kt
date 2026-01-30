package com.mcl.mini_commerce_lab.product.api

import com.mcl.mini_commerce_lab.product.api.dto.CreateOrderRequest
import com.mcl.mini_commerce_lab.product.api.dto.CreatedOrderResponse
import com.mcl.mini_commerce_lab.product.service.OrderService
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class OrderController(
    private val orderService: OrderService
) {

    @PostMapping("/orders")
    fun createOrder(
        @RequestBody @Valid req: CreateOrderRequest
    ): CreatedOrderResponse{
        return orderService.createOrder(req)
    }
}