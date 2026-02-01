package com.mcl.mini_commerce_lab.search.order

import org.springframework.data.annotation.Id
import org.springframework.data.elasticsearch.annotations.Document
import java.time.OffsetDateTime

/**
 * RDB 와 별개로 검색/조회 최적화를 위해 ES(ElasticSearch)에 저장하는 검색용 모델 생성
 * CQRS(ex : 쓰기 DB(RDB) + 조회/검색(ES) 분리 패턴)에서 Read Model 역할
 * 검색 최적화를 위해 필요한 필드만 보유
 * - JPA Entity 가 아니며 트랜잭션 개념 없음
 */
@Document(indexName = "orders") // ES index 이름
data class OrderDocument(

    /**
     * Elasticsearch 문서의 고유 ID(_id)
     * - 보통 RDB 의 orderId 를 그대로 사용
     * - 동일 orderId면 덮어쓰기(upsert) 됨
     */
    @Id
    val orderId: String,

    val skuId: Long,
    val quantity: Int,
    val userId: Long,
    val status: String,
    val correlationId: String?,
    val createdAt: OffsetDateTime = OffsetDateTime.now(),
)