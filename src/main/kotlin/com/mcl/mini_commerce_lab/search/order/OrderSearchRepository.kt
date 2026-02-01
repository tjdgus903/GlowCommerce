package com.mcl.mini_commerce_lab.search.order

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository

// JpaRepository 가 아닌 ElasticsearchRepository 인게 포인트
interface OrderSearchRepository : ElasticsearchRepository<OrderDocument, String>