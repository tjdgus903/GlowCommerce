package com.mcl.mini_commerce_lab.product.repository

import com.mcl.mini_commerce_lab.product.domain.Product
import org.springframework.data.jpa.repository.JpaRepository

interface ProductRepository: JpaRepository<Product, Long>