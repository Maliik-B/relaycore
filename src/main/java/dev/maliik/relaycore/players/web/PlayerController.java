package dev.maliik.relaycore.players.web;

import java.util.UUID;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.maliik.relaycore.common.security.AuthenticatedPlayer;
import dev.maliik.relaycore.players.service.PlayerService;
import dev.maliik.relaycore.players.web.dto.PlayerProfile;
import dev.maliik.relaycore.players.web.dto.UpdateProfileRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/players")
@Tag(name = "Players", description = "Player profiles and lifetime stats.")
@SecurityRequirement(name = "bearerAuth")
public class PlayerController {

    private final PlayerService playerService;

    public PlayerController(PlayerService playerService) {
        this.playerService = playerService;
    }

    @GetMapping("/me")
    @Operation(summary = "Profile of the authenticated player.")
    public PlayerProfile me(@AuthenticationPrincipal AuthenticatedPlayer principal) {
        return playerService.getProfile(principal.id());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Profile of any player by id.")
    public PlayerProfile byId(@PathVariable UUID id) {
        return playerService.getProfile(id);
    }

    @PatchMapping("/me")
    @Operation(summary = "Update the authenticated player's display name (evicts the cached profile).")
    public PlayerProfile updateMe(@AuthenticationPrincipal AuthenticatedPlayer principal,
            @Valid @RequestBody UpdateProfileRequest request) {
        return playerService.updateDisplayName(principal.id(), request.displayName());
    }
}
