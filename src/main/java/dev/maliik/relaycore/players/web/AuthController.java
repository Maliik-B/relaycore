package dev.maliik.relaycore.players.web;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import dev.maliik.relaycore.players.service.PlayerService;
import dev.maliik.relaycore.players.web.dto.AuthResponse;
import dev.maliik.relaycore.players.web.dto.LoginRequest;
import dev.maliik.relaycore.players.web.dto.RegisterRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/auth")
@Tag(name = "Auth", description = "Registration and login — issues JWT access tokens.")
public class AuthController {

    private final PlayerService playerService;

    public AuthController(PlayerService playerService) {
        this.playerService = playerService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Register a new player and issue an access token.")
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return AuthResponse.from(
                playerService.register(request.username(), request.email(), request.password()));
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate with username and password and issue an access token.")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return AuthResponse.from(
                playerService.login(request.username(), request.password()));
    }
}
