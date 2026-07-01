package dev.maliik.relaycore.leaderboard.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

@ExtendWith(MockitoExtension.class)
class LeaderboardServiceTest {

    @Mock
    private StringRedisTemplate redis;

    @Mock
    private SetOperations<String, String> setOps;

    @Mock
    private ZSetOperations<String, String> zsetOps;

    private LeaderboardService service;

    private final UUID player = UUID.randomUUID();
    private final UUID eventId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new LeaderboardService(redis);
    }

    @Test
    void recordsAWinTheFirstTimeAnEventIsSeen() {
        when(redis.opsForSet()).thenReturn(setOps);
        when(setOps.add("leaderboard:processed:s1", eventId.toString())).thenReturn(1L);
        when(redis.opsForZSet()).thenReturn(zsetOps);

        service.recordWin("s1", player, eventId);

        verify(zsetOps).incrementScore("leaderboard:s1", player.toString(), 1.0);
    }

    @Test
    void ignoresARedeliveredEvent() {
        when(redis.opsForSet()).thenReturn(setOps);
        when(setOps.add(eq("leaderboard:processed:s1"), anyString())).thenReturn(0L);

        service.recordWin("s1", player, eventId);

        // Dedupe short-circuits before the (non-idempotent) increment.
        verify(redis, never()).opsForZSet();
    }

    @Test
    void rejectsAnOutOfRangeTopN() {
        assertThatThrownBy(() -> service.top("s1", 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.top("s1", 1000)).isInstanceOf(IllegalArgumentException.class);
    }
}
