package dev.maliik.relaycore.matchmaking.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dev.maliik.relaycore.common.error.InvalidMatchResultException;
import dev.maliik.relaycore.common.error.MatchAlreadyCompletedException;
import dev.maliik.relaycore.common.error.ResourceNotFoundException;
import dev.maliik.relaycore.common.event.MatchCompleted;
import dev.maliik.relaycore.common.event.Topics;
import dev.maliik.relaycore.common.outbox.OutboxAppender;
import dev.maliik.relaycore.matchmaking.domain.Match;
import dev.maliik.relaycore.matchmaking.domain.MatchParticipant;
import dev.maliik.relaycore.matchmaking.domain.MatchStatus;
import dev.maliik.relaycore.matchmaking.repository.MatchParticipantRepository;
import dev.maliik.relaycore.matchmaking.repository.MatchRepository;
import dev.maliik.relaycore.matchmaking.web.dto.MatchView;

@ExtendWith(MockitoExtension.class)
class MatchServiceTest {

    @Mock
    private MatchRepository matches;

    @Mock
    private MatchParticipantRepository participants;

    @Mock
    private OutboxAppender outboxAppender;

    private MatchService service;

    private final UUID winner = UUID.randomUUID();
    private final UUID opponent = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new MatchService(matches, participants, outboxAppender);
    }

    @Test
    void reportResultCompletesAnActiveMatchAndAppendsMatchCompletedEvent() {
        Match match = Match.active("na", 1);
        UUID matchId = match.getId();
        when(matches.findById(matchId)).thenReturn(Optional.of(match));
        when(participants.findByMatchId(matchId)).thenReturn(List.of(
                MatchParticipant.of(matchId, winner), MatchParticipant.of(matchId, opponent)));

        MatchView view = service.reportResult(matchId, winner);

        assertThat(view.status()).isEqualTo("COMPLETED");
        assertThat(view.winnerId()).isEqualTo(winner);
        assertThat(view.participantIds()).containsExactlyInAnyOrder(winner, opponent);
        assertThat(match.getStatus()).isEqualTo(MatchStatus.COMPLETED);

        ArgumentCaptor<Object> payload = ArgumentCaptor.forClass(Object.class);
        verify(outboxAppender).append(eq("match"), eq(matchId.toString()),
                eq(MatchCompleted.class.getSimpleName()), eq(Topics.MATCH_COMPLETED), payload.capture());
        assertThat(payload.getValue()).isInstanceOfSatisfying(MatchCompleted.class, event -> {
            assertThat(event.matchId()).isEqualTo(matchId);
            assertThat(event.winnerId()).isEqualTo(winner);
            assertThat(event.participantIds()).containsExactlyInAnyOrder(winner, opponent);
            assertThat(event.eventId()).isNotNull();
        });
    }

    @Test
    void reportResultRejectsAnUnknownMatch() {
        UUID missing = UUID.randomUUID();
        when(matches.findById(missing)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.reportResult(missing, winner))
                .isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(outboxAppender);
    }

    @Test
    void reportResultRejectsAnAlreadyCompletedMatch() {
        Match match = Match.active("na", 1);
        match.complete(winner, Instant.now());
        UUID matchId = match.getId();
        when(matches.findById(matchId)).thenReturn(Optional.of(match));

        assertThatThrownBy(() -> service.reportResult(matchId, winner))
                .isInstanceOf(MatchAlreadyCompletedException.class);
        verifyNoInteractions(outboxAppender);
    }

    @Test
    void reportResultRejectsAWinnerWhoDidNotPlay() {
        Match match = Match.active("na", 1);
        UUID matchId = match.getId();
        when(matches.findById(matchId)).thenReturn(Optional.of(match));
        when(participants.findByMatchId(matchId)).thenReturn(List.of(MatchParticipant.of(matchId, opponent)));

        assertThatThrownBy(() -> service.reportResult(matchId, winner))
                .isInstanceOf(InvalidMatchResultException.class);
        verifyNoInteractions(outboxAppender);
        assertThat(match.getStatus()).isEqualTo(MatchStatus.ACTIVE);
    }

    @Test
    void getReturnsTheMatchWithItsParticipants() {
        Match match = Match.active("eu", 3);
        UUID matchId = match.getId();
        when(matches.findById(matchId)).thenReturn(Optional.of(match));
        when(participants.findByMatchId(matchId)).thenReturn(List.of(MatchParticipant.of(matchId, winner)));

        MatchView view = service.get(matchId);

        assertThat(view.region()).isEqualTo("eu");
        assertThat(view.skillBucket()).isEqualTo(3);
        assertThat(view.participantIds()).containsExactly(winner);

        UUID missing = UUID.randomUUID();
        when(matches.findById(missing)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.get(missing)).isInstanceOf(ResourceNotFoundException.class);
    }
}
