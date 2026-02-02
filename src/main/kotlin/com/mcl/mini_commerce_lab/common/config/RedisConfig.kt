package com.mcl.mini_commerce_lab.common.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.core.StringRedisTemplate

@Configuration
/**
 * Redis 설정 클래스
 *
 * - Spring Boot 가 자동으로 생성해주는 RedisConnectionFactory를 주입받아서
 * - StringRedisTemplate Bean 을 명시적으로 등록
 *
 * StringRedisTemplate 이란
 * - Redis에 데이터를 "문자열(String)" 형태로 저장/조회하기 위한 템플릿
 * - key / value 모두 String
 * - JSON 캐싱에 가장 많이 사용됨
 */
class RedisConfig {

    /**
     * StringRedisTemplate Bean 등록
     *
     * cf : RedisConnectionFactory
     * - application.yml 의 spring.data.redis 설정(host,port)을 기반으로
     *   Spring 이 자동 생성해주는 Redis 연결 객체
     *
     * 아래 Bean 이 있어야 Service 에서 StringRedisTemplate 을 주입받아 사용할 수 있음
     */
    @Bean
    fun stringRedisTemplate(cf: RedisConnectionFactory): StringRedisTemplate =
        StringRedisTemplate(cf)
}