package com.mcl.mini_commerce_lab.common.config

import com.mcl.mini_commerce_lab.common.interceptor.RequestTimingInterceptor
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

/**
 * Interceptor 등록
 */

/**
 * 예외캐이스
 *
 * 없는 function 을 호출했을 때
 * [FILTER] request start - method=GET, uri=/members/1
 * [INTERCEPTOR] preHandle - uri=/members/1
 * [INTERCEPTOR] afterCompletion - uri=/members/1, duration=2ms
 * [FILTER] request end - uri=/members/1
 * [INTERCEPTOR] preHandle - uri=/error
 * [INTERCEPTOR] afterCompletion - uri=/error, duration=3ms
 *
 * 이렇게 Filter 가 종료된 후 Interceptor 가 호출이 되는데 순서를 보자면
 * 1. 클라이언트가 /members/1 요청
 * 2. 필터 start → 인터셉터 preHandle/afterCompletion → 필터 end
 * 3. 그런데 이 요청 결과가 404(핸들러 없음)로 끝남
 * 4. Spring Boot(정확히는 서블릿 컨테이너 + Spring의 에러 처리)가 에러 응답을 만들기 위해 /error로 한 번 더 디스패치(내부 이동)
 * 5. 그 /error 디스패치가 또 MVC 흐름을 타니까 인터셉터가 /error에 대해 다시 찍힘
 *    (Filter 가 안탄 이유는 error 는 제외하도록 조건 처리)
 *
 * 즉, /members/1 처리 끝난 뒤에 “다시 호출”된 게 아니라,
 * 에러 처리 경로(/error)가 별도로 한 번 더 실행된 것.
 */

@Configuration
class WebConfig(
    private val requestTimingInterceptor: RequestTimingInterceptor
) : WebMvcConfigurer {

    // 해당 요청이 Controller 에 갈지말지 설정
    override fun addInterceptors(registry: InterceptorRegistry){
        registry.addInterceptor(requestTimingInterceptor)
                .addPathPatterns("/**")
    }
}