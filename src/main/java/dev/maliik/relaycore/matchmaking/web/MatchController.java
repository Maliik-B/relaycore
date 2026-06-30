package dev.maliik.relaycore.matchmaking.web;

import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.maliik.relaycore.matchmaking.service.MatchService;
import dev.maliik.relaycore.matchmaking.web.dto.MatchResultRequest;
import dev.maliik.relaycore.matchmaking.web.dto.MatchView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/matches")
@Tag(name = "Matches", description = "Match results — the event source for the reward loop.")
@SecurityRequirement(name = "bearerAuth")
public class MatchController {

    private final MatchService matchService;

    public MatchController(MatchService matchService) {
        this.matchService = matchService;
    }

    @PostMapping("/{matchId}/result")
    @Operation(summary = "Record a completed match result; emits match-completed via the transactional outbox.")
    public MatchView reportResult(@PathVariable UUID matchId, @Valid @RequestBody MatchResultRequest request) {
        return matchService.reportResult(matchId, request.winnerId());
    }

    @GetMapping("/{matchId}")
    @Operation(summary = "Fetch a recorded match.")
    public MatchView get(@PathVariable UUID matchId) {
        return matchService.get(matchId);
    }
}
