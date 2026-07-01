package dev.maliik.relaycore.matchmaking.web.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Enqueue request: the region and skill bucket to be matched within. The player is taken from the
 * authenticated principal, not the body — you queue yourself.
 */
public record QueueRequest(
        @NotBlank @Size(max = 32) String region,
        @Min(0) @Max(9) int skillBucket) {
}
