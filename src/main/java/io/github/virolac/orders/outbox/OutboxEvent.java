package io.github.virolac.orders.outbox;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Entity
public class OutboxEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    @Column(nullable = false, length = 255)
    private String exchange;

    @Column(nullable = false, length = 255)
    private String routingKey;

    @Column(nullable = false, length = 255)
    private String eventType;

    @Column(nullable = false)
    @JdbcTypeCode(SqlTypes.JSON)
    private String payload;

    @Column(nullable = false, updatable = false)
    @CreationTimestamp
    private Instant createdAt;

    private Instant publishedAt;

    protected OutboxEvent() {
    }

    public OutboxEvent(String exchange, String routingKey, String eventType, String payload) {
        this.exchange = exchange;
        this.routingKey = routingKey;
        this.eventType = eventType;
        this.payload = payload;
    }

    public Long getId() {
        return id;
    }

    public String getExchange() {
        return exchange;
    }

    public String getRoutingKey() {
        return routingKey;
    }

    public String getEventType() {
        return eventType;
    }

    public String getPayload() {
        return payload;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }

    public void markPublished() {
        this.publishedAt = Instant.now();
    }
}
