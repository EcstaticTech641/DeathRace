package com.ronlab.deathrace;

import com.ronlab.deathrace.listener.DeathRaceEventListener;
import com.ronlab.rga.api.event.MinigameConcludeEvent;
import com.ronlab.rga.api.event.MinigameStartEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class DeathRaceEventListenerTest {

    @ParameterizedTest
    @ValueSource(strings = {"deathrace", "rga:deathrace", "death_race", "DEATHRACE", "RGA:DEATHRACE"})
    @DisplayName("Matching minigame IDs are recognized as DeathRace events")
    void testMatchingMinigameIds(String minigameId) {
        assertTrue(DeathRaceEventListener.isDeathRaceMinigame(minigameId));
    }

    @ParameterizedTest
    @ValueSource(strings = {"rga:sumo", "rga:turfwars", "rga:ultimate_tag", "sumo", "turfwars", "other_game"})
    @DisplayName("Non-matching minigame IDs are safely ignored")
    void testNonMatchingMinigameIds(String minigameId) {
        assertFalse(DeathRaceEventListener.isDeathRaceMinigame(minigameId));
    }

    @Test
    @DisplayName("Null or empty minigame IDs return false")
    void testNullOrEmptyMinigameIds() {
        assertFalse(DeathRaceEventListener.isDeathRaceMinigame(null));
        assertFalse(DeathRaceEventListener.isDeathRaceMinigame(""));
    }

    @Test
    @DisplayName("Non-matching MinigameStartEvent does not throw and ignores non-deathrace payload")
    void testNonMatchingMinigameStartEventIgnored() {
        DeathRaceEventListener listener = new DeathRaceEventListener(null);

        MinigameStartEvent sumoStart = new MinigameStartEvent(
                "rga:sumo", "Sumo", "sumo_world", List.of(UUID.randomUUID())
        );

        assertDoesNotThrow(() -> listener.onMinigameStart(sumoStart));
    }

    @Test
    @DisplayName("Non-matching MinigameConcludeEvent is safely ignored")
    void testNonMatchingMinigameConcludeEventIgnored() {
        DeathRaceEventListener listener = new DeathRaceEventListener(null);

        MinigameConcludeEvent turfConclude = new MinigameConcludeEvent(
                "rga:turfwars", "Turf Wars", "turf_world", List.of(UUID.randomUUID()), null
        );

        assertDoesNotThrow(() -> listener.onMinigameConclude(turfConclude));
    }
}
