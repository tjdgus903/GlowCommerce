package com.mcl.mini_commerce_lab.product.domain

import com.fasterxml.jackson.databind.JsonNode
import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.OffsetDateTime

@Entity
@Table(name = "outbox_events")
class OutboxEvent(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "outbox_id")
    val id: Long? = null,

    @Column(name = "aggregate_type", nullable = false, length = 50)
    val aggregateType: String,

    @Column(name = "aggregate_id", nullable = false, length = 80)
    val aggregateId: String,

    @Column(name = "event_type", nullable = false, length = 80)
    val eventType: String,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", nullable = false, columnDefinition = "jsonb")
    val payload: JsonNode,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    var status: OutboxStatus = OutboxStatus.NEW,

    @Column(name = "correlation_id", length = 80)
    val correlationId: String,

    @Column(name = "created_at", nullable = false)
    val createdAt: OffsetDateTime = OffsetDateTime.now(),

    @Column(name = "sent_at")
    var sentAt: OffsetDateTime? = null,
)

enum class OutboxStatus{
    NEW, SENT, FAILED
}