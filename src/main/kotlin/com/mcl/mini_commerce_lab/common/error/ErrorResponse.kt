package com.mcl.mini_commerce_lab.common.error

import java.time.OffsetDateTime

// 공통 에러 응답 DTO
data class ErrorResponse (
    val timestamp: OffsetDateTime = OffsetDateTime.now(),
    val status: Int,
    val error: String,
    val message: String,
    val path: String,
    val correlationId: String?,
)