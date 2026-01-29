package com.mcl.mini_commerce_lab.common.filter

import jakarta.servlet.Filter
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/**
 * Filter 는 DispacherServlet 보다 먼저 수행
 * MVC 이전 단계로 컨트롤러가 있든 없든 무조건 수행(Spring 밖)
 *
 * 역할(기술 레벨)
 * Request/Response 로깅
 * 인증 토큰 파싱
 * Charset / CORS / 압축
 * MDC 세팅
 */

@Component
class RequestLoggingFilter : Filter {

    private val log = LoggerFactory.getLogger(this::class.java)

    override fun doFilter(
        request: ServletRequest,
        response: ServletResponse,
        chain: FilterChain
    ) {
        val httpRequest = request as HttpServletRequest

        log.info("[FILTER] request start - method={}, uri={}",
            httpRequest.method,
            httpRequest.requestURI,
        )

        // 현재 Filter 이후 다음 Filter 혹은 컨트롤러로 요청을 전달하는 역할
        chain.doFilter(request, response)

        log.info("[FILTER] request end - uri={}",
            httpRequest.requestURI
        )
    }
}