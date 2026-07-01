package dev.maliik.relaycore.leaderboard.web;

import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import dev.maliik.relaycore.leaderboard.service.LeaderboardService;
import dev.maliik.relaycore.leaderboard.web.dto.LeaderboardEntry;
import dev.maliik.relaycore.leaderboard.web.dto.PlayerRankView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/leaderboards")
@Tag(name = "Leaderboards", description = "Season rankings backed by a Redis sorted set, fed by the match event stream.")
@SecurityRequirement(name = "bearerAuth")
public class LeaderboardController {

    private final LeaderboardService leaderboardService;

    public LeaderboardController(LeaderboardService leaderboardService) {
        this.leaderboardService = leaderboardService;
    }

    @GetMapping("/{season}/top")
    @Operation(summary = "Top n players in a season, highest score first.")
    public List<LeaderboardEntry> top(@PathVariable String season,
            @RequestParam(defaultValue = "10") int n) {
        return leaderboardService.top(season, n);
    }

    @GetMapping("/{season}/players/{playerId}/rank")
    @Operation(summary = "A player's rank and score in a season.")
    public PlayerRankView rank(@PathVariable String season, @PathVariable UUID playerId) {
        return leaderboardService.rank(season, playerId);
    }
}
