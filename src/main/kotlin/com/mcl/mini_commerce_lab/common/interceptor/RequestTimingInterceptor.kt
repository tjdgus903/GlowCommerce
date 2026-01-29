package com.mcl.mini_commerce_lab.common.interceptor

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerInterceptor
import java.lang.Exception

/**
 * Interceptor 선언
 * MVC 단계로 해당 요청이 컨트롤러로 갈 수 있는지 없는지 판단
 *
 * 역할(요청 맥락)
 * 인증 여부 판단
 * 사용자 컨텍스트 주입
 * 권한 체크
 * 요청 처리 시간
 * Handler 단위 제어
 */

@Component
class RequestTimingInterceptor : HandlerInterceptor{

    private val log = LoggerFactory.getLogger(this::class.java)

    override fun preHandle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any
    ): Boolean {
        request.setAttribute("startTime", System.currentTimeMillis())

        log.info("[INTERCEPTOR] preHandle - uri={}",request.requestURI)
        return true
    }

    override fun afterCompletion(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any,
        ex: Exception?
    ) {
        val startTime = request.getAttribute("startTime") as Long
        val duration = System.currentTimeMillis() - startTime

        log.info(
            "[INTERCEPTOR] afterCompletion - uri={}, duration={}ms",
            request.requestURI,
            duration
        )
    }
}