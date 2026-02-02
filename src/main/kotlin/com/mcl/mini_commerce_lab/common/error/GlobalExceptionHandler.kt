package com.mcl.mini_commerce_lab.common.error

import jakarta.servlet.http.HttpServletRequest
import org.slf4j.MDC
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

// 모든 컨트롤러에서 발생하는 예외를 한 곳에서 공통으로 처리하는 전역 예외 처리 어노테이션
@RestControllerAdvice
class GlobalExceptionHandler {

    // 요청한 리소스가 존재하지 않을 때 예외(404)
    @ExceptionHandler(NotFoundException::class)
    fun handleNotFound(ex: NotFoundException, req: HttpServletRequest): ResponseEntity<ErrorResponse>{
        val status = HttpStatus.NOT_FOUND
        return ResponseEntity.status(status).body(
            ErrorResponse(
                status = status.value(),
                error = status.reasonPhrase,
                message = ex.message ?: "Not Found",
                path = req.requestURI,
                correlationId = MDC.get("correlationId")
            )
        )
    }

    // 요청값 검증에 실패했을 때 예외(400)
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(ex: MethodArgumentNotValidException, req: HttpServletRequest): ResponseEntity<ErrorResponse>{
        val status = HttpStatus.BAD_REQUEST
        val msg = ex.bindingResult.fieldErrors.joinToString(", ") { "${it.field}: ${it.defaultMessage}" }
        return ResponseEntity.status(status).body(
            ErrorResponse(
                status = status.value(),
                error = status.reasonPhrase,
                message = msg.ifBlank{ "Bad Request" },
                path = req.requestURI,
                correlationId = MDC.get("correlationId")
            )
        )
    }

    // 예기치 못한 예외(500)
    @ExceptionHandler(Exception::class)
    fun handleUnexcpected(ex: Exception, req: HttpServletRequest): ResponseEntity<ErrorResponse>{
        val status = HttpStatus.INTERNAL_SERVER_ERROR
        return ResponseEntity.status(status).body(
            ErrorResponse(
                status = status.value(),
                error = status.reasonPhrase,
                message = ex.message ?: "Unexpected error",
                path = req.requestURI,
                correlationId = MDC.get("correlationId")
            )
        )
    }

    /**
     * 주문 생성 중 "이미 처리 중" 같은 충돌 상황을 409로 응답하기 위한 핸들러
     *
     * ex) 같은 idempotencyKey 로 동시에 두번 호출되면,
     *     한쪽은 처리권(락)을 잡고 진행 중이고
     *     다른 쪽은 "processing" 상태를 보고 충돌로 판단할 수 있음
     */
    @ExceptionHandler(IllegalStateException::class)
    fun handleConflict(ex: IllegalStateException, req: HttpServletRequest): ResponseEntity<ErrorResponse> {
        val status = HttpStatus.CONFLICT
        return ResponseEntity.status(status).body(
            ErrorResponse(
                status = status.value(),
                error = status.reasonPhrase,
                message = ex.message ?: "Conflict",
                path = req.requestURI,
                correlationId = MDC.get("correlationId")
            )
        )
    }
}