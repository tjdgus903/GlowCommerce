package com.mcl.mini_commerce_lab

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

// @EnableScheduling 는 @Scheduled 로 선언한 메서드들을 실제로 동작하게 만드는 활성화 어노테이션
@EnableScheduling
@SpringBootApplication
class MiniCommerceLabApplication

fun main(args: Array<String>) {
    runApplication<MiniCommerceLabApplication>(*args)
}