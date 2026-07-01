package dev.maliik.relaycore.leaderboard.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations.TypedTuple;
import org.springframework.stereotype.Service;

import dev.maliik.relaycore.common.error.ResourceNotFoundException;
import dev.maliik.relaycore.leaderboard.web.dto.LeaderboardEntry;
import dev.maliik.relaycore.leaderboard.web.dto.PlayerRankView;

/**
 * Season rankings backed by a Redis sorted set (ZSET), one per season: member = player id, score = wins.
 * The ZSET gives O(log n) top-N ({@code ZREVRANGE}), player rank ({@code ZREVRANK}) and neighbour queries —
 * Redis used beyond caching.
 *
 * <p>{@code ZINCRBY} is not idempotent, so {@link #recordWin} first records the source event id in a
 * per-season set ({@code SADD}) and only increments when that add is new. A Kafka redelivery of the same
 * match event therefore ranks the winner exactly once — the same effectively-once theme as the inventory
 * grant, applied to a non-idempotent operation.
 */
@Service
public class LeaderboardService {

    /** M4 runs a single fixed season; date-derived seasons are future work. */
    public static final String DEFAULT_SEASON = "s1";

    private static final int MAX_TOP_N = 100;

    private final StringRedisTemplate redis;

    public LeaderboardService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    /** Credits the winner one point for {@code eventId}, skipping the increment if the event was already seen. */
    public void recordWin(String season, UUID playerId, UUID eventId) {
        Long added = redis.opsForSet().add(processedKey(season), eventId.toString());
        if (added == null || added == 0L) {
            return;
        }
        redis.opsForZSet().incrementScore(rankingKey(season), playerId.toString(), 1.0);
    }

    /** Top {@code n} players by score, highest first, each with its 1-based rank. */
    public List<LeaderboardEntry> top(String season, int n) {
        if (n < 1 || n > MAX_TOP_N) {
            throw new IllegalArgumentException("n must be between 1 and " + MAX_TOP_N);
        }
        Set<TypedTuple<String>> tuples = redis.opsForZSet()
                .reverseRangeWithScores(rankingKey(season), 0, n - 1L);
        List<LeaderboardEntry> entries = new ArrayList<>();
        long rank = 1;
        if (tuples != null) {
            for (TypedTuple<String> tuple : tuples) {
                entries.add(new LeaderboardEntry(rank++, UUID.fromString(tuple.getValue()), scoreOf(tuple.getScore())));
            }
        }
        return entries;
    }

    /** A single player's 1-based rank and score, or 404 if they are not on the board yet. */
    public PlayerRankView rank(String season, UUID playerId) {
        String member = playerId.toString();
        Long rank = redis.opsForZSet().reverseRank(rankingKey(season), member);
        if (rank == null) {
            throw new ResourceNotFoundException("leaderboard entry", playerId + " in season " + season);
        }
        Double score = redis.opsForZSet().score(rankingKey(season), member);
        return new PlayerRankView(playerId, rank + 1, scoreOf(score));
    }

    private long scoreOf(Double score) {
        return score == null ? 0L : score.longValue();
    }

    private String rankingKey(String season) {
        return "leaderboard:" + season;
    }

    private String processedKey(String season) {
        return "leaderboard:processed:" + season;
    }
}
