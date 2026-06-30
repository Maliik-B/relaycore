package dev.maliik.relaycore.common.idempotency;

import java.util.Optional;
import java.util.function.Supplier;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import dev.maliik.relaycore.common.error.IdempotencyConflictException;

/**
 * Generic replay-safe execution of a mutating action, keyed by ({@code scope}, {@code key}).
 *
 * <p>The action and the persistence of the idempotency record run in <b>one transaction</b>, so a
 * failure rolls back both — a failed request leaves no record and can be safely retried. Concurrent
 * requests with the same key race to insert the record; the unique constraint lets exactly one win,
 * and the loser's transaction rolls back (undoing its side effect) and replays the winner's result.
 * A replay carrying a different request fingerprint is rejected as a key-reuse conflict.
 */
@Service
public class IdempotencyService {

    private final IdempotencyRecordRepository records;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate transactionTemplate;

    public IdempotencyService(IdempotencyRecordRepository records, ObjectMapper objectMapper,
            PlatformTransactionManager transactionManager) {
        this.records = records;
        this.objectMapper = objectMapper;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    public <T> IdempotentOutcome<T> process(String scope, String key, Class<T> type, String requestHash,
            Supplier<T> action) {
        Optional<IdempotencyRecord> existing = records.findByScopeAndIdempotencyKey(scope, key);
        if (existing.isPresent()) {
            return replay(existing.get(), type, requestHash);
        }
        try {
            T result = transactionTemplate.execute(status -> {
                // Reserve the key first: concurrent same-key requests block on this insert and roll back
                // before running the action, so the action executes at most once with no downstream
                // contention. The response body is filled in once the action succeeds.
                IdempotencyRecord record = records.saveAndFlush(IdempotencyRecord.reserve(scope, key, requestHash));
                T fresh = action.get();
                record.complete(serialize(fresh));
                return fresh;
            });
            return new IdempotentOutcome<>(result, false);
        } catch (DataIntegrityViolationException concurrentWinner) {
            IdempotencyRecord winner = records.findByScopeAndIdempotencyKey(scope, key)
                    .orElseThrow(() -> concurrentWinner);
            return replay(winner, type, requestHash);
        }
    }

    private <T> IdempotentOutcome<T> replay(IdempotencyRecord record, Class<T> type, String requestHash) {
        if (requestHash != null && !requestHash.equals(record.getRequestHash())) {
            throw new IdempotencyConflictException(record.getScope(), record.getIdempotencyKey());
        }
        return new IdempotentOutcome<>(deserialize(record.getResponseBody(), type), true);
    }

    private String serialize(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize idempotent response", ex);
        }
    }

    private <T> T deserialize(String body, Class<T> type) {
        try {
            return objectMapper.readValue(body, type);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to deserialize idempotent response", ex);
        }
    }
}
