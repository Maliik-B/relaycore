package dev.maliik.relaycore.players.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * A player's lifetime match statistics. Embedded into the {@code players} table. Starts at zero on
 * registration and is updated later by the event loop (a {@code match-completed} consumer).
 */
@Embeddable
public class PlayerStats {

    @Column(name = "matches_played", nullable = false)
    private int matchesPlayed;

    @Column(nullable = false)
    private int wins;

    @Column(nullable = false)
    private int losses;

    protected PlayerStats() {
    }

    public int getMatchesPlayed() {
        return matchesPlayed;
    }

    public int getWins() {
        return wins;
    }

    public int getLosses() {
        return losses;
    }

    /** Records the outcome of one completed match. */
    public void recordResult(boolean won) {
        matchesPlayed++;
        if (won) {
            wins++;
        } else {
            losses++;
        }
    }
}
