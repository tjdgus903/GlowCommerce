package com.mcl.mini_commerce_lab.common.filter

import jakarta.servlet.Filter
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.stereotype.Component
import java.util.*

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

    // 로그 추적을 위한 상수 정의 블록
    // 클래스에 딸린 Static 객체
    // 요청 최상단에서 한번만 생성해야됨
    companion object{
        const val CORRELATION_ID_HEADER = "X-Correlation-Id"
        const val MDC_KEY = "correlationId"
    }

    override fun doFilter(
        request: ServletRequest,
        response: ServletResponse,
        chain: FilterChain
    ) {
        val httpRequest = request as HttpServletRequest
        val httpResponse = response as HttpServletResponse

        val correlationId =
            httpRequest.getHeader(CORRELATION_ID_HEADER) ?: UUID.randomUUID().toString()

        // MDC 에 저장(Thread 단위)
        // 같은 스레드 안에서 로그 컨텍스트 공유
        MDC.put(MDC_KEY, correlationId)

        // 응답 헤더에도 내려주기
        httpResponse.setHeader(CORRELATION_ID_HEADER, correlationId)

        log.info(
            "[FILTER] start method={}, uri={}, correlationId={}",
            httpRequest.method,
            httpRequest.requestURI,
            correlationId
        )

        try{
            // 현재 Filter 이후 다음 Filter 혹은 컨트롤러로 요청을 전달하는 역할
            chain.doFilter(request, response)
        } finally{
            log.info(
                "[FILTER] end uri={}, correlationId={}",
                httpRequest.requestURI,
                correlationId
            )
            MDC.clear()
        }
    }
}