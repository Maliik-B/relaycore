package dev.maliik.relaycore.common.idempotency;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * A processed mutating request, keyed by ({@code scope}, {@code idempotencyKey}). The stored response
 * body is replayed on retries so the side effect happens exactly once. Write-once — no updated_at.
 */
@Entity
@Table(name = "idempotency_keys")
@EntityListeners(AuditingEntityListener.class)
public class IdempotencyRecord {

    @Id
    private UUID id;

    @Column(nullable = false, length = 64)
    private String scope;

    @Column(name = "idempotency_key", nullable = false, length = 200)
    private String idempotencyKey;

    @Column(name = "request_hash", length = 64)
    private String requestHash;

    @Column(name = "response_body", nullable = false, length = 8000)
    private String responseBody;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected IdempotencyRecord() {
    }

    /**
     * Reserves the key with an empty body. Inserting this first in the transaction is what serializes
     * concurrent same-key requests: the winner holds the unique key while it does its work, and the
     * response body is filled in by {@link #complete(String)} before commit.
     */
    public static IdempotencyRecord reserve(String scope, String idempotencyKey, String requestHash) {
        IdempotencyRecord record = new IdempotencyRecord();
        record.id = UUID.randomUUID();
        record.scope = scope;
        record.idempotencyKey = idempotencyKey;
        record.requestHash = requestHash;
        record.responseBody = "";
        return record;
    }

    public void complete(String responseBody) {
        this.responseBody = responseBody;
    }

    public String getScope() {
        return scope;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public String getRequestHash() {
        return requestHash;
    }

    public String getResponseBody() {
        return responseBody;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
