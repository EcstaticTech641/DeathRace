package com.ronlab.deathrace;

import com.ronlab.deathrace.prompt.DeathPrompt;
import com.ronlab.deathrace.session.DeathRaceSession;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class DeathRaceSessionTest {

    @Test
    @DisplayName("Session initializes players and prompt assignments correctly")
    void testSessionInitialization() {
        UUID player1 = UUID.randomUUID();
        UUID player2 = UUID.randomUUID();
        List<UUID> players = List.of(player1, player2);

        DeathRaceSession session = new DeathRaceSession("deathrace", "Death Race", "session_world_1", players);

        assertEquals("deathrace", session.getMinigameId());
        assertEquals("Death Race", session.getMinigameName());
        assertEquals("session_world_1", session.getWorldName());
        assertEquals(2, session.getInitialPlayers().size());
        assertEquals(0, session.getScore(player1));

        session.assignPrompt(player1, DeathPrompt.ANVIL);
        session.assignPrompt(player2, DeathPrompt.LAVA);

        assertEquals(DeathPrompt.ANVIL, session.getAssignedPrompt(player1));
        assertEquals(DeathPrompt.LAVA, session.getAssignedPrompt(player2));
        assertFalse(session.hasWinner());
    }

    @Test
    @DisplayName("First-to-3 point scoring and victory detection works correctly")
    void testFirstTo3PointScoring() {
        UUID player1 = UUID.randomUUID();
        UUID player2 = UUID.randomUUID();
        List<UUID> players = List.of(player1, player2);

        DeathRaceSession session = new DeathRaceSession("deathrace", "Death Race", "session_world_2", players);

        assertEquals(1, session.addPoint(player1));
        assertFalse(session.hasWinner());

        assertEquals(2, session.addPoint(player1));
        assertFalse(session.hasWinner());

        assertEquals(3, session.addPoint(player1));
        assertTrue(session.hasWinner());
        assertEquals(player1, session.getWinnerUuid());

        Map<UUID, Number> scores = session.getScores();
        assertEquals(3, scores.get(player1));
        assertEquals(0, scores.get(player2));
    }
}
