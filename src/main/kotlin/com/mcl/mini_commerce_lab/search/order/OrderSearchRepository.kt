package com.mcl.mini_commerce_lab.search.order

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository

/**
 * Elasticsearch 전용 Repository(JpaRepository 가 아닌 ElasticsearchRepository)
 * - 메서드 이름 규칙으로 ES Query 를 자동 생성
 * - 실제로는 내부적으로 ES _search API 호출
 */
interface OrderSearchRepository : ElasticsearchRepository<OrderDocument, String>{
    fun findAllByUserId(userId: Long): List<OrderDocument>
    fun findAllBySkuId(skuId: Long): List<OrderDocument>
    fun findAllByCorrelationId(correlationId: String): List<OrderDocument>
}