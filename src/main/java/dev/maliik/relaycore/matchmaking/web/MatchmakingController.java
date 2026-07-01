package dev.maliik.relaycore.matchmaking.web;

import java.util.UUID;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.maliik.relaycore.common.security.AuthenticatedPlayer;
import dev.maliik.relaycore.matchmaking.service.MatchmakingService;
import dev.maliik.relaycore.matchmaking.web.dto.QueueRequest;
import dev.maliik.relaycore.matchmaking.web.dto.TicketView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/matchmaking")
@Tag(name = "Matchmaking", description = "Queue for a match; a background worker forms matches by region + skill bucket.")
@SecurityRequirement(name = "bearerAuth")
public class MatchmakingController {

    private final MatchmakingService matchmakingService;

    public MatchmakingController(MatchmakingService matchmakingService) {
        this.matchmakingService = matchmakingService;
    }

    @PostMapping("/queue")
    @Operation(summary = "Enqueue the authenticated player for matchmaking (region + skill bucket).")
    public TicketView queue(@AuthenticationPrincipal AuthenticatedPlayer principal,
            @Valid @RequestBody QueueRequest request) {
        return matchmakingService.enqueue(principal.id(), request.region(), request.skillBucket());
    }

    @GetMapping("/tickets/{ticketId}")
    @Operation(summary = "Fetch a matchmaking ticket; carries the formed match id once matched.")
    public TicketView get(@PathVariable UUID ticketId) {
        return matchmakingService.get(ticketId);
    }
}
