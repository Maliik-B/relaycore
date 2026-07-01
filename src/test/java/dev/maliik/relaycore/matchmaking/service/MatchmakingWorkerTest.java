package dev.maliik.relaycore.matchmaking.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dev.maliik.relaycore.matchmaking.domain.Match;
import dev.maliik.relaycore.matchmaking.domain.MatchParticipant;
import dev.maliik.relaycore.matchmaking.domain.Ticket;
import dev.maliik.relaycore.matchmaking.domain.TicketStatus;
import dev.maliik.relaycore.matchmaking.repository.MatchParticipantRepository;
import dev.maliik.relaycore.matchmaking.repository.MatchRepository;
import dev.maliik.relaycore.matchmaking.repository.TicketRepository;

@ExtendWith(MockitoExtension.class)
class MatchmakingWorkerTest {

    @Mock
    private TicketRepository tickets;

    @Mock
    private MatchRepository matches;

    @Mock
    private MatchParticipantRepository participants;

    private MatchmakingWorker worker;

    @BeforeEach
    void setUp() {
        worker = new MatchmakingWorker(tickets, matches, participants);
    }

    @Test
    void formsAMatchForTwoTicketsSharingRegionAndBucket() {
        Ticket a = ticket("na", 1);
        Ticket b = ticket("na", 1);
        whenQueued(a, b);
        echoSavedMatch();

        worker.formMatches();

        verify(matches, times(1)).save(any(Match.class));
        ArgumentCaptor<MatchParticipant> saved = ArgumentCaptor.forClass(MatchParticipant.class);
        verify(participants, times(2)).save(saved.capture());
        assertThat(saved.getAllValues()).extracting(MatchParticipant::getPlayerId)
                .containsExactlyInAnyOrder(a.getPlayerId(), b.getPlayerId());

        assertThat(a.getStatus()).isEqualTo(TicketStatus.MATCHED);
        assertThat(b.getStatus()).isEqualTo(TicketStatus.MATCHED);
        assertThat(a.getMatchId()).isNotNull().isEqualTo(b.getMatchId());
    }

    @Test
    void doesNotMatchTicketsInDifferentRegionsOrBuckets() {
        Ticket differentRegion = ticket("na", 1);
        Ticket alsoDifferentRegion = ticket("eu", 1);
        Ticket differentBucket = ticket("na", 2);
        whenQueued(differentRegion, alsoDifferentRegion, differentBucket);

        worker.formMatches();

        verify(matches, never()).save(any());
        verifyNoInteractions(participants);
        assertThat(differentRegion.getStatus()).isEqualTo(TicketStatus.QUEUED);
    }

    @Test
    void leavesAnOddTicketWaitingInTheQueue() {
        Ticket a = ticket("na", 1);
        Ticket b = ticket("na", 1);
        Ticket c = ticket("na", 1);
        whenQueued(a, b, c);
        echoSavedMatch();

        worker.formMatches();

        verify(matches, times(1)).save(any(Match.class));
        verify(participants, times(2)).save(any(MatchParticipant.class));
        long matched = Stream.of(a, b, c).filter(t -> t.getStatus() == TicketStatus.MATCHED).count();
        assertThat(matched).isEqualTo(2);
    }

    @Test
    void formsNothingWhenTheQueueIsEmpty() {
        whenQueued();

        worker.formMatches();

        verify(matches, never()).save(any());
        verifyNoInteractions(participants);
    }

    // --- helpers ---

    private void whenQueued(Ticket... queued) {
        when(tickets.findByStatusOrderByCreatedAtAsc(eq(TicketStatus.QUEUED), any())).thenReturn(List.of(queued));
    }

    private void echoSavedMatch() {
        when(matches.save(any(Match.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private Ticket ticket(String region, int skillBucket) {
        return Ticket.queue(UUID.randomUUID(), region, skillBucket);
    }
}
