package dev.maliik.relaycore.matchmaking.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dev.maliik.relaycore.common.error.DuplicateResourceException;
import dev.maliik.relaycore.common.error.ResourceNotFoundException;
import dev.maliik.relaycore.common.event.MatchCompleted;
import dev.maliik.relaycore.common.event.Topics;
import dev.maliik.relaycore.common.outbox.OutboxAppender;
import dev.maliik.relaycore.matchmaking.domain.Match;
import dev.maliik.relaycore.matchmaking.repository.MatchRepository;
import dev.maliik.relaycore.matchmaking.web.dto.MatchView;

@ExtendWith(MockitoExtension.class)
class MatchServiceTest {

    @Mock
    private MatchRepository matches;

    @Mock
    private OutboxAppender outboxAppender;

    private MatchService service;

    private final UUID matchId = UUID.randomUUID();
    private final UUID winner = UUID.randomUUID();

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        service = new MatchService(matches, outboxAppender);
    }

    @Test
    void reportResultPersistsCompletedMatchAndAppendsMatchCompletedEvent() {
        when(matches.existsById(matchId)).thenReturn(false);
        when(matches.save(any(Match.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MatchView view = service.reportResult(matchId, winner);

        assertThat(view.id()).isEqualTo(matchId);
        assertThat(view.status()).isEqualTo("COMPLETED");
        assertThat(view.winnerId()).isEqualTo(winner);
        assertThat(view.completedAt()).isNotNull();

        ArgumentCaptor<Object> payload = ArgumentCaptor.forClass(Object.class);
        verify(outboxAppender).append(eq("match"), eq(matchId.toString()),
                eq(MatchCompleted.class.getSimpleName()), eq(Topics.MATCH_COMPLETED), payload.capture());
        assertThat(payload.getValue()).isInstanceOfSatisfying(MatchCompleted.class, event -> {
            assertThat(event.matchId()).isEqualTo(matchId);
            assertThat(event.winnerId()).isEqualTo(winner);
            assertThat(event.eventId()).isNotNull();
        });
    }

    @Test
    void reportResultRejectsAnAlreadyRecordedMatch() {
        when(matches.existsById(matchId)).thenReturn(true);

        assertThatThrownBy(() -> service.reportResult(matchId, winner))
                .isInstanceOf(DuplicateResourceException.class);

        verify(matches, never()).save(any());
        verifyNoInteractions(outboxAppender);
    }

    @Test
    void getReturnsTheMatchWhenPresentAndThrowsWhenMissing() {
        when(matches.findById(matchId)).thenReturn(Optional.of(Match.completed(matchId, winner, null)));
        assertThat(service.get(matchId).winnerId()).isEqualTo(winner);

        UUID missing = UUID.randomUUID();
        when(matches.findById(missing)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.get(missing)).isInstanceOf(ResourceNotFoundException.class);
    }
}
